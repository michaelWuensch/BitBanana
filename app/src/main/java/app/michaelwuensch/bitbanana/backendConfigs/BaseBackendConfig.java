package app.michaelwuensch.bitbanana.backendConfigs;

/**
 * Base class meant to be extended for more specific backend configurations like
 * - BTCPay Configuration
 * - LndConnect Configuration
 * <p>
 * The macaroon should always be encoded as base16 string (hex)
 */
public abstract class BaseBackendConfig {

    public static final String BACKEND_TYPE_NONE = "none";
    public static final String BACKEND_TYPE_LND_GRPC = "lnd-grpc";
    public static final String BACKEND_TYPE_CORE_LIGHTNING_GRPC = "core-lightning-grpc";
    public static final String LOCATION_LOCAL = "local";
    public static final String LOCATION_REMOTE = "remote";
    public static final String NETWORK_UNKNOWN = "unknown";
    public static final String NETWORK_MAINNET = "mainnet";
    public static final String NETWORK_TESTNET = "testnet";
    public static final String NETWORK_REGTEST = "regtest";
    public static final String NETWORK_SIGNET = "signet";

    private String backendType;
    private String host;
    private int port;
    private String location;
    private String network;
    private String cert;
    private String macaroon;
    private boolean UseTor;
    private boolean VerifyCertificate;


    public String getBackendType() {
        return this.backendType;
    }

    public void setBackendType(String backendType) {
        this.backendType = backendType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNetwork() {
        return this.network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public String getMacaroon() {
        return macaroon;
    }

    public void setMacaroon(String macaroon) {
        this.macaroon = macaroon;
    }

    public boolean getUseTor() {
        return this.UseTor;
    }

    public void setUseTor(boolean useTor) {
        this.UseTor = useTor;
    }

    public boolean getVerifyCertificate() {
        return this.VerifyCertificate;
    }

    public void setVerifyCertificate(boolean verifyCertificate) {
        this.VerifyCertificate = verifyCertificate;
    }

    public boolean isLocal() {
        if (this.location != null)
            return this.location.equals(LOCATION_LOCAL);
        return false;
    }

    public boolean isHostAddressTor() {
        if (getHost() != null)
            return this.getHost().toLowerCase().contains(".onion");
        return false;
    }
}
