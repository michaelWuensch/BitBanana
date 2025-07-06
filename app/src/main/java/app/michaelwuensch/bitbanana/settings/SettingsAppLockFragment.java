package app.michaelwuensch.bitbanana.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.appLock.PasswordSetupActivity;
import app.michaelwuensch.bitbanana.appLock.PinSetupActivity;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.util.AppLockUtil;
import app.michaelwuensch.bitbanana.util.BiometricUtil;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UserGuardian;


public class SettingsAppLockFragment extends BBPreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsAppLockFragment.class.getSimpleName();

    private Preference mPinPref;
    private Preference mPasswordPref;
    private Preference mEmergencyPref;
    private SwitchPreference mSwBiometrics;
    private ListPreference mListLockScreenTimeout;
    private PreferenceCategory mPinOptionsCategory;
    private SwitchPreference mSwScrambledPin;
    private CustomFakePreferenceCategory mEmergencyCategory;
    private ListPreference mListEmergencyMode;
    private ListPreference mListWalletToShow;
    private ListPreference mListEmergencyModeFake;
    private ListPreference mListWalletToShowFake;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings_app_lock, rootKey);

        mPinPref = findPreference("pinPref");
        mPasswordPref = findPreference("passwordPref");
        mEmergencyPref = findPreference("appLockEmergencyPref");
        mSwBiometrics = findPreference("biometricsEnabled");
        mListLockScreenTimeout = findPreference("lockScreenTimeoutPref");
        mPinOptionsCategory = findPreference("pinOptionsCategory");
        mEmergencyCategory = findPreference("appLockEmergencyCategory");
        mListEmergencyMode = findPreference("appLockEmergencyModePref");
        mListWalletToShow = findPreference("appLockEmergencyWalletToShowPref");
        mListEmergencyModeFake = findPreference("appLockEmergencyModePrefFake");
        mListWalletToShowFake = findPreference("appLockEmergencyWalletToShowPrefFake");

        lockScreenTimeoutDisplayEntries();

        // Action when clicked on the pin preference
        mPinPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (PrefsUtil.isPinEnabled()) {
                    Intent intent = new Intent(getActivity(), PinSetupActivity.class);
                    intent.putExtra(RefConstants.SETUP_MODE, PinSetupActivity.CHANGE_PIN);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getActivity(), PinSetupActivity.class);
                    intent.putExtra(RefConstants.SETUP_MODE, PinSetupActivity.ADD_PIN);
                    startActivity(intent);
                }
                return true;
            }
        });

        // Action when clicked on the password preference
        mPasswordPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (PrefsUtil.isPasswordEnabled()) {
                    Intent intent = new Intent(getActivity(), PasswordSetupActivity.class);
                    intent.putExtra(RefConstants.SETUP_MODE, PasswordSetupActivity.CHANGE_PASSWORD);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getActivity(), PasswordSetupActivity.class);
                    intent.putExtra(RefConstants.SETUP_MODE, PasswordSetupActivity.ADD_PASSWORD);
                    startActivity(intent);
                }
                return true;
            }
        });

        mEmergencyCategory.setOnButtonClickListener(new CustomFakePreferenceCategory.OnButtonClickListener() {
            @Override
            public void onButtonClick() {
                HelpDialogUtil.showDialogWithLink(getActivity(), R.string.help_dialog_emergency_pin_password, getString(R.string.documentation), RefConstants.URL_DOCS_EMERGENCY_PASSWORD_PIN);
            }
        });

        // Action when clicked on the emergency PIN/password preference
        mEmergencyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (BiometricUtil.hardwareAvailable() && PrefsUtil.isBiometricEnabled()) {
                    new UserGuardian(getActivity(), new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onConfirmed() {
                            goToEmergencyUnlockSetup();
                        }

                        @Override
                        public void onCancelled() {

                        }
                    }).securityEmergencyUnlockIneffectiveDisableBiometrics();
                } else {
                    goToEmergencyUnlockSetup();
                }
                return true;
            }
        });

        mListEmergencyMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateSettingsScreen();
                    }
                }, 10);
                return true;
            }
        });

        mListEmergencyModeFake.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateSettingsScreen();
                    }
                }, 10);
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

        mSwBiometrics.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!mSwBiometrics.isChecked() && (PrefsUtil.isEmergencyPinEnabled() || PrefsUtil.isEmergencyPasswordEnabled())) {
                    // Ask user to confirm enabling biometrics if an emergency unlock is set
                    new UserGuardian(getActivity(), new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onConfirmed() {
                            mSwBiometrics.setChecked(true);
                        }

                        @Override
                        public void onCancelled() {

                        }
                    }).securityEmergencyUnlockIneffectiveDoNotEnableBiometrics();
                    // the value is set from the guardian callback, that's why we don't change switch state here.
                    return false;
                } else {
                    return true;
                }
            }
        });

        updateSettingsScreen();
    }

    private void goToEmergencyUnlockSetup() {
        if (PrefsUtil.isPinEnabled()) {
            if (PrefsUtil.isEmergencyPinEnabled() && !AppLockUtil.isEmergencyUnlocked) {
                Intent intent = new Intent(getActivity(), PinSetupActivity.class);
                intent.putExtra(RefConstants.SETUP_MODE, PinSetupActivity.CHANGE_EMERGENCY_PIN);
                startActivity(intent);
            } else {
                Intent intent = new Intent(getActivity(), PinSetupActivity.class);
                intent.putExtra(RefConstants.SETUP_MODE, PinSetupActivity.ADD_EMERGENCY_PIN);
                startActivity(intent);
            }
        } else if (PrefsUtil.isPasswordEnabled()) {
            if (PrefsUtil.isEmergencyPasswordEnabled() && !AppLockUtil.isEmergencyUnlocked) {
                Intent intent = new Intent(getActivity(), PasswordSetupActivity.class);
                intent.putExtra(RefConstants.SETUP_MODE, PasswordSetupActivity.CHANGE_EMERGENCY_PASSWORD);
                startActivity(intent);
            } else {
                Intent intent = new Intent(getActivity(), PasswordSetupActivity.class);
                intent.putExtra(RefConstants.SETUP_MODE, PasswordSetupActivity.ADD_EMERGENCY_PASSWORD);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSettingsScreen();
        updateWalletToShowList();
    }

    private void updatePinPrefText() {
        // Display add or change pin
        if (PrefsUtil.isPinEnabled()) {
            mPinPref.setTitle(R.string.settings_changePin);
        } else {
            mPinPref.setTitle(R.string.settings_addPin);
        }
    }

    private void updatePasswordPrefText() {
        // Display add or change password
        if (PrefsUtil.isPasswordEnabled()) {
            mPasswordPref.setTitle(R.string.settings_change_password);
        } else {
            mPasswordPref.setTitle(R.string.settings_add_password);
        }
    }

    private void updateEmergencyPrefsTexts() {
        if (PrefsUtil.isPinEnabled()) {
            mEmergencyCategory.setTitle(R.string.settings_app_lock_emergency_category_pin);
            if (PrefsUtil.isEmergencyPinEnabled() && !AppLockUtil.isEmergencyUnlocked)
                mEmergencyPref.setTitle(R.string.settings_changeEmergencyPin);
            else
                mEmergencyPref.setTitle(R.string.settings_addEmergencyPin);
        } else {
            mEmergencyCategory.setTitle(R.string.settings_app_lock_emergency_category_password);
            if (PrefsUtil.isEmergencyPasswordEnabled() && !AppLockUtil.isEmergencyUnlocked)
                mEmergencyPref.setTitle(R.string.settings_changeEmergencyPassword);
            else
                mEmergencyPref.setTitle(R.string.settings_addEmergencyPassword);
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

    private void updateSettingsScreen() {
        updatePinPrefText();
        updatePasswordPrefText();
        updateEmergencyPrefsTexts();
        mPinPref.setVisible(!PrefsUtil.isPasswordEnabled());
        mPinOptionsCategory.setVisible(PrefsUtil.isPinEnabled());
        mPasswordPref.setVisible(!PrefsUtil.isPinEnabled());
        mSwBiometrics.setVisible(BiometricUtil.hardwareAvailable() && (PrefsUtil.isPasswordEnabled() || PrefsUtil.isPinEnabled()));

        if (PrefsUtil.isPasswordEnabled() || PrefsUtil.isPinEnabled()) {
            mListLockScreenTimeout.setVisible(true);
            mEmergencyCategory.setVisible(true);
            mEmergencyPref.setVisible(true);
            mListEmergencyMode.setVisible(!AppLockUtil.isEmergencyUnlocked);
            mListEmergencyModeFake.setVisible(AppLockUtil.isEmergencyUnlocked);

            String emergencyMode = PrefsUtil.getPrefs().getString("appLockEmergencyModePref", "erase");
            String emergencyModeFake = PrefsUtil.getPrefs().getString("appLockEmergencyModePrefFake", "erase");
            mListWalletToShow.setVisible(!AppLockUtil.isEmergencyUnlocked && emergencyMode.equals("show_selected_only"));
            mListWalletToShowFake.setVisible(AppLockUtil.isEmergencyUnlocked && emergencyModeFake.equals("show_selected_only"));
        } else {
            mListLockScreenTimeout.setVisible(false);
            mEmergencyCategory.setVisible(false);
            mEmergencyPref.setVisible(false);
            mListEmergencyMode.setVisible(false);
            mListWalletToShow.setVisible(false);
            mListEmergencyModeFake.setVisible(false);
            mListWalletToShowFake.setVisible(false);
        }
    }

    private void updateWalletToShowList() {
        CharSequence[] items;
        CharSequence[] itemValues;

        List<BackendConfig> backendConfigs = new ArrayList<>();
        for (BackendConfig config : BackendConfigsManager.getInstance().getAllBackendConfigs(false)) {
            if (config.wasAddedInEmergencyMode() == AppLockUtil.isEmergencyUnlocked || (AppLockUtil.isEmergencyUnlocked && PrefsUtil.getEmergencyUnlockMode().equals("show_selected_only") && PrefsUtil.getPrefs().getString("appLockEmergencyWalletToShowPref", "").equals(config.getId())))
                backendConfigs.add(config);
        }
        items = new String[backendConfigs.size()];
        itemValues = new String[items.length];
        for (int i = 0; i < backendConfigs.size(); i++) {
            if (backendConfigs.get(i).getNetwork() == BackendConfig.Network.MAINNET || backendConfigs.get(i).getNetwork() == BackendConfig.Network.UNKNOWN || backendConfigs.get(i).getNetwork() == null)
                items[i] = backendConfigs.get(i).getAlias();
            else
                items[i] = backendConfigs.get(i).getAlias() + " (" + backendConfigs.get(i).getNetwork().getDisplayName() + ")";
            itemValues[i] = backendConfigs.get(i).getId();
        }

        mListWalletToShow.setEntries(items);
        mListWalletToShow.setEntryValues(itemValues);
        mListWalletToShowFake.setEntries(items);
        mListWalletToShowFake.setEntryValues(itemValues);
    }
}
