package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;

public class BBAmountInput extends ConstraintLayout implements SharedPreferences.OnSharedPreferenceChangeListener {

    private CheckBox mAllFundsCheckBox;
    private View mAllFundsLayout;
    private ImageButton mAllFundsHelpButton;
    private EditText mEtAmount;
    private TextView mTvUnit;
    private LinearLayout mLlUnit;
    private ImageView mIvSwitchUnit;
    private boolean mAmountValid;
    private boolean mIsFixedAmount;
    private boolean mIsOnChain;
    private long mAmount;
    private OnAmountInputActionListener mOnAmountInputActionListener;
    private boolean mBlockOnTextChangedValidation;
    private long mUtxoSelectionAmount;
    private boolean mAllowMsats = true;

    public BBAmountInput(Context context) {
        super(context);
        init(context, null);
    }

    public BBAmountInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBAmountInput(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(getContext(), R.layout.view_amount_input, this);

        // Get references to the child views.
        mEtAmount = view.findViewById(R.id.amountEditText);
        mAllFundsCheckBox = view.findViewById(R.id.allFundsCheckBox);
        mAllFundsHelpButton = view.findViewById(R.id.allFundsHelpButton);
        mAllFundsLayout = view.findViewById(R.id.allFundsLayout);
        mTvUnit = view.findViewById(R.id.amountUnit);
        mLlUnit = view.findViewById(R.id.unitLayout);
        mIvSwitchUnit = view.findViewById(R.id.switchUnitImage);
    }

    public void setupView() {
        PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

        // set unit to current primary unit
        mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());

        // Action when clicked on unit layout
        if (MonetaryUtil.getInstance().hasMoreThanOneCurrency()) {

            mLlUnit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MonetaryUtil.getInstance().switchToNextCurrency();
                }
            });
        } else {
            mIvSwitchUnit.setVisibility(View.GONE);
        }

        // Input validation for the amount field.
        mEtAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                if (mAllFundsCheckBox.isChecked())
                    return;

                // remove the last inputted character if not valid
                if (!mAmountValid) {
                    mBlockOnTextChangedValidation = true;
                    removeOneDigit();
                    mBlockOnTextChangedValidation = false;
                    return;
                }

                boolean valid = mOnAmountInputActionListener.onAfterTextChanged(mEtAmount.getText().toString(), mAmount, mIsFixedAmount, mIsOnChain);
                mOnAmountInputActionListener.onInputChanged(valid);
                mOnAmountInputActionListener.onInputValidityChanged(valid);
                if (valid)
                    mEtAmount.setTextColor(getResources().getColor(R.color.white));
                else
                    mEtAmount.setTextColor(getResources().getColor(R.color.red));
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {

            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before,
                                      int count) {

                // We only want to validate the input if it was done through the keyboard, not when it was set by code
                if (mBlockOnTextChangedValidation || mAllFundsCheckBox.isChecked())
                    return;

                // validate input
                mAmountValid = MonetaryUtil.getInstance().validateCurrentCurrencyInput(arg0.toString(), !mIsOnChain && mAllowMsats);

                if (mAmountValid && !mIsFixedAmount)
                    mAmount = MonetaryUtil.getInstance().convertCurrentCurrencyTextInputToMsat(arg0.toString());
            }
        });

        mAllFundsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOnAmountInputActionListener != null) {
                if (isChecked) {
                    if (mUtxoSelectionAmount > 0)
                        mAmount = mUtxoSelectionAmount;
                    else
                        mAmount = Wallet_Balance.getInstance().getBalances().onChainConfirmed();
                    mAmountValid = true;
                    mEtAmount.clearFocus();
                    mEtAmount.setFocusable(false);
                    mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mAmount, false));
                    mEtAmount.setTextColor(getResources().getColor(R.color.white));
                    hideKeyboard();
                    mOnAmountInputActionListener.onInputValidityChanged(true);
                } else {
                    mEtAmount.setFocusableInTouchMode(true);
                    mEtAmount.setFocusable(true);
                    mEtAmount.setText("");
                    mEtAmount.requestFocus();
                    showKeyboard();
                }
                mOnAmountInputActionListener.onSendAllCheckboxChanged(isChecked);
            }
        });

        // All funds help button
        if (FeatureManager.isHelpButtonsEnabled()) {
            mAllFundsHelpButton.setVisibility(View.VISIBLE);
            mAllFundsHelpButton.setOnClickListener(view1 -> {
                String helpString = "";
                if (mUtxoSelectionAmount > 0)
                    helpString = getContext().getString(R.string.help_dialog_allFunds_1_utxo_selection);
                else
                    helpString = getContext().getString(R.string.help_dialog_allFunds_1);
                helpString = helpString + " " + getContext().getString(R.string.help_dialog_allFunds_2);
                HelpDialogUtil.showDialog(getContext(), helpString);
            });
        } else {
            mAllFundsHelpButton.setVisibility(View.GONE);
        }
    }

    public void setAllowMsats(boolean allowMsats) {
        mAllowMsats = allowMsats;
    }

    public void setOnChain(boolean onChain) {
        mIsOnChain = onChain;
    }

    public void setSendAllEnabled(boolean enabled) {
        if (enabled && BackendManager.getCurrentBackend().supportsSendAllOnChain())
            mAllFundsLayout.setVisibility(VISIBLE);
        else
            mAllFundsLayout.setVisibility(GONE);
    }

    public void setFixedAmount(long msats) {
        mIsFixedAmount = true;
        mAmount = msats;
        mAmountValid = true;
        mBlockOnTextChangedValidation = true;
        mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(msats, !mIsOnChain && mAllowMsats));
        mBlockOnTextChangedValidation = false;
        mAllFundsLayout.setVisibility(GONE);
        mEtAmount.clearFocus();
        mEtAmount.setFocusable(false);
    }

    private void removeOneDigit() {
        boolean selection = mEtAmount.getSelectionStart() != mEtAmount.getSelectionEnd();

        int start = Math.max(mEtAmount.getSelectionStart(), 0);
        int end = Math.max(mEtAmount.getSelectionEnd(), 0);

        String before = mEtAmount.getText().toString().substring(0, start);
        String after = mEtAmount.getText().toString().substring(end);

        if (selection) {
            String outputText = before + after;
            mEtAmount.setText(outputText);
            mEtAmount.setSelection(start);
        } else {
            if (before.length() >= 1) {
                String newBefore = before.substring(0, before.length() - 1);
                String outputText = newBefore + after;
                mEtAmount.setText(outputText);
                mEtAmount.setSelection(start - 1);
            }
        }
    }

    /**
     * Get the amount in msats
     */
    public long getAmount() {
        return mAmount;
    }

    public boolean getSendAllChecked() {
        return mAllFundsCheckBox.isChecked();
    }

    public void selectText() {
        mEtAmount.requestFocus();
        mEtAmount.selectAll();
    }

    public void setAmount(long amount) {
        mAmount = amount;
        mBlockOnTextChangedValidation = true;
        mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(amount, !mIsOnChain && mAllowMsats));
        mBlockOnTextChangedValidation = false;
    }

    public void requestFocusDelayed() {
        new Handler().postDelayed(() -> {
            // We have to call this delayed, as otherwise it will still bring up the softKeyboard
            mEtAmount.requestFocus();
        }, 1);
    }

    public void setOnAmountInputActionListener(OnAmountInputActionListener listener) {
        this.mOnAmountInputActionListener = listener;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key != null) {
            if (key.equals(PrefsUtil.CURRENT_CURRENCY_INDEX)) {
                mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
                mBlockOnTextChangedValidation = true;
                mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mAmount, !mIsOnChain && mAllowMsats));
                mBlockOnTextChangedValidation = false;
            }
        }
    }

    public interface OnAmountInputActionListener {
        boolean onAfterTextChanged(String newText, long amount, boolean isFixedAmount, boolean isOnChain);

        void onInputValidityChanged(boolean valid);

        void onInputChanged(boolean valid);

        void onSendAllCheckboxChanged(boolean checked);

        void onError(String message, int duration);
    }

    public void updateUtxoSelectionAmount(long selectionAmount) {
        mUtxoSelectionAmount = selectionAmount;
        mAllFundsCheckBox.setText(selectionAmount > 0 ? R.string.use_all_selected_funds : R.string.use_all_funds);
        if (mAllFundsCheckBox.isChecked()) {
            if (selectionAmount == 0)
                mAmount = Wallet_Balance.getInstance().getBalances().onChainConfirmed();
            else
                mAmount = selectionAmount;
            mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mAmount, false));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
        super.onDetachedFromWindow();
    }

    public void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getRootView().getWindowToken(), 0);
    }
}