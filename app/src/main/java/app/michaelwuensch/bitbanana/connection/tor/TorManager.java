package app.michaelwuensch.bitbanana.connection.tor;

import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.matthewnelson.topl_service.TorServiceController;

/**
 * Singleton to manage Tor.
 */
public class TorManager {
    private static TorManager mTorManagerInstance;
    private static final String LOG_TAG = TorManager.class.getSimpleName();

    private final Set<TorErrorListener> mTorErrorListeners = new HashSet<>();

    private int mHttpProxyPort;
    private int mSocksProxyPort;
    private boolean isProxyRunning = false;
    private boolean isConnecting = false;

    public boolean isConnecting() {
        return isConnecting;
    }

    public int getHttpProxyPort() {
        return mHttpProxyPort;
    }

    public void setHttpProxyPort(int httpProxyPort) {
        this.mHttpProxyPort = httpProxyPort;
    }

    public int getSocksProxyPort() {
        return mSocksProxyPort;
    }

    public void setSocksProxyPort(int socksProxyPort) {
        this.mSocksProxyPort = socksProxyPort;
    }

    public boolean isProxyRunning() {
        return isProxyRunning;
    }

    public void setIsProxyRunning(boolean proxyRunning) {
        if (!proxyRunning)
            isConnecting = false;
        isProxyRunning = proxyRunning;
    }

    public void setIsConnecting(boolean connecting) {
        isConnecting = connecting;
    }

    private TorManager() {
    }

    public void startTor() {
        BBLog.d(LOG_TAG, "Start Tor called.");
        isConnecting = true;
        TorServiceController.startTor();
    }

    public void stopTor() {
        BBLog.d(LOG_TAG, "Stop Tor called.");
        isConnecting = false;
        TorServiceController.stopTor();
    }

    public void restartTor() {
        BBLog.d(LOG_TAG, "Restart Tor called.");
        TorServiceController.restartTor();
    }

    public void switchTorPrefState(boolean newActive) {
        if (newActive) {
            if (!isProxyRunning()) {
                startTor();
                // HTTP Client gets restarted automatically once tor connection is established.
            } else {
                // restart HTTP Client
                HttpClient.getInstance().restartHttpClient();
            }
        } else {
            if (!isCurrentNodeConnectionTor() && (isConnecting || isProxyRunning)) {
                // Stop tor service if not used by current node.
                stopTor();
            }
            // restart HTTP Client
            HttpClient.getInstance().restartHttpClient();
        }
    }

    public boolean isCurrentNodeConnectionTor() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            return BackendConfigsManager.getInstance().getCurrentBackendConfig().getUseTor();
        } else {
            return false;
        }
    }

    public int getTorTimeoutMultiplier() {
        if (PrefsUtil.isTorEnabled() || isCurrentNodeConnectionTor()) {
            return RefConstants.TOR_TIMEOUT_MULTIPLIER;
        } else {
            return 1;
        }
    }

    public static synchronized TorManager getInstance() {
        if (mTorManagerInstance == null) {
            mTorManagerInstance = new TorManager();
        }
        return mTorManagerInstance;
    }

    public void broadcastTorError() {
        for (TorErrorListener listener : mTorErrorListeners) {
            listener.onTorBootstrappingFailed();
        }
    }

    public void registerTorErrorListener(TorErrorListener listener) {
        mTorErrorListeners.add(listener);
    }

    public void unregisterTorErrorListener(TorErrorListener listener) {
        mTorErrorListeners.remove(listener);
    }

    public interface TorErrorListener {
        void onTorBootstrappingFailed();
    }
}
