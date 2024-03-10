package app.michaelwuensch.bitbanana.util;

import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.backends.BackendManager;

/**
 * This class is used to determine if a feature is available or not.
 * It can depend on the users settings as well as the lightning node implementation that is used.
 */
public class FeatureManager {

    private static final String LOG_TAG = FeatureManager.class.getSimpleName();

    private static final Set<FeatureChangedListener> featureChangedListeners = new HashSet<>();

    public static boolean isHelpButtonsEnabled() {
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureHelpButtons", true);
        return settingEnabled;
    }

    public static boolean isRoutingListViewEnabled() {
        boolean backendSupported = getBackend().supportsRouting();
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureRoutingSummary", true);
        return settingEnabled && backendSupported;
    }

    public static boolean isEditRoutingPoliciesEnabled() {
        return getBackend().supportsRoutingPolicyManagement();
    }

    public static boolean isUTXOListViewEnabled() {
        boolean backendSupported = getBackend().supportsCoinControl();
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureCoinControl", true);
        return settingEnabled && backendSupported;
    }

    public static boolean isContactsEnabled() {
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureContacts", true);
        return settingEnabled;
    }

    public static boolean isChannelManagementEnabled() {
        return getBackend().supportsChannelManagement();
    }

    public static boolean isPeersListViewEnabled() {
        boolean backendSupported = getBackend().supportsPeerManagement();
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featurePeers", false);
        return settingEnabled && backendSupported;
    }

    public static boolean isSignVerifyEnabled() {
        boolean backendSupported = getBackend().supportsMessageSigningByNodePrivateKey();
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureSignVerify", true);
        return settingEnabled && backendSupported;
    }

    public static boolean isBalanceDetailsEnabled() {
        return getBackend().supportsBalanceDetails();
    }

    public interface FeatureChangedListener {
        void onFeatureChanged();
    }

    /**
     * Notify all listeners to feature change updates.
     */
    public static void broadcastFeatureChange() {
        for (FeatureChangedListener listener : featureChangedListeners) {
            listener.onFeatureChanged();
        }
    }

    public static void registerFeatureChangedListener(FeatureChangedListener listener) {
        featureChangedListeners.add(listener);
    }

    public static void unregisterFeatureChangedListener(FeatureChangedListener listener) {
        featureChangedListeners.remove(listener);
    }

    private static Backend getBackend() {
        return BackendManager.getCurrentBackend();
    }
}
