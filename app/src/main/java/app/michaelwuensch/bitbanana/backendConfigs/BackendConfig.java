package app.michaelwuensch.bitbanana.backendConfigs;

import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import app.michaelwuensch.bitbanana.labels.Labels;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import app.michaelwuensch.bitbanana.wallet.QuickReceiveConfig;

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
     * This string contains all information that we need to connect to the backend.
     * It is only intended to be used for backends where we never get the information in a separate manner but only in a connect string.
     * An example for this is NWC.
     */
    private String fullConnectString;

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
     * The walletID is a unique id on a per wallet basis, not on a connection basis.
     * You can for example have two connections in BitBanana both connecting to the same LND instance.
     * For example one over Tor and the other over Tailscale.
     * The WalletID can for example be used to display an Avatar images or to reference labels.
     */
    private String walletID;

    /**
     * DEPRECATED
     */
    private String avatarMaterial;

    /**
     * The quickReceiveConfig is used by BitBanana to show a default receive QR Code instead of going through lots of options first..
     */
    private QuickReceiveConfig quickReceiveConfig;

    /**
     * The labels the user gave to transactions and UTXOs, etc.
     */
    private Labels labels;

    /**
     * Whether or not this backend was added in emergency mode. This is used to filter the wallets list so that it is impossible to tell if you are in emergency mode or not.
     */
    private Boolean addedInEmergencyMode;


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

    public String getFullConnectString() {
        return this.fullConnectString;
    }

    public void setFullConnectString(String fullConnectString) {
        this.fullConnectString = fullConnectString;
    }

    public String getHost() {
        return host;
    }

    /* This allows us to fake replace a host with another, which is useful when you have saved all your node connections with a local IP that changed later.
     *  If no override is present, the original host will be returned
     */
    public String getHostWithOverride() {
        if (host == null)
            return null;
        String source = PrefsUtil.getPrefs().getString("overrideHostSource", "");
        if (source.isEmpty())
            return host;
        String target = PrefsUtil.getPrefs().getString("overrideHostTarget", "");
        return host.replace(source, target);
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

    public String getWalletID() {
        return walletID;
    }

    public void setWalletID(String walletID) {
        this.walletID = walletID;
    }

    public String getAvatarMaterial() {
        return avatarMaterial;
    }

    public void setAvatarMaterial(String avatarMaterial) {
        this.avatarMaterial = avatarMaterial;
    }

    public QuickReceiveConfig getQuickReceiveConfig() {
        if (this.quickReceiveConfig == null)
            return new QuickReceiveConfig();
        else
            return this.quickReceiveConfig;
    }

    public boolean hasQuickReceiveConfig() {
        return this.quickReceiveConfig != null;
    }

    public void setQuickReceiveConfig(QuickReceiveConfig quickReceiveConfig) {
        this.quickReceiveConfig = quickReceiveConfig;
    }

    public boolean hasAddedInEmergencyMode() {
        return this.addedInEmergencyMode != null;
    }

    public boolean wasAddedInEmergencyMode() {
        if (this.addedInEmergencyMode == null)
            return false;
        return this.addedInEmergencyMode;
    }

    public void setAddedInEmergencyMode(boolean addedInEmergencyMode) {
        this.addedInEmergencyMode = addedInEmergencyMode;
    }

    public boolean isLocal() {
        if (this.location != null)
            return this.location == Location.LOCAL;
        return false;
    }

    public boolean isHostAddressTor() {
        if (getHostWithOverride() != null)
            return this.getHostWithOverride().toLowerCase().contains(".onion");
        return false;
    }

    public boolean isTorHostAddress() {
        return RemoteConnectUtil.isTorHostAddress(getHostWithOverride());
    }

    public BackendConfig getCopy() {
        BackendConfig copy = new BackendConfig();
        copy.setId(getId());
        copy.setLocation(getLocation());
        copy.setNetwork(getNetwork());
        copy.setFullConnectString(getFullConnectString());
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
        copy.setWalletID(getWalletID());
        copy.setAvatarMaterial(getAvatarMaterial());
        copy.setQuickReceiveConfig(getQuickReceiveConfig());
        copy.setAddedInEmergencyMode(wasAddedInEmergencyMode());
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
        LND_HUB,
        NOSTR_WALLET_CONNECT;

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
                case NOSTR_WALLET_CONNECT:
                    return "Nostr Wallet Connect";
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
        BTC_PAY_DATA,
        NOSTR_WALLET_CONNECT;
    }
}
