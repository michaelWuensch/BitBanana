package app.michaelwuensch.bitbanana.forwarding.listItems;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.Date;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.forwarding.ForwardingEventSelectListener;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.Wallet;

public class ForwardingEventItemViewHolder extends ForwardingItemViewHolder {

    private static final String LOG_TAG = ForwardingEventItemViewHolder.class.getSimpleName();

    private TextView mTimeOfDay;
    private TextView mInChannel;
    private TextView mOutChannel;
    private AmountView mEarnedFee;
    private AmountView mForwardingAmount;
    private View mRootView;
    private ForwardingEventSelectListener mForwardingEventSelectListener;


    public ForwardingEventItemViewHolder(View v) {
        super(v);

        mTimeOfDay = itemView.findViewById(R.id.timeOfDay);
        mInChannel = v.findViewById(R.id.inChannel);
        mOutChannel = v.findViewById(R.id.outChannel);
        mEarnedFee = v.findViewById(R.id.earnedFeeAmount);
        mForwardingAmount = v.findViewById(R.id.forwardingAmount);
        mRootView = v.findViewById(R.id.forwardingEventRootView);
    }

    public void bindForwardingEventListItem(ForwardingEventListItem forwardingEventListItem) {

        // Set time of day
        setTimeOfDay(forwardingEventListItem.getTimestampMS());

        // Set in channel name
        long inChanID = forwardingEventListItem.getForwardingEvent().getChanIdIn();
        String inChanPubKey = Wallet.getInstance().getRemotePubKeyFromChannelId(inChanID);
        String inChanName = "";
        if (inChanPubKey == null) {
            inChanName = mContext.getResources().getString(R.string.forwarding_closed_channel);
        } else {
            inChanName = AliasManager.getInstance().getAlias(inChanPubKey);
        }
        mInChannel.setText(inChanName);

        // Set out channel name
        long outChanID = forwardingEventListItem.getForwardingEvent().getChanIdOut();
        String outChanPubKey = Wallet.getInstance().getRemotePubKeyFromChannelId(outChanID);
        String outChanName = "";
        if (outChanPubKey == null) {
            outChanName = mContext.getResources().getString(R.string.forwarding_closed_channel);
        } else {
            outChanName = AliasManager.getInstance().getAlias(outChanPubKey);
        }
        mOutChannel.setText(outChanName);


        // Set earned fee amount
        mEarnedFee.setStyleBasedOnValue(true);
        mEarnedFee.setAmountMsat(forwardingEventListItem.getForwardingEvent().getFeeMsat());

        // Set forwarded amount
        mForwardingAmount.setAmountSat(forwardingEventListItem.getForwardingEvent().getAmtIn());

        // Set on click listener
        setOnRootViewClickListener(forwardingEventListItem);
    }

    public void addOnForwardingEventSelectListener(ForwardingEventSelectListener forwardingEventSelectListener) {
        mForwardingEventSelectListener = forwardingEventSelectListener;
    }

    void setTimeOfDay(long creationDate) {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, mContext.getResources().getConfiguration().locale);
        String formattedTime = df.format(new Date(creationDate));
        mTimeOfDay.setText(formattedTime);
    }

    void setOnRootViewClickListener(@NonNull ForwardingEventListItem item) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mForwardingEventSelectListener != null) {
                    mForwardingEventSelectListener.onForwardingEventSelect(item.getForwardingEvent().toByteString());
                }
            }
        });
    }
}
