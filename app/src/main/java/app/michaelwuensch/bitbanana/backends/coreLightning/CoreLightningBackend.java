package app.michaelwuensch.bitbanana.backends.coreLightning;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.backends.BackendFeature;
import app.michaelwuensch.bitbanana.util.Version;

public class CoreLightningBackend extends Backend {

    public CoreLightningBackend(BackendConfig backendConfig) {

        // General
        mApi = new CoreLightningApi();
        mNodeImplementationName = "Core Lightning";
        mMinRequiredVersion = new Version("24.02.2");
        mMinRequiredVersionName = "v24.02.2";

        // Features
        FeatureBolt11Receive = new BackendFeature(true);
        FeatureBolt11Sending = new BackendFeature(true);
        FeatureBolt12Receive = new BackendFeature(true);
        FeatureBolt12Sending = new BackendFeature(true);
        FeatureOnChainReceive = new BackendFeature(true);
        FeatureOnChainSending = new BackendFeature(true);
        FeatureBalanceDetails = new BackendFeature(true);
        FeatureChannelManagement = new BackendFeature(true);
        FeatureMessageSigningByNodePrivateKey = new BackendFeature(true);
        FeaturePeerManagement = new BackendFeature(true);
        FeaturePeerModification = new BackendFeature(true);
        FeatureRouting = new BackendFeature(true);
        FeatureRoutingPolicyManagement = new BackendFeature(true);
        FeatureOpenChannel = new BackendFeature(true);
        FeatureCloseChannel = new BackendFeature(true);
        FeatureCoinControl = new BackendFeature(true);
        FeatureLnurlAuth = new BackendFeature(true);
        FeatureKeysend = new BackendFeature(true);
        FeatureIdentityScreen = new BackendFeature(true);
        FeatureBolt11WithoutAmount = new BackendFeature(true);
    }
}
