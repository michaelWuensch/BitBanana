package app.michaelwuensch.bitbanana.util;

import android.os.SystemClock;

import java.util.function.LongSupplier;

/**
 * Keeps track of when the app was last closed (moved to background) or unlocked to decide if the lock screen has to be shown again.
 * <p>
 * The time is measured with SystemClock.elapsedRealtime(). Unlike the system time it can not be changed by the user,
 * so the lock screen can not be circumvented by setting the time of the device manually.
 */
public class TimeOutUtil {
    private static final String LOG_TAG = TimeOutUtil.class.getSimpleName();

    private static TimeOutUtil instance = null;
    private final LongSupplier mElapsedRealtime;
    private final LongSupplier mLockScreenTimeoutSeconds;
    // elapsedRealtime when the app was closed or unlocked
    private long appClosed = 0L;
    // As long as the timer was never started (e.g. fresh process start), the app is always considered timed out.
    // This is necessary, as elapsedRealtime starts at 0 when the device boots.
    private boolean timerStarted = false;
    private boolean canBeRestarted = true;

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

    public void restartTimer() {
        appClosed = mElapsedRealtime.getAsLong();
        timerStarted = true;
        BBLog.d(LOG_TAG, "App lock timer restarted");
    }

    public boolean isTimedOut() {
        return isTimedOut(mLockScreenTimeoutSeconds.getAsLong());
    }

    public boolean isFullyTimedOut() {
        return isTimedOut(RefConstants.DISCONNECT_TIMEOUT);
    }

    private boolean isTimedOut(long timeoutSeconds) {
        if (!timerStarted)
            return true;
        return (mElapsedRealtime.getAsLong() - appClosed) > timeoutSeconds * 1000;
    }

    public boolean getCanBeRestarted() {
        return canBeRestarted;
    }

    public void setCanBeRestarted(boolean canBeRestarted) {
        this.canBeRestarted = canBeRestarted;
    }
}
