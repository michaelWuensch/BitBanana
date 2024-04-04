package app.michaelwuensch.bitbanana.backendConfigs.btcPay;

public class BTCPayConfig {

    public static String TYPE_GRPC = "GRPC";
    public static String CRYPTO_TYPE_BTC = "BTC";

    private String host;
    private int port;
    private String macaroon;
    private String type;
    private String cryptoCode;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getMacaroon() {
        return macaroon;
    }

    public String getType() {
        return this.type;
    }

    public String getCryptoCode() {
        return this.cryptoCode;
    }
}
