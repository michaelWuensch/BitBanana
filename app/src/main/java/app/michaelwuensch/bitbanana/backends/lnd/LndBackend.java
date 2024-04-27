package app.michaelwuensch.bitbanana.backends.lnd;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.util.Version;

public class LndBackend extends Backend {

    public LndBackend(BackendConfig backendConfig) {

        // General
        mApi = new LndApi();
        mNodeImplementationName = "LND";
        mMinRequiredVersion = new Version("0.17.0");
        mMinRequiredVersionName = "v0.17.0-beta";

        // Features
        bSupportsBolt11Receive = true;
        bSupportsBolt11Sending = true;
        bSupportsOnChainReceive = true;
        bSupportsOnChainSending = true;
        bSupportsChannelManagement = true;
        bSupportsOpenChannel = true;
        bSupportsCloseChannel = true;
        bSupportsPeerManagement = true;
        bSupportsRouting = true;
        bSupportsRoutingPolicyManagement = true;
        bSupportsCoinControl = true;
        bSupportsBalanceDetails = true;
        bSupportsMessageSigningByNodePrivateKey = true;
        bSupportsLnurlAuth = true;
        bSupportsKeysend = true;
        bSupportsOnChainFeeEstimation = true;
        bSupportsAbsoluteOnChainFeeEstimation = true;
        bSupportsRoutingFeeEstimation = true;
        bSupportsIdentityScreen = true;
        bSupportsBolt11WithoutAmount = true;
    }
}