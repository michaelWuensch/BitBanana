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
        long availableCapacity = closedChannelItem.getChannel().getCapacity();
        setBalances(closedChannelItem.getChannel().getSettledBalance(), availableCapacity - closedChannelItem.getChannel().getSettledBalance(), availableCapacity);

        // Set name
        setName(closedChannelItem.getChannel().getRemotePubkey());

        setOnRootViewClickListener(closedChannelItem, ChannelListItem.TYPE_CLOSED_CHANNEL);
    }
}
