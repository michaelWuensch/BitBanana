package app.michaelwuensch.bitbanana.connection.tor;

import androidx.annotation.NonNull;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.lnd.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.util.BBLog;
import io.matthewnelson.topl_service_base.TorPortInfo;

public class TorServiceEventBroadcaster extends io.matthewnelson.topl_service_base.TorServiceEventBroadcaster {

    private static final String LOG_TAG = "Tor Event";

    @Override
    public void broadcastPortInformation(TorPortInfo torPortInfo) {
        BBLog.d(LOG_TAG, "PortInfo: " + torPortInfo.getHttpPort());

        if (torPortInfo.getHttpPort() != null) {

            int port = Integer.valueOf(torPortInfo.getHttpPort().split(":")[1]);
            TorManager.getInstance().setIsProxyRunning(true);
            TorManager.getInstance().setProxyPort(port);

            // restart HTTP Client
            HttpClient.getInstance().restartHttpClient();

            // restart LND Connection
            if (BackendConfigsManager.getInstance().getCurrentBackendConfig().getUseTor())
                LndConnection.getInstance().reconnect();
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
        }
    }

    @Override
    public void broadcastTorState(@NonNull String torState, @NonNull String networkState) {
        BBLog.d(LOG_TAG, torState + ", " + networkState);
    }
}
