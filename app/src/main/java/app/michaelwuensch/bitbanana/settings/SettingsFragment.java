package app.michaelwuensch.bitbanana.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import app.michaelwuensch.bitbanana.BuildConfig;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.liveTests.LiveTestingActivity;
import app.michaelwuensch.bitbanana.pin.PinSetupActivity;
import app.michaelwuensch.bitbanana.util.AppUtil;
import app.michaelwuensch.bitbanana.util.KeystoreUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;


public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsFragment.class.getSimpleName();

    private SwitchPreference mSwTor;
    private Preference mCurrencyPref;
    private Preference mPinPref;
    private ListPreference mListLanguage;
    private ListPreference mListLockScreenTimeout;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings, rootKey);

        mPinPref = findPreference("pinPref");
        mListLockScreenTimeout = findPreference("lockScreenTimeoutPref");
        lockScreenTimeoutDisplayEntries();

        // Action when clicked on "Features"
        final Preference prefFeaturesPresets = findPreference("goToFeaturesSettings");
        prefFeaturesPresets.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), SettingsFeaturesActivity.class);
                startActivity(intent);
                return true;
            }
        });

        // Action when clicked on "Features"
        mCurrencyPref = findPreference("goToCurrencySettings");
        mCurrencyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), SettingsCurrenciesActivity.class);
                startActivity(intent);
                return true;
            }
        });


        // Show warning on language change as a restart is required.
        mListLanguage = findPreference("language");
        createLanguagesList();
        mListLanguage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().equals("system")) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newValue.toString()));
                }
                return true;
            }
        });

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


        // Action when clicked on "reset all"
        final Preference prefResetAll = findPreference("resetAll");
        prefResetAll.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // We have to use commit here, apply would not finish before the app is restarted.
                PrefsUtil.editPrefs().clear().commit();
                try {
                    PrefsUtil.editEncryptedPrefs().clear().commit();
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
                try {
                    new KeystoreUtil().removePinActiveKey();
                } catch (KeyStoreException | CertificateException | IOException |
                         NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                getActivity().finishAffinity();
                AppUtil.getInstance(getActivity()).restartApp();
                return true;
            }
        });
        // Hide development category in release build
        if (!BuildConfig.BUILD_TYPE.equals("debug")) {
            final PreferenceCategory devCategory = findPreference("devCategory");
            devCategory.setVisible(false);
        }

        // Action when clicked on "advanced settings"
        final Preference prefAdvanced = findPreference("goToAdvanced");
        prefAdvanced.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), AdvancedSettingsActivity.class);
                startActivity(intent);
                return true;
            }
        });

        // Action when clicked on "advanced settings"
        final Preference prefLiveTests = findPreference("goToTests");
        prefLiveTests.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), LiveTestingActivity.class);
                startActivity(intent);
                return true;
            }
        });

        // On hide balance option changed
        ListPreference listPrefHideBalance = findPreference("hideBalanceType");
        listPrefHideBalance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().equals("total")) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.note)
                            .setMessage(R.string.settings_hideTotalBalance_explanation)
                            .setCancelable(true)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }).show();
                    return true;
                } else if (newValue.toString().equals("all")) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.note)
                            .setMessage(R.string.settings_hideAllBalances_explanation)
                            .setCancelable(true)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }).show();
                    return true;
                } else {
                    return true;
                }
            }
        });

        // On tor changed
        mSwTor = findPreference("isTorEnabled");
        mSwTor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newState = Boolean.valueOf(newValue.toString());
                // we have to update this preferences value here, otherwise the value of the preference would be updated AFTER the reconnection has taken place.
                mSwTor.setChecked(newState);

                TorManager.getInstance().switchTorPrefState(newState);

                return true;
            }
        });

        updateLockScreenTimeoutVisibility(PrefsUtil.isPinEnabled());
    }

    @Override
    public void onResume() {
        super.onResume();
        pinOptionText();
        currencyPrefSummary();
        updateLockScreenTimeoutVisibility(PrefsUtil.isPinEnabled());
    }

    private void createLanguagesList() {
        // This is necessary as we want to have "system language translatable, while the languages themselves will not be translatable
        CharSequence[] languageDisplayValues = getActivity().getResources().getStringArray(R.array.languageDisplayValues);
        languageDisplayValues[0] = getActivity().getResources().getString(R.string.settings_systemLanguage);
        mListLanguage.setEntries(languageDisplayValues);
    }

    private void pinOptionText() {
        // Display add or change pin
        if (PrefsUtil.isPinEnabled()) {
            mPinPref.setTitle(R.string.settings_changePin);
        } else {
            mPinPref.setTitle(R.string.settings_addPin);
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

    private void updateLockScreenTimeoutVisibility(boolean isEnabled) {
        mListLockScreenTimeout.setVisible(isEnabled);
    }

    private void currencyPrefSummary() {
        String summary = CurrencyCodeToDisplayString(PrefsUtil.getFirstCurrencyCode());
        if (!PrefsUtil.getSecondCurrencyCode().equals("none"))
            summary = summary + ", " + CurrencyCodeToDisplayString(PrefsUtil.getSecondCurrencyCode());
        if (!PrefsUtil.getThirdCurrencyCode().equals("none"))
            summary = summary + ", " + CurrencyCodeToDisplayString(PrefsUtil.getThirdCurrencyCode());
        if (!PrefsUtil.getForthCurrencyCode().equals("none"))
            summary = summary + ", " + CurrencyCodeToDisplayString(PrefsUtil.getForthCurrencyCode());
        if (!PrefsUtil.getFifthCurrencyCode().equals("none"))
            summary = summary + ", " + CurrencyCodeToDisplayString(PrefsUtil.getFifthCurrencyCode());
        mCurrencyPref.setSummary(summary);
    }

    private String CurrencyCodeToDisplayString(String code) {
        return code.replace("ccBTC", "BTC").replace("ccMBTC", "mBTC").replace("ccBIT", "bit").replace("ccSAT", "sat");
    }
}
