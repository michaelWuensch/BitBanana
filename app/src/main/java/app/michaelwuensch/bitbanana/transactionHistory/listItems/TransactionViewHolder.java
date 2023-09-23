package app.michaelwuensch.bitbanana.transactionHistory.listItems;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.util.Date;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.transactionHistory.TransactionSelectListener;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class TransactionViewHolder extends HistoryItemViewHolder {

    View mRootView;
    View mContentView;

    private TransactionSelectListener mTransactionSelectListener;
    private ImageView mIcon;
    private TextView mTimeOfDay;
    private TextView mPrimaryDescription;
    private TextView mSecondaryDescription;
    private AmountView mAmount;
    private AmountView mTransactionFee;

    TransactionViewHolder(@NonNull View itemView) {
        super(itemView);

        mIcon = itemView.findViewById(R.id.transactionTypeIcon);
        mTimeOfDay = itemView.findViewById(R.id.timeOfDay);
        mPrimaryDescription = itemView.findViewById(R.id.primaryTransactionDescription);
        mSecondaryDescription = itemView.findViewById(R.id.secondaryTransactionDescription);
        mAmount = itemView.findViewById(R.id.transactionAmount);
        mTransactionFee = itemView.findViewById(R.id.transactionFeeAmount);
        mRootView = itemView.findViewById(R.id.transactionRootView);
        mContentView = itemView.findViewById(R.id.transactionContent);
        mContext = itemView.getContext();
    }

    void setTimeOfDay(long creationDate) {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, mContext.getResources().getConfiguration().locale);
        String formattedTime = df.format(new Date(creationDate * 1000L));
        mTimeOfDay.setText(formattedTime);
    }

    void setIcon(@NonNull TransactionIcon transactionIcon) {
        switch (transactionIcon) {
            case LIGHTNING:
                mIcon.setImageResource(R.drawable.bolt_black_filled_24dp);
                mIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.banana_yellow)));
                break;
            case ONCHAIN:
                mIcon.setImageResource(R.drawable.ic_onchain_black_24dp);
                mIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.banana_yellow)));
                break;
            case INTERNAL:
                mIcon.setImageResource(R.drawable.ic_internal_black_24dp);
                mIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.gray)));
                break;
            case PENDING:
                mIcon.setImageResource(R.drawable.ic_clock_black_24dp);
                mIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.gray)));
                break;
            default:
                throw new IllegalStateException("Unknown transaction Icon");
        }

    }

    void setAmount(Long amount, boolean visible) {
        setAmount(amount, visible, false);
    }

    void setAmountPending(Long amount, boolean fixedValue, boolean visible) {
        if (fixedValue) {
            setAmount(amount, visible, true);
        } else {
            mAmount.setUndefinedValue();
        }
    }

    private void setAmount(Long amount, boolean visible, boolean pending) {
        mAmount.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (pending) {
            mAmount.setStyleBasedOnValue(false);
            mAmount.setTextColor(ContextCompat.getColor(mContext, R.color.gray));
        } else {
            mAmount.setStyleBasedOnValue(true);
        }
        mAmount.setAmountSat(amount);
    }

    void setFeeSat(long amount, boolean visible) {
        mTransactionFee.setVisibility(visible ? View.VISIBLE : View.GONE);

        String feeText = mContext.getResources().getString(R.string.fee) + ": ";
        mTransactionFee.setLabelText(feeText);
        mTransactionFee.setAmountSat(amount);
    }

    void setFeeMSat(long amount, boolean visible) {
        mTransactionFee.setVisibility(visible ? View.VISIBLE : View.GONE);

        String feeText = mContext.getResources().getString(R.string.fee) + ": ";
        mTransactionFee.setLabelText(feeText);
        mTransactionFee.setAmountMsat(amount);
    }

    void setPrimaryDescription(String description) {
        mPrimaryDescription.setText(description);
    }

    void setSecondaryDescription(String description, boolean visible) {
        mSecondaryDescription.setVisibility(visible ? View.VISIBLE : View.GONE);
        mSecondaryDescription.setText(description);
    }

    void setDisplayMode(boolean isOpaque) {
        mContentView.setAlpha(isOpaque ? 1f : 0.5f);
    }

    public void addOnTransactionSelectListener(TransactionSelectListener transactionSelectListener) {
        mTransactionSelectListener = transactionSelectListener;
    }

    void setOnRootViewClickListener(@NonNull TransactionItem item, int type) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mTransactionSelectListener != null) {
                    mTransactionSelectListener.onTransactionSelect(item.getTransactionByteString(), type);
                }
            }
        });
    }

    enum TransactionIcon {
        LIGHTNING,
        ONCHAIN,
        INTERNAL,
        PENDING
    }
}
