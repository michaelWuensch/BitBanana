package app.michaelwuensch.bitbanana.backends;

import android.content.Context;
import android.os.Handler;

import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.coreLightning.CoreLightningBackend;
import app.michaelwuensch.bitbanana.backends.coreLightning.connection.CoreLightningConnection;
import app.michaelwuensch.bitbanana.backends.demo.DemoBackend;
import app.michaelwuensch.bitbanana.backends.lnd.LndBackend;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.backends.lndHub.LndHubBackend;
import app.michaelwuensch.bitbanana.backends.lndHub.LndHubHttpClient;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.internetConnectionStatus.NetworkUtil;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import app.michaelwuensch.bitbanana.connection.vpn.VPNUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet;

public class BackendManager {

    public static final int ERROR_NO_INTERNET = 0;
    public static final int ERROR_VPN_NOT_INSTALLED = 1;
    public static final int ERROR_VPN_NO_CONTROL_PERMISSION = 2;
    public static final int ERROR_VPN_UNKNOWN_START_ISSUE = 3;
    public static final int ERROR_TOR_BOOTSTRAPPING_FAILED = 4;
    public static final int ERROR_UNKNOWN_BACKEND_TYPE = 5;
    public static final int ERROR_GRPC_CREATING_STUBS = 6;

    private static final String LOG_TAG = BackendManager.class.getSimpleName();

    private static final Set<BackendStateChangedListener> backendStateChangedListeners = new HashSet<>();

    private static BackendConfig currentBackendConfig = null;
    private static Backend currentBackend = new Backend();
    private static BackendState currentBackendState = BackendState.NO_BACKEND_SELECTED;

    private static Handler delayHandler;
    private static int VPNCheckAttempts = 0;

    public static boolean hasBackendConfigs() {
        return BackendConfigsManager.getInstance().hasAnyBackendConfigs();
    }

    public static void activateCurrentBackendConfig(Context ctx, boolean force) {
        activateBackendConfig(BackendConfigsManager.getInstance().getCurrentBackendConfig(), ctx, force);
    }

    public static void activateBackendConfig(BackendConfig backendConfig, Context ctx, boolean forceCleanReload) {
        if (!hasBackendConfigs()) {
            BBLog.d(LOG_TAG, "Activating demo backend.");
            currentBackend = createBackend();
            setBackendState(BackendState.ACTIVATING_BACKEND);
            setBackendState(BackendState.BACKEND_CONNECTED);
            return;
        }

        if (backendConfig == null)
            return;

        BBLog.d(LOG_TAG, "Activate backendConfig " + backendConfig.getAlias() + " called.");

        setupDelayHandler();
        delayHandler.removeCallbacksAndMessages(null);

        // Allow resetting the Pin timeout
        TimeOutUtil.getInstance().setCanBeRestarted(true);

        // Stop if requested backend is already active and we don't forceCleanReload to reload
        if (!forceCleanReload && backendConfig.equals(currentBackendConfig) && currentBackendState == BackendState.BACKEND_CONNECTED) {
            BBLog.d(LOG_TAG, "The requested backend is already active.");
            return;
        }

        if (currentBackendConfig == null)
            activateBackendConfig2(backendConfig, ctx);
        else {
            // Deactivate current BackendConfig if present, keep VPN or Tor if the backend to activate has the same settings like the one that gets deactivated.
            boolean keepVPN = false;
            boolean keepTor = false;

            if (!forceCleanReload) {
                if (currentBackendConfig.getVpnConfig() != null && currentBackendConfig.getVpnConfig().isSameVPN(backendConfig.getVpnConfig()))
                    keepVPN = true;
                if (currentBackendConfig.getUseTor() == backendConfig.getUseTor())
                    keepTor = true;
            }

            deactivateCurrentBackendConfig(ctx, keepVPN, keepTor);

            // After deactivating the old config we need to wait a bit so everything is cleanly shut down.
            delayHandler.postDelayed(() -> activateBackendConfig2(backendConfig, ctx), 500);
        }
    }

    private static void activateBackendConfig2(BackendConfig backendConfig, Context ctx) {
        BBLog.d(LOG_TAG, "Activating backendConfig: " + backendConfig.getAlias());
        currentBackendConfig = backendConfig;
        currentBackend = createBackend();

        // Save the new chosen node in prefs
        PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_BACKEND_CONFIG, backendConfig.getId()).commit();

        setBackendState(BackendState.ACTIVATING_BACKEND);

        // Check internet connection
        if (!NetworkUtil.isConnectedToInternet(ctx)) {
            setError(ERROR_NO_INTERNET);
            return;
        }

        // Start VPN
        VPNConfig vpnConfig = currentBackendConfig.getVpnConfig();
        if (vpnConfig != null && vpnConfig.getVpnType() != VPNConfig.VPNType.NONE && vpnConfig.getStartVPNOnOpen()) {
            setBackendState(BackendState.STARTING_VPN);

            // If this VPN is already active, nothing will happen and it doesn't harm to call it here again.
            // If another VPN is already active, it will stop that other VPN automatically before starting the new one.
            VPNUtil.startVPN(vpnConfig, ctx);

            // We have to call this delayed, as otherwise if another VPN was active before we would skip the waiting for the new VPN.
            // The old VPN needs a little time to get deactivated.
            VPNCheckAttempts = 0;
            delayHandler.postDelayed(() -> waitingForVPN(ctx), 500);
        } else {
            activateBackendConfig3();
        }
    }

    private static void waitingForVPN(Context ctx) {
        if (VPNCheckAttempts < RefConstants.VPN_START_TIMEOUT * 2) {
            if (!VPNUtil.isVpnActive(ctx)) {
                VPNCheckAttempts++;
                BBLog.v(LOG_TAG, "Check VPN available: " + VPNCheckAttempts);
                delayHandler.postDelayed(() -> waitingForVPN(ctx), 500);
            } else {
                VPNCheckAttempts = 0;
                // We delay this again, it seems to cause less problems if we wait again.
                delayHandler.postDelayed(BackendManager::activateBackendConfig3, 500);
            }
        } else {
            setBackendState(BackendState.ERROR);
            if (!VPNUtil.isVpnAppInstalled(getCurrentBackendConfig().getVpnConfig(), ctx)) {
                setError(ERROR_VPN_NOT_INSTALLED);
            } else if (!VPNUtil.hasPermissionToControlVpn(getCurrentBackendConfig().getVpnConfig(), ctx)) {
                setError(ERROR_VPN_NO_CONTROL_PERMISSION);
            } else {
                setError(ERROR_VPN_UNKNOWN_START_ISSUE);
            }
        }
    }

    private static void activateBackendConfig3() {
        // Start Tor
        if (currentBackendConfig.getUseTor()) {
            if (!TorManager.getInstance().isProxyRunning()) {
                setBackendState(BackendState.STARTING_TOR);

                // The activateBackendConfig4() function is called as soon as tor is successfully started.
                TorManager.getInstance().startTor();
            } else {
                activateBackendConfig4();
            }
        } else {
            activateBackendConfig4();
        }
    }

    public static void activateBackendConfig4() {
        if (currentBackendConfig.getUseTor())
            setBackendState(BackendState.TOR_CONNECTED);

        // Connect to backend
        setBackendState(BackendState.CONNECTING_TO_BACKEND);
        switch (currentBackendConfig.getBackendType()) {
            case LND_GRPC:
                LndConnection.getInstance().openConnection();
                break;
            case CORE_LIGHTNING_GRPC:
                CoreLightningConnection.getInstance().openConnection();
                break;
            case LND_HUB:
                LndHubHttpClient.getInstance().createHttpClient();
                break;
            default:
                setError(ERROR_UNKNOWN_BACKEND_TYPE);
        }
        if (currentBackendState == BackendState.ERROR)
            return;

        setBackendState(BackendState.BACKEND_CONNECTED);
        Wallet.getInstance().open();
    }

    public static void deactivateCurrentBackendConfig(Context context, boolean keepVPN, boolean keepTor) {
        if (currentBackendConfig != null) {
            String backendConfigAlias = currentBackendConfig.getAlias();
            BBLog.d(LOG_TAG, "Deactivating backendConfig: " + backendConfigAlias);
            setBackendState(BackendState.DISCONNECTING);
            switch (currentBackendConfig.getBackendType()) {
                case LND_GRPC:
                    LndConnection.getInstance().closeConnection();
                    break;
                case CORE_LIGHTNING_GRPC:
                    CoreLightningConnection.getInstance().closeConnection();
                    break;
                case LND_HUB:
                    LndHubHttpClient.getInstance().cancelAllRequests();
                    break;
            }

            // Stop VPN
            if (!keepVPN && currentBackendConfig.getVpnConfig() != null && currentBackendConfig.getVpnConfig().getStopVPNOnClose()) {
                VPNUtil.stopVPN(currentBackendConfig.getVpnConfig(), context);
            }

            // Stop Tor
            if (!keepTor && !PrefsUtil.isTorEnabled() && (TorManager.getInstance().isProxyRunning() || TorManager.getInstance().isConnecting())) {
                TorManager.getInstance().stopTor();
            }

            Wallet.getInstance().reset();
            currentBackendConfig = null;
            currentBackend = new Backend(null);
            BBLog.d(LOG_TAG, backendConfigAlias + " deactivated.");
            setBackendState(BackendState.NO_BACKEND_SELECTED);
        }
    }

    public static BackendConfig getCurrentBackendConfig() {
        return currentBackendConfig;
    }

    public static BackendConfig.BackendType getCurrentBackendType() {
        if (currentBackendConfig == null)
            return BackendConfig.BackendType.NONE;
        return currentBackendConfig.getBackendType();
    }

    private static Backend createBackend() {
        if (getCurrentBackendConfig() != null) {
            switch (getCurrentBackendConfig().getBackendType()) {
                case NONE:
                    return new Backend();
                case LND_GRPC:
                    return new LndBackend(getCurrentBackendConfig());
                case CORE_LIGHTNING_GRPC:
                    return new CoreLightningBackend(getCurrentBackendConfig());
                case LND_HUB:
                    return new LndHubBackend(getCurrentBackendConfig());
            }
        }
        return new DemoBackend(null);
    }

    public static Backend getCurrentBackend() {
        return currentBackend;
    }

    public static Api api() {
        return currentBackend.api();
    }

    public static BackendState getBackendState() {
        return currentBackendState;
    }

    private static void setBackendState(BackendState backendState) {
        if (currentBackendState != backendState) {
            BBLog.v(LOG_TAG, "New backend state: " + backendState);
            currentBackendState = backendState;
            broadcastBackendStateChange(backendState);
        }
    }

    private static void setupDelayHandler() {
        if (delayHandler == null) {
            delayHandler = new Handler();
        }
    }

    public static void setError(int errorCode) {
        setBackendState(BackendState.ERROR);
        switch (errorCode) {
            case ERROR_NO_INTERNET:
                broadcastBackendStateError(App.getAppContext().getString(R.string.error_connection_no_internet), errorCode);
                break;
            case ERROR_VPN_NOT_INSTALLED:
                broadcastBackendStateError(App.getAppContext().getString(R.string.vpn_unable_to_start) + "\n\n" + App.getAppContext().getString(R.string.vpn_unable_to_start_not_installed, currentBackendConfig.getVpnConfig().getVpnType().getDisplayName()), errorCode);
                break;
            case ERROR_VPN_NO_CONTROL_PERMISSION:
                broadcastBackendStateError(App.getAppContext().getString(R.string.vpn_unable_to_start) + "\n\n" + App.getAppContext().getString(R.string.vpn_unable_to_start_no_permission, currentBackendConfig.getVpnConfig().getVpnType().getDisplayName()), errorCode);
                break;
            case ERROR_VPN_UNKNOWN_START_ISSUE:
                broadcastBackendStateError(App.getAppContext().getString(R.string.vpn_unable_to_start) + "\n\n" + App.getAppContext().getString(R.string.vpn_unable_to_start_unknown, currentBackendConfig.getVpnConfig().getVpnType().getDisplayName()), errorCode);
                break;
            case ERROR_TOR_BOOTSTRAPPING_FAILED:
                broadcastBackendStateError(App.getAppContext().getString(R.string.error_tor_bootstrapping_failed), errorCode);
                break;
            case ERROR_UNKNOWN_BACKEND_TYPE:
                broadcastBackendStateError(App.getAppContext().getString(R.string.error_unknown_backend), errorCode);
                break;
            case ERROR_GRPC_CREATING_STUBS:
                broadcastBackendStateError(App.getAppContext().getString(R.string.error_grpc_setup_failed), errorCode);
                break;
        }
    }

    public interface BackendStateChangedListener {
        void onBackendStateChanged(BackendState backendState);

        void onBackendStateError(String message, int errorCode);
    }

    /**
     * Notify all listeners to BackendState changed events.
     */
    private static void broadcastBackendStateChange(BackendState backendState) {
        for (BackendStateChangedListener listener : backendStateChangedListeners) {
            listener.onBackendStateChanged(backendState);
        }
    }

    private static void broadcastBackendStateError(String message, int errorCode) {
        for (BackendStateChangedListener listener : backendStateChangedListeners) {
            listener.onBackendStateError(message, errorCode);
        }
    }

    public static void registerBackendStateChangedListener(BackendStateChangedListener listener) {
        backendStateChangedListeners.add(listener);
    }

    public static void unregisterBackendStateChangedListener(BackendStateChangedListener listener) {
        backendStateChangedListeners.remove(listener);
    }

    public enum BackendState {
        NO_BACKEND_SELECTED,
        DISCONNECTING,
        ACTIVATING_BACKEND,
        STARTING_VPN,
        STARTING_TOR,
        TOR_CONNECTED,
        CONNECTING_TO_BACKEND,
        BACKEND_CONNECTED,
        ERROR;

        public static BackendState parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return NO_BACKEND_SELECTED;
            }
        }
    }
}
