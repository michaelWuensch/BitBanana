package app.michaelwuensch.bitbanana.util;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

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


    public static void startTailscale(Activity activity) {
        BBLog.d(LOG_TAG, "Starting VPN (Tailscale)");
        Intent intent = new Intent();
        intent.setAction(ACTION_START_TAILSCALE);
        intent.setPackage(PACKAGE_TAILSCALE);
        activity.sendBroadcast(intent);
    }

    public static void stopTailscale(Activity activity) {
        BBLog.d(LOG_TAG, "Stopping VPN (Tailscale)");
        Intent intent = new Intent();
        intent.setAction(ACTION_STOP_TAILSCALE);
        intent.setPackage(PACKAGE_TAILSCALE);
        activity.sendBroadcast(intent);
    }

    public static void startWireGuard(Activity activity, String tunnelName) {
        if (PermissionsUtil.hasPermission(activity, PERMISSION_WIREGUARD)) {
            BBLog.d(LOG_TAG, "Starting VPN (WireGuard)");
            Intent intent = new Intent();
            intent.setAction(ACTION_START_WIREGUARD);
            intent.setPackage(PACKAGE_WIREGUARD);
            activity.sendBroadcast(intent);
        } else {
            PermissionsUtil.requestPermissions(activity, new String[]{PERMISSION_WIREGUARD}, 1, false);
        }
    }

    public static boolean isTailscaleInstalled(Context ctx) {
        return isAppInstalled(ctx, PACKAGE_TAILSCALE, APP_NAME_TAILSCALE);
    }

    public static boolean isWireGuardInstalled(Context ctx) {
        return isAppInstalled(ctx, PACKAGE_TAILSCALE, APP_NAME_TAILSCALE);
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
