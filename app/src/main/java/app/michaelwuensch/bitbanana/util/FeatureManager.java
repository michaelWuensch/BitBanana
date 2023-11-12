package app.michaelwuensch.bitbanana.util;

import java.util.HashSet;
import java.util.Set;

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

    public static boolean isRoutingEnabled() {
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureRoutingSummary", true);
        return settingEnabled;
    }

    public static boolean isEditRoutingPoliciesEnabled() {
        return true;
    }

    public static boolean isCoinControlEnabled() {
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureCoinControl", true);
        return settingEnabled;
    }

    public static boolean isContactsEnabled() {
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureContacts", true);
        return settingEnabled;
    }

    public static boolean isChannelManagementEnabled() {
        return true;
    }

    public static boolean isPeersEnabled() {
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featurePeers", false);
        return settingEnabled;
    }

    public static boolean isSignVerifyEnabled() {
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureSignVerify", true);
        return settingEnabled;
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
}
