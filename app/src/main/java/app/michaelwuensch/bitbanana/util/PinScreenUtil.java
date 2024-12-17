package app.michaelwuensch.bitbanana.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.pin.PinEntryActivity;

public class PinScreenUtil {

    private static final String LOG_TAG = PinScreenUtil.class.getSimpleName();
    public static boolean isPinScreenShown;

    static public void askForAccess(Activity activity, boolean forceRestart, OnSecurityCheckPerformedListener onSecurityCheckPerformedListener) {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs() && TimeOutUtil.getInstance().isTimedOut()) {
            if (PrefsUtil.isPinEnabled()) {
                if (TimeOutUtil.getInstance().isFullyTimedOut() || forceRestart) {
                    // Go to PIN entry screen, remove all history, full reconnect is needed.
                    BBLog.d(LOG_TAG, "Show PIN screen, remove history.");
                    Intent pinIntent = new Intent(activity, PinEntryActivity.class);
                    pinIntent.putExtra(PinEntryActivity.EXTRA_CLEAR_HISTORY, true);
                    pinIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(pinIntent);
                    isPinScreenShown = true;
                } else {
                    if (!isPinScreenShown) {
                        // Go to PIN entry screen, but don't clear current state. This allows the user to continue where he left but is less secure as sensitive data is still kept in memory and could theoretically be read out by malicious apps or hackers.
                        BBLog.d(LOG_TAG, "Show PIN screen, keep history.");
                        Intent pinIntent = new Intent(activity, PinEntryActivity.class);
                        pinIntent.putExtra(PinEntryActivity.EXTRA_CLEAR_HISTORY, false);
                        activity.startActivity(pinIntent);
                        isPinScreenShown = true;
                    }
                }
            } else {

                // Check if pin is active according to key store
                boolean isPinActive = false;
                try {
                    isPinActive = new KeystoreUtil().isPinActive();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Only allow access if pin is not active in key store!
                if (isPinActive) {
                    // According to the key store, the pin is still active. This happens if the pin got deleted from the prefs file without also removing the keystore entry.
                    // Basically this would be the case if the PIN hash was removed in a different way than from the apps settings menu. (For example with a file explorer on a rooted device)
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

    public interface OnSecurityCheckPerformedListener {
        void onAccessGranted();
    }

}


