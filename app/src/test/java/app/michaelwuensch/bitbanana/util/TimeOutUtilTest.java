package app.michaelwuensch.bitbanana.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeOutUtilTest {

    private static final long LOCK_TIMEOUT_SECONDS = 30;

    private final AtomicLong mElapsedRealtime = new AtomicLong();
    private final TimeOutUtil mTimeOutUtil = new TimeOutUtil(mElapsedRealtime::get, () -> LOCK_TIMEOUT_SECONDS);

    @Test
    public void timerNeverStarted_isTimedOut() {
        // Right after booting elapsedRealtime is small. The app must still be locked on a fresh start.
        mElapsedRealtime.set(5_000);

        assertTrue(mTimeOutUtil.isTimedOut());
        assertTrue(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void withinTimeout_isNotTimedOut() {
        mElapsedRealtime.set(1_000_000);
        mTimeOutUtil.restartTimer();
        mElapsedRealtime.addAndGet(LOCK_TIMEOUT_SECONDS * 1000);

        assertFalse(mTimeOutUtil.isTimedOut());
        assertFalse(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void afterTimeout_isTimedOut() {
        mElapsedRealtime.set(1_000_000);
        mTimeOutUtil.restartTimer();
        mElapsedRealtime.addAndGet(LOCK_TIMEOUT_SECONDS * 1000 + 1);

        assertTrue(mTimeOutUtil.isTimedOut());
        assertFalse(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void afterDisconnectTimeout_isFullyTimedOut() {
        mElapsedRealtime.set(1_000_000);
        mTimeOutUtil.restartTimer();
        mElapsedRealtime.addAndGet(RefConstants.DISCONNECT_TIMEOUT * 1000L + 1);

        assertTrue(mTimeOutUtil.isTimedOut());
        assertTrue(mTimeOutUtil.isFullyTimedOut());
    }

    @Test
    public void restartTimer_resetsTimeout() {
        mElapsedRealtime.set(1_000_000);
        mTimeOutUtil.restartTimer();
        mElapsedRealtime.addAndGet(LOCK_TIMEOUT_SECONDS * 1000 + 1);
        mTimeOutUtil.restartTimer();

        assertFalse(mTimeOutUtil.isTimedOut());
    }
}
