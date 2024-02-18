package app.michaelwuensch.bitbanana.connection.vpn;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.PowerManager;

import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PermissionsUtil;

public class VPNUtil {

    private static final String LOG_TAG = VPNUtil.class.getSimpleName();

    private static final String APP_NAME_TAILSCALE = "Tailscale";
    private static final String PACKAGE_TAILSCALE = "com.tailscale.ipn";
    private static final String ACTION_START_TAILSCALE = "com.tailscale.ipn.CONNECT_VPN";
    private static final String ACTION_STOP_TAILSCALE = "com.tailscale.ipn.DISCONNECT_VPN";

    private static final String APP_NAME_WIREGUARD = "WireGuard";
    private static final String PACKAGE_WIREGUARD = "com.wireguard.android";
    public static final String PERMISSION_WIREGUARD = "com.wireguard.android.permission.CONTROL_TUNNELS";
    public static final int PERMISSION_WIREGUARD_REQUEST_CODE = 1;
    private static final String ACTION_START_WIREGUARD = "com.wireguard.android.action.SET_TUNNEL_UP";
    private static final String ACTION_STOP_WIREGUARD = "com.wireguard.android.action.SET_TUNNEL_DOWN";

    public static void startVPN(VPNConfig vpnConfig, Context context) {
        if (vpnConfig != null) {
            switch (vpnConfig.getVpnType()) {
                case TAILSCALE:
                    startTailscale(context);
                    break;
                case WIREGUARD:
                    startWireGuardTunnel(context, vpnConfig.getTunnelName());
                    break;
            }
        }
    }

    public static void stopVPN(VPNConfig vpnConfig, Context context) {
        if (vpnConfig != null) {
            switch (vpnConfig.getVpnType()) {
                case TAILSCALE:
                    stopTailscale(context);
                    break;
                case WIREGUARD:
                    stopWireGuardTunnel(context, vpnConfig.getTunnelName());
                    break;
            }
        }
    }

    private static void startTailscale(Context context) {
        BBLog.d(LOG_TAG, "Starting VPN (Tailscale)");
        Intent intent = new Intent();
        intent.setAction(ACTION_START_TAILSCALE);
        intent.setPackage(PACKAGE_TAILSCALE);
        context.sendBroadcast(intent);
    }

    private static void stopTailscale(Context context) {
        BBLog.d(LOG_TAG, "Stopping VPN (Tailscale)");
        Intent intent = new Intent();
        intent.setAction(ACTION_STOP_TAILSCALE);
        intent.setPackage(PACKAGE_TAILSCALE);
        context.sendBroadcast(intent);
    }

    private static void startWireGuardTunnel(Context context, String tunnelName) {
        if (hasPermissionToControlWireguard(context)) {
            BBLog.d(LOG_TAG, "Starting VPN (WireGuard, Tunnel: " + tunnelName + ")");
            Intent intent = new Intent();
            intent.setAction(ACTION_START_WIREGUARD);
            intent.setPackage(PACKAGE_WIREGUARD);
            intent.putExtra("tunnel", tunnelName);
            context.sendBroadcast(intent);
        }
    }

    private static void stopWireGuardTunnel(Context context, String tunnelName) {
        if (hasPermissionToControlWireguard(context)) {
            BBLog.d(LOG_TAG, "Stopping VPN (WireGuard, Tunnel" + tunnelName + ")");
            Intent intent = new Intent();
            intent.setAction(ACTION_STOP_WIREGUARD);
            intent.setPackage(PACKAGE_WIREGUARD);
            intent.putExtra("tunnel", tunnelName);
            context.sendBroadcast(intent);
        }
    }

    public static boolean isVpnAppInstalled(VPNConfig vpnConfig, Context ctx) {
        if (vpnConfig != null) {
            switch (vpnConfig.getVpnType()) {
                case NONE:
                    return true;
                case TAILSCALE:
                    return isAppInstalled(ctx, PACKAGE_TAILSCALE, APP_NAME_TAILSCALE);
                case WIREGUARD:
                    return isAppInstalled(ctx, PACKAGE_WIREGUARD, APP_NAME_WIREGUARD);
            }
        }
        return true;
    }

    private static boolean isAppInstalled(Context ctx, String package_name, String app_name) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo info = pm.getPackageInfo("" + package_name, PackageManager.GET_META_DATA);
            BBLog.v(LOG_TAG, app_name + " is installed.");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            BBLog.w(LOG_TAG, app_name + " is not installed.");
            return false;
        }
    }

    /**
     * Check whether or not the vpn app associated with the vpnConfig ignores battery optimization.
     * <p>
     * Unfortunately this function always returned true on the S22 test device. Therefore we do not use it.
     *
     * @param vpnConfig The vpnConfig
     * @param context   The application context.
     */
    public static boolean isVpnAppIgnoringBatteryOptimization(VPNConfig vpnConfig, Context context) {
        if (vpnConfig != null) {
            switch (vpnConfig.getVpnType()) {
                case NONE:
                    return true;
                case TAILSCALE:
                    isAppIgnoringBatteryOptimization(context, PACKAGE_TAILSCALE);
                    break;
                case WIREGUARD:
                    isAppIgnoringBatteryOptimization(context, PACKAGE_WIREGUARD);
                    break;
            }
        }
        return true;
    }

    private static boolean isAppIgnoringBatteryOptimization(Context context, String PACKAGE_NAME) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(PACKAGE_NAME);
    }

    /**
     * Open the settings page for the vpn app associated with the vpnConfig.
     *
     * @param vpnConfig The vpnConfig
     * @param context   The application context.
     */
    public static void openVpnAppSettings(VPNConfig vpnConfig, Context context) {
        if (vpnConfig != null) {
            switch (vpnConfig.getVpnType()) {
                case NONE:
                    return;
                case TAILSCALE:
                    openAppSettings(context, PACKAGE_TAILSCALE);
                    break;
                case WIREGUARD:
                    openAppSettings(context, PACKAGE_WIREGUARD);
                    break;
            }
        }
    }

    private static void openAppSettings(Context context, String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Check whether or not BitBanana has the permission to control the vpn app associated with the vpnConfig.
     *
     * @param vpnConfig The vpnConfig
     * @param context   The application context.
     */
    public static boolean hasPermissionToControlVpn(VPNConfig vpnConfig, Context context) {
        if (vpnConfig != null) {
            switch (vpnConfig.getVpnType()) {
                case NONE:
                    return true;
                case TAILSCALE:
                    return true;
                case WIREGUARD:
                    return hasPermissionToControlWireguard(context);
            }
        }
        return true;
    }

    private static boolean hasPermissionToControlWireguard(Context context) {
        if (PermissionsUtil.hasPermission(context, PERMISSION_WIREGUARD)) {
            return true;
        } else {
            BBLog.w(LOG_TAG, "No permission to control VPN (WireGuard Tunnel). Requesting permission.");
            PermissionsUtil.requestPermissions(context, new String[]{PERMISSION_WIREGUARD}, PERMISSION_WIREGUARD_REQUEST_CODE, false);
            return false;
        }
    }

    /**
     * This function can only tell us if ANY vpn is active. Unfortunately we cannot find out what name the VPN Service has.
     */
    public static boolean isVpnActive(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = cm.getActiveNetwork();
        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);

        if (caps != null) {
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        }
        return false;
    }

    public static void debugPrintNetworkInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = cm.getAllNetworks();
        BBLog.i(LOG_TAG, "Network count: " + networks.length);
        for (int i = 0; i < networks.length; i++) {

            NetworkCapabilities caps = cm.getNetworkCapabilities(networks[i]);

            BBLog.i(LOG_TAG, "Network " + i + ": " + networks[i].toString());
            BBLog.i(LOG_TAG, "VPN transport is: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
            BBLog.i(LOG_TAG, "NOT_VPN capability is: " + caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
        }
    }

    public static void debugRegisterVPNCallback(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerNetworkCallback(
                new NetworkRequest.Builder().removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(),
                new ConnectivityManager.NetworkCallback() {

                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        BBLog.i(LOG_TAG, "Network " + network + " is available");
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        BBLog.i(LOG_TAG, "Network " + network + " is lost");
                    }

                    @Override
                    public void onLinkPropertiesChanged(Network network, LinkProperties props) {
                        super.onLinkPropertiesChanged(network, props);
                        BBLog.i(LOG_TAG, "Network " + network + " link properties changed");
                    }

                    @Override
                    public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                        super.onCapabilitiesChanged(network, caps);
                        BBLog.i(LOG_TAG, "Network " + network + " capabilities changed: " + caps);
                    }
                }
        );
    }
}
