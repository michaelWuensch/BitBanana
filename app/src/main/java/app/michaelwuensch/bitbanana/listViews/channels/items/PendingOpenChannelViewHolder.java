package app.michaelwuensch.bitbanana.listViews.channels.items;

import android.view.View;

import app.michaelwuensch.bitbanana.R;

public class PendingOpenChannelViewHolder extends PendingChannelViewHolder {

    public PendingOpenChannelViewHolder(View v) {
        super(v);
    }

    @Override
    int getStatusColor() {
        return R.color.banana_yellow;
    }

    @Override
    int getStatusText() {
        return R.string.channel_state_pending_open;
    }

    public void bindPendingOpenChannelItem(PendingOpenChannelItem pendingOpenChannelItem) {
        bindPendingChannelItem(pendingOpenChannelItem.getChannel().getChannel());

        setOnRootViewClickListener(pendingOpenChannelItem, ChannelListItem.TYPE_PENDING_OPEN_CHANNEL);
    }
}
