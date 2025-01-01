package app.michaelwuensch.bitbanana.backends.demo;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.backends.BackendFeature;

/**
 * This backend is used when noting is connected yet. It basically defines what the user can se when he starts the app for the first time.
 */
public class DemoBackend extends Backend {
    public DemoBackend(BackendConfig backendConfig) {

        // General
        mApi = new Api();

        // Features
        FeatureChannelManagement = new BackendFeature(true);
        FeatureOpenChannel = new BackendFeature(true);
        FeaturePeerManagement = new BackendFeature(true);
        FeaturePeerModification = new BackendFeature(true);
        FeatureRouting = new BackendFeature(true);
        FeatureRoutingPolicyManagement = new BackendFeature(true);
        FeatureCoinControl = new BackendFeature(true);
        FeatureMessageSigningByNodePrivateKey = new BackendFeature(true);
        FeatureWatchtowers = new BackendFeature(true);
        FeatureBolt12Receive = new BackendFeature(true);
    }
}
