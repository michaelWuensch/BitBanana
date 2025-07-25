package app.michaelwuensch.bitbanana.settings;

import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;


public class SettingsCurrenciesFragment extends BBPreferenceFragmentCompat {

    private static final String LOG_TAG = SettingsCurrenciesFragment.class.getSimpleName();

    private ListPreference mSecondCurrency;
    private ListPreference mThirdCurrency;
    private ListPreference mForthCurrency;
    private ListPreference mFifthCurrency;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the settings from an XML resource
        setPreferencesFromResource(R.xml.settings_currencies, rootKey);

        mSecondCurrency = findPreference("secondCurrency");
        mThirdCurrency = findPreference("thirdCurrency");
        mForthCurrency = findPreference("forthCurrency");
        mFifthCurrency = findPreference("fifthCurrency");

        final ListPreference listBtcUnit = findPreference("firstCurrency");
        listBtcUnit.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onCurrencySelectionChanged();
                return true;
            }
        });

        mSecondCurrency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onCurrencySelectionChanged();
                return true;
            }
        });

        mThirdCurrency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onCurrencySelectionChanged();
                return true;
            }
        });

        mForthCurrency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onCurrencySelectionChanged();
                return true;
            }
        });

        mFifthCurrency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onCurrencySelectionChanged();
                return true;
            }
        });
    }

    private void onCurrencySelectionChanged() {
        // We have to delay this, the new value is not set in Preferences yet, when this function is called.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MonetaryUtil.getInstance().reloadAllCurrencies();
                MonetaryUtil.getInstance().updateCurrencyUIs();
            }
        }, 200);
    }

    private CharSequence[] joinCharSequenceArrays(CharSequence[] first, CharSequence[] second) {
        if (first == null && second == null) {
            return null;
        } else if (first == null) {
            return second;
        } else if (second == null) {
            // No exchange rate has been fetched so far. This could happen if the app was started for the first time
            // without internet. Or if the user blocks connection to the exchange rate provider for example.
            return first;
        } else {
            List<CharSequence> both = new ArrayList<CharSequence>(first.length + second.length);
            Collections.addAll(both, first);
            Collections.addAll(both, second);
            return both.toArray(new CharSequence[both.size()]);
        }
    }

    private void updateAvailableCurrencyLists() {
        CharSequence[] manualEntryValues = {"none"};
        CharSequence[] manualEntryDisplayValues = {getActivity().getResources().getString(R.string.settings_currency_none)};
        CharSequence[] btcEntryValues = getActivity().getResources().getStringArray(R.array.btcCurrencyCodes);
        CharSequence[] btcEntryDisplayValue = getActivity().getResources().getStringArray(R.array.btcUnitDisplayValues);
        CharSequence[] fiatEntryValues = null;
        CharSequence[] fiatEntryDisplayValue = null;

        try {
            JSONObject jsonAvailableCurrencies = new JSONObject(PrefsUtil.getPrefs().getString(PrefsUtil.AVAILABLE_FIAT_CURRENCIES, PrefsUtil.DEFAULT_FIAT_CURRENCIES));

            JSONArray currencies = jsonAvailableCurrencies.getJSONArray("currencies");
            fiatEntryValues = new CharSequence[currencies.length()];
            fiatEntryDisplayValue = new CharSequence[currencies.length()];

            List<Pair<String, String>> fiatCurrencyList = new ArrayList<>();

            for (int i = 0, count = currencies.length(); i < count; i++) {
                String code = currencies.getString(i);
                String name = MonetaryUtil.getInstance().getCurrencyNameFromCurrencyCode(code);
                String narrowSymbol = MonetaryUtil.getInstance().getCurrencyNarrowSymbolFromCurrencyCode(code);
                if (name == null) {
                    name = code;
                } else {
                    name = name + " (" + narrowSymbol + ")";
                }
                fiatCurrencyList.add(new Pair<>(code, name));
            }

            // Sort alphabetically by display name (second in the Pair)
            Collections.sort(fiatCurrencyList, new Comparator<Pair<String, String>>() {
                @Override
                public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                    return o1.second.compareToIgnoreCase(o2.second);
                }
            });

            // Convert back to arrays
            fiatEntryValues = new CharSequence[fiatCurrencyList.size()];
            fiatEntryDisplayValue = new CharSequence[fiatCurrencyList.size()];

            for (int i = 0; i < fiatCurrencyList.size(); i++) {
                fiatEntryValues[i] = fiatCurrencyList.get(i).first;
                fiatEntryDisplayValue[i] = fiatCurrencyList.get(i).second;
            }

        } catch (JSONException e) {
            BBLog.d(LOG_TAG, "Error reading JSON from Preferences: " + e.getMessage());
        }

        // Combine the lists
        CharSequence[] entryValues = joinCharSequenceArrays(manualEntryValues, btcEntryValues);
        entryValues = joinCharSequenceArrays(entryValues, fiatEntryValues);
        CharSequence[] entryDisplayValues = joinCharSequenceArrays(manualEntryDisplayValues, btcEntryDisplayValue);
        entryDisplayValues = joinCharSequenceArrays(entryDisplayValues, fiatEntryDisplayValue);

        // Use the arrays for the list preference
        mSecondCurrency.setEntryValues(entryValues);
        mSecondCurrency.setEntries(entryDisplayValues);
        mThirdCurrency.setEntryValues(entryValues);
        mThirdCurrency.setEntries(entryDisplayValues);
        mForthCurrency.setEntryValues(entryValues);
        mForthCurrency.setEntries(entryDisplayValues);
        mFifthCurrency.setEntryValues(entryValues);
        mFifthCurrency.setEntries(entryDisplayValues);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSecondCurrency.setTitle("2. " + getString(R.string.currency));
        mSecondCurrency.setValue(PrefsUtil.getSecondCurrencyCode());
        mSecondCurrency.setSummary("%s");
        mThirdCurrency.setTitle("3. " + getString(R.string.currency));
        mThirdCurrency.setValue(PrefsUtil.getThirdCurrencyCode());
        mThirdCurrency.setSummary("%s");
        mForthCurrency.setTitle("4. " + getString(R.string.currency));
        mForthCurrency.setValue(PrefsUtil.getForthCurrencyCode());
        mForthCurrency.setSummary("%s");
        mFifthCurrency.setTitle("5. " + getString(R.string.currency));
        mFifthCurrency.setValue(PrefsUtil.getFifthCurrencyCode());
        mFifthCurrency.setSummary("%s");

        updateAvailableCurrencyLists();
    }
}
