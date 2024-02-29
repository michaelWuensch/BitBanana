package app.michaelwuensch.bitbanana.backendConfigs;

import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;

/**
 * This class extends the BaseBackendConfig with information needed by the app for switching between backends.
 * This is what finally gets saved to disk when a node is connected. Basically, a parsed backendConfig of a specific type
 * is always first converted into a BackendConfig before being saved.
 */
public class BackendConfig extends BaseBackendConfig implements Comparable<BackendConfig> {
    private String id;
    private String alias;

    private VPNConfig vpnConfig;

    public String getId() {
        return this.id;
    }

    public String getAlias() {
        return this.alias;
    }

    public VPNConfig getVpnConfig() {
        return this.vpnConfig;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setVpnConfig(VPNConfig vpnConfig) {
        this.vpnConfig = vpnConfig;
    }

    public BackendConfig() {

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
        copy.setMacaroon(getMacaroon());
        copy.setServerCert(getServerCert());
        copy.setUser(getUser());
        copy.setPassword(getPassword());
        copy.setUseTor(getUseTor());
        copy.setVerifyCertificate(getVerifyCertificate());
        copy.setVpnConfig(getVpnConfig());
        copy.setBackendType(getBackendType());
        copy.setClientCert(getClientCert());
        copy.setClientKey(getClientKey());
        return copy;
    }

    @Override
    public int compareTo(BackendConfig BackendConfig) {
        BackendConfig other = BackendConfig;
        return this.getAlias().compareTo(other.getAlias());
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
}
