package app.michaelwuensch.bitbanana.backends;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.util.Version;

public class Backend {
    protected BackendConfig mBackendConfig;

    protected Api mApi = new Api();
    protected Version mMinRequiredVersion = new Version("0.0.0");
    protected String mMinRequiredVersionName = "v0.0.0-beta";
    protected String mNodeImplementationName = "UNKNOWN";
    protected BackendFeature FeatureChannelManagement = new BackendFeature(false);
    protected BackendFeature FeatureOpenChannel = new BackendFeature(false);
    protected BackendFeature FeatureCloseChannel = new BackendFeature(false);
    protected BackendFeature FeaturePeerManagement = new BackendFeature(false);
    protected BackendFeature FeaturePeerModification = new BackendFeature(false);
    protected BackendFeature FeatureRouting = new BackendFeature(false);
    protected BackendFeature FeatureRoutingPolicyManagement = new BackendFeature(false);
    protected BackendFeature FeatureCoinControl = new BackendFeature(false);
    protected BackendFeature FeatureBalanceDetails = new BackendFeature(false);
    protected BackendFeature FeatureMessageSigningByNodePrivateKey = new BackendFeature(false);
    protected BackendFeature FeatureLnurlAuth = new BackendFeature(false);
    protected BackendFeature FeatureDisplayPaymentRoute = new BackendFeature(false);

    /**
     * If the backend has a function to get recommended on-chain fees
     */
    protected BackendFeature FeatureOnChainFeeEstimation = new BackendFeature(false);

    /**
     * If the backend has a way to calculate the on-chain transaction before actually sending it.
     * This allows to display the absolute fee that will be payed for the transaction rather than just a sat/vB value.
     */
    protected BackendFeature FeatureAbsoluteOnChainFeeEstimation = new BackendFeature(false);
    protected BackendFeature FeatureRoutingFeeEstimation = new BackendFeature(false);
    protected BackendFeature FeatureOnChainSending = new BackendFeature(false);
    protected BackendFeature FeatureKeysend = new BackendFeature(false);
    protected BackendFeature FeatureBolt11Sending = new BackendFeature(false);
    protected BackendFeature FeatureBolt12Sending = new BackendFeature(false);
    protected BackendFeature FeatureBolt12Receive = new BackendFeature(false);
    protected BackendFeature FeatureOnChainReceive = new BackendFeature(false);
    protected BackendFeature FeatureBolt11Receive = new BackendFeature(false);
    protected BackendFeature FeatureBolt11WithoutAmount = new BackendFeature(false);
    protected BackendFeature FeatureIdentityScreen = new BackendFeature(false);

    /**
     * Whether or not it is possible to subscribe to events that happen on the backend.
     * If this is not possible BitBanana needs to poll for new information in some situations like after executing a payment or while waiting for a invoice to be paid.
     */
    protected BackendFeature FeatureEventSubscriptions = new BackendFeature(false);
    protected BackendFeature FeatureWatchtowers = new BackendFeature(false);
    protected BackendFeature FeatureManuallyLeaseUTXOs = new BackendFeature(false);
    protected BackendFeature FeatureUtxoSelectOnSend = new BackendFeature(false);
    protected BackendFeature FeatureUtxoSelectOnChannelOpen = new BackendFeature(false);
    protected BackendFeature FeatureSendAllOnChain = new BackendFeature(false);

    public Backend() {
        this(null);
    }

    public Backend(BackendConfig backendConfig) {
    }

    public Api api() {
        return mApi;
    }

    public Version getMinRequiredVersion() {
        return mMinRequiredVersion;
    }

    public String getMinRequiredVersionName() {
        return mMinRequiredVersionName;
    }

    public String getNodeImplementationName() {
        return mNodeImplementationName;
    }

    public boolean supportsChannelManagement() {
        return FeatureChannelManagement.isAvailable();
    }

    public boolean supportsOpenChannel() {
        return FeatureOpenChannel.isAvailable();
    }

    public boolean supportsCloseChannel() {
        return FeatureCloseChannel.isAvailable();
    }

    public boolean supportsPeerManagement() {
        return FeaturePeerManagement.isAvailable();
    }

    public boolean supportsPeerModification() {
        return FeaturePeerModification.isAvailable();
    }

    public boolean supportsRouting() {
        return FeatureRouting.isAvailable();
    }

    public boolean supportsRoutingPolicyManagement() {
        return FeatureRoutingPolicyManagement.isAvailable();
    }

    public boolean supportsCoinControl() {
        return FeatureCoinControl.isAvailable();
    }

    public boolean supportsBalanceDetails() {
        return FeatureBalanceDetails.isAvailable();
    }

    public boolean supportsMessageSigningByNodePrivateKey() {
        return FeatureMessageSigningByNodePrivateKey.isAvailable();
    }

    public boolean supportsLnurlAuth() {
        return FeatureLnurlAuth.isAvailable();
    }

    public boolean supportsKeysend() {
        return FeatureKeysend.isAvailable();
    }

    public boolean supportsOnChainFeeEstimation() {
        return FeatureOnChainFeeEstimation.isAvailable();
    }

    public boolean supportsAbsoluteOnChainFeeEstimation() {
        return FeatureAbsoluteOnChainFeeEstimation.isAvailable();
    }


    public boolean supportsRoutingFeeEstimation() {
        return FeatureRoutingFeeEstimation.isAvailable();
    }

    public boolean supportsOnChainSending() {
        return FeatureOnChainSending.isAvailable();
    }

    public boolean supportsBolt11Sending() {
        return FeatureBolt11Sending.isAvailable();
    }

    public boolean supportsBolt12Sending() {
        return FeatureBolt12Sending.isAvailable();
    }

    public boolean supportsBolt12Receive() {
        return FeatureBolt12Receive.isAvailable();
    }

    public boolean supportsOnChainReceive() {
        return FeatureOnChainReceive.isAvailable();
    }

    public boolean supportsBolt11Receive() {
        return FeatureBolt11Receive.isAvailable();
    }

    public boolean supportsIdentityScreen() {
        return FeatureIdentityScreen.isAvailable();
    }

    public boolean supportsBolt11WithoutAmount() {
        return FeatureBolt11WithoutAmount.isAvailable();
    }

    public boolean supportsEventSubscriptions() {
        return FeatureEventSubscriptions.isAvailable();
    }

    public boolean supportsWatchtowers() {
        return FeatureWatchtowers.isAvailable();
    }

    public boolean supportsDisplayPaymentRoute() {
        return FeatureDisplayPaymentRoute.isAvailable();
    }

    public boolean supportsManuallyLeasingUTXOs() {
        return FeatureManuallyLeaseUTXOs.isAvailable();
    }

    public boolean supportsUtxoSelectOnSend() {
        return FeatureUtxoSelectOnSend.isAvailable();
    }

    public boolean supportsUtxoSelectOnChannelOpen() {
        return FeatureUtxoSelectOnChannelOpen.isAvailable();
    }

    public boolean supportsSendAllOnChain() {
        return FeatureSendAllOnChain.isAvailable();
    }
}
