package app.michaelwuensch.bitbanana.backends.demo;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.Backend;

/**
 * This backend is used when noting is connected yet. It basically defines what the user can se when he starts the app for the first time.
 */
public class DemoBackend extends Backend {
    public DemoBackend(BackendConfig backendConfig) {

        // General
        mApi = new Api();

        // Features
        bSupportsChannelManagement = true;
        bSupportsOpenChannel = true;
        bSupportsPeerManagement = true;
        bSupportsPeerModification = true;
        bSupportsRouting = true;
        bSupportsRoutingPolicyManagement = true;
        bSupportsCoinControl = true;
        bSupportsMessageSigningByNodePrivateKey = true;
        bSupportsWatchtowers = true;
    }
}
