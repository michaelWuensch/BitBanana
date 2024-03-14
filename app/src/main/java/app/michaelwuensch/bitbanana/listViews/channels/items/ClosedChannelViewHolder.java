package app.michaelwuensch.bitbanana.listViews.channels.items;

import android.view.View;

public class ClosedChannelViewHolder extends ChannelViewHolder {

    public ClosedChannelViewHolder(View v) {
        super(v);
    }

    public void bindClosedChannelItem(final ClosedChannelItem closedChannelItem) {
        mStatusDot.setVisibility(View.GONE);
        mStatus.setVisibility(View.GONE);
        mContentView.setAlpha(0.65f);

        // Set balances
        setBalances(closedChannelItem.getChannel().getLocalBalance(), closedChannelItem.getChannel().getCapacity() - closedChannelItem.getChannel().getLocalBalance(), closedChannelItem.getChannel().getCapacity());

        // Set name
        setName(closedChannelItem.getChannel().getRemotePubKey());

        setOnRootViewClickListener(closedChannelItem, ChannelListItem.TYPE_CLOSED_CHANNEL);
    }
}
