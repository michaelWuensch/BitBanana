package app.michaelwuensch.bitbanana.listViews.channels.items;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;

public class PendingChannelItem extends ChannelListItem {
    private PendingChannel mChannel;

    public PendingChannelItem(PendingChannel channel) {
        mChannel = channel;
    }

    @Override
    public int getType() {
        return TYPE_PENDING_CHANNEL;
    }

    @Override
    public Serializable getSerializedChannel() {
        return mChannel;
    }

    public PendingChannel getChannel() {
        return mChannel;
    }
}
