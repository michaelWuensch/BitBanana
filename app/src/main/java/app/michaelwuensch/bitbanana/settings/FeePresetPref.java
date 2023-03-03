package app.michaelwuensch.bitbanana.settings;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;

public class FeePresetPref extends EditTextPreference {
    public FeePresetPref(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public FeePresetPref(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FeePresetPref(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FeePresetPref(@NonNull Context context) {
        super(context);
        init();
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        super.onSetInitialValue(defaultValue);
    }

    public void init() {

        if (getKey().equals(PrefsUtil.FEE_PRESET_FAST))
            setSummary(createFeeDescription(Integer.parseInt(PrefsUtil.getPrefs().getString(getKey(), PrefsUtil.DEFAULT_FEE_PRESET_VALUE_FAST))));
        if (getKey().equals(PrefsUtil.FEE_PRESET_MEDIUM))
            setSummary(createFeeDescription(Integer.parseInt(PrefsUtil.getPrefs().getString(getKey(), PrefsUtil.DEFAULT_FEE_PRESET_VALUE_MEDIUM))));
        if (getKey().equals(PrefsUtil.FEE_PRESET_SLOW))
            setSummary(createFeeDescription(Integer.parseInt(PrefsUtil.getPrefs().getString(getKey(), PrefsUtil.DEFAULT_FEE_PRESET_VALUE_SLOW))));


        setOnBindEditTextListener(new OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                // Only allow numbers as input
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                editText.selectAll(); // select all text
                int maxLength = 4;
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)}); // set maxLength to 2
            }
        });

        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                int newIntValue = Integer.parseInt(newValue.toString());
                if (newIntValue > 0) {
                    preference.setSummary(createFeeDescription(newIntValue));
                    return true;
                } else {
                    Toast.makeText(getContext(), R.string.error_invalid_fee_preset_value, Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
    }

    private String createFeeDescription(int numberOfBlocks) {
        String blocks = getContext().getResources().getQuantityString(R.plurals.blocks, numberOfBlocks, numberOfBlocks);
        return "" + blocks + " (" + TimeFormatUtil.formattedBlockDuration(numberOfBlocks, getContext()) + ")";
    }
}
