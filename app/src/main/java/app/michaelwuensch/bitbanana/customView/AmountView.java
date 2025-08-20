package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;

public class AmountView extends LinearLayout implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView mTvLabel;
    private TextView mTvAmount;
    private boolean mSwitchesValueOnClick = true;
    private boolean mStyleBasedOnValue = false;
    private boolean mIsWithoutUnit = false;
    private boolean mIsUndefinedValue = false;
    private long mValue = 0;
    private Context mContext;
    private boolean mIsTemporaryRevealed;
    private CountDownTimer mCountDownTimer;
    private boolean mCanBlur;
    private boolean mIsBlurred = false;
    private CharSequence mAmountText;
    private boolean mSubscribeToPrefChange;
    private boolean mIsOverriddenWithText = false;

    private boolean mMsatPrecision;


    public AmountView(Context context) {
        super(context);
        init(context, null);
    }

    public AmountView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AmountView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(getContext(), R.layout.view_amount, this);

        mContext = context;
        mTvLabel = view.findViewById(R.id.amountLabel);
        mTvAmount = view.findViewById(R.id.amountTextView);

        // Apply attributes from XML
        if (attrs != null) {
            // Obtain the custom attribute value from the XML attributes
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AmountView);
            if (!isInEditMode()) // For some unexplainable reason this is necessary so that the preview works
                mSwitchesValueOnClick = a.getBoolean(R.styleable.AmountView_switchesValueOnClick, true);
            mStyleBasedOnValue = a.getBoolean(R.styleable.AmountView_styleBasedOnValue, false);
            mIsWithoutUnit = a.getBoolean(R.styleable.AmountView_isWithoutUnit, false);
            boolean attrShowLabel = a.getBoolean(R.styleable.AmountView_showLabel, false);
            mCanBlur = a.getBoolean(R.styleable.AmountView_canBlur, true);
            mSubscribeToPrefChange = a.getBoolean(R.styleable.AmountView_subscribeToPrefChange, true);
            int attrTextSize = a.getDimensionPixelSize(R.styleable.AmountView_textSize, 0);
            ColorStateList attrTextColor = a.getColorStateList(R.styleable.AmountView_textColor);
            mMsatPrecision = a.getBoolean(R.styleable.AmountView_amountView_msatPrecision, true);

            if (a.hasValue(R.styleable.AmountView_setLabelText)) {
                mTvLabel.setText(a.getString(R.styleable.AmountView_setLabelText));
            }

            if (attrTextColor != null)
                setTextColor(attrTextColor);
            if (attrTextSize > 0) {
                mTvAmount.setTextSize(TypedValue.COMPLEX_UNIT_PX, attrTextSize);
                mTvLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, attrTextSize);
            }
            mTvLabel.setVisibility(attrShowLabel ? VISIBLE : GONE);
            // Don't forget to recycle the TypedArray
            a.recycle();
        }

        if (!isInEditMode() && mSubscribeToPrefChange)
            PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

        if (mSwitchesValueOnClick || isBlurActivated()) {
            mTvAmount.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCountDownTimer != null) {
                        mCountDownTimer.cancel();
                        mCountDownTimer.start();
                    }
                    if (!mIsTemporaryRevealed && isBlurActivated() && mCanBlur) {
                        removeBlur(true);
                        mCountDownTimer = new CountDownTimer(3000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                            }

                            @Override
                            public void onFinish() {
                                applyBlur();
                                mCountDownTimer = null;
                            }
                        }.start();
                    } else {
                        if (mSwitchesValueOnClick) {
                            MonetaryUtil.getInstance().switchToNextCurrency();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            if (key.equals(PrefsUtil.CURRENT_CURRENCY_INDEX)) {
                if (mIsUndefinedValue) {
                    setUndefinedValue();
                    return;
                }
                if (mIsOverriddenWithText)
                    return;
                setAmountMsat(mValue);
            }
            if (key.equals(PrefsUtil.BALANCE_HIDE_TYPE)) {
                if (isBlurActivated())
                    applyBlur();
                else
                    removeBlur(false);
            }
        }
    }

    public void setMsatPrecision(boolean mMsatPrecision) {
        this.mMsatPrecision = mMsatPrecision;
    }

    public void setAmountMsat(long value) {
        mValue = value;
        mIsUndefinedValue = false;
        mIsOverriddenWithText = false;
        if (mIsWithoutUnit)
            mAmountText = MonetaryUtil.getInstance().getCurrentCurrencyDisplayAmountStringFromMSats(value, mMsatPrecision);
        else
            mAmountText = MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(value, mMsatPrecision);
        updateAmountText();
        styleBasedOnValue(value);
        if (!mIsTemporaryRevealed)
            applyBlur();
    }

    public long getAmount() {
        return mValue;
    }

    private void styleBasedOnValue(long value) {
        if (mStyleBasedOnValue) {
            Long valueLong = value;
            int result = valueLong.compareTo(0L);
            switch (result) {
                case 0:
                    // amount = 0
                    setTextColor(ContextCompat.getColor(mContext, R.color.white));
                    break;
                case 1:
                    // amount > 0
                    mAmountText = TextUtils.concat("+ ", mAmountText);
                    setTextColor(ContextCompat.getColor(mContext, R.color.green));
                    break;
                case -1:
                    // amount < 0
                    mAmountText = TextUtils.replace(
                            mAmountText,
                            new String[]{"-"},
                            new CharSequence[]{"- "}
                    );
                    setTextColor(ContextCompat.getColor(mContext, R.color.red));
                    break;
            }
            updateAmountText();
        }
    }

    public void setTextColor(ColorStateList colorStateList) {
        mTvAmount.setTextColor(colorStateList);
    }

    public void setTextColor(int color) {
        mTvAmount.setTextColor(color);
    }

    public void setStyleBasedOnValue(boolean styleBasedOnValue) {
        mStyleBasedOnValue = styleBasedOnValue;
    }

    public void setLabelVisibility(boolean visible) {
        mTvLabel.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setLabelText(String text) {
        mTvLabel.setText(text);
    }

    public void setUndefinedValue() {
        mIsUndefinedValue = true;
        mTvAmount.setText("? " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
    }

    public void overrideWithText(String text) {
        mTvAmount.setText(text);
        mIsOverriddenWithText = true;
    }

    public void overrideWithText(int text) {
        mTvAmount.setText(getResources().getText(text));
        mIsUndefinedValue = true;
    }

    public void setCanBlur(boolean canBlur) {
        mCanBlur = canBlur;
        removeBlur(false);
    }

    public void setSwitchValueOnClick(boolean switchValueOnClick) {
        mSwitchesValueOnClick = switchValueOnClick;
    }

    private void applyBlur() {
        if (mCanBlur && isBlurActivated() && !mIsUndefinedValue) {
            if (!mIsBlurred) {
                float radius = mTvAmount.getTextSize() / 2;
                BlurMaskFilter filter = new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL);
                mTvAmount.getPaint().setMaskFilter(filter);
                mIsTemporaryRevealed = false;
                mIsBlurred = true;
                updateAmountText();
            }
        }
    }

    private void removeBlur(boolean temporary) {
        if (mIsBlurred) {
            mTvAmount.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            mTvAmount.getPaint().setMaskFilter(null);
            mIsTemporaryRevealed = temporary;
            mIsBlurred = false;
            updateAmountText();
        }
    }

    private void updateAmountText() {
        if (mIsBlurred)
            mTvAmount.setText(" " + mAmountText + " "); //hacky fix to prevent ugly clipping
        else
            mTvAmount.setText(mAmountText);
    }

    private boolean isBlurActivated() {
        return PrefsUtil.getPrefs().getString(PrefsUtil.BALANCE_HIDE_TYPE, "off").equals("all");
    }
}
