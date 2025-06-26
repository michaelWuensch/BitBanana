package app.michaelwuensch.bitbanana.settings;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

abstract public class BBPreferenceFragmentCompat extends PreferenceFragmentCompat {

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ListPreference) {
            final DialogFragment fragment =
                    MessageListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), "MessageListPreferenceDialog");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
