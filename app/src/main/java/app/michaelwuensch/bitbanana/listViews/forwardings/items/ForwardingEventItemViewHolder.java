package app.michaelwuensch.bitbanana.listViews.forwardings.items;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.Date;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.listViews.forwardings.ForwardingEventSelectListener;
import app.michaelwuensch.bitbanana.models.Forward;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.WalletUtil;

public class ForwardingEventItemViewHolder extends ForwardingItemViewHolder {

    private static final String LOG_TAG = ForwardingEventItemViewHolder.class.getSimpleName();

    private final TextView mTimeOfDay;
    private final TextView mInChannel;
    private final TextView mOutChannel;
    private final AmountView mEarnedFee;
    private final AmountView mForwardingAmount;
    private final View mRootView;
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

        Forward forward = forwardingEventListItem.getForwardingEvent();

        // Set time of day
        setTimeOfDay(forwardingEventListItem.getTimestampMS());

        // Set in channel name
        String inChanPubKey = WalletUtil.getRemotePubKeyFromChannelId(forward.getChannelIdIn());
        String inChanName = "";
        if (inChanPubKey == null) {
            inChanName = mContext.getResources().getString(R.string.forwarding_closed_channel);
        } else {
            inChanName = AliasManager.getInstance().getAlias(inChanPubKey);
        }
        mInChannel.setText(inChanName);

        // Set out channel name
        String outChanPubKey = WalletUtil.getRemotePubKeyFromChannelId(forward.getChannelIdOut());
        String outChanName = "";
        if (outChanPubKey == null) {
            outChanName = mContext.getResources().getString(R.string.forwarding_closed_channel);
        } else {
            outChanName = AliasManager.getInstance().getAlias(outChanPubKey);
        }
        mOutChannel.setText(outChanName);


        // Set earned fee amount
        mEarnedFee.setStyleBasedOnValue(true);
        mEarnedFee.setAmountMsat(forward.getFee());

        // Set forwarded amount
        mForwardingAmount.setAmountMsat(forward.getAmountIn());

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
                    mForwardingEventSelectListener.onForwardingEventSelect(item.getForwardingEvent());
                }
            }
        });
    }
}
