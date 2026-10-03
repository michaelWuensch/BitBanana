package app.michaelwuensch.bitbanana.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeOutUtilTest {

    private static final long LOCK_TIMEOUT_SECONDS = 30;

    private final AtomicLong mElapsedRealtime = new AtomicLong(1_000_000);
    private final TimeOutUtil mTimeOutUtil = new TimeOutUtil(mElapsedRealtime::get, () -> LOCK_TIMEOUT_SECONDS);

    private void passSeconds(long seconds) {
        mElapsedRealtime.addAndGet(seconds * 1000);
    }

    @Test
    public void freshStart_isTimedOut() {
        // Right after booting elapsedRealtime is small. The app must still be locked on a fresh start.
        mElapsedRealtime.set(5_000);

        assertTrue(mTimeOutUtil.isTimedOut());
        assertTrue(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void unlockedInForeground_neverTimesOut() {
        mTimeOutUtil.setUnlocked();
        // Using the app for a long time must not lock it (e.g. when a new HomeActivity gets created).
        passSeconds(RefConstants.DISCONNECT_TIMEOUT * 2L);

        assertFalse(mTimeOutUtil.isTimedOut());
        assertFalse(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void shortBackgroundAfterLongUsage_isNotTimedOut() {
        // This is the reported bug: after unlocking and using the app for a while, a short switch to another app locked it.
        mTimeOutUtil.setUnlocked();
        passSeconds(40);
        mTimeOutUtil.onMovedToBackground();
        passSeconds(3);

        assertFalse(mTimeOutUtil.isTimedOut());
    }

    @Test
    public void backgroundWithinTimeout_isNotTimedOut() {
        mTimeOutUtil.setUnlocked();
        mTimeOutUtil.onMovedToBackground();
        passSeconds(LOCK_TIMEOUT_SECONDS);

        assertFalse(mTimeOutUtil.isTimedOut());
        assertFalse(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void backgroundLongerThanTimeout_isTimedOut() {
        mTimeOutUtil.setUnlocked();
        mTimeOutUtil.onMovedToBackground();
        passSeconds(LOCK_TIMEOUT_SECONDS + 1);

        assertTrue(mTimeOutUtil.isTimedOut());
        assertFalse(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void backgroundLongerThanDisconnectTimeout_isFullyTimedOut() {
        mTimeOutUtil.setUnlocked();
        mTimeOutUtil.onMovedToBackground();
        passSeconds(RefConstants.DISCONNECT_TIMEOUT + 1);

        assertTrue(mTimeOutUtil.isTimedOut());
        assertTrue(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void backToForegroundWithinTimeout_resetsTimeout() {
        mTimeOutUtil.setUnlocked();
        mTimeOutUtil.onMovedToBackground();
        passSeconds(20);
        // Access granted because within timeout
        mTimeOutUtil.setUnlocked();
        passSeconds(20);
        mTimeOutUtil.onMovedToBackground();
        passSeconds(20);

        assertFalse(mTimeOutUtil.isTimedOut());
    }

    @Test
    public void backgroundWhileLockScreenIsShown_doesNotExtendTimeout() {
        mTimeOutUtil.setUnlocked();
        mTimeOutUtil.onMovedToBackground();
        passSeconds(LOCK_TIMEOUT_SECONDS + 1);
        // Lock screen is shown
        mTimeOutUtil.setLocked();
        // The user leaves the lock screen and opens the app again immediately (e.g. via the launcher)
        mTimeOutUtil.onMovedToBackground();
        passSeconds(1);

        assertTrue(mTimeOutUtil.isTimedOut());
    }

    @Test
    public void lockedState_isFullyTimedOutMeasuredFromLeavingTheApp() {
        mTimeOutUtil.setUnlocked();
        mTimeOutUtil.onMovedToBackground();
        passSeconds(LOCK_TIMEOUT_SECONDS + 1);
        mTimeOutUtil.setLocked();

        assertFalse(mTimeOutUtil.isFullyTimedOut());
        passSeconds(RefConstants.DISCONNECT_TIMEOUT);
        assertTrue(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void unlockAfterLock_startsNewSession() {
        mTimeOutUtil.setUnlocked();
        mTimeOutUtil.onMovedToBackground();
        passSeconds(LOCK_TIMEOUT_SECONDS + 1);
        mTimeOutUtil.setLocked();
        mTimeOutUtil.setUnlocked();

        assertFalse(mTimeOutUtil.isTimedOut());
        assertFalse(mTimeOutUtil.isFullyTimedOut());
    }
}
