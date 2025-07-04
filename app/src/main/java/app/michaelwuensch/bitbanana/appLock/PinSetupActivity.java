package app.michaelwuensch.bitbanana.appLock;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;
import java.security.GeneralSecurityException;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.util.AppLockUtil;
import app.michaelwuensch.bitbanana.util.KeystoreUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.util.UtilFunctions;


public class PinSetupActivity extends BaseAppCompatActivity implements AppLockInterface {

    public static final int ADD_PIN = 0;
    public static final int CHANGE_PIN = 1;
    public static final int ADD_EMERGENCY_PIN = 2;
    public static final int CHANGE_EMERGENCY_PIN = 3;

    private static final String LOG_TAG = PinSetupActivity.class.getSimpleName();

    private Fragment mCurrentFragment = null;
    private FragmentTransaction mFt;
    private int mSetupMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSetupMode = extras.getInt(RefConstants.SETUP_MODE, 0);
        }


        // Set create pin fragment as beginning fragment
        showCreatePin();

        switch (mSetupMode) {
            case ADD_PIN:
            case ADD_EMERGENCY_PIN:
                showCreatePin();
                break;
            case CHANGE_PIN:
            case CHANGE_EMERGENCY_PIN:
                showEnterPin();
                break;
        }
    }

    public void pinCreated(String value) {
        String hashedValue = UtilFunctions.appLockDataHash(value);
        if (mSetupMode == ADD_EMERGENCY_PIN || mSetupMode == CHANGE_EMERGENCY_PIN) {
            String pinHash = "";
            try {
                pinHash = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.PIN_HASH, "");
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
            if (hashedValue.equals(pinHash)) {
                showError(getString(R.string.error_pin_and_emergency_pin_equal), 3000);
            } else {
                showConfirmPin(value);
            }
        } else if (mSetupMode == ADD_PIN || mSetupMode == CHANGE_PIN) {
            if (PrefsUtil.isEmergencyPinEnabled()) {
                String emergencyPinHash = "";
                try {
                    emergencyPinHash = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.EMERGENCY_PIN_HASH, "");
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
                if (hashedValue.equals(emergencyPinHash) && !AppLockUtil.isEmergencyUnlocked) {
                    showError(getString(R.string.error_pin_and_emergency_pin_equal), 3000);
                } else {
                    showConfirmPin(value);
                }
            } else {
                showConfirmPin(value);
            }
        }
    }

    public void pinConfirmed(String value) {

        if (mSetupMode == ADD_PIN || mSetupMode == CHANGE_PIN) {
            // save pin hash in encrypted prefs
            try {
                PrefsUtil.editEncryptedPrefs()
                        .putString(PrefsUtil.PIN_HASH, UtilFunctions.appLockDataHash(value))
                        .commit();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }

            // save pin length in preferences
            PrefsUtil.editPrefs()
                    .putInt(PrefsUtil.PIN_LENGTH, value.length())
                    .commit();

            // Make sure to delete all connections but the displayed one when the PIN is changed during an emergency unlock
            if (AppLockUtil.isEmergencyUnlocked && PrefsUtil.getEmergencyUnlockMode().equals("show_selected_only")) {
                AppLockUtil.emergencyClearAllButWalletToShow();
                // Also remove the emergency unlock afterwards
                try {
                    PrefsUtil.editEncryptedPrefs()
                            .remove(PrefsUtil.EMERGENCY_PIN_HASH)
                            .commit();
                } catch (GeneralSecurityException | IOException e) {
                }
                PrefsUtil.editPrefs().remove("appLockEmergencyModePref").commit();
                AppLockUtil.isEmergencyUnlocked = false;
            }
        } else if (mSetupMode == ADD_EMERGENCY_PIN || mSetupMode == CHANGE_EMERGENCY_PIN) {
            if (AppLockUtil.isEmergencyUnlocked) {
                // In emergency mode when setting a new emergency PIN, we use the old emergency PIN and set the actual PIN to that. That way it still looks like you gave him the correct PIN.
                try {
                    PrefsUtil.editEncryptedPrefs()
                            .putString(PrefsUtil.PIN_HASH, PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.EMERGENCY_PIN_HASH, ""))
                            .commit();
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
                // Finally we also delete the connections
                AppLockUtil.emergencyClearAllButWalletToShow();
            }
            // save emergency pin hash in encrypted prefs
            try {
                PrefsUtil.editEncryptedPrefs()
                        .putString(PrefsUtil.EMERGENCY_PIN_HASH, UtilFunctions.appLockDataHash(value))
                        .commit();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        }

        if (mSetupMode == ADD_PIN) {
            try {
                new KeystoreUtil().addAppLockActiveKey();
            } catch (Exception e) {
                e.printStackTrace();
            }
            finish();
        }
        if (mSetupMode == ADD_EMERGENCY_PIN) {
            finish();
        }
        if (mSetupMode == CHANGE_PIN) {
            // Show success message
            Toast.makeText(PinSetupActivity.this, R.string.pin_changed, Toast.LENGTH_SHORT).show();

            // Reset the app lock timeout. We don't want to ask for PIN again...
            TimeOutUtil.getInstance().restartTimer();

            // Go to home screen
            Intent intent = new Intent(PinSetupActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (mSetupMode == CHANGE_EMERGENCY_PIN) {
            // Show success message
            Toast.makeText(PinSetupActivity.this, R.string.emergency_pin_changed, Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    public void correctAccessDataEntered() {
        if (mSetupMode == CHANGE_PIN || mSetupMode == CHANGE_EMERGENCY_PIN) {
            showCreatePin();
        }
    }

    private void showCreatePin() {
        if (mSetupMode == CHANGE_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CREATE_MODE, getResources().getString(R.string.pin_enter_new)));
        } else if (mSetupMode == ADD_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CREATE_MODE, getResources().getString(R.string.pin_create)));
        } else if (mSetupMode == CHANGE_EMERGENCY_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CREATE_EMERGENCY_MODE, getResources().getString(R.string.emergency_pin_enter_new)));
        } else {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CREATE_EMERGENCY_MODE, getResources().getString(R.string.emergency_pin_create)));
        }
    }

    private void showConfirmPin(String tempPin) {
        if (mSetupMode == CHANGE_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CONFIRM_MODE, getResources().getString(R.string.pin_confirm_new), tempPin));
        } else if (mSetupMode == ADD_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CONFIRM_MODE, getResources().getString(R.string.pin_confirm), tempPin));
        } else if (mSetupMode == CHANGE_EMERGENCY_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CONFIRM_EMERGENCY_MODE, getResources().getString(R.string.emergency_pin_confirm_new), tempPin));
        } else {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CONFIRM_EMERGENCY_MODE, getResources().getString(R.string.emergency_pin_confirm), tempPin));
        }
    }

    private void showEnterPin() {
        if (mSetupMode == CHANGE_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.ENTER_MODE, getResources().getString(R.string.pin_enter_old)));
        } else {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.ENTER_MODE, getResources().getString(R.string.pin_enter)));
        }
    }

    private void changeFragment(Fragment fragment) {
        mFt = getSupportFragmentManager().beginTransaction();
        mFt.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        mCurrentFragment = fragment;
        mFt.replace(R.id.mainContent, mCurrentFragment);
        mFt.commit();
    }

}
