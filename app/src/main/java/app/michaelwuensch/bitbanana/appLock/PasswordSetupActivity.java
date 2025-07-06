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


public class PasswordSetupActivity extends BaseAppCompatActivity implements AppLockInterface {

    public static final int ADD_PASSWORD = 0;
    public static final int CHANGE_PASSWORD = 1;
    public static final int ADD_EMERGENCY_PASSWORD = 2;
    public static final int CHANGE_EMERGENCY_PASSWORD = 3;

    private static final String LOG_TAG = PasswordSetupActivity.class.getSimpleName();

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

        // Set create password fragment as beginning fragment
        showCreatePassword();

        switch (mSetupMode) {
            case ADD_PASSWORD:
            case ADD_EMERGENCY_PASSWORD:
                showCreatePassword();
                break;
            case CHANGE_PASSWORD:
            case CHANGE_EMERGENCY_PASSWORD:
                showEnterPassword();
                break;
        }
    }

    public void passwordCreated(String value) {
        String hashedValue = UtilFunctions.appLockDataHash(value);
        if (mSetupMode == ADD_PASSWORD || mSetupMode == CHANGE_PASSWORD) {
            if (PrefsUtil.isEmergencyPasswordEnabled()) {
                String emergencyPasswordHash = "";
                try {
                    emergencyPasswordHash = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.EMERGENCY_PASSWORD_HASH, "");
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
                if (hashedValue.equals(emergencyPasswordHash) && !AppLockUtil.isEmergencyUnlocked) {
                    showError(getString(R.string.error_password_and_emergency_password_equal), 3000);
                    return;
                }
            }
            // Make sure to delete all connections but the displayed one when the password is changed during an emergency unlock
            if (AppLockUtil.isEmergencyUnlocked && PrefsUtil.getEmergencyUnlockMode().equals("show_selected_only")) {
                AppLockUtil.emergencyClearAllButWalletToShow();
                // Also remove the emergency unlock afterwards
                try {
                    PrefsUtil.editEncryptedPrefs()
                            .remove(PrefsUtil.EMERGENCY_PASSWORD_HASH)
                            .commit();
                } catch (GeneralSecurityException | IOException e) {
                }
                PrefsUtil.editPrefs().remove("appLockEmergencyModePref").commit();
                AppLockUtil.isEmergencyUnlocked = false;
            }

            // save password hash in encrypted prefs
            try {
                PrefsUtil.editEncryptedPrefs()
                        .putString(PrefsUtil.PASSWORD_HASH, hashedValue)
                        .commit();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }

            if (mSetupMode == ADD_PASSWORD) {
                try {
                    new KeystoreUtil().addAppLockActiveKey();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }
            if (mSetupMode == CHANGE_PASSWORD) {
                // Show success message
                Toast.makeText(PasswordSetupActivity.this, R.string.password_changed, Toast.LENGTH_SHORT).show();

                // Reset the app lock timeout. We don't want to ask for password again...
                TimeOutUtil.getInstance().restartTimer();

                // Go to home screen
                Intent intent = new Intent(PasswordSetupActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } else if (mSetupMode == ADD_EMERGENCY_PASSWORD || mSetupMode == CHANGE_EMERGENCY_PASSWORD) {
            if (AppLockUtil.isEmergencyUnlocked) {
                // In emergency mode when setting a new emergency password, we use the old emergency password and set the actual password to that. That way it still looks like you gave him the correct password.
                try {
                    PrefsUtil.editEncryptedPrefs()
                            .putString(PrefsUtil.PASSWORD_HASH, PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.EMERGENCY_PASSWORD_HASH, ""))
                            .commit();
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
                // Finally we also delete the connections
                AppLockUtil.emergencyClearAllButWalletToShow();
            }
            String passwordHash = "";
            try {
                passwordHash = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.PASSWORD_HASH, "");
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
            if (hashedValue.equals(passwordHash)) {
                showError(getString(R.string.error_password_and_emergency_password_equal), 3000);
                return;
            } else {
                // save emergency password hash in encrypted prefs
                try {
                    PrefsUtil.editEncryptedPrefs()
                            .putString(PrefsUtil.EMERGENCY_PASSWORD_HASH, hashedValue)
                            .commit();
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }

            if (mSetupMode == ADD_EMERGENCY_PASSWORD) {
                finish();
            }
            if (mSetupMode == CHANGE_EMERGENCY_PASSWORD) {
                // Show success message
                Toast.makeText(PasswordSetupActivity.this, R.string.emergency_password_changed, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void correctAccessDataEntered() {
        if (mSetupMode == CHANGE_PASSWORD || mSetupMode == CHANGE_EMERGENCY_PASSWORD) {
            showCreatePassword();
        }
    }

    private void showCreatePassword() {
        if (mSetupMode == ADD_PASSWORD || mSetupMode == CHANGE_PASSWORD) {
            changeFragment(PasswordSetupFragment.newInstance(PasswordSetupFragment.CREATE_MODE));
        } else {
            changeFragment(PasswordSetupFragment.newInstance(PasswordSetupFragment.CREATE_EMERGENCY_MODE));
        }
    }

    private void showEnterPassword() {
        if (mSetupMode == CHANGE_PASSWORD)
            changeFragment(PasswordSetupFragment.newInstance(PasswordSetupFragment.ENTER_MODE));
        else
            changeFragment(PasswordSetupFragment.newInstance(PasswordSetupFragment.EMERGENCY_ENTER_MODE));
    }

    private void changeFragment(Fragment fragment) {
        mFt = getSupportFragmentManager().beginTransaction();
        mFt.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        mCurrentFragment = fragment;
        mFt.replace(R.id.mainContent, mCurrentFragment);
        mFt.commit();
    }
}
