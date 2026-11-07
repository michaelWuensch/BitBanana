package app.michaelwuensch.bitbanana.baseClasses;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.LandingActivity;
import app.michaelwuensch.bitbanana.LauncherActivity;
import app.michaelwuensch.bitbanana.appLock.PasswordEntryActivity;
import app.michaelwuensch.bitbanana.appLock.PinEntryActivity;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.connection.ConnectionKeepAliveService;
import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.util.AppLockUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PermissionsUtil;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
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
                if (activity instanceof HomeActivity)
                    homeActivityRefs++;
            }

            int activityRefs = 0;
            int homeActivityRefs = 0;

            @Override
            public void onActivityStarted(Activity activity) {
                if (++activityRefs == 1) {
                    // App enters foreground

                    // Show the lock screen if necessary. Usually the HomeActivity does this when the app is moved to foreground (see HomeActivity.onMoveToForeground).
                    // But if the process was killed while the app was in background, Android only recreates the activity that was on top (e.g. the node details) and not the HomeActivity.
                    // Without this check, that activity would be shown without asking for the PIN.
                    if (homeActivityRefs == 0 && isProtectedByAppLock(activity))
                        AppLockUtil.askForAccess(activity, false, () -> {
                        });

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
                    // Start the app lock timeout. This has to happen here and not in an observer of an activity:
                    // The ProcessLifecycleOwner reports ON_STOP with a delay of 700 ms. If the activity gets destroyed in the meantime (e.g. when the app is swiped away in the recent apps),
                    // the event would be missed, the timeout would never start and the lock screen would not be shown when the app is opened again.
                    // Configuration changes (e.g. rotation) are not a move to background.
                    if (!activity.isChangingConfigurations())
                        TimeOutUtil.getInstance().onMovedToBackground();

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
                if (activity instanceof HomeActivity)
                    homeActivityRefs--;
            }

            // other methods...
        });
    }

    /**
     * Activities of other libraries and the decoy apps are not based on BaseAppCompatActivity and therefore not protected.
     * The lock screens themselves and the activities that start the app (they check the app lock on their own) are excluded as well.
     */
    private static boolean isProtectedByAppLock(Activity activity) {
        return activity instanceof BaseAppCompatActivity
                && !(activity instanceof LauncherActivity)
                && !(activity instanceof LandingActivity)
                && !(activity instanceof PinEntryActivity)
                && !(activity instanceof PasswordEntryActivity);
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