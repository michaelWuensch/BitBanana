package app.michaelwuensch.bitbanana.listViews.channels.items;

import android.content.res.ColorStateList;
import android.view.View;

import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.R;

public class OpenChannelViewHolder extends ChannelViewHolder {

    public OpenChannelViewHolder(View v) {
        super(v);
    }

    void setState(boolean isActive) {
        if (isActive) {
            mStatus.setText(R.string.channel_state_open);
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.green)));
            mContentView.setAlpha(1f);
        } else {
            mStatus.setText(R.string.channel_state_offline);
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.gray)));
            mContentView.setAlpha(0.65f);
        }
    }

    public void bindOpenChannelItem(final OpenChannelItem openChannelItem) {
        // Set state
        setState(openChannelItem.getChannel().isActive());

        // Set balances
        long availableCapacity = openChannelItem.getChannel().getCapacity() - openChannelItem.getChannel().getCommitFee();
        setBalances(openChannelItem.getChannel().getLocalBalance(), openChannelItem.getChannel().getRemoteBalance(), availableCapacity);

        // Set name
        setName(openChannelItem.getChannel().getRemotePubKey());

        setOnRootViewClickListener(openChannelItem, ChannelListItem.TYPE_OPEN_CHANNEL);
    }
}
