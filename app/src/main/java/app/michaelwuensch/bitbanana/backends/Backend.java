package app.michaelwuensch.bitbanana.backends;

import app.michaelwuensch.bitbanana.util.Version;

public class Backend {

    protected Api mApi = new Api();
    protected Version mMinRequiredVersion = new Version("0.0.0");
    protected String mMinRequiredVersionName = "v0.0.0-beta";
    protected String mNodeImplementationName = "UNKNOWN";
    protected boolean bSupportsChannelManagement = false;
    protected boolean bSupportsOpenChannel = false;
    protected boolean bSupportsCloseChannel = false;
    protected boolean bSupportsPeerManagement = false;
    protected boolean bSupportsRouting = false;
    protected boolean bSupportsRoutingPolicyManagement = false;
    protected boolean bSupportsCoinControl = false;
    protected boolean bSupportsBalanceDetails = false;
    protected boolean bSupportsMessageSigningByNodePrivateKey = false;


    public Backend() {
    }

    public Api api() {
        return mApi;
    }

    public Version getMinRequiredVersion() {
        return mMinRequiredVersion;
    }

    public String getMinRequiredVersionName() {
        return mMinRequiredVersionName;
    }

    public String getNodeImplementationName() {
        return mNodeImplementationName;
    }

    public boolean supportsChannelManagement() {
        return bSupportsChannelManagement;
    }

    public boolean supportsOpenChannel() {
        return bSupportsOpenChannel;
    }

    public boolean supportsCloseChannel() {
        return bSupportsCloseChannel;
    }

    public boolean supportsPeerManagement() {
        return bSupportsPeerManagement;
    }

    public boolean supportsRouting() {
        return bSupportsRouting;
    }

    public boolean supportsRoutingPolicyManagement() {
        return bSupportsRoutingPolicyManagement;
    }

    public boolean supportsCoinControl() {
        return bSupportsCoinControl;
    }

    public boolean supportsBalanceDetails() {
        return bSupportsBalanceDetails;
    }

    public boolean supportsMessageSigningByNodePrivateKey() {
        return bSupportsMessageSigningByNodePrivateKey;
    }
}
