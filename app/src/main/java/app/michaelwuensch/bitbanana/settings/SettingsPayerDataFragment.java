package app.michaelwuensch.bitbanana.settings;

import android.os.Bundle;

import app.michaelwuensch.bitbanana.R;


public class SettingsPayerDataFragment extends BBPreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsPayerDataFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings_payer_data, rootKey);
    }
}
