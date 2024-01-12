package app.michaelwuensch.bitbanana.util;

import android.content.Context;
import android.os.Handler;

import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backends.lnd.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.connection.internetConnectionStatus.NetworkUtil;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import app.michaelwuensch.bitbanana.connection.vpn.VPNUtil;

public class BackendSwitcher {

    public static final int ERROR_NO_INTERNET = 0;
    public static final int ERROR_VPN_NOT_INSTALLED = 1;
    public static final int ERROR_VPN_NO_CONTROL_PERMISSION = 2;
    public static final int ERROR_VPN_UNKNOWN_START_ISSUE = 3;
    public static final int ERROR_TOR_FAILED = 4;

    private static final String LOG_TAG = BackendSwitcher.class.getSimpleName();

    private static final Set<BackendStateChangedListener> backendStateChangedListeners = new HashSet<>();

    private static BackendConfig currentBackendConfig = null;
    private static BackendState currentBackendState = BackendState.NO_BACKEND_SELECTED;

    private static Handler delayHandler;
    private static int VPNCheckAttempts = 0;

    public static boolean hasBackendConfigs() {
        return BackendConfigsManager.getInstance().hasAnyBackendConfigs();
    }

    public static void activateCurrentBackendConfig(Context ctx, boolean force) {
        activateBackendConfig(BackendConfigsManager.getInstance().getCurrentBackendConfig(), ctx, force);
    }

    public static void activateBackendConfig(BackendConfig backendConfig, Context ctx, boolean force) {
        if (!hasBackendConfigs() || backendConfig == null)
            return;

        BBLog.d(LOG_TAG, "Activate backendConfig " + backendConfig.getAlias() + " called.");

        setupDelayHandler();
        delayHandler.removeCallbacksAndMessages(null);

        // Allow resetting the Pin timeout
        TimeOutUtil.getInstance().setCanBeRestarted(true);

        // Stop if requested backend is already active and we don't force to reload
        if (!force && backendConfig.equals(currentBackendConfig) && currentBackendState == BackendState.BACKEND_CONNECTED) {
            BBLog.d(LOG_TAG, "The requested backend is already active.");
            return;
        }

        if (currentBackendConfig == null)
            activateBackendConfig2(backendConfig, ctx);
        else {
            // Deactivate current BackendConfig if present, keep VPN or Tor if the backend to activate has the same settings like the one that gets deactivated.
            boolean keepVPN = false;
            boolean keepTor = false;

            if (currentBackendConfig != null && currentBackendConfig.getVpnConfig() != null && currentBackendConfig.getVpnConfig().isSameVPN(backendConfig.getVpnConfig()))
                keepVPN = true;
            if (currentBackendConfig != null && currentBackendConfig.getUseTor() == backendConfig.getUseTor())
                keepTor = true;
            if (currentBackendConfig != null)
                deactivateCurrentBackendConfig(ctx, keepVPN, keepTor);

            // After deactivating the old config we need to wait a bit so everything is cleanly shut down.
            delayHandler.postDelayed(() -> activateBackendConfig2(backendConfig, ctx), 500);
        }
    }

    private static void activateBackendConfig2(BackendConfig backendConfig, Context ctx) {
        BBLog.d(LOG_TAG, "Activating backendConfig: " + backendConfig.getAlias());
        currentBackendConfig = backendConfig;

        // Save the new chosen node in prefs
        PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_BACKEND_CONFIG, backendConfig.getId()).commit();

        setBackendState(BackendState.ACTIVATING_BACKEND);

        // Check internet connection
        if (!NetworkUtil.isConnectedToInternet(ctx)) {
            setBackendState(BackendState.ERROR);
            broadcastBackendStateError(ctx.getString(R.string.error_connection_no_internet), ERROR_NO_INTERNET);
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
        if (VPNCheckAttempts < 10) {
            if (!VPNUtil.isVpnActive(ctx)) {
                VPNCheckAttempts++;
                BBLog.v(LOG_TAG, "Check VPN available: " + VPNCheckAttempts);
                delayHandler.postDelayed(() -> waitingForVPN(ctx), 500);
            } else {
                VPNCheckAttempts = 0;
                // We delay this again, it seems to cause less problems if we wait again.
                delayHandler.postDelayed(BackendSwitcher::activateBackendConfig3, 500);
            }
        } else {
            setBackendState(BackendState.ERROR);
            if (!VPNUtil.isVpnAppInstalled(getCurrentBackendConfig().getVpnConfig(), ctx)) {
                broadcastBackendStateError(ctx.getString(R.string.vpn_unable_to_start) + "\n\n" + ctx.getString(R.string.vpn_unable_to_start_not_installed, currentBackendConfig.getVpnConfig().getVpnType().getDisplayName()), ERROR_VPN_NOT_INSTALLED);
            } else if (!VPNUtil.hasPermissionToControlVpn(getCurrentBackendConfig().getVpnConfig(), ctx)) {
                broadcastBackendStateError(ctx.getString(R.string.vpn_unable_to_start) + "\n\n" + ctx.getString(R.string.vpn_unable_to_start_no_permission, currentBackendConfig.getVpnConfig().getVpnType().getDisplayName()), ERROR_VPN_NO_CONTROL_PERMISSION);
            } else {
                broadcastBackendStateError(ctx.getString(R.string.vpn_unable_to_start) + "\n\n" + ctx.getString(R.string.vpn_unable_to_start_unknown, currentBackendConfig.getVpnConfig().getVpnType().getDisplayName()), ERROR_VPN_UNKNOWN_START_ISSUE);
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
            case BaseBackendConfig.BACKEND_TYPE_LND_GRPC:
                LndConnection.getInstance().openConnection();
        }
        setBackendState(BackendState.BACKEND_CONNECTED);

        // Start opening the wallet depending on the implementation. Unlock if necessary
        Wallet.getInstance().checkIfLndIsUnlockedAndConnect();
    }

    public static void deactivateCurrentBackendConfig(Context context, boolean keepVPN, boolean keepTor) {
        if (currentBackendConfig != null) {
            String backendConfigAlias = currentBackendConfig.getAlias();
            BBLog.d(LOG_TAG, "Deactivating backendConfig: " + backendConfigAlias);
            setBackendState(BackendState.DISCONNECTING);
            switch (currentBackendConfig.getBackendType()) {
                case BaseBackendConfig.BACKEND_TYPE_LND_GRPC:
                    LndConnection.getInstance().closeConnection();
            }

            // Stop VPN
            if (!keepVPN && currentBackendConfig.getVpnConfig() != null && currentBackendConfig.getVpnConfig().getStopVPNOnClose()) {
                VPNUtil.stopVPN(currentBackendConfig.getVpnConfig(), context);
            }

            // Stop Tor
            if (!keepTor && !PrefsUtil.isTorEnabled() && TorManager.getInstance().isProxyRunning()) {
                TorManager.getInstance().stopTor();
            }

            Wallet.getInstance().reset();
            currentBackendConfig = null;
            BBLog.d(LOG_TAG, backendConfigAlias + " deactivated.");
            setBackendState(BackendState.NO_BACKEND_SELECTED);
        }
    }

    public static BackendConfig getCurrentBackendConfig() {
        return currentBackendConfig;
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
