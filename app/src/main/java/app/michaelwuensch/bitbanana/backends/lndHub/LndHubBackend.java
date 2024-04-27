package app.michaelwuensch.bitbanana.backends.lndHub;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Backend;

public class LndHubBackend extends Backend {

    public LndHubBackend(BackendConfig backendConfig) {
        // General
        mApi = new LndHubApi();
        mBackendConfig = backendConfig;
        mNodeImplementationName = "LNDHub";

        // Features
        bSupportsBolt11Receive = true;
        bSupportsBolt11Sending = !(isLnBits() && backendConfig.getUser().equals("invoice"));
        bSupportsOnChainReceive = !(isLnBits() || isAlby());
    }

    private boolean isLnBits() {
        return mBackendConfig.getHost().contains("/lndhub/ext/");
    }

    private boolean isAlby() {
        return mBackendConfig.getHost().contains("alby");
    }
}
