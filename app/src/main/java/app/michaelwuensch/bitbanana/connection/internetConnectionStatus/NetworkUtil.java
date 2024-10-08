package app.michaelwuensch.bitbanana.connection.internetConnectionStatus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int TYPE_NOT_CONNECTED = 0;
    public static final int NETWORK_STATUS_NOT_CONNECTED = 0;
    public static final int NETWORK_STATUS_WIFI = 1;
    public static final int NETWORK_STATUS_MOBILE = 2;

    private static int getConnectivity(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static int getConnectivityStatus(Context context) {
        int conn = NetworkUtil.getConnectivity(context);
        int status = 0;
        if (conn == NetworkUtil.TYPE_WIFI) {
            status = NETWORK_STATUS_WIFI;
        } else if (conn == NetworkUtil.TYPE_MOBILE) {
            status = NETWORK_STATUS_MOBILE;
        } else if (conn == NetworkUtil.TYPE_NOT_CONNECTED) {
            status = NETWORK_STATUS_NOT_CONNECTED;
        }
        return status;
    }

    public static String getConnectivityStatusString(Context context) {
        int conn = NetworkUtil.getConnectivity(context);
        String status = "";
        if (conn == NetworkUtil.TYPE_WIFI) {
            status = "WIFI";
        } else if (conn == NetworkUtil.TYPE_MOBILE) {
            status = "MOBILE";
        } else if (conn == NetworkUtil.TYPE_NOT_CONNECTED) {
            status = "NOT CONNECTED";
        }
        return status;
    }

    public static boolean isConnectedToInternet(Context context) {
        int status = getConnectivityStatus(context);
        return status != NetworkUtil.NETWORK_STATUS_NOT_CONNECTED;
    }
}
