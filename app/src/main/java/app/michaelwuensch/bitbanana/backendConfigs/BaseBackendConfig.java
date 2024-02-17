package app.michaelwuensch.bitbanana.backendConfigs;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;

/**
 * Base class meant to be extended for more specific backend configurations like
 * - BTCPay Configuration
 * - LndConnect Configuration
 * <p>
 * The macaroon should always be encoded as base16 string (hex)
 */
public abstract class BaseBackendConfig {

    private BackendType backendType;
    private String host;
    private int port;
    private Location location;
    private Network network;
    private String cert;
    private String macaroon;
    private String user;
    private String password;
    private boolean UseTor;
    private boolean VerifyCertificate;


    public BackendType getBackendType() {
        return this.backendType;
    }

    public void setBackendType(BackendType backendType) {
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

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Network getNetwork() {
        return this.network;
    }

    public void setNetwork(Network network) {
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
            return this.location == Location.LOCAL;
        return false;
    }

    public boolean isHostAddressTor() {
        if (getHost() != null)
            return this.getHost().toLowerCase().contains(".onion");
        return false;
    }

    public enum BackendType {
        NONE,
        LND_GRPC,
        CORE_LIGHTNING_GRPC,
        LND_HUB;

        public static BaseBackendConfig.BackendType parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return NONE;
            }
        }

        public String getDisplayName() {
            switch (this) {
                case LND_GRPC:
                    return "LND (gRPC)";
                case CORE_LIGHTNING_GRPC:
                    return "Core Lightning (gRPC)";
                case LND_HUB:
                    return "LNDHub";
                default:
                    return App.getAppContext().getString(R.string.none);
            }
        }
    }

    public enum Network {
        UNKNOWN,
        MAINNET,
        TESTNET,
        REGTEST,
        SIGNET;

        public static BaseBackendConfig.Network parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return UNKNOWN;
            }
        }

        public String getDisplayName() {
            switch (this) {
                case MAINNET:
                    return "Mainnet";
                case TESTNET:
                    return "Testnet";
                case REGTEST:
                    return "RegTest";
                case SIGNET:
                    return "Signet";
                default:
                    return App.getAppContext().getString(R.string.unknown);
            }
        }
    }

    public enum Location {
        LOCAL,
        REMOTE;

        public static BaseBackendConfig.Location parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return REMOTE;
            }
        }

        public String getDisplayName() {
            switch (this) {
                case LOCAL:
                    return "Local";
                default:
                    return "Remote";
            }
        }
    }
}
