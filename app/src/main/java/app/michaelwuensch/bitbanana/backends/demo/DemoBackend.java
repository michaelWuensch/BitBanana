package app.michaelwuensch.bitbanana.backends.demo;

import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.backends.lnd.LndApi;

/**
 * This backend is used when noting is connected yet. It basically defines what the user can se when he starts the app for the first time.
 */
public class DemoBackend extends Backend {
    public DemoBackend() {

        // General
        mApi = new LndApi();

        // Features
        bSupportsChannelManagement = true;
        bSupportsOpenChannel = true;
        bSupportsPeerManagement = true;
        bSupportsRouting = true;
        bSupportsRoutingPolicyManagement = true;
        bSupportsCoinControl = true;
        bSupportsMessageSigningByNodePrivateKey = true;
    }
}
