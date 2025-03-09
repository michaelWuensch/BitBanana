package app.michaelwuensch.bitbanana.backends.lnd.connection;


import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndAutopilotService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndChainKitService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndChainNotifierService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndInvoicesService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndLightningService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndPeersService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndRouterService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndSignerService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndStateService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndVersionerService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndWalletKitService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndWalletUnlockerService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndWatchtowerClientService;
import app.michaelwuensch.bitbanana.backends.lnd.services.LndWatchtowerService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndAutopilotService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndChainKitService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndChainNotifierService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndInvoicesService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndLightningService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndPeersService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndRouterService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndSignerService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndStateService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndVersionerService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndWalletKitService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndWalletUnlockerService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndWatchtowerClientService;
import app.michaelwuensch.bitbanana.backends.lnd.services.RemoteLndWatchtowerService;
import app.michaelwuensch.bitbanana.connection.BlindHostnameVerifier;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.connection.tor.TorProxyDetector;
import app.michaelwuensch.bitbanana.util.BBLog;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

/**
 * Singleton to handle the connection to lnd
 */
public class LndConnection {

    private static final String LOG_TAG = LndConnection.class.getSimpleName();

    private static LndConnection mLndConnectionInstance;

    private ManagedChannel mSecureChannel;
    private LndAutopilotService mLndAutopilotService;
    private LndChainKitService mLndChainKitService;
    private LndChainNotifierService mLndChainNotifierService;
    private LndInvoicesService mLndInvoicesService;
    private LndLightningService mLndLightningService;
    private LndPeersService mLndPeersService;
    private LndRouterService mLndRouterService;
    private LndSignerService mLndSignerService;
    private LndStateService mLndStateService;
    private LndVersionerService mLndVersionerService;
    private LndWalletKitService mLndWalletKitService;
    private LndWalletUnlockerService mLndWalletUnlockerService;
    private LndWatchtowerService mLndWatchtowerService;
    private LndWatchtowerClientService mLndWatchtowerClientService;
    private boolean isConnected = false;

    private LndConnection() {
    }

    public static synchronized LndConnection getInstance() {
        if (mLndConnectionInstance == null) {
            mLndConnectionInstance = new LndConnection();
        }
        return mLndConnectionInstance;
    }

    public LndAutopilotService getAutopilotService() {
        return mLndAutopilotService;
    }

    public LndChainKitService getChainKitService() {
        return mLndChainKitService;
    }

    public LndChainNotifierService getChainNotifierService() {
        return mLndChainNotifierService;
    }

    public LndInvoicesService getInvoicesService() {
        return mLndInvoicesService;
    }

    public LndLightningService getLightningService() {
        return mLndLightningService;
    }

    public LndPeersService getPeersService() {
        return mLndPeersService;
    }

    public LndRouterService getRouterService() {
        return mLndRouterService;
    }

    public LndSignerService getSignerService() {
        return mLndSignerService;
    }

    public LndStateService getStateService() {
        return mLndStateService;
    }

    public LndVersionerService getVersionerService() {
        return mLndVersionerService;
    }

    public LndWalletKitService getWalletKitService() {
        return mLndWalletKitService;
    }

    public LndWalletUnlockerService getWalletUnlockerService() {
        return mLndWalletUnlockerService;
    }

    public LndWatchtowerService getWatchtowerService() {
        return mLndWatchtowerService;
    }

    public LndWatchtowerClientService getWatchtowerClientService() {
        return mLndWatchtowerClientService;
    }

    private void generateChannelAndStubs() {
        String host = BackendManager.getCurrentBackendConfig().getHostWithOverride();
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
                        .sslSocketFactory(LndSSLSocketFactory.create(BackendManager.getCurrentBackendConfig())) // null = default SSLSocketFactory
                        .maxInboundMessageSize(10 * 1024 * 1024) // set max message size to 10 mb
                        .build();
            } else {
                mSecureChannel = OkHttpChannelBuilder
                        .forAddress(host, port)
                        .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                        .sslSocketFactory(LndSSLSocketFactory.create(BackendManager.getCurrentBackendConfig())) // null = default SSLSocketFactory
                        .maxInboundMessageSize(10 * 1024 * 1024) // set max message size to 10 mb
                        .build();
            }

            MacaroonCallCredential macaroon = new MacaroonCallCredential(BackendManager.getCurrentBackendConfig().getAuthenticationToken());

            mLndAutopilotService = new RemoteLndAutopilotService(mSecureChannel, macaroon);
            mLndChainKitService = new RemoteLndChainKitService(mSecureChannel, macaroon);
            mLndChainNotifierService = new RemoteLndChainNotifierService(mSecureChannel, macaroon);
            mLndInvoicesService = new RemoteLndInvoicesService(mSecureChannel, macaroon);
            mLndLightningService = new RemoteLndLightningService(mSecureChannel, macaroon);
            mLndPeersService = new RemoteLndPeersService(mSecureChannel, macaroon);
            mLndRouterService = new RemoteLndRouterService(mSecureChannel, macaroon);
            mLndSignerService = new RemoteLndSignerService(mSecureChannel, macaroon);
            mLndStateService = new RemoteLndStateService(mSecureChannel, macaroon);
            mLndVersionerService = new RemoteLndVersionerService(mSecureChannel, macaroon);
            mLndWalletKitService = new RemoteLndWalletKitService(mSecureChannel, macaroon);
            mLndWatchtowerService = new RemoteLndWatchtowerService(mSecureChannel, macaroon);
            mLndWatchtowerClientService = new RemoteLndWatchtowerClientService(mSecureChannel, macaroon);
            mLndWalletUnlockerService = new RemoteLndWalletUnlockerService(mSecureChannel, macaroon);
        } catch (Exception e) {
            BackendManager.setError(BackendManager.ERROR_GRPC_CREATING_STUBS);
        }
    }

    public void openConnection() {
        if (!isConnected) {
            isConnected = true;
            BBLog.d(LOG_TAG, "Starting LND connection...(Open Http Channel)");
            generateChannelAndStubs();
        }
    }

    public void closeConnection() {
        if (mSecureChannel != null) {
            BBLog.d(LOG_TAG, "Shutting down LND connection...(Closing Http Channel)");
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
                BBLog.d(LOG_TAG, "LND channel shutdown successfully...");
            } else {
                BBLog.e(LOG_TAG, "LND channel shutdown failed...");
            }
        } catch (InterruptedException e) {
            BBLog.e(LOG_TAG, "LND channel shutdown exception: " + e.getMessage());
        }
    }
}
