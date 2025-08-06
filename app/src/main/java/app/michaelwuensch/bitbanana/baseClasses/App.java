package app.michaelwuensch.bitbanana.baseClasses;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.connection.ConnectionKeepAliveService;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PermissionsUtil;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;


// This class is used as Application class for BitBanana.

public class App extends Application {
    private static App mContext;

    // keep the data from the URI Scheme in memory, so we can access it from anywhere.
    private String uriSchemeData;
    private long ForegroundServiceStartTimestamp;

    private Handler mBackgroundCloseHandler;
    private Handler mBackgroundServiceHandler;

    public App() {
        mContext = this;
        mBackgroundCloseHandler = new Handler();
        mBackgroundServiceHandler = new Handler();

        RxJavaPlugins.setErrorHandler(e -> {
            if (e.getMessage() != null && e.getMessage().contains("shutdownNow")) {
                // Is propagated from gRPC when shutting down channel
            } else {
                BBLog.d("RxJava", e.getMessage());
            }
        });

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            int activityRefs = 0;

            @Override
            public void onActivityStarted(Activity activity) {
                if (++activityRefs == 1) {
                    // App enters foreground

                    // Stop foreground service to keep connection alive.
                    if (System.currentTimeMillis() - ForegroundServiceStartTimestamp > 5000)
                        stopService(new Intent(App.getAppContext(), ConnectionKeepAliveService.class));
                    else {
                        mBackgroundServiceHandler.postDelayed(() -> {
                            stopService(new Intent(App.getAppContext(), ConnectionKeepAliveService.class));
                        }, 5000 - (System.currentTimeMillis() - ForegroundServiceStartTimestamp));
                    }
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (--activityRefs == 0) {
                    // App goes to background. This fires faster than our Lifecycle event on the home activity. Starting the keepAlive service faster ensures the onDestroy event of the home activity gets executed, which ensures VPN is stopped when closing the app through swiping.

                    // Start foreground service to keep connection alive.
                    if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()
                            && BackendManager.getBackendState() != BackendManager.BackendState.NO_BACKEND_SELECTED
                            && BackendManager.getBackendState() != BackendManager.BackendState.ERROR
                            && PermissionsUtil.hasNotificationPermission(App.this)) {
                        mBackgroundServiceHandler.removeCallbacksAndMessages(null);
                        ContextCompat.startForegroundService(App.this, new Intent(App.getAppContext(), ConnectionKeepAliveService.class));
                        ForegroundServiceStartTimestamp = System.currentTimeMillis();
                    }
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }

            // other methods...
        });
    }

    public static App getAppContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public String getUriSchemeData() {
        return uriSchemeData;
    }

    public void setUriSchemeData(String data) {
        uriSchemeData = data;
    }

    public Handler getBackgroundCloseHandler() {
        return mBackgroundCloseHandler;
    }
}