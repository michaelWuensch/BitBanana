package app.michaelwuensch.bitbanana.channelManagement;

import android.view.View;

import app.michaelwuensch.bitbanana.R;

public class PendingForceClosingChannelViewHolder extends PendingChannelViewHolder {

    public PendingForceClosingChannelViewHolder(View v) {
        super(v);
    }

    @Override
    int getStatusColor() {
        return R.color.red;
    }

    @Override
    int getStatusText() {
        return R.string.channel_state_pending_force_closing;
    }

    void bindPendingForceClosingChannelItem(PendingForceClosingChannelItem pendingForceClosedChannelItem) {
        bindPendingChannelItem(pendingForceClosedChannelItem.getChannel().getChannel());

        setOnRootViewClickListener(pendingForceClosedChannelItem, ChannelListItem.TYPE_PENDING_FORCE_CLOSING_CHANNEL);
    }
}
