package app.michaelwuensch.bitbanana.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.ExchangeRateUtil;
import app.michaelwuensch.bitbanana.util.FeeEstimationUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UserGuardian;


public class AdvancedSettingsFragment extends BBPreferenceFragmentCompat {

    private static final String LOG_TAG = AdvancedSettingsFragment.class.getSimpleName();
    private SwitchPreference mSwScreenProtection;
    private SwitchPreference mSwUnspecifiedAmountInvoices;
    private ListPreference mListBlockExplorer;
    private ListPreference mListLnExpiry;
    private ListPreference mListFeeLimit;
    private Preference mPrefCustomBlockExplorer;
    private Preference mPrefCustomExchangeRateProvider;
    private Preference mPrefCustomFeeEstimationProvider;
    private EditTextPreference mEtBackendTimeout;
    private EditTextPreference mEtPaymentTimeout;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.advanced_settings, rootKey);

        // On change block explorer option
        mListBlockExplorer = findPreference(PrefsUtil.BLOCK_EXPLORER);
        mListBlockExplorer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && newValue.toString().equalsIgnoreCase("Blockstream (v3 Tor)")) {
                    Toast.makeText(getActivity(), R.string.settings_blockExplorer_tor_toast, Toast.LENGTH_LONG).show();
                }
                updateCustomBlockExplorerOptions(newValue.toString().equalsIgnoreCase("Custom"));
                return true;
            }
        });

        mPrefCustomBlockExplorer = findPreference("goToCustomBlockExplorerSettings");
        mPrefCustomBlockExplorer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent intent = new Intent(getActivity(), SettingsCustomBlockExplorerActivity.class);
                startActivity(intent);
                return true;
            }
        });

        mPrefCustomExchangeRateProvider = findPreference("goToCustomExchangeRateProviderSettings");
        mPrefCustomExchangeRateProvider.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent intent = new Intent(getActivity(), SettingsCustomExchangeRateProviderActivity.class);
                startActivity(intent);
                return true;
            }
        });

        // Request exchange rates when the provider changed
        ListPreference listExchangeRateProvider = findPreference("exchangeRateProvider");
        listExchangeRateProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && newValue.toString().contains("(v3 Tor)") && !PrefsUtil.isTorEnabled()) {
                    Toast.makeText(getActivity(), R.string.settings_requires_tor_toast, Toast.LENGTH_LONG).show();
                }
                boolean isCustom = newValue.toString().equalsIgnoreCase("Custom");
                if (!isCustom) {
                    PrefsUtil.editPrefs().putString(PrefsUtil.EXCHANGE_RATE_PROVIDER, newValue.toString()).commit();
                    ExchangeRateUtil.getInstance().getExchangeRates(true);
                }
                updateCustomExchangeRateProviderOptions(isCustom);
                return true;
            }
        });

        // Request fee estimates when the provider changed
        ListPreference listFeeEstimationProvider = findPreference("feeEstimationProvider");
        listFeeEstimationProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && newValue.toString().contains("(v3 Tor)") && !PrefsUtil.isTorEnabled()) {
                    Toast.makeText(getActivity(), R.string.settings_requires_tor_toast, Toast.LENGTH_LONG).show();
                }
                boolean isCustom = newValue.toString().equalsIgnoreCase("Custom");
                if (!isCustom) {
                    PrefsUtil.editPrefs().putString(PrefsUtil.FEE_ESTIMATION_PROVIDER, newValue.toString()).commit();
                    FeeEstimationUtil.getInstance().getFeeEstimates();
                }
                updateCustomFeeEstimationProviderOptions(isCustom);
                return true;
            }
        });

        mPrefCustomFeeEstimationProvider = findPreference("goToCustomFeeEstimationProviderSettings");
        mPrefCustomFeeEstimationProvider.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent intent = new Intent(getActivity(), SettingsCustomFeeEstimationProviderActivity.class);
                startActivity(intent);
                return true;
            }
        });

        mEtBackendTimeout = findPreference("backendTimeout");
        mEtBackendTimeout.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setText(String.valueOf(PrefsUtil.getBackendTimeout()));
            }
        });
        mEtBackendTimeout.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                int timeout = 0;
                try {
                    timeout = Integer.parseInt(newValue.toString());
                } catch (NumberFormatException ignored) {
                    timeout = 20;
                }
                if (timeout < 10) {
                    timeout = 10;
                }
                if (timeout > 300)
                    timeout = 300;
                PrefsUtil.editPrefs().putString(PrefsUtil.BACKEND_TIMEOUT, String.valueOf(timeout)).commit();
                setTimeoutSummaries();
                return false;
            }
        });

        // Action when clicked on "payment identification"
        final Preference prefPayerData = findPreference("goToPayerDataSettings");
        prefPayerData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), SettingsPayerDataActivity.class);
                startActivity(intent);
                return true;
            }
        });

        mEtPaymentTimeout = findPreference("paymentTimeout");
        mEtPaymentTimeout.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setText(String.valueOf(PrefsUtil.getPaymentTimeout()));
            }
        });
        mEtPaymentTimeout.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                int timeout = 0;
                try {
                    timeout = Integer.parseInt(newValue.toString());
                } catch (NumberFormatException ignored) {
                    timeout = 60;
                }
                if (timeout < 20) {
                    timeout = 20;
                }
                if (timeout > 300)
                    timeout = 300;
                PrefsUtil.editPrefs().putString(PrefsUtil.PAYMENT_TIMEOUT, String.valueOf(timeout)).commit();
                setTimeoutSummaries();
                return false;
            }
        });

        // Create invoice expiry display entries. For the sake of plurals this has to be done by code.
        mListLnExpiry = findPreference("lightning_expiry");
        createLnExpiryDisplayEntries();

        mListFeeLimit = findPreference("lightning_feeLimit");
        mListFeeLimit.setOnPreferenceChangeListener((preference, newValue) -> {
            setFeeSummary(preference, newValue.toString());
            return true;
        });

        setFeeSummary(mListFeeLimit, mListFeeLimit.getValue());
        setTimeoutSummaries();

        // On change unspecified amount invoices
        mSwUnspecifiedAmountInvoices = findPreference("unspecifiedAmountInvoices");
        mSwUnspecifiedAmountInvoices.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!mSwUnspecifiedAmountInvoices.isChecked()) {
                    // Ask user to confirm enabling unspecified amount invoices
                    new UserGuardian(getActivity(), new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onConfirmed() {
                            mSwUnspecifiedAmountInvoices.setChecked(true);
                        }

                        @Override
                        public void onCancelled() {

                        }
                    }).securityAllowUnspecifiedAmountInvoices();
                    // the value is set from the guardian callback, that's why we don't change switch state here.
                    return false;
                } else {
                    return true;
                }
            }
        });

        // On change screen recording option
        mSwScreenProtection = findPreference("preventScreenRecording");
        mSwScreenProtection.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mSwScreenProtection.isChecked()) {
                    // Ask user to confirm disabling screen protection
                    new UserGuardian(getActivity(), new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onConfirmed() {
                            mSwScreenProtection.setChecked(false);
                            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                        }

                        @Override
                        public void onCancelled() {

                        }
                    }).securityScreenProtection();
                    // the value is set from the guardian callback, that's why we don't change switch state here.
                    return false;
                } else {
                    getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    return true;
                }
            }
        });

        // Action when clicked on "reset security warnings"
        final Preference prefResetGuardian = findPreference("resetGuardian");
        prefResetGuardian.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                UserGuardian.reenableAllSecurityWarnings();
                Toast.makeText(getActivity(), R.string.guardian_reset, Toast.LENGTH_LONG).show();
                return true;
            }
        });

        // Action when clicked on "On-chainFeePresets"
        final Preference prefOnDecoyAppSettings = findPreference("goToDecoyAppSettings");
        prefOnDecoyAppSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), SettingsDecoyAppsActivity.class);
                startActivity(intent);
                return true;
            }
        });

        updateCustomBlockExplorerOptions(PrefsUtil.getBlockExplorer().equalsIgnoreCase("Custom"));
        updateCustomExchangeRateProviderOptions(PrefsUtil.getExchangeRateProvider().equalsIgnoreCase("Custom"));
        updateCustomFeeEstimationProviderOptions(PrefsUtil.getFeeEstimationProvider().equalsIgnoreCase("Custom"));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCustomBlockExplorerOptions(PrefsUtil.getBlockExplorer().equalsIgnoreCase("Custom"));
        updateCustomExchangeRateProviderOptions(PrefsUtil.getExchangeRateProvider().equalsIgnoreCase("Custom"));
        updateCustomFeeEstimationProviderOptions(PrefsUtil.getFeeEstimationProvider().equalsIgnoreCase("Custom"));
    }

    private void setFeeSummary(Preference preference, String value) {
        String s = value.replace("%", "%%");
        String string = getString(R.string.fee_limit_threshold, RefConstants.LN_PAYMENT_FEE_THRESHOLD, s);
        preference.setSummary(string);
    }

    private void setTimeoutSummaries() {
        int requestTimeoutSeconds = PrefsUtil.getBackendTimeout();
        mEtBackendTimeout.setSummary(getResources().getQuantityString(R.plurals.duration_second, requestTimeoutSeconds, requestTimeoutSeconds));
        int paymentTimeoutSeconds = PrefsUtil.getPaymentTimeout();
        mEtPaymentTimeout.setSummary(getResources().getQuantityString(R.plurals.duration_second, paymentTimeoutSeconds, paymentTimeoutSeconds));
    }

    private void createLnExpiryDisplayEntries() {
        CharSequence[] lnExpiryDisplayEntries = new CharSequence[9];
        lnExpiryDisplayEntries[0] = getActivity().getResources().getQuantityString(R.plurals.duration_minute, 1, 1);
        lnExpiryDisplayEntries[1] = getActivity().getResources().getQuantityString(R.plurals.duration_minute, 10, 10);
        lnExpiryDisplayEntries[2] = getActivity().getResources().getQuantityString(R.plurals.duration_minute, 30, 30);
        lnExpiryDisplayEntries[3] = getActivity().getResources().getQuantityString(R.plurals.duration_hour, 1, 1);
        lnExpiryDisplayEntries[4] = getActivity().getResources().getQuantityString(R.plurals.duration_hour, 6, 6);
        lnExpiryDisplayEntries[5] = getActivity().getResources().getQuantityString(R.plurals.duration_day, 1, 1);
        lnExpiryDisplayEntries[6] = getActivity().getResources().getQuantityString(R.plurals.duration_week, 1, 1);
        lnExpiryDisplayEntries[7] = getActivity().getResources().getQuantityString(R.plurals.duration_month, 1, 1);
        lnExpiryDisplayEntries[8] = getActivity().getResources().getQuantityString(R.plurals.duration_year, 1, 1);

        mListLnExpiry.setEntries(lnExpiryDisplayEntries);
    }

    private void updateCustomBlockExplorerOptions(boolean customEnabled) {
        mPrefCustomBlockExplorer.setVisible(customEnabled);
        mPrefCustomBlockExplorer.setSummary(PrefsUtil.getCustomBlockExplorerHost());
    }

    private void updateCustomExchangeRateProviderOptions(boolean customEnabled) {
        mPrefCustomExchangeRateProvider.setVisible(customEnabled);
        mPrefCustomExchangeRateProvider.setSummary(PrefsUtil.getCustomExchangeRateProviderHost());
    }

    private void updateCustomFeeEstimationProviderOptions(boolean customEnabled) {
        mPrefCustomFeeEstimationProvider.setVisible(customEnabled);
        mPrefCustomFeeEstimationProvider.setSummary(PrefsUtil.getCustomFeeEstimationProviderHost());
    }
}
