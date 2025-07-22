package app.michaelwuensch.bitbanana.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * This class helps to easily manage the permission necessary for the app.
 */
public class PermissionsUtil {

    // These codes defined here will be used to respond to user input on the request permission dialogs.
    public static final int CAMERA_PERMISSION_CODE = 0;
    public static final int NOTIFICATION_PERMISSION_CODE = 1;


    public static boolean hasCameraPermission(Context context) {
        return hasPermission(context, Manifest.permission.CAMERA);
    }

    public static void requestCameraPermission(Context context, boolean forceRequest) {
        requestPermissions(context, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE, forceRequest);
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPermission(context, Manifest.permission.POST_NOTIFICATIONS);
        }
        return true;
    }

    public static void requestNotificationPermission(Context context, boolean forceRequest) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(context, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE, forceRequest);
        }
    }


    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissions(Context context, String[] permissions, int code, boolean forceRequest) {

        for (int i = 0; i < permissions.length; i++) {
            // Do not request permission if user already denied it.
            // If forceRequest is true, the user will still be asked unless he ticked "don't ask me again".
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permissions[i]) & !forceRequest) {
                BBLog.w("PermissionsUtil", "User denied this request before, no permission requested");
            } else {
                ActivityCompat.requestPermissions((Activity) context, permissions, code);
                break;
            }
        }

    }
}
