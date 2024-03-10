package app.michaelwuensch.bitbanana.backends.coreLightning;

import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.util.Version;

public class CoreLightningBackend extends Backend {

    public CoreLightningBackend() {

        // General
        mApi = new CoreLightningApi();
        mNodeImplementationName = "Core Lightning";
        mMinRequiredVersion = new Version("23.05.2");
        mMinRequiredVersionName = "v23.05.2";

        // Features
        bSupportsBalanceDetails = true;
        bSupportsMessageSigningByNodePrivateKey = true;
        bSupportsCoinControl = true;
    }
}
