package app.michaelwuensch.bitbanana.connection.parseConnectionData.lndConnect;

import app.michaelwuensch.bitbanana.connection.BaseNodeConfig;

public class LndConnectConfig extends BaseNodeConfig {

    private String cert;

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }
}
