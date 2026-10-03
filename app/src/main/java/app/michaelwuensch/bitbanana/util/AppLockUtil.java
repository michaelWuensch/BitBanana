package app.michaelwuensch.bitbanana.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemClock;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.appLock.PasswordEntryActivity;
import app.michaelwuensch.bitbanana.appLock.PinEntryActivity;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;

public class AppLockUtil {

    private static final String LOG_TAG = AppLockUtil.class.getSimpleName();
    private static final String FAILED_UNLOCK_ELAPSED_REALTIME = "failedUnlockElapsedRealtime";
    public static boolean isLockScreenShown;
    public static boolean isEmergencyUnlocked;

    static public void askForAccess(Activity activity, boolean forceRestart, OnSecurityCheckPerformedListener onSecurityCheckPerformedListener) {
        if (TimeOutUtil.getInstance().isTimedOut()) {
            if (PrefsUtil.isPinEnabled()) {
                if (TimeOutUtil.getInstance().isFullyTimedOut() || forceRestart) {
                    // Go to PIN entry screen, remove all history, full reconnect is needed.
                    BBLog.d(LOG_TAG, "Show lock screen, remove history.");
                    Intent pinIntent = new Intent(activity, PinEntryActivity.class);
                    pinIntent.putExtra(PinEntryActivity.EXTRA_CLEAR_HISTORY, true);
                    pinIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(pinIntent);
                    isLockScreenShown = true;
                    TimeOutUtil.getInstance().setLocked();
                } else {
                    if (!isLockScreenShown) {
                        // Go to PIN entry screen, but don't clear current state. This allows the user to continue where he left but is less secure as sensitive data is still kept in memory and could theoretically be read out by malicious apps or hackers.
                        BBLog.d(LOG_TAG, "Show lock screen, keep history.");
                        Intent pinIntent = new Intent(activity, PinEntryActivity.class);
                        pinIntent.putExtra(PinEntryActivity.EXTRA_CLEAR_HISTORY, false);
                        activity.startActivity(pinIntent);
                        isLockScreenShown = true;
                        TimeOutUtil.getInstance().setLocked();
                    }
                }
            } else if (PrefsUtil.isPasswordEnabled()) {
                if (TimeOutUtil.getInstance().isFullyTimedOut() || forceRestart) {
                    // Go to password entry screen, remove all history, full reconnect is needed.
                    BBLog.d(LOG_TAG, "Show lock screen, remove history.");
                    Intent passwordIntent = new Intent(activity, PasswordEntryActivity.class);
                    passwordIntent.putExtra(PasswordEntryActivity.EXTRA_CLEAR_HISTORY, true);
                    passwordIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(passwordIntent);
                    isLockScreenShown = true;
                    TimeOutUtil.getInstance().setLocked();
                } else {
                    if (!isLockScreenShown) {
                        // Go to PIN entry screen, but don't clear current state. This allows the user to continue where he left but is less secure as sensitive data is still kept in memory and could theoretically be read out by malicious apps or hackers.
                        BBLog.d(LOG_TAG, "Show lock screen, keep history.");
                        Intent passwordIntent = new Intent(activity, PasswordEntryActivity.class);
                        passwordIntent.putExtra(PinEntryActivity.EXTRA_CLEAR_HISTORY, false);
                        activity.startActivity(passwordIntent);
                        isLockScreenShown = true;
                        TimeOutUtil.getInstance().setLocked();
                    }
                }
            } else {
                // Check if app lock is active according to key store
                boolean isAppLockActive = false;
                try {
                    isAppLockActive = new KeystoreUtil().isAppLockActive();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Only allow access if app lock is not active in key store!
                if (isAppLockActive) {
                    // According to the key store, the app lock is still active. This happens if the pin or password got deleted from the prefs file without also removing the keystore entry.
                    // Basically this would be the case if the PIN hash or password hash was removed in a different way than from the apps settings menu. (For example with a file explorer on a rooted device)
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.error_pin_deactivation_attempt)
                            .setCancelable(false)
                            .setPositiveButton(R.string.continue_string, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    activity.finish();
                                }
                            }).show();
                } else {
                    // Access granted
                    TimeOutUtil.getInstance().setUnlocked();
                    onSecurityCheckPerformedListener.onAccessGranted();
                }
            }
        } else {
            // Access granted
            TimeOutUtil.getInstance().setUnlocked();
            onSecurityCheckPerformedListener.onAccessGranted();
        }
    }

    /**
     * Has to be called after a failed unlock attempt, once APP_NUM_UNLOCK_FAILS was increased.
     * Starts the input delay if the number of failed attempts requires one.
     *
     * @param numFails number of failed attempts including the current one
     * @return the input delay in milliseconds, 0 if there is none
     */
    public static long registerFailedUnlockAttempt(int numFails) {
        long delay = getUnlockDelayMillis(numFails);
        if (delay > 0) {
            // We use the time since boot instead of the wall clock, as the wall clock can be changed by the user to skip the delay.
            PrefsUtil.editPrefs().putLong(FAILED_UNLOCK_ELAPSED_REALTIME, SystemClock.elapsedRealtime()).apply();
        }
        return delay;
    }

    /**
     * Returns how long the user still has to wait before the next unlock attempt is allowed.
     *
     * @param numFails number of failed attempts so far
     * @return remaining input delay in milliseconds, 0 if input is allowed
     */
    public static long getRemainingUnlockDelayMillis(int numFails) {
        long delay = getUnlockDelayMillis(numFails);
        if (delay == 0)
            return 0;

        long now = SystemClock.elapsedRealtime();
        long start = PrefsUtil.getPrefs().getLong(FAILED_UNLOCK_ELAPSED_REALTIME, -1);
        if (start < 0 || start > now) {
            // Either no start is known (e.g. update from a version that used the wall clock) or the device was rebooted since the failed attempt.
            // In both cases we cannot know how much time passed, therefore the full delay starts again.
            PrefsUtil.editPrefs().putLong(FAILED_UNLOCK_ELAPSED_REALTIME, now).apply();
            return delay;
        }
        // If a reboot happened and the uptime already exceeds the stored value, the calculated time is shorter than the actual time passed.
        // This only results in a longer delay, never in a shorter one.
        return Math.max(0, delay - (now - start));
    }

    /**
     * The delay starts after APP_LOCK_MAX_FAILS failed attempts and doubles with every further failed attempt up to APP_LOCK_MAX_DELAY_TIME.
     */
    private static long getUnlockDelayMillis(int numFails) {
        if (numFails < RefConstants.APP_LOCK_MAX_FAILS)
            return 0;
        int doublings = Math.min(numFails - RefConstants.APP_LOCK_MAX_FAILS, 20);
        long delay = (RefConstants.APP_LOCK_START_DELAY_TIME * 1000L) << doublings;
        return Math.min(delay, RefConstants.APP_LOCK_MAX_DELAY_TIME * 1000L);
    }

    public static String getUnlockDelayMessage(Context context, long delayMillis) {
        return context.getString(R.string.pin_entered_wrong_wait, String.valueOf((delayMillis + 999) / 1000));
    }

    public static void emergencyClearAll() {
        BackendConfigsManager bcm = BackendConfigsManager.getInstance();
        bcm.removeAllBackendConfigs();
        try {
            bcm.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ContactsManager cm = ContactsManager.getInstance();
        cm.removeAllContacts();
        try {
            cm.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void emergencyClearAllButWalletToShow() {
        BackendConfigsManager bcm = BackendConfigsManager.getInstance();
        for (BackendConfig bc : bcm.getAllBackendConfigs(false)) {
            if (!bc.getId().equals(PrefsUtil.getPrefs().getString("appLockEmergencyWalletToShowPref", "")))
                bcm.removeBackendConfig(bc);
        }
        try {
            bcm.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ContactsManager cm = ContactsManager.getInstance();
        cm.removeAllContacts();
        try {
            cm.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface OnSecurityCheckPerformedListener {
        void onAccessGranted();
    }
}


