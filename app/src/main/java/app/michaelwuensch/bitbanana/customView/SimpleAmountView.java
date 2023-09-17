package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;

public class SimpleAmountView extends FrameLayout implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView mTvAmount;
    private boolean mIsMsatAmount = false;
    private boolean mSwitchesValueOnClick = true;
    private long mValue = 0;


    public SimpleAmountView(Context context) {
        super(context);
        init(context, null);
    }

    public SimpleAmountView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SimpleAmountView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(getContext(), R.layout.view_simple_amount, this);

        mTvAmount = view.findViewById(R.id.amountTextView);

        if (!isInEditMode())
            PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

        // Apply attributes from XML
        if (attrs != null) {
            // Obtain the custom attribute value from the XML attributes
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SimpleAmountView);
            mIsMsatAmount = a.getBoolean(R.styleable.SimpleAmountView_isMsatAmount, false);
            mSwitchesValueOnClick = a.getBoolean(R.styleable.SimpleAmountView_switchesValueOnClick, true);
            int attrTextSize = a.getDimensionPixelSize(R.styleable.SimpleAmountView_textSize, 0);
            ColorStateList attrTextColor = a.getColorStateList(R.styleable.SimpleAmountView_textColor);

            if (attrTextColor != null)
                mTvAmount.setTextColor(attrTextColor);
            if (attrTextSize > 0)
                mTvAmount.setTextSize(TypedValue.COMPLEX_UNIT_PX, attrTextSize);

            // Don't forget to recycle the TypedArray
            a.recycle();
        }

        mTvAmount.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSwitchesValueOnClick)
                    MonetaryUtil.getInstance().switchCurrencies();
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            if (key.equals("firstCurrencyIsPrimary")) {
                setAmount(mValue);
            }
        }
    }

    public void setAmount(long value) {
        mValue = value;
        if (mIsMsatAmount)
            setAmountMsat(value);
        else
            setAmountSat(value);
    }

    private void setAmountSat(long value) {
        mTvAmount.setText(MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(value));
    }

    private void setAmountMsat(long value) {

    }
}
