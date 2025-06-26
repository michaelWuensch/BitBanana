package app.michaelwuensch.bitbanana.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import app.michaelwuensch.bitbanana.R;

public class MessageListPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private CharSequence[] entries;
    private CharSequence[] entryValues;
    private int selectedIndex = -1;

    public static MessageListPreferenceDialogFragmentCompat newInstance(String key) {
        MessageListPreferenceDialogFragmentCompat fragment = new MessageListPreferenceDialogFragmentCompat();
        Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.list_preference_dialog_with_message, null);

        ListPreference preference = (ListPreference) getPreference();
        entries = preference.getEntries();
        entryValues = preference.getEntryValues();
        selectedIndex = preference.findIndexOfValue(preference.getValue());

        // Set dialog title
        builder.setTitle(preference.getDialogTitle());
        builder.setView(view);

        // Set dialog message
        TextView messageView = view.findViewById(R.id.dialog_message);
        if (preference.getDialogMessage() != null) {
            messageView.setText(preference.getDialogMessage());
        } else {
            messageView.setVisibility(View.GONE);
        }

        // Set up ListView manually
        ListView listView = view.findViewById(android.R.id.list);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.list_item_single_choice_custom,
                android.R.id.text1,  // <--- tells adapter where to set the text
                entries
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                RadioButton radio = view.findViewById(R.id.radio);
                radio.setChecked(position == selectedIndex);
                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(selectedIndex, true);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            selectedIndex = position;
            onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
            getDialog().dismiss();
        });

        listView.postDelayed(() -> {
            if (listView.canScrollVertically(1) || listView.canScrollVertically(-1)) {
                listView.smoothScrollBy(1, 0);

            }
        }, 0);

        listView.postDelayed(() -> {
            if (listView.canScrollVertically(1) || listView.canScrollVertically(-1)) {

                listView.smoothScrollBy(-1, 0);
            }
        }, 100);

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
        });

        return builder.create();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && selectedIndex >= 0) {
            ListPreference preference = (ListPreference) getPreference();
            String value = entryValues[selectedIndex].toString();
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }
    }
}