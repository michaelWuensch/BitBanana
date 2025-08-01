package app.michaelwuensch.bitbanana.settings;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.PrefsUtil;


public class SettingsCustomFeeEstimationProviderFragment extends BBPreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsCustomFeeEstimationProviderFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings_custom_fee_estimation_provider, rootKey);

        EditTextPreference etPrefHost = findPreference(PrefsUtil.CUSTOM_FEE_ESTIMATION_PROVIDER_HOST);
        etPrefHost.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && newValue.toString().toLowerCase().contains(".onion") && !PrefsUtil.isTorEnabled()) {
                    Toast.makeText(getActivity(), R.string.settings_requires_tor_toast, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
    }
}