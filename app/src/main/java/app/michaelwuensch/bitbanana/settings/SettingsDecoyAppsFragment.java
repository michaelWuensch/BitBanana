package app.michaelwuensch.bitbanana.settings;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.PrefsUtil;


public class SettingsDecoyAppsFragment extends PreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsDecoyAppsFragment.class.getSimpleName();
    private SwitchPreference mSwOnOff;
    private Preference mExplanation;
    public static final String PACKAGENAME = "app.michaelwuensch.bitbanana";


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings_decoy_apps, rootKey);

        mExplanation = findPreference("decoyAppExplanation");

        mSwOnOff = findPreference("stealthModeActive");
        mSwOnOff.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if (mSwOnOff.isChecked()) {
                    getActivity().getPackageManager().setComponentEnabledSetting(
                            new ComponentName(getActivity().getPackageName(), PACKAGENAME + ".LauncherActivity"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    getActivity().getPackageManager().setComponentEnabledSetting(
                            new ComponentName(getActivity().getPackageName(), PACKAGENAME + ".decoyApps.CalcActivity"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    mExplanation.setVisible(false);
                } else {
                    getActivity().getPackageManager().setComponentEnabledSetting(
                            new ComponentName(getActivity().getPackageName(), PACKAGENAME + ".LauncherActivity"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    getActivity().getPackageManager().setComponentEnabledSetting(
                            new ComponentName(getActivity().getPackageName(), PACKAGENAME + ".decoyApps.CalcActivity"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    mExplanation.setVisible(true);
                }
                return true;
            }
        });

        mExplanation.setVisible(PrefsUtil.getPrefs().getBoolean("stealthModeActive", false));
    }
}
