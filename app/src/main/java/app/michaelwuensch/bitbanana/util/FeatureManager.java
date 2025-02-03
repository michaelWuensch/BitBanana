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

    public static boolean isBolt12OffersViewEnabled() {
        return getBackend().supportsBolt12Receive();
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

    public static boolean isUtxoSelectionOnSendEnabled() {
        boolean backendSupported = getBackend().supportsUtxoSelectOnSend();
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureCoinControl", true);
        return settingEnabled && backendSupported;
    }

    public static boolean isUtxoSelectionOnChannelOpenEnabled() {
        boolean backendSupported = getBackend().supportsUtxoSelectOnChannelOpen();
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

    public static boolean isPeersModificationEnabled() {
        return getBackend().supportsPeerModification();
    }

    public static boolean isSignVerifyEnabled() {
        boolean backendSupported = getBackend().supportsMessageSigningByNodePrivateKey();
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureSignVerify", true);
        return settingEnabled && backendSupported;
    }

    public static boolean isOpenChannelEnabled() {
        return getBackend().supportsOpenChannel() && getBackend().supportsChannelManagement();
    }

    public static boolean isCloseChannelEnabled() {
        return getBackend().supportsCloseChannel() && getBackend().supportsChannelManagement();
    }

    public static boolean isBalanceDetailsEnabled() {
        return getBackend().supportsBalanceDetails();
    }

    public static boolean isOffchainSendingEnabled() {
        return getBackend().supportsBolt11Sending() || getBackend().supportsBolt12Sending();
    }

    public static boolean isSendingEnabled() {
        return isOffchainSendingEnabled() || getBackend().supportsOnChainSending();
    }

    public static boolean isReceivingEnabled() {
        return getBackend().supportsBolt11Receive() || getBackend().supportsOnChainReceive();
    }

    public static boolean isBolt11WithoutAmountEnabled() {
        boolean backendSupported = getBackend().supportsBolt11WithoutAmount();
        boolean settingEnabled = PrefsUtil.getAreInvoicesWithoutSpecifiedAmountAllowed();
        return settingEnabled && backendSupported;
    }

    public static boolean isDisplayPaymentRouteEnabled() {
        return getBackend().supportsDisplayPaymentRoute();
    }

    public static boolean isLnurlAuthEnabled() {
        return getBackend().supportsLnurlAuth();
    }

    public static boolean isKeysendEnabled() {
        return getBackend().supportsKeysend();
    }

    public static boolean isWatchtowersEnabled() {
        boolean backendSupported = getBackend().supportsWatchtowers();
        boolean settingEnabled = PrefsUtil.getPrefs().getBoolean("featureWatchtowers", false);
        return settingEnabled && backendSupported;
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
