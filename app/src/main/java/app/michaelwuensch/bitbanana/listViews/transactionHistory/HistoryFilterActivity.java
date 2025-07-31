package app.michaelwuensch.bitbanana.listViews.transactionHistory;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Calendar;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;

public class HistoryFilterActivity extends BaseAppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = HistoryFilterActivity.class.getSimpleName();

    private SwitchCompat mExpiredSwitch;
    private SwitchCompat mUnpaidSwitch;
    private SwitchCompat mInternalSwitch;
    private SwitchCompat mOnChainSwitch;
    private SwitchCompat mUnconfirmedSwitch;
    private SwitchCompat mLightningSwitch;
    private SwitchCompat mReceivedSwitch;
    private SwitchCompat mSentSwitch;
    private EditText mMinInput;
    private TextView mMinUnit;
    private LinearLayout mMinUnitLayout;
    private EditText mMaxInput;
    private TextView mMaxUnit;
    private LinearLayout mMaxUnitLayout;
    private TextView mStartDateText;
    private BBButton mPickStartDateButton;
    private BBButton mDeleteStartDateButton;
    private TextView mEndDateText;
    private BBButton mPickEndDateButton;
    private BBButton mDeleteEndDateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history_filters);

        mExpiredSwitch = findViewById(R.id.switchExpired);
        mUnpaidSwitch = findViewById(R.id.switchUnpaid);
        mInternalSwitch = findViewById(R.id.switchInternal);
        mOnChainSwitch = findViewById(R.id.switchOnChain);
        mUnconfirmedSwitch = findViewById(R.id.switchUnconfirmed);
        mLightningSwitch = findViewById(R.id.switchLightning);
        mReceivedSwitch = findViewById(R.id.switchReceive);
        mSentSwitch = findViewById(R.id.switchSent);
        mMinInput = findViewById(R.id.minAmountEditText);
        mMinUnit = findViewById(R.id.minAmountUnit);
        mMinUnitLayout = findViewById(R.id.minAmountUnitLayout);
        mMaxInput = findViewById(R.id.maxAmountEditText);
        mMaxUnit = findViewById(R.id.maxAmountUnit);
        mMaxUnitLayout = findViewById(R.id.maxAmountUnitLayout);
        mStartDateText = findViewById(R.id.startDateText);
        mPickStartDateButton = findViewById(R.id.buttonPickStartDate);
        mDeleteStartDateButton = findViewById(R.id.buttonDeleteStartDate);
        mEndDateText = findViewById(R.id.endDateText);
        mPickEndDateButton = findViewById(R.id.buttonPickEndDate);
        mDeleteEndDateButton = findViewById(R.id.buttonDeleteEndDate);

        // Set the current values
        updateToPreferences();

        // Register shared prefs change listener
        PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

        // Setup on click listeners
        mExpiredSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.FILTER_HISTORY_EXPIRED_REQUESTS, isChecked).commit();
                updateResetButton();
            }
        });

        mUnpaidSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.FILTER_HISTORY_UNPAID_REQUESTS, isChecked).commit();
                mExpiredSwitch.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                updateResetButton();
            }
        });

        mInternalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.FILTER_HISTORY_INTERNAL_TRANSACTIONS, isChecked).commit();
                updateResetButton();
            }
        });

        mOnChainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.FILTER_HISTORY_ON_CHAIN_TRANSACTIONS, isChecked).commit();
                mUnconfirmedSwitch.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                updateResetButton();
            }
        });

        mUnconfirmedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.FILTER_HISTORY_ON_CHAIN_UNCONFIRMED, isChecked).commit();
                updateResetButton();
            }
        });

        mLightningSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.FILTER_HISTORY_LIGHTNING_PAYMENTS, isChecked).commit();
                mUnpaidSwitch.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                mExpiredSwitch.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                updateResetButton();
            }
        });

        mSentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.FILTER_HISTORY_SENT, isChecked).commit();
                updateResetButton();
            }
        });

        mReceivedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.FILTER_HISTORY_RECEIVED, isChecked).commit();
                updateResetButton();
            }
        });

        if (MonetaryUtil.getInstance().hasMoreThanOneCurrency()) {
            mMinUnitLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MonetaryUtil.getInstance().switchToNextCurrency();
                }
            });

            mMaxUnitLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MonetaryUtil.getInstance().switchToNextCurrency();
                }
            });
        }

        mPickStartDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(true);
            }
        });

        mPickEndDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(false);
            }
        });

        mStartDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(true, PrefsUtil.getPrefs().getLong(PrefsUtil.FILTER_HISTORY_DATE_START, 0));
            }
        });

        mEndDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(false, PrefsUtil.getPrefs().getLong(PrefsUtil.FILTER_HISTORY_DATE_END, 0));
            }
        });

        mDeleteStartDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStartDateText.setVisibility(View.GONE);
                mPickStartDateButton.setVisibility(View.VISIBLE);
                mDeleteStartDateButton.setVisibility(View.GONE);
                PrefsUtil.editPrefs().putLong(PrefsUtil.FILTER_HISTORY_DATE_START, 0).commit();
                updateResetButton();
            }
        });

        mDeleteEndDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEndDateText.setVisibility(View.GONE);
                mPickEndDateButton.setVisibility(View.VISIBLE);
                mDeleteEndDateButton.setVisibility(View.GONE);
                PrefsUtil.editPrefs().putLong(PrefsUtil.FILTER_HISTORY_DATE_END, 0).commit();
                updateResetButton();
            }
        });


        // Input validation for the min amount field.
        mMinInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                // validate input
                boolean amountValid = MonetaryUtil.getInstance().validateCurrentCurrencyInput(arg0.toString(), false);

                // remove the last inputted character if not valid
                if (!amountValid) {
                    removeOneDigit(mMinInput);
                } else {
                    PrefsUtil.editPrefs().putLong(PrefsUtil.FILTER_HISTORY_VALUE_MIN, MonetaryUtil.getInstance().convertCurrentCurrencyTextInputToMsat(arg0.toString())).commit();
                    updateResetButton();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {

            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before,
                                      int count) {
            }
        });

        // Input validation for the max amount field.
        mMaxInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                // validate input
                boolean amountValid = MonetaryUtil.getInstance().validateCurrentCurrencyInput(arg0.toString(), false);

                // remove the last inputted character if not valid
                if (!amountValid) {
                    removeOneDigit(mMaxInput);
                } else {
                    PrefsUtil.editPrefs().putLong(PrefsUtil.FILTER_HISTORY_VALUE_MAX, MonetaryUtil.getInstance().convertCurrentCurrencyTextInputToMsat(arg0.toString())).commit();
                    updateResetButton();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {

            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before,
                                      int count) {
            }
        });
    }

    private void removeOneDigit(EditText editText) {
        boolean selection = editText.getSelectionStart() != editText.getSelectionEnd();

        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);

        String before = editText.getText().toString().substring(0, start);
        String after = editText.getText().toString().substring(end);

        if (selection) {
            String outputText = before + after;
            editText.setText(outputText);
            editText.setSelection(start);
        } else {
            if (before.length() >= 1) {
                String newBefore = before.substring(0, before.length() - 1);
                String outputText = newBefore + after;
                editText.setText(outputText);
                editText.setSelection(start - 1);
            }
        }
    }

    private void updateToPreferences() {
        mExpiredSwitch.setChecked(PrefsUtil.getPrefs().getBoolean(PrefsUtil.FILTER_HISTORY_EXPIRED_REQUESTS, false));
        mUnpaidSwitch.setChecked(PrefsUtil.getPrefs().getBoolean(PrefsUtil.FILTER_HISTORY_UNPAID_REQUESTS, true));
        mInternalSwitch.setChecked(PrefsUtil.getPrefs().getBoolean(PrefsUtil.FILTER_HISTORY_INTERNAL_TRANSACTIONS, true));
        mOnChainSwitch.setChecked(PrefsUtil.getPrefs().getBoolean(PrefsUtil.FILTER_HISTORY_ON_CHAIN_TRANSACTIONS, true));
        mUnconfirmedSwitch.setChecked(PrefsUtil.getPrefs().getBoolean(PrefsUtil.FILTER_HISTORY_ON_CHAIN_UNCONFIRMED, true));
        mLightningSwitch.setChecked(PrefsUtil.getPrefs().getBoolean(PrefsUtil.FILTER_HISTORY_LIGHTNING_PAYMENTS, true));
        mSentSwitch.setChecked(PrefsUtil.getPrefs().getBoolean(PrefsUtil.FILTER_HISTORY_SENT, true));
        mReceivedSwitch.setChecked(PrefsUtil.getPrefs().getBoolean(PrefsUtil.FILTER_HISTORY_RECEIVED, true));

        mMinInput.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(PrefsUtil.getPrefs().getLong(PrefsUtil.FILTER_HISTORY_VALUE_MIN, 0), false));
        mMaxInput.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(PrefsUtil.getPrefs().getLong(PrefsUtil.FILTER_HISTORY_VALUE_MAX, 0), false));
        mMinUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
        mMaxUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());

        long startDate = PrefsUtil.getPrefs().getLong(PrefsUtil.FILTER_HISTORY_DATE_START, 0);
        if (startDate == 0) {
            mStartDateText.setVisibility(View.GONE);
            mPickStartDateButton.setVisibility(View.VISIBLE);
            mDeleteStartDateButton.setVisibility(View.GONE);
        } else {
            mStartDateText.setVisibility(View.VISIBLE);
            mPickStartDateButton.setVisibility(View.GONE);
            mDeleteStartDateButton.setVisibility(View.VISIBLE);
            setDateText(true, startDate);
        }
        long endDate = PrefsUtil.getPrefs().getLong(PrefsUtil.FILTER_HISTORY_DATE_END, 0);
        if (endDate == 0) {
            mEndDateText.setVisibility(View.GONE);
            mPickEndDateButton.setVisibility(View.VISIBLE);
            mDeleteEndDateButton.setVisibility(View.GONE);
        } else {
            mEndDateText.setVisibility(View.VISIBLE);
            mPickEndDateButton.setVisibility(View.GONE);
            mDeleteEndDateButton.setVisibility(View.VISIBLE);
            setDateText(false, endDate);
        }
    }

    private void showDatePickerDialog(boolean start) {
        showDatePickerDialog(start, 0);
    }

    private void showDatePickerDialog(boolean start, long UnixTimeStamp) {
        // Get today’s date as default
        final Calendar calendar = Calendar.getInstance();
        if (UnixTimeStamp > 0)
            calendar.setTimeInMillis(UnixTimeStamp * 1000);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                HistoryFilterActivity.this,
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    // Use Calendar to build the date
                    Calendar conversionCalendar = Calendar.getInstance();
                    conversionCalendar.set(Calendar.YEAR, selectedYear);
                    conversionCalendar.set(Calendar.MONTH, selectedMonth);
                    conversionCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    conversionCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    conversionCalendar.set(Calendar.MINUTE, 0);
                    conversionCalendar.set(Calendar.SECOND, 0);
                    conversionCalendar.set(Calendar.MILLISECOND, 0);

                    // Convert to Unix timestamp
                    long timestampMillis = conversionCalendar.getTimeInMillis(); // in milliseconds
                    long timestampSeconds = timestampMillis / 1000; // in seconds
                    onDatePicked(start, timestampSeconds);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void onDatePicked(boolean start, long UnixTimestamp) {
        if (start) {
            mStartDateText.setVisibility(View.VISIBLE);
            mPickStartDateButton.setVisibility(View.GONE);
            mDeleteStartDateButton.setVisibility(View.VISIBLE);
            PrefsUtil.editPrefs().putLong(PrefsUtil.FILTER_HISTORY_DATE_START, UnixTimestamp).commit();
            setDateText(true, UnixTimestamp);
        } else {
            mEndDateText.setVisibility(View.VISIBLE);
            mPickEndDateButton.setVisibility(View.GONE);
            mDeleteEndDateButton.setVisibility(View.VISIBLE);
            PrefsUtil.editPrefs().putLong(PrefsUtil.FILTER_HISTORY_DATE_END, UnixTimestamp + 86399).commit(); // We want to include that day therefore we add a day full of seconds to the timestamp.
            setDateText(false, UnixTimestamp);
        }
        updateResetButton();
    }

    private void setDateText(boolean start, long UnixTimestamp) {
        if (start) {
            mStartDateText.setText(TimeFormatUtil.formatDate(UnixTimestamp, HistoryFilterActivity.this));
        } else {
            mEndDateText.setText(TimeFormatUtil.formatDate(UnixTimestamp, HistoryFilterActivity.this));
        }
    }

    private void resetFilters() {
        PrefsUtil.editPrefs()
                .putBoolean(PrefsUtil.FILTER_HISTORY_EXPIRED_REQUESTS, false)
                .putBoolean(PrefsUtil.FILTER_HISTORY_UNPAID_REQUESTS, true)
                .putBoolean(PrefsUtil.FILTER_HISTORY_INTERNAL_TRANSACTIONS, true)
                .putBoolean(PrefsUtil.FILTER_HISTORY_ON_CHAIN_TRANSACTIONS, true)
                .putBoolean(PrefsUtil.FILTER_HISTORY_ON_CHAIN_UNCONFIRMED, true)
                .putBoolean(PrefsUtil.FILTER_HISTORY_LIGHTNING_PAYMENTS, true)
                .putBoolean(PrefsUtil.FILTER_HISTORY_SENT, true)
                .putBoolean(PrefsUtil.FILTER_HISTORY_RECEIVED, true)
                .putLong(PrefsUtil.FILTER_HISTORY_VALUE_MIN, 0)
                .putLong(PrefsUtil.FILTER_HISTORY_VALUE_MAX, 0)
                .putLong(PrefsUtil.FILTER_HISTORY_DATE_START, 0)
                .putLong(PrefsUtil.FILTER_HISTORY_DATE_END, 0)
                .commit();
        updateToPreferences();
        updateResetButton();
    }

    private boolean isDefaultFilterSetting() {
        return PrefsUtil.areDefaultHistoryFiltersActive();
    }

    private void updateResetButton() {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.delete_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteItem = menu.findItem(R.id.action_delete);

        if (isDefaultFilterSetting()) { // your custom condition
            deleteItem.setVisible(false);
        } else {
            deleteItem.setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_delete) {
            resetFilters();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key != null) {
            if (key.equals(PrefsUtil.CURRENT_CURRENCY_INDEX)) {
                mMinUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
                mMaxUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());

                mMinInput.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(PrefsUtil.getPrefs().getLong(PrefsUtil.FILTER_HISTORY_VALUE_MIN, 0), false));
                mMaxInput.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(PrefsUtil.getPrefs().getLong(PrefsUtil.FILTER_HISTORY_VALUE_MAX, 0), false));
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister shared prefs listener
        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }
}