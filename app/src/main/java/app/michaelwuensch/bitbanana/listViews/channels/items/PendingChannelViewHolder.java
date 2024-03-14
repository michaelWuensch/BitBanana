package app.michaelwuensch.bitbanana.listViews.channels.items;

import android.content.res.ColorStateList;
import android.view.View;

import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.R;

public class PendingChannelViewHolder extends ChannelViewHolder {

    public PendingChannelViewHolder(View v) {
        super(v);
    }

    public void bindPendingChannelItem(PendingChannelItem pendingChannelItem) {
        // Set state
        setState(pendingChannelItem);

        // Set balances
        setBalances(pendingChannelItem.getChannel().getLocalBalance(), pendingChannelItem.getChannel().getRemoteBalance(), pendingChannelItem.getChannel().getCapacity());

        // Set name
        setName(pendingChannelItem.getChannel().getRemotePubKey());

        setOnRootViewClickListener(pendingChannelItem, ChannelListItem.TYPE_PENDING_CHANNEL);
    }

    private void setState(PendingChannelItem pendingChannelItem) {
        mStatusDot.setVisibility(View.VISIBLE);
        mContentView.setAlpha(0.65f);
        switch (pendingChannelItem.getChannel().getPendingType()) {
            case PENDING_OPEN:
                mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.banana_yellow)));
                mStatus.setText(R.string.channel_state_pending_open);
                break;
            case PENDING_CLOSE:
                mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.red)));
                mStatus.setText(R.string.channel_state_pending_closing);
                break;
            case PENDING_FORCE_CLOSE:
                mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.red)));
                mStatus.setText(R.string.channel_state_pending_force_closing);
                break;
            default:
                mStatusDot.setVisibility(View.GONE);
        }
    }
}
