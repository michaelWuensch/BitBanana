package app.michaelwuensch.bitbanana.backendConfigs;

import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;

/**
 * A BackendConfig contains all information that BitBanana needs to connect to a backend.
 */
public class BackendConfig implements Comparable<BackendConfig> {

    /**
     * This variable is used to store the information where we parsed the connect info from.
     * As this value is transient, we do not save it to the json.
     */
    private transient Source source;

    /**
     * The id is used in BitBanana to identify a backend config. It is a unique UUID.
     */
    private String id;

    /**
     * The alias is used to make organization in BitBanana simple.
     * It just defines the name that is shown in the wallet list an has no other effect.
     */
    private String alias;

    /**
     * The backend type we use. Depending on what is chosen here some of the fields below might not be used.
     */
    private BackendType backendType;

    /**
     * The host where the backend is available.
     */
    private String host;

    /**
     * The port on which the backend exposes the API that we want to access
     */
    private int port;

    /**
     * Is the backend accessed remotely or locally on the device?
     */
    private Location location;

    /**
     * The bitcoin network our backend is connected to.
     */
    private Network network;

    /**
     * The certificate of the server (node we connect to) to authenticate to the client in mutual TLS
     * The certificate is encoded as Base64 DER string.
     */
    private String cert;

    /**
     * The certificate for the client to authenticate to the Server in mutual TLS
     * The certificate is encoded as Base64 DER string.
     */
    private String clientCert;

    /**
     * The private key for the client to authenticate to the Server in mutual TLS
     * The private key is encoded as Base64 DER string.
     */
    private String clientKey;

    /**
     * DEPRECATED!!! This allows for fine grained api restriction. E.g. only allow to create invoices etc.
     * LND calls this "macaroon".
     * CoreLightning calls this "rune".
     */
    private String macaroon;

    /**
     * This allows for fine grained api restriction. E.g. only allow to create invoices etc.
     * LND calls this "macaroon".
     * CoreLightning calls this "rune".
     * <p>
     * The data is saved as HEX string.
     */
    private String authenticationToken;

    /**
     * Username for APIs that require a user & password authentication
     */
    private String user;

    /**
     * Password for APIs that require a user & password authentication
     */
    private String password;

    /**
     * The VPNConfig is used by BitBanana to automatically handle VPN connection.
     * It is an optional convenience setting, VPNs can be used without setting this up.
     * The user then has to manually connect to the VPN.
     */
    private VPNConfig vpnConfig;

    /**
     * Whether or not tor is used to connect to the backend.
     */
    private boolean UseTor;

    /**
     * Whether or not the server certificate should be verified by the client.
     */
    private boolean VerifyCertificate;

    /**
     * A place to save the accessToken for a connection that have an authentication flow with tokens.
     */
    private String tempAccessToken;

    /**
     * A place to save the refreshToken for a connection that have an authentication flow with tokens.
     */
    private String tempRefreshToken;

    /**
     * This allows us to display avatars on the node management screen. We fill this information after getting the GetCurrentNodeInfo information.
     */
    private String avatarMaterial;


    public BackendConfig() {

    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

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

    public String getServerCert() {
        return cert;
    }

    public void setServerCert(String serverCert) {
        this.cert = serverCert;
    }

    public String getClientCert() {
        return clientCert;
    }

    public void setClientCert(String clientCert) {
        this.clientCert = clientCert;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getMacaroon() {
        return macaroon;
    }

    public void setMacaroon(String macaroon) {
        this.macaroon = macaroon;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
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

    public VPNConfig getVpnConfig() {
        if (this.vpnConfig == null)
            return new VPNConfig();
        else
            return this.vpnConfig;
    }

    public boolean hasVpnConfig() {
        return this.vpnConfig != null;
    }

    public void setVpnConfig(VPNConfig vpnConfig) {
        this.vpnConfig = vpnConfig;
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

    public String getTempAccessToken() {
        return tempAccessToken;
    }

    public void setTempAccessToken(String tempAccessToken) {
        this.tempAccessToken = tempAccessToken;
    }

    public String getTempRefreshToken() {
        return tempRefreshToken;
    }

    public void setTempRefreshToken(String tempRefreshToken) {
        this.tempRefreshToken = tempRefreshToken;
    }

    public String getAvatarMaterial() {
        return avatarMaterial;
    }

    public void setAvatarMaterial(String avatarMaterial) {
        this.avatarMaterial = avatarMaterial;
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

    public boolean isTorHostAddress() {
        return RemoteConnectUtil.isTorHostAddress(getHost());
    }

    public BackendConfig getCopy() {
        BackendConfig copy = new BackendConfig();
        copy.setId(getId());
        copy.setLocation(getLocation());
        copy.setNetwork(getNetwork());
        copy.setHost(getHost());
        copy.setPort(getPort());
        copy.setAlias(getAlias());
        copy.setAuthenticationToken(getAuthenticationToken());
        copy.setServerCert(getServerCert());
        copy.setUser(getUser());
        copy.setPassword(getPassword());
        copy.setUseTor(getUseTor());
        copy.setVerifyCertificate(getVerifyCertificate());
        copy.setVpnConfig(getVpnConfig());
        copy.setBackendType(getBackendType());
        copy.setClientCert(getClientCert());
        copy.setClientKey(getClientKey());
        copy.setTempAccessToken(getTempAccessToken());
        copy.setTempRefreshToken(getTempRefreshToken());
        copy.setAvatarMaterial(getAvatarMaterial());
        return copy;
    }

    @Override
    public int compareTo(BackendConfig BackendConfig) {
        BackendConfig other = BackendConfig;
        return this.getAlias().toLowerCase().compareTo(other.getAlias().toLowerCase());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        BackendConfig BackendConfig = (BackendConfig) obj;
        return BackendConfig.getId().equals(this.getId());
    }

    @Override
    public int hashCode() {
        if (this.id != null) {
            return this.id.hashCode();
        } else {
            return this.alias.hashCode();
        }
    }

    public enum BackendType {
        NONE,
        LND_GRPC,
        CORE_LIGHTNING_GRPC,
        LND_HUB;

        public static BackendType parseFromString(String enumAsString) {
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
                    return "LndHub";
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

        public static Network parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString.toUpperCase());
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
                    return "Regtest";
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

        public static Location parseFromString(String enumAsString) {
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

    public enum Source {
        UNKNOWN,
        MANUAL_INPUT,
        LND_CONNECT,
        CLN_GRPC,
        LND_HUB_CONNECT,
        BTC_PAY_DATA;
    }
}
