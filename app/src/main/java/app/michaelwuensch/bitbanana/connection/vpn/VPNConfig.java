package app.michaelwuensch.bitbanana.connection.vpn;

import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;

/**
 * Class to store vpn information for a BackendConfig.
 * With this information a VPN connection can be automated for a smooth user experience.
 */
public class VPNConfig {

    private VPNType vpnType = VPNType.NONE;
    private String tunnelName;
    private boolean startVPNOnOpen = true;
    private boolean stopVPNOnClose = true;


    public VPNType getVpnType() {
        return this.vpnType;
    }

    public void setVpnType(VPNType vpnType) {
        this.vpnType = vpnType;
    }

    public String getTunnelName() {
        return this.tunnelName;
    }

    public void setTunnelName(String tunnelName) {
        this.tunnelName = tunnelName;
    }

    public boolean getStartVPNOnOpen() {
        return this.startVPNOnOpen;
    }

    public void setStartVPNOnOpen(boolean startVPNOnOpen) {
        this.startVPNOnOpen = startVPNOnOpen;
    }

    public boolean getStopVPNOnClose() {
        return this.stopVPNOnClose;
    }

    public void setStopVPNOnClose(boolean stopVPNOnClose) {
        this.stopVPNOnClose = stopVPNOnClose;
    }

    public boolean isSameVPN(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        VPNConfig vpnConfig = (VPNConfig) obj;
        return (vpnConfig.getVpnType() == this.getVpnType() && vpnConfig.getTunnelName() == this.getTunnelName());
    }

    public enum VPNType {
        NONE,
        TAILSCALE,
        WIREGUARD;

        public static VPNConfig.VPNType parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return NONE;
            }
        }

        public String getDisplayName() {
            switch (this) {
                case TAILSCALE:
                    return "Tailscale";
                case WIREGUARD:
                    return "WireGuard";
                default:
                    return App.getAppContext().getString(R.string.vpn_no_vpn);
            }
        }
    }
}
