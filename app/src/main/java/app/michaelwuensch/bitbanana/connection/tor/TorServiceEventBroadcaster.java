package app.michaelwuensch.bitbanana.connection.tor;

import androidx.annotation.NonNull;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.util.BBLog;
import io.matthewnelson.topl_service_base.TorPortInfo;

public class TorServiceEventBroadcaster extends io.matthewnelson.topl_service_base.TorServiceEventBroadcaster {

    private static final String LOG_TAG = "Tor Event";

    @Override
    public void broadcastPortInformation(TorPortInfo torPortInfo) {
        if (torPortInfo.getHttpPort() != null || torPortInfo.getSocksPort() != null) {
            if (torPortInfo.getHttpPort() != null) {
                BBLog.d(LOG_TAG, "HttpPortInfo: " + torPortInfo.getHttpPort());
                int port = Integer.valueOf(torPortInfo.getHttpPort().split(":")[1]);
                TorManager.getInstance().setHttpProxyPort(port);
            }
            if (torPortInfo.getSocksPort() != null) {
                BBLog.d(LOG_TAG, "SocksPortInfo: " + torPortInfo.getSocksPort());
                int port = Integer.valueOf(torPortInfo.getSocksPort().split(":")[1]);
                TorManager.getInstance().setSocksProxyPort(port);
            }
            TorManager.getInstance().setIsProxyRunning(true);
            TorManager.getInstance().setIsConnecting(false);

            // restart HTTP Client
            HttpClient.getInstance().restartHttpClient();

            // Continue backend connection process if it waited for Tor connection to be established
            if (BackendManager.getCurrentBackendConfig() != null && BackendManager.getCurrentBackendConfig().getUseTor() && BackendManager.getBackendState() == BackendManager.BackendState.STARTING_TOR) {
                BackendManager.activateBackendConfig4();
            }
        } else {
            TorManager.getInstance().setIsProxyRunning(false);
        }
    }

    @Override
    public void broadcastBandwidth(@NonNull String download, @NonNull String upload) {
        BBLog.v(LOG_TAG, "bandwidth: " + download + ", " + upload);
    }

    @Override
    public void broadcastDebug(@NonNull String s) {
        BBLog.d(LOG_TAG, "debug: " + s);
    }

    @Override
    public void broadcastException(String s, Exception e) {
        BBLog.e(LOG_TAG, "exception: " + s + ", " + e.getMessage());
    }

    @Override
    public void broadcastLogMessage(String message) {
        BBLog.d(LOG_TAG, message);
    }

    @Override
    public void broadcastNotice(@NonNull String notice) {
        BBLog.v(LOG_TAG, notice);
        if (notice.startsWith("WARN|BaseEventListener|Problem bootstrapping.")) {
            TorManager.getInstance().broadcastTorError();
            // Show error message on connection screen
            if (BackendManager.getCurrentBackendConfig() != null && BackendManager.getCurrentBackendConfig().getUseTor() && BackendManager.getBackendState() == BackendManager.BackendState.STARTING_TOR) {
                BackendManager.setError(BackendManager.ERROR_TOR_BOOTSTRAPPING_FAILED);
            }
        }
    }

    @Override
    public void broadcastTorState(@NonNull String torState, @NonNull String networkState) {
        BBLog.d(LOG_TAG, torState + ", " + networkState);
    }
}
