package app.michaelwuensch.bitbanana.backends.nostrWalletConnect;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.backends.BackendFeature;

public class NostrWalletConnectBackend extends Backend {

    public NostrWalletConnectBackend(BackendConfig backendConfig) {
        // General
        mApi = new NostrWalletConnectApi();
        mBackendConfig = backendConfig;
        mNodeImplementationName = "Nostr Wallet Connect";

        // Features
        FeatureBolt11Receive = new BackendFeature(true);
        FeatureBolt11Sending = new BackendFeature(true);
        FeatureQuickReceive = new BackendFeature(true);
        FeatureQuickReceiveLnAddress = new BackendFeature(true);

        /* Further features are set later when the info response is received as this depends on the implementation of the service.
         *
         *  Features that are updated later:
         *  - Keysend
         */
    }
}
