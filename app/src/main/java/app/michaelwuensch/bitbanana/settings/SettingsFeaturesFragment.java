package app.michaelwuensch.bitbanana.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import app.michaelwuensch.bitbanana.R;


public class SettingsFeaturesFragment extends BBPreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsFeaturesFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings_features, rootKey);

        findPreference("featureHelpButtons").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                getActivity().invalidateOptionsMenu();
                return true;
            }
        });
    }
}
