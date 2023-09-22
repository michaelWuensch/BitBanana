package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
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
    private boolean mIsMsatAmount = false;
    private boolean mSwitchesValueOnClick = true;
    private boolean mStyleBasedOnValue = false;
    private boolean mIsWithoutUnit = false;
    private boolean mIsUndefinedValue = false;
    private long mValue = 0;
    private Context mContext;


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

        if (!isInEditMode())
            PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

        // Apply attributes from XML
        if (attrs != null) {
            // Obtain the custom attribute value from the XML attributes
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AmountView);
            mSwitchesValueOnClick = a.getBoolean(R.styleable.AmountView_switchesValueOnClick, true);
            mStyleBasedOnValue = a.getBoolean(R.styleable.AmountView_styleBasedOnValue, false);
            mIsWithoutUnit = a.getBoolean(R.styleable.AmountView_isWithoutUnit, false);
            boolean attrShowLabel = a.getBoolean(R.styleable.AmountView_showLabel, false);
            int attrTextSize = a.getDimensionPixelSize(R.styleable.AmountView_textSize, 0);
            ColorStateList attrTextColor = a.getColorStateList(R.styleable.AmountView_textColor);

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
        if (mSwitchesValueOnClick) {
            mTvAmount.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    MonetaryUtil.getInstance().switchCurrencies();
                }
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            if (key.equals("firstCurrencyIsPrimary")) {
                if (mIsUndefinedValue)
                    return;
                if (mIsMsatAmount)
                    setAmountMsat(mValue);
                else
                    setAmountSat(mValue);
            }
        }
    }

    public void setAmountSat(long value) {
        mValue = value;
        mIsMsatAmount = false;
        mIsUndefinedValue = false;
        if (mIsWithoutUnit)
            mTvAmount.setText(MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromSats(value));
        else
            mTvAmount.setText(MonetaryUtil.getInstance().getPrimaryDisplayStringFromSats(value));
        styleBasedOnValue(value);
    }

    public void setAmountMsat(long value) {
        mValue = value;
        mIsMsatAmount = true;
        mIsUndefinedValue = false;
        if (mIsWithoutUnit)
            mTvAmount.setText(MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromMSats(value));
        else
            mTvAmount.setText(MonetaryUtil.getInstance().getPrimaryDisplayStringFromMSats(value));
        styleBasedOnValue(value);
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
                    mTvAmount.setText("+ " + mTvAmount.getText().toString());
                    setTextColor(ContextCompat.getColor(mContext, R.color.green));
                    break;
                case -1:
                    // amount < 0
                    mTvAmount.setText(mTvAmount.getText().toString().replace("-", "- "));
                    setTextColor(ContextCompat.getColor(mContext, R.color.red));
                    break;
            }
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
        mTvAmount.setText("+ ? " + MonetaryUtil.getInstance().getPrimaryDisplayUnit());
    }

    public void setIsMsatAmount(boolean isMsatAmount) {
        mIsMsatAmount = isMsatAmount;
    }

    public void overrideWithText(String text) {
        mTvAmount.setText(text);
        mIsUndefinedValue = true;
    }

    public void overrideWithText(int text) {
        mTvAmount.setText(getResources().getText(text));
        mIsUndefinedValue = true;
    }
}
