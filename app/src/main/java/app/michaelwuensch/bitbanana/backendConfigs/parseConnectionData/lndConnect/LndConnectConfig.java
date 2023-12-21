package app.michaelwuensch.bitbanana.backendConfigs.parseConnectionData.lndConnect;

import app.michaelwuensch.bitbanana.backendConfigs.BaseNodeConfig;

public class LndConnectConfig extends BaseNodeConfig {

    private String cert;

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }
}
