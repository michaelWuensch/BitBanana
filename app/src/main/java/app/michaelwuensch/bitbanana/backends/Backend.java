package app.michaelwuensch.bitbanana.backends;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.util.Version;

public class Backend {
    protected BackendConfig mBackendConfig;

    protected Api mApi = new Api();
    protected Version mMinRequiredVersion = new Version("0.0.0");
    protected String mMinRequiredVersionName = "v0.0.0-beta";
    protected String mNodeImplementationName = "UNKNOWN";
    protected boolean bSupportsChannelManagement = false;
    protected boolean bSupportsOpenChannel = false;
    protected boolean bSupportsCloseChannel = false;
    protected boolean bSupportsPeerManagement = false;
    protected boolean bSupportsPeerModification = false;
    protected boolean bSupportsRouting = false;
    protected boolean bSupportsRoutingPolicyManagement = false;
    protected boolean bSupportsCoinControl = false;
    protected boolean bSupportsBalanceDetails = false;
    protected boolean bSupportsMessageSigningByNodePrivateKey = false;
    protected boolean bSupportsLnurlAuth = false;

    /**
     * If the backend has a function to get recommended on-chain fees
     */
    protected boolean bSupportsOnChainFeeEstimation = false;

    /**
     * If the backend has a way to calculate the on-chain transaction before actually sending it.
     * This allows to display the absolute fee that will be payed for the transaction rather than just a sat/vB value.
     */
    protected boolean bSupportsAbsoluteOnChainFeeEstimation = false;
    protected boolean bSupportsRoutingFeeEstimation = false;
    protected boolean bSupportsOnChainSending = false;
    protected boolean bSupportsKeysend = false;
    protected boolean bSupportsBolt11Sending = false;
    protected boolean bSupportsBolt12Sending = false;
    protected boolean bSupportsOnChainReceive = false;
    protected boolean bSupportsBolt11Receive = false;
    protected boolean bSupportsBolt11WithoutAmount = false;
    protected boolean bSupportsIdentityScreen = false;

    /**
     * Whether or not it is possible to subscribe to events that happen on the backend.
     * If this is not possible BitBanana needs to poll for new information in some situations like after executing a payment or while waiting for a invoice to be paid.
     */
    protected boolean bSupportsEventSubscription = false;
    protected boolean bSupportsWatchtowers = false;

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
        return bSupportsChannelManagement;
    }

    public boolean supportsOpenChannel() {
        return bSupportsOpenChannel;
    }

    public boolean supportsCloseChannel() {
        return bSupportsCloseChannel;
    }

    public boolean supportsPeerManagement() {
        return bSupportsPeerManagement;
    }

    public boolean supportsPeerModification() {
        return bSupportsPeerModification;
    }

    public boolean supportsRouting() {
        return bSupportsRouting;
    }

    public boolean supportsRoutingPolicyManagement() {
        return bSupportsRoutingPolicyManagement;
    }

    public boolean supportsCoinControl() {
        return bSupportsCoinControl;
    }

    public boolean supportsBalanceDetails() {
        return bSupportsBalanceDetails;
    }

    public boolean supportsMessageSigningByNodePrivateKey() {
        return bSupportsMessageSigningByNodePrivateKey;
    }

    public boolean supportsLnurlAuth() {
        return bSupportsLnurlAuth;
    }

    public boolean supportsKeysend() {
        return bSupportsKeysend;
    }

    public boolean supportsOnChainFeeEstimation() {
        return bSupportsOnChainFeeEstimation;
    }

    public boolean supportsAbsoluteOnChainFeeEstimation() {
        return bSupportsAbsoluteOnChainFeeEstimation;
    }


    public boolean supportsRoutingFeeEstimation() {
        return bSupportsRoutingFeeEstimation;
    }

    public boolean supportsOnChainSending() {
        return bSupportsOnChainSending;
    }

    public boolean supportsBolt11Sending() {
        return bSupportsBolt11Sending;
    }

    public boolean supportsBolt12Sending() {
        return bSupportsBolt12Sending;
    }

    public boolean supportsOnChainReceive() {
        return bSupportsOnChainReceive;
    }

    public boolean supportsBolt11Receive() {
        return bSupportsBolt11Receive;
    }

    public boolean supportsIdentityScreen() {
        return bSupportsIdentityScreen;
    }

    public boolean supportsBolt11WithoutAmount() {
        return bSupportsBolt11WithoutAmount;
    }

    public boolean supportsEventSubscriptions() {
        return bSupportsEventSubscription;
    }

    public boolean supportsWatchtowers() {
        return bSupportsWatchtowers;
    }
}
