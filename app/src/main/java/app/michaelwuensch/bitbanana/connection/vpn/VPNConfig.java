package app.michaelwuensch.bitbanana.connection.vpn;

/**
 * Class to store vpn information for a BackendConfig.
 * With this information a VPN connection can be automated for a smooth user experience.
 */
public class VPNConfig {

    public final static String VPN_NAME_TAILSCALE = "tailscale";
    public final static String VPN_NAME_WIREGUARD = "wireguard";

    private String vpnName;
    private String tunnelName;
    private boolean startVPNOnOpen = true;
    private boolean stopVPNOnClose = true;


    public String getVpnName() {
        return this.vpnName;
    }

    public void setVpnName(String vpnName) {
        this.vpnName = vpnName;
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

}
