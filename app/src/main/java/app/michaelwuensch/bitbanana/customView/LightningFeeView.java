package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import app.michaelwuensch.bitbanana.R;

public class LightningFeeView extends ConstraintLayout {

    private AmountView mTvSendFeeAmount;
    private TextView mTvSendFeeAmountPercent;
    private View mVFeeAmountLayout;
    private ProgressBar mPbCalculateFee;

    public LightningFeeView(Context context) {
        super(context);
        init();
    }

    public LightningFeeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LightningFeeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_lightning_fee, this);
        mTvSendFeeAmount = view.findViewById(R.id.sendFeeLightningAmount);
        mTvSendFeeAmountPercent = view.findViewById(R.id.sendFeeLightningAmountPercent);
        mPbCalculateFee = view.findViewById(R.id.sendFeeLightningProgressBar);
        mVFeeAmountLayout = view.findViewById(R.id.sendFeeLightningAmountLayout);
    }

    /**
     * Show progress bar while calculating fee
     */
    public void onCalculating() {
        mTvSendFeeAmount.overrideWithText(null);
        mTvSendFeeAmountPercent.setText(null);
        mPbCalculateFee.setVisibility(View.VISIBLE);
        mVFeeAmountLayout.setVisibility(View.GONE);
    }

    public void setAmountMsat(long msats, String percentString, boolean showMax) {
        mTvSendFeeAmount.setAmountMsat(msats);
        mTvSendFeeAmountPercent.setText(percentString);
        mVFeeAmountLayout.setVisibility(View.VISIBLE);
        mPbCalculateFee.setVisibility(View.GONE);
        if (showMax) {
            mTvSendFeeAmount.setLabelVisibility(true);
            mTvSendFeeAmount.setLabelText(getResources().getString(R.string.maximum_abbreviation) + " ");
        } else {
            mTvSendFeeAmount.setLabelVisibility(false);
        }
    }

    public void onFeeFailure() {
        mTvSendFeeAmount.overrideWithText(R.string.fee_not_available);
        mTvSendFeeAmountPercent.setText(null);
        mTvSendFeeAmount.setVisibility(View.VISIBLE);
        mPbCalculateFee.setVisibility(View.GONE);
    }
}
