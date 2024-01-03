package app.michaelwuensch.bitbanana.connection.vpn;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

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
    private static final String PERMISSION_WIREGUARD = "com.wireguard.android.permission.CONTROL_TUNNELS";
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
        if (PermissionsUtil.hasPermission(context, PERMISSION_WIREGUARD)) {
            BBLog.d(LOG_TAG, "Starting VPN (WireGuard, Tunnel: " + tunnelName + ")");
            Intent intent = new Intent();
            intent.setAction(ACTION_START_WIREGUARD);
            intent.setPackage(PACKAGE_WIREGUARD);
            intent.putExtra("tunnel", tunnelName);
            context.sendBroadcast(intent);
        } else {
            BBLog.w(LOG_TAG, "No permission to start VPN (WireGuard Tunnel). Requesting permission.");
            PermissionsUtil.requestPermissions(context, new String[]{PERMISSION_WIREGUARD}, 1, false);
        }
    }

    private static void stopWireGuardTunnel(Context context, String tunnelName) {
        if (PermissionsUtil.hasPermission(context, PERMISSION_WIREGUARD)) {
            BBLog.d(LOG_TAG, "Stopping VPN (WireGuard, Tunnel" + tunnelName + ")");
            Intent intent = new Intent();
            intent.setAction(ACTION_STOP_WIREGUARD);
            intent.setPackage(PACKAGE_WIREGUARD);
            intent.putExtra("tunnel", tunnelName);
            context.sendBroadcast(intent);
        } else {
            BBLog.w(LOG_TAG, "No permission to stop VPN (WireGuard Tunnel). Requesting permission.");
            PermissionsUtil.requestPermissions(context, new String[]{PERMISSION_WIREGUARD}, 1, false);
        }
    }

    public static boolean isVpnAppInstalled(VPNConfig vpnConfig, Context ctx) {
        if (vpnConfig != null) {
            switch (vpnConfig.getVpnType()) {
                case NONE:
                    return true;
                case TAILSCALE:
                    return isTailscaleInstalled(ctx);
                case WIREGUARD:
                    return isWireGuardInstalled(ctx);
            }
        }
        return true;
    }

    private static boolean isTailscaleInstalled(Context ctx) {
        return isAppInstalled(ctx, PACKAGE_TAILSCALE, APP_NAME_TAILSCALE);
    }

    private static boolean isWireGuardInstalled(Context ctx) {
        return isAppInstalled(ctx, PACKAGE_TAILSCALE, APP_NAME_WIREGUARD);
    }

    private static boolean isAppInstalled(Context ctx, String package_name, String app_name) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo info = pm.getPackageInfo("" + package_name, PackageManager.GET_META_DATA);
            BBLog.v(LOG_TAG, app_name + " is installed.");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            BBLog.w(LOG_TAG, app_name + " is not installed.");
            Toast.makeText(ctx.getApplicationContext(), "Your device has not installed " + app_name, Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
    }
}
