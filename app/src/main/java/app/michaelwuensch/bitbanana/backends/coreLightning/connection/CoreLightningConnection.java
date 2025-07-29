package app.michaelwuensch.bitbanana.backends.coreLightning.connection;


import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.InterceptingSSLSocketFactory;
import app.michaelwuensch.bitbanana.backends.coreLightning.services.CoreLightningNodeService;
import app.michaelwuensch.bitbanana.backends.coreLightning.services.RemoteCoreLightningNodeService;
import app.michaelwuensch.bitbanana.connection.BlindHostnameVerifier;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.connection.tor.TorProxyDetector;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

/**
 * Singleton to handle the connection to CoreLightning
 */
public class CoreLightningConnection {

    private static final String LOG_TAG = CoreLightningConnection.class.getSimpleName();

    private static CoreLightningConnection mCoreLightningConnectionInstance;

    private ManagedChannel mSecureChannel;
    private CoreLightningNodeService mCoreLightningNodeService;

    private boolean isConnected = false;
    private boolean isConnectionProcess = false;

    private CoreLightningConnection() {
    }

    public static synchronized CoreLightningConnection getInstance() {
        if (mCoreLightningConnectionInstance == null) {
            mCoreLightningConnectionInstance = new CoreLightningConnection();
        }
        return mCoreLightningConnectionInstance;
    }

    public CoreLightningNodeService getCoreLightningNodeService() {
        return mCoreLightningNodeService;
    }


    private void generateChannelAndStubs() {
        if (mSecureChannel != null && !mSecureChannel.isShutdown()) {
            BBLog.d(LOG_TAG, "Closing old gRPC channel...");
            mSecureChannel.shutdownNow();
        }

        BBLog.d(LOG_TAG, "Generating channels and stubs.");
        String host = BackendManager.getCurrentBackendConfig().getHostWithOverride();
        int port = BackendManager.getCurrentBackendConfig().getPort();

        HostnameVerifier hostnameVerifier = null;
        if (!BackendManager.getCurrentBackendConfig().getVerifyCertificate() || BackendManager.getCurrentBackendConfig().isTorHostAddress()) {
            // On Tor we do not need it, as tor already makes sure we are connected with the correct host.
            hostnameVerifier = new BlindHostnameVerifier();
        }

        try {

            SSLSocketFactory baseFactory = CoreLightningSSLSocketFactory.create(BackendManager.getCurrentBackendConfig());
            SSLSocketFactory inspectingFactory = new InterceptingSSLSocketFactory(baseFactory);

            // Channels are expensive to create. We want to create it once and then reuse it on all our requests.
            if (BackendManager.getCurrentBackendConfig().getUseTor()) {
                mSecureChannel = OkHttpChannelBuilder
                        .forAddress(host, port)
                        .proxyDetector(new TorProxyDetector(TorManager.getInstance().getHttpProxyPort()))
                        .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                        .sslSocketFactory(inspectingFactory) // null = default SSLSocketFactory
                        .maxInboundMessageSize(RefConstants.MAX_GRPC_MESSAGE_SIZE)
                        .build();
            } else {
                mSecureChannel = OkHttpChannelBuilder
                        .forAddress(host, port)
                        .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                        .sslSocketFactory(inspectingFactory) // null = default SSLSocketFactory
                        .overrideAuthority("cln") // the grpc plugin for core lightning does not know the domain, therefore it uses 'cln' by default. See https://docs.corelightning.org/docs/grpc.
                        .maxInboundMessageSize(RefConstants.MAX_GRPC_MESSAGE_SIZE)
                        .build();
            }

            // Call credentials are not needed/supported right now. Couldn't find anything about runes with gRPC so far. We just pass null for now.
            mCoreLightningNodeService = new RemoteCoreLightningNodeService(mSecureChannel, null);

            monitorChannelState(mSecureChannel);
            mSecureChannel.getState(true); // This initiates the connection without a request
        } catch (Exception e) {
            BackendManager.setError(BackendManager.ERROR_GRPC_CREATING_STUBS);
        }
    }

    private void monitorChannelState(ManagedChannel channel) {
        ConnectivityState state = channel.getState(false);

        channel.notifyWhenStateChanged(state, () -> {
            ConnectivityState newState = channel.getState(false);

            BBLog.d(LOG_TAG, "GRPC channel state changed from " + state + " to " + newState);

            if (newState == ConnectivityState.READY && isConnectionProcess) { // We check for isConnectionProcess here as we don't want to continue the connection procedure and wallet loading if it was just a gRPC internal reconnection.
                isConnectionProcess = false;
                new Handler(Looper.getMainLooper()).post(() -> {
                    BackendManager.activateBackendConfig5();
                });
            }

            if (newState == ConnectivityState.TRANSIENT_FAILURE && isConnectionProcess) {
                BBLog.w(LOG_TAG, "GRPC channel failed! We still continue so that actual gRPC requests are emitted and gRPC tries to reconnect with its internal logic. Let's hope it works...");
                isConnectionProcess = false;
                mSecureChannel.resetConnectBackoff();
                new Handler(Looper.getMainLooper()).post(() -> {
                    BackendManager.activateBackendConfig5();
                });
            }

            if (state == ConnectivityState.SHUTDOWN) {
                return;
            }

            // Continue monitoring recursively
            monitorChannelState(channel);
        });
    }

    public void openConnection() {
        if (!isConnected) {
            isConnected = true;
            this.isConnectionProcess = true;
            generateChannelAndStubs();
        }
    }

    public void closeConnection() {
        if (mSecureChannel != null) {
            BBLog.d(LOG_TAG, "Shutting down CoreLightning connection...");
            shutdownChannel();
        }
        isConnected = false;
    }

    public void restartConnection() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            BBLog.d(LOG_TAG, "Restarting CoreLightning connection.");
            closeConnection();
            openConnection();
        }
    }

    /**
     * Will shutdown the channel and cancel all active calls.
     * Waits for shutdown (blocking) and logs result.
     */
    private void shutdownChannel() {
        try {
            if (mSecureChannel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS)) {
                BBLog.d(LOG_TAG, "CoreLightning channel shutdown successfully...");
            } else {
                BBLog.e(LOG_TAG, "CoreLightning channel shutdown failed...");
            }
        } catch (InterruptedException e) {
            BBLog.e(LOG_TAG, "CoreLightning channel shutdown exception: " + e.getMessage());
        }
    }
}
