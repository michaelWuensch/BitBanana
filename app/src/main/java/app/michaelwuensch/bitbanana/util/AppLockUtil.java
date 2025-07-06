package app.michaelwuensch.bitbanana.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.appLock.PasswordEntryActivity;
import app.michaelwuensch.bitbanana.appLock.PinEntryActivity;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;

public class AppLockUtil {

    private static final String LOG_TAG = AppLockUtil.class.getSimpleName();
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
                } else {
                    if (!isLockScreenShown) {
                        // Go to PIN entry screen, but don't clear current state. This allows the user to continue where he left but is less secure as sensitive data is still kept in memory and could theoretically be read out by malicious apps or hackers.
                        BBLog.d(LOG_TAG, "Show lock screen, keep history.");
                        Intent pinIntent = new Intent(activity, PinEntryActivity.class);
                        pinIntent.putExtra(PinEntryActivity.EXTRA_CLEAR_HISTORY, false);
                        activity.startActivity(pinIntent);
                        isLockScreenShown = true;
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
                } else {
                    if (!isLockScreenShown) {
                        // Go to PIN entry screen, but don't clear current state. This allows the user to continue where he left but is less secure as sensitive data is still kept in memory and could theoretically be read out by malicious apps or hackers.
                        BBLog.d(LOG_TAG, "Show lock screen, keep history.");
                        Intent passwordIntent = new Intent(activity, PasswordEntryActivity.class);
                        passwordIntent.putExtra(PinEntryActivity.EXTRA_CLEAR_HISTORY, false);
                        activity.startActivity(passwordIntent);
                        isLockScreenShown = true;
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
                    onSecurityCheckPerformedListener.onAccessGranted();
                }
            }
        } else {
            // Access granted
            onSecurityCheckPerformedListener.onAccessGranted();
        }
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


