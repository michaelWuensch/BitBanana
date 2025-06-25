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
import app.michaelwuensch.bitbanana.util.KeystoreUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.util.UtilFunctions;


public class PasswordSetupActivity extends BaseAppCompatActivity implements AppLockInterface {

    public static final int ADD_PASSWORD = 0;
    public static final int CHANGE_PASSWORD = 1;

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
                showCreatePassword();
                break;
            case CHANGE_PASSWORD:
                showEnterPassword();
                break;
        }
    }

    public void passwordCreated(String value) {
        // save password hash in encrypted prefs
        try {
            PrefsUtil.editEncryptedPrefs()
                    .putString(PrefsUtil.PASSWORD_HASH, UtilFunctions.appLockDataHash(value))
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
    }

    public void correctAccessDataEntered() {
        if (mSetupMode == CHANGE_PASSWORD) {
            showCreatePassword();
        }
    }

    private void showCreatePassword() {
        if (mSetupMode == CHANGE_PASSWORD) {
            changeFragment(PasswordSetupFragment.newInstance(PasswordSetupFragment.CREATE_MODE, getResources().getString(R.string.pin_enter_new)));
        } else {
            changeFragment(PasswordSetupFragment.newInstance(PasswordSetupFragment.CREATE_MODE, getResources().getString(R.string.pin_create)));
        }
    }

    private void showEnterPassword() {
        if (mSetupMode == CHANGE_PASSWORD) {
            changeFragment(PasswordSetupFragment.newInstance(PasswordSetupFragment.ENTER_MODE, getResources().getString(R.string.pin_enter_old)));
        } else {
            changeFragment(PasswordSetupFragment.newInstance(PasswordSetupFragment.ENTER_MODE, getResources().getString(R.string.pin_enter)));
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
