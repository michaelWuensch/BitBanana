package app.michaelwuensch.bitbanana.backends.lnd;

import app.michaelwuensch.bitbanana.backends.Backend;

public class LndBackend extends Backend {

    public LndBackend() {
        bSupportsChannelManagement = true;
        bSupportsPeerManagement = true;
        bSupportsRouting = true;
        bSupportsRoutingPolicyManagement = true;
        bSupportsCoinControl = true;
        bSupportsBalanceDetails = true;
        bSupportsMessageSigningByNodePrivateKey = true;
    }
}
