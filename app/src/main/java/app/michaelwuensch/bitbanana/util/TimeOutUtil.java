package app.michaelwuensch.bitbanana.util;

public class TimeOutUtil {
    private static final String LOG_TAG = TimeOutUtil.class.getSimpleName();

    private static long appClosed = 0L;
    private static TimeOutUtil instance = null;
    private boolean canBeRestarted = true;

    private TimeOutUtil() {
    }

    public static TimeOutUtil getInstance() {

        if (instance == null) {
            instance = new TimeOutUtil();
        }

        return instance;
    }

    public void restartTimer() {
        appClosed = System.currentTimeMillis();
        BBLog.d(LOG_TAG, "PIN timer restarted");
    }

    public boolean isTimedOut() {
        boolean timedOut = (System.currentTimeMillis() - appClosed) > PrefsUtil.getLockScreenTimeout() * 1000;
        // Do also not allow times prior to "appClosed".
        // This would allow to circumventing timeout check by setting the time of the device manually.
        boolean invalidTime = System.currentTimeMillis() < appClosed;
        return timedOut || invalidTime;
    }

    public boolean isFullyTimedOut() {
        boolean timedOut = (System.currentTimeMillis() - appClosed) > RefConstants.DISCONNECT_TIMEOUT * 1000;
        // Do also not allow times prior to "appClosed".
        // This would allow to circumventing timeout check by setting the time of the device manually.
        boolean invalidTime = System.currentTimeMillis() < appClosed;
        return timedOut || invalidTime;
    }

    public boolean getCanBeRestarted() {
        return canBeRestarted;
    }

    public void setCanBeRestarted(boolean canBeRestarted) {
        this.canBeRestarted = canBeRestarted;
    }
}
