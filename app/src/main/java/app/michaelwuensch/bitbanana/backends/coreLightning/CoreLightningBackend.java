package app.michaelwuensch.bitbanana.backends.coreLightning;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.util.Version;

public class CoreLightningBackend extends Backend {

    public CoreLightningBackend(BackendConfig backendConfig) {

        // General
        mApi = new CoreLightningApi();
        mNodeImplementationName = "Core Lightning";
        mMinRequiredVersion = new Version("24.02.2");
        mMinRequiredVersionName = "v24.02.2";

        // Features
        bSupportsBolt11Receive = true;
        bSupportsBolt11Sending = true;
        bSupportsOnChainReceive = true;
        bSupportsOnChainSending = true;
        bSupportsBalanceDetails = true;
        bSupportsChannelManagement = true;
        bSupportsMessageSigningByNodePrivateKey = true;
        bSupportsPeerManagement = true;
        bSupportsPeerModification = true;
        bSupportsRouting = true;
        bSupportsRoutingPolicyManagement = true;
        bSupportsOpenChannel = true;
        bSupportsCloseChannel = true;
        bSupportsCoinControl = true;
        bSupportsLnurlAuth = true;
        bSupportsKeysend = true;
        bSupportsIdentityScreen = true;
        bSupportsBolt11WithoutAmount = true;
    }
}
