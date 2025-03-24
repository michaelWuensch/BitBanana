package app.michaelwuensch.bitbanana.listViews.channels.items;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.util.AliasManager;

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

    @Override
    public String getAlias() {
        return AliasManager.getInstance().getAlias(mChannel.getRemotePubKey());
    }

    @Override
    public long getCapacity() {
        return mChannel.getCapacity();
    }

    @Override
    public long getLocalBalance() {
        return mChannel.getLocalBalance();
    }

    @Override
    public long getRemoteBalance() {
        return mChannel.getRemoteBalance();
    }

    @Override
    public double getBalanceRatio() {
        if (mChannel.getLocalBalance() > mChannel.getRemoteBalance())
            return (double) mChannel.getRemoteBalance() / (double) mChannel.getLocalBalance();
        else
            return (double) mChannel.getLocalBalance() / (double) mChannel.getRemoteBalance();
    }
}
