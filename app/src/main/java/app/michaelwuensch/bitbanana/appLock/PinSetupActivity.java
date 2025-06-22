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


public class PinSetupActivity extends BaseAppCompatActivity implements AppLockInterface {

    public static final int ADD_PIN = 0;
    public static final int CHANGE_PIN = 1;

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
                showCreatePin();
                break;
            case CHANGE_PIN:
                showEnterPin();
                break;
        }
    }

    public void pinCreated(String value) {
        showConfirmPin(value);
    }

    public void pinConfirmed(String value) {

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


        if (mSetupMode == ADD_PIN) {
            try {
                new KeystoreUtil().addAppLockActiveKey();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        }
    }

    public void correctAccessDataEntered() {
        if (mSetupMode == CHANGE_PIN) {
            showCreatePin();
        }
    }

    private void showCreatePin() {
        if (mSetupMode == CHANGE_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CREATE_MODE, getResources().getString(R.string.pin_enter_new)));
        } else {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CREATE_MODE, getResources().getString(R.string.pin_create)));
        }
    }

    private void showConfirmPin(String tempPin) {
        if (mSetupMode == CHANGE_PIN) {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CONFIRM_MODE, getResources().getString(R.string.pin_confirm_new), tempPin));
        } else {
            changeFragment(PinSetupFragment.newInstance(PinSetupFragment.CONFIRM_MODE, getResources().getString(R.string.pin_confirm), tempPin));
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
