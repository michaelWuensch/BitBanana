package app.michaelwuensch.bitbanana.listViews.channels.items;

import androidx.annotation.Nullable;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.util.AliasManager;

public abstract class ChannelListItem implements Comparable<ChannelListItem> {

    public static final int TYPE_OPEN_CHANNEL = 0;
    public static final int TYPE_PENDING_CHANNEL = 1;
    public static final int TYPE_CLOSED_CHANNEL = 2;

    abstract public int getType();

    abstract public Serializable getSerializedChannel();

    @Override
    public int compareTo(ChannelListItem channelListItem) {
        ChannelListItem other = channelListItem;

        String ownPubkey = "";
        switch (this.getType()) {
            case TYPE_OPEN_CHANNEL:
                ownPubkey = ((OpenChannelItem) this).getChannel().getRemotePubKey();
                break;
            case TYPE_PENDING_CHANNEL:
                ownPubkey = ((PendingChannelItem) this).getChannel().getRemotePubKey();
                break;
            case TYPE_CLOSED_CHANNEL:
                ownPubkey = ((ClosedChannelItem) this).getChannel().getRemotePubKey();
        }

        String otherPubkey = "";
        switch (other.getType()) {
            case TYPE_OPEN_CHANNEL:
                otherPubkey = ((OpenChannelItem) other).getChannel().getRemotePubKey();
                break;
            case TYPE_PENDING_CHANNEL:
                otherPubkey = ((PendingChannelItem) other).getChannel().getRemotePubKey();
                break;
            case TYPE_CLOSED_CHANNEL:
                otherPubkey = ((ClosedChannelItem) other).getChannel().getRemotePubKey();
        }

        String ownAlias = AliasManager.getInstance().getAlias(ownPubkey).toLowerCase();
        String otherAlias = AliasManager.getInstance().getAlias(otherPubkey).toLowerCase();

        return ownAlias.compareTo(otherAlias);
    }

    public boolean equalsWithSameContent(@Nullable Object obj) {
        if (!equals(obj)) {
            return false;
        }

        ChannelListItem that = (ChannelListItem) obj;

        switch (this.getType()) {
            case TYPE_OPEN_CHANNEL:
                return (((OpenChannelItem) this).getChannel().getTotalSent() == ((OpenChannelItem) that).getChannel().getTotalSent()
                        && ((OpenChannelItem) this).getChannel().getTotalReceived() == ((OpenChannelItem) that).getChannel().getTotalReceived())
                        && ((OpenChannelItem) this).getChannel().isActive() == ((OpenChannelItem) that).getChannel().isActive();
            case TYPE_PENDING_CHANNEL:
                return ((PendingChannelItem) this).getChannel().getLocalBalance() == ((PendingChannelItem) that).getChannel().getLocalBalance();
            case TYPE_CLOSED_CHANNEL:
                return ((ClosedChannelItem) this).getChannel().getLocalBalance() == ((ClosedChannelItem) that).getChannel().getLocalBalance();
        }
        return false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ChannelListItem that = (ChannelListItem) obj;
        if (that.getType() != this.getType()) {
            return false;
        }

        switch (this.getType()) {
            case TYPE_OPEN_CHANNEL:
                return ((OpenChannelItem) this).getChannel().getFundingOutpoint().toString().equals(((OpenChannelItem) that).getChannel().getFundingOutpoint().toString());
            case TYPE_PENDING_CHANNEL:
                return ((PendingChannelItem) this).getChannel().getFundingOutpoint().toString().equals(((PendingChannelItem) that).getChannel().getFundingOutpoint().toString());
            case TYPE_CLOSED_CHANNEL:
                return ((ClosedChannelItem) this).getChannel().getFundingOutpoint().toString().equals(((ClosedChannelItem) that).getChannel().getFundingOutpoint().toString());
            default:
                return false;
        }
    }

    @Override
    public int hashCode() {
        switch (this.getType()) {
            case TYPE_OPEN_CHANNEL:
                return ((OpenChannelItem) this).getChannel().getFundingOutpoint().toString().hashCode();
            case TYPE_PENDING_CHANNEL:
                return ((PendingChannelItem) this).getChannel().getFundingOutpoint().toString().hashCode();
            case TYPE_CLOSED_CHANNEL:
                return ((ClosedChannelItem) this).getChannel().getFundingOutpoint().toString().hashCode();
            default:
                return 0;
        }
    }
}
