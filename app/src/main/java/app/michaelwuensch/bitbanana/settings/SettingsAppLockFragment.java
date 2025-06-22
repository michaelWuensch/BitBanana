package app.michaelwuensch.bitbanana.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.appLock.PasswordSetupActivity;
import app.michaelwuensch.bitbanana.appLock.PinSetupActivity;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.util.BiometricUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UserGuardian;


public class SettingsAppLockFragment extends PreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsAppLockFragment.class.getSimpleName();

    private Preference mPinPref;
    private Preference mPasswordPref;
    private SwitchPreference mSwBiometrics;
    private ListPreference mListLockScreenTimeout;
    private PreferenceCategory mPinOptionsCategory;
    private SwitchPreference mSwScrambledPin;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings_app_lock, rootKey);

        mPinPref = findPreference("pinPref");
        mPasswordPref = findPreference("passwordPref");
        mSwBiometrics = findPreference("biometricsEnabled");
        mListLockScreenTimeout = findPreference("lockScreenTimeoutPref");
        mPinOptionsCategory = findPreference("pinOptionsCategory");
        lockScreenTimeoutDisplayEntries();

        // Action when clicked on the pin preference
        mPinPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                    if (PrefsUtil.isPinEnabled()) {
                        Intent intent = new Intent(getActivity(), PinSetupActivity.class);
                        intent.putExtra(RefConstants.SETUP_MODE, PinSetupActivity.CHANGE_PIN);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getActivity(), PinSetupActivity.class);
                        intent.putExtra(RefConstants.SETUP_MODE, PinSetupActivity.ADD_PIN);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        // Action when clicked on the password preference
        mPasswordPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                    if (PrefsUtil.isPasswordEnabled()) {
                        Intent intent = new Intent(getActivity(), PasswordSetupActivity.class);
                        intent.putExtra(RefConstants.SETUP_MODE, PasswordSetupActivity.CHANGE_PASSWORD);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getActivity(), PasswordSetupActivity.class);
                        intent.putExtra(RefConstants.SETUP_MODE, PasswordSetupActivity.ADD_PASSWORD);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });


        // On change scramble pin option
        mSwScrambledPin = findPreference("scramblePin");
        mSwScrambledPin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mSwScrambledPin.isChecked()) {
                    // Ask user to confirm disabling scramble
                    new UserGuardian(getActivity(), new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onConfirmed() {
                            mSwScrambledPin.setChecked(false);
                        }

                        @Override
                        public void onCancelled() {

                        }
                    }).securityScrambledPin();
                    // the value is set from the guardian callback, that's why we don't change switch state here.
                    return false;
                } else {
                    return true;
                }
            }
        });

        updateVisibilities();
    }

    @Override
    public void onResume() {
        super.onResume();
        pinOptionText();
        passwordOptionText();
        updateVisibilities();
    }

    private void pinOptionText() {
        // Display add or change pin
        if (PrefsUtil.isPinEnabled()) {
            mPinPref.setTitle(R.string.settings_changePin);
        } else {
            mPinPref.setTitle(R.string.settings_addPin);
        }
    }

    private void passwordOptionText() {
        // Display add or change password
        if (PrefsUtil.isPasswordEnabled()) {
            mPasswordPref.setTitle(R.string.settings_change_password);
        } else {
            mPasswordPref.setTitle(R.string.settings_add_password);
        }
    }

    private void lockScreenTimeoutDisplayEntries() {
        CharSequence[] lockScreenTimeoutDisplayEntries = new CharSequence[5];
        lockScreenTimeoutDisplayEntries[0] = getActivity().getResources().getString(R.string.immediately);
        lockScreenTimeoutDisplayEntries[1] = getActivity().getResources().getQuantityString(R.plurals.duration_second, 10, 10);
        lockScreenTimeoutDisplayEntries[2] = getActivity().getResources().getQuantityString(R.plurals.duration_second, 30, 30);
        lockScreenTimeoutDisplayEntries[3] = getActivity().getResources().getQuantityString(R.plurals.duration_minute, 1, 1);
        lockScreenTimeoutDisplayEntries[4] = getActivity().getResources().getQuantityString(R.plurals.duration_minute, 5, 5);

        mListLockScreenTimeout.setEntries(lockScreenTimeoutDisplayEntries);
    }

    private void updateVisibilities() {
        mPinPref.setVisible(!PrefsUtil.isPasswordEnabled());
        mPasswordPref.setVisible(!PrefsUtil.isPinEnabled());
        mListLockScreenTimeout.setVisible(PrefsUtil.isPasswordEnabled() || PrefsUtil.isPinEnabled());
        if (!BiometricUtil.hardwareAvailable()) {
            mSwBiometrics.setVisible(false);
        } else {
            mSwBiometrics.setVisible(PrefsUtil.isPasswordEnabled() || PrefsUtil.isPinEnabled());
        }
        mPinOptionsCategory.setVisible(PrefsUtil.isPinEnabled());
    }
}
