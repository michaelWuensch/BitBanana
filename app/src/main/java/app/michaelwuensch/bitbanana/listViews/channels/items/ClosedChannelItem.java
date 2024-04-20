package app.michaelwuensch.bitbanana.listViews.channels.items;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;

public class ClosedChannelItem extends ChannelListItem {
    private ClosedChannel mChannel;

    public ClosedChannelItem(ClosedChannel channel) {
        mChannel = channel;
    }

    @Override
    public int getType() {
        return TYPE_CLOSED_CHANNEL;
    }

    @Override
    public Serializable getSerializedChannel() {
        return (Serializable) mChannel;
    }

    public ClosedChannel getChannel() {
        return mChannel;
    }
}
