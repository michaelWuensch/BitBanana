package app.michaelwuensch.bitbanana.channelManagement.listItems;

import com.github.lightningnetwork.lnd.lnrpc.ChannelCloseSummary;
import com.google.protobuf.ByteString;

public class ClosedChannelItem extends ChannelListItem {
    private ChannelCloseSummary mChannel;

    public ClosedChannelItem(ChannelCloseSummary channel) {
        mChannel = channel;
    }

    @Override
    public int getType() {
        return TYPE_CLOSED_CHANNEL;
    }

    @Override
    public ByteString getChannelByteString() {
        return mChannel.toByteString();
    }

    public ChannelCloseSummary getChannel() {
        return mChannel;
    }
}
