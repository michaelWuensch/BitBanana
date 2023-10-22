package app.michaelwuensch.bitbanana.settings;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.PrefsUtil;


public class SettingsCustomBlockExplorerFragment extends PreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsCustomBlockExplorerFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings_custom_block_eplorer, rootKey);

        EditTextPreference etPrefHost = findPreference(PrefsUtil.CUSTOM_BLOCK_EXPLORER_HOST);
        etPrefHost.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && newValue.toString().toLowerCase().contains(".onion")) {
                    Toast.makeText(getActivity(), R.string.settings_blockExplorer_tor_toast, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
    }
}