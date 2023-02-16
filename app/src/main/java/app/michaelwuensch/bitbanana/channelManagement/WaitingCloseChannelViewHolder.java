package app.michaelwuensch.bitbanana.channelManagement;

import android.view.View;

import app.michaelwuensch.bitbanana.R;

public class WaitingCloseChannelViewHolder extends PendingChannelViewHolder {

    public WaitingCloseChannelViewHolder(View v) {
        super(v);
    }

    @Override
    int getStatusColor() {
        return R.color.super_red;
    }

    @Override
    int getStatusText() {
        return R.string.channel_state_waiting_close;
    }

    void bindWaitingCloseChannelItem(WaitingCloseChannelItem pendingWaitingCloseChannelItem) {
        bindPendingChannelItem(pendingWaitingCloseChannelItem.getChannel().getChannel());

        setOnRootViewClickListener(pendingWaitingCloseChannelItem, ChannelListItem.TYPE_WAITING_CLOSE_CHANNEL);
    }
}
