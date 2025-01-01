package app.michaelwuensch.bitbanana.backends.lndHub;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.backends.BackendFeature;

public class LndHubBackend extends Backend {

    public LndHubBackend(BackendConfig backendConfig) {
        // General
        mApi = new LndHubApi();
        mBackendConfig = backendConfig;
        mNodeImplementationName = "LNDHub";

        // Features
        FeatureBolt11Receive = new BackendFeature(true);
        FeatureBolt11Sending = new BackendFeature(!(isLnBits() && backendConfig.getUser().equals("invoice")));
        FeatureOnChainReceive = new BackendFeature(!(isLnBits() || isAlby()));
    }

    private boolean isLnBits() {
        return mBackendConfig.getHost().contains("/lndhub/ext/");
    }

    private boolean isAlby() {
        return mBackendConfig.getHost().contains("alby");
    }
}
