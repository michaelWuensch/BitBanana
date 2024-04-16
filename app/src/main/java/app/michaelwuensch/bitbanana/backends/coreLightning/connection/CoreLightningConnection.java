package app.michaelwuensch.bitbanana.backends.coreLightning.connection;


import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.coreLightning.services.CoreLightningNodeService;
import app.michaelwuensch.bitbanana.backends.coreLightning.services.RemoteCoreLightningNodeService;
import app.michaelwuensch.bitbanana.connection.BlindHostnameVerifier;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.connection.tor.TorProxyDetector;
import app.michaelwuensch.bitbanana.util.BBLog;
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
        String host = BackendManager.getCurrentBackendConfig().getHost();
        int port = BackendManager.getCurrentBackendConfig().getPort();

        HostnameVerifier hostnameVerifier = null;
        if (!BackendManager.getCurrentBackendConfig().getVerifyCertificate() || BackendManager.getCurrentBackendConfig().isTorHostAddress()) {
            // On Tor we do not need it, as tor already makes sure we are connected with the correct host.
            hostnameVerifier = new BlindHostnameVerifier();
        }

        try {

            // Channels are expensive to create. We want to create it once and then reuse it on all our requests.
            if (BackendManager.getCurrentBackendConfig().getUseTor()) {
                mSecureChannel = OkHttpChannelBuilder
                        .forAddress(host, port)
                        .proxyDetector(new TorProxyDetector(TorManager.getInstance().getHttpProxyPort()))
                        .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                        .sslSocketFactory(CoreLightningSSLSocketFactory.create(BackendManager.getCurrentBackendConfig())) // null = default SSLSocketFactory
                        .build();
            } else {
                mSecureChannel = OkHttpChannelBuilder
                        .forAddress(host, port)
                        .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                        .sslSocketFactory(CoreLightningSSLSocketFactory.create(BackendManager.getCurrentBackendConfig())) // null = default SSLSocketFactory
                        .overrideAuthority("cln") // the grpc plugin for core lightning does not know the domain, therefore it uses 'cln' by default. See https://docs.corelightning.org/docs/grpc.
                        .build();
            }

            // Call credentials are not needed/supported right now. Couldn't find anything about runes with gRPC so far. We just pass null for now.
            mCoreLightningNodeService = new RemoteCoreLightningNodeService(mSecureChannel, null);
        } catch (Exception e) {
            BackendManager.setError(BackendManager.ERROR_GRPC_CREATING_STUBS);
        }
    }

    public void openConnection() {
        if (!isConnected) {
            isConnected = true;
            BBLog.d(LOG_TAG, "Starting CoreLightning connection...(Open Http Channel)");
            generateChannelAndStubs();
        }
    }

    public void closeConnection() {
        if (mSecureChannel != null) {
            BBLog.d(LOG_TAG, "Shutting down CoreLightning connection...(Closing Http Channel)");
            shutdownChannel();
        }
        isConnected = false;
    }

    public void restartConnection() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
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
            if (mSecureChannel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS)) {
                BBLog.d(LOG_TAG, "CoreLightning channel shutdown successfully...");
            } else {
                BBLog.e(LOG_TAG, "CoreLightning channel shutdown failed...");
            }
        } catch (InterruptedException e) {
            BBLog.e(LOG_TAG, "CoreLightning channel shutdown exception: " + e.getMessage());
        }
    }
}
