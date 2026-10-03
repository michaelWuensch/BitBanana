package app.michaelwuensch.bitbanana.util;

import android.os.SystemClock;

import java.util.function.LongSupplier;

/**
 * Decides if the lock screen has to be shown, based on how long the unlocked app was in background.
 * <p>
 * While the app is unlocked and in foreground it never times out. The timeout only starts when the unlocked app is moved to background.
 * Moving the app to background while the lock screen is shown does not start or extend anything, so the lock screen can not be circumvented this way.
 * <p>
 * The time is measured with SystemClock.elapsedRealtime(). Unlike the system time it can not be changed by the user,
 * so the lock screen can not be circumvented by setting the time of the device manually.
 */
public class TimeOutUtil {
    private static final String LOG_TAG = TimeOutUtil.class.getSimpleName();

    private static TimeOutUtil instance = null;
    private final LongSupplier mElapsedRealtime;
    private final LongSupplier mLockScreenTimeoutSeconds;
    // True if access was granted (unlocked or no app lock) and the lock screen was not shown since.
    // It is false on a fresh process start.
    private boolean unlocked = false;
    // elapsedRealtime when the unlocked app was moved to background, -1 while it is in foreground.
    private long backgroundSince = -1;

    private TimeOutUtil() {
        this(SystemClock::elapsedRealtime, PrefsUtil::getLockScreenTimeout);
    }

    // used for unit tests
    TimeOutUtil(LongSupplier elapsedRealtime, LongSupplier lockScreenTimeoutSeconds) {
        mElapsedRealtime = elapsedRealtime;
        mLockScreenTimeoutSeconds = lockScreenTimeoutSeconds;
    }

    public static TimeOutUtil getInstance() {

        if (instance == null) {
            instance = new TimeOutUtil();
        }

        return instance;
    }

    /**
     * Has to be called when access to the app was granted (correct PIN/password, biometrics, within timeout or no app lock active).
     */
    public void setUnlocked() {
        unlocked = true;
        backgroundSince = -1;
    }

    /**
     * Has to be called when the lock screen is shown.
     */
    public void setLocked() {
        unlocked = false;
    }

    /**
     * Has to be called when the app was moved to background. Only starts the timeout if the app is currently unlocked.
     */
    public void onMovedToBackground() {
        if (unlocked && backgroundSince < 0) {
            backgroundSince = mElapsedRealtime.getAsLong();
            BBLog.d(LOG_TAG, "App lock timeout started");
        }
    }

    /**
     * @return true if the lock screen has to be shown.
     */
    public boolean isTimedOut() {
        if (!unlocked)
            return true;
        return isInBackgroundLongerThan(mLockScreenTimeoutSeconds.getAsLong());
    }

    /**
     * @return true if the app was in background so long, that a full reconnect is necessary.
     */
    public boolean isFullyTimedOut() {
        if (backgroundSince < 0)
            // Either a fresh process start or the unlocked app is in foreground.
            return !unlocked;
        return isInBackgroundLongerThan(RefConstants.DISCONNECT_TIMEOUT);
    }

    private boolean isInBackgroundLongerThan(long timeoutSeconds) {
        if (backgroundSince < 0)
            return false;
        return (mElapsedRealtime.getAsLong() - backgroundSince) > timeoutSeconds * 1000;
    }
}
