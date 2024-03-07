package app.michaelwuensch.bitbanana.backends;

public class Backend {

    protected boolean bSupportsChannelManagement = false;
    protected boolean bSupportsPeerManagement = false;
    protected boolean bSupportsRouting = false;
    protected boolean bSupportsRoutingPolicyManagement = false;
    protected boolean bSupportsCoinControl = false;
    protected boolean bSupportsBalanceDetails = false;
    protected boolean bSupportsMessageSigningByNodePrivateKey = false;


    public Backend() {
    }


    public boolean supportsChannelManagement() {
        return bSupportsChannelManagement;
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
