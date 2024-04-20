package app.michaelwuensch.bitbanana.listViews.channels.items;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;

public class OpenChannelItem extends ChannelListItem {
    private OpenChannel mChannel;

    public OpenChannelItem(OpenChannel channel) {
        mChannel = channel;
    }

    @Override
    public int getType() {
        return TYPE_OPEN_CHANNEL;
    }

    @Override
    public Serializable getSerializedChannel() {
        return (Serializable) mChannel;
    }

    public OpenChannel getChannel() {
        return mChannel;
    }
}
