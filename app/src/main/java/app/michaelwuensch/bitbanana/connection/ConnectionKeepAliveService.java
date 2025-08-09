package app.michaelwuensch.bitbanana.connection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.BBLog;

public class ConnectionKeepAliveService extends Service {

    private static final String LOG_TAG = ConnectionKeepAliveService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private Notification createNotification() {
        String channelId = "connection_keepalive_channel";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                channelId,
                getString(R.string.keep_alive_notification_channel_name),
                NotificationManager.IMPORTANCE_MIN // No sounds, minimal visual
        );
        channel.setDescription(getString(R.string.keep_alive_notification_channel_description));
        channel.setShowBadge(false); // No badge count on app icon
        manager.createNotificationChannel(channel);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.bitbanana_logo);

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.keep_alive_notification_title))
                .setContentText(getString(R.string.keep_alive_notification_description))
                .setSmallIcon(R.drawable.bitbanana_logo_small_mono)
                .setLargeIcon(largeIcon)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Do nothing, just keep alive
        BBLog.i(LOG_TAG, "ConnectionKeepAliveService started.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC | ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(1, createNotification());
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        BBLog.i(LOG_TAG, "ConnectionKeepAliveService stopped as app was closed.");
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BBLog.i(LOG_TAG, "ConnectionKeepAliveService stopped.");
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        // Android 15+ only allows foreground services of specific types to be active for 6 hours (ACROSS APPS!!!) every 24 hours.
        // Therefore we need to stop it if the limit is reached. See: https://developer.android.com/develop/background-work/services/fgs/timeout
        BBLog.w(LOG_TAG, "Foreground service timed out: type=" + fgsType + ", startId=" + startId);
        BBLog.w(LOG_TAG, "Stopping ConnectionKeepAliveService. The Android limit for foreground services (across apps!) has been reached. We have to do this as Android will crash the app if we don't stop the service now.");
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf(startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
