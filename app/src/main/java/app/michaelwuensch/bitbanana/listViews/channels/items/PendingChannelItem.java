package app.michaelwuensch.bitbanana.listViews.channels.items;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.util.AliasManager;

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
