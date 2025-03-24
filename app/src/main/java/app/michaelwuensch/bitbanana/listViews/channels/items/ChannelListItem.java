package app.michaelwuensch.bitbanana.listViews.channels.items;

import androidx.annotation.Nullable;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.util.PrefsUtil;

public abstract class ChannelListItem implements Comparable<ChannelListItem> {

    public static final int TYPE_OPEN_CHANNEL = 0;
    public static final int TYPE_PENDING_CHANNEL = 1;
    public static final int TYPE_CLOSED_CHANNEL = 2;

    abstract public int getType();

    abstract public Serializable getSerializedChannel();

    abstract public String getAlias();

    abstract public long getCapacity();

    abstract public long getLocalBalance();

    abstract public long getRemoteBalance();

    abstract public double getBalanceRatio();

    public enum SortCriteria {
        NAME_ASC,
        NAME_DESC,
        CAPACITY_ASC,
        CAPACITY_DESC,
        INBOUND_CAPACITY_ASC,
        INBOUND_CAPACITY_DESC,
        OUTBOUND_CAPACITY_ASC,
        OUTBOUND_CAPACITY_DESC,
        SYMMETRY_ASC,
        SYMMETRY_DESC
    }

    @Override
    public int compareTo(ChannelListItem other) {
        SortCriteria currentCriteria = SortCriteria.valueOf(PrefsUtil.getPrefs().getString(PrefsUtil.CHANNEL_SORT_CRITERIA, SortCriteria.NAME_ASC.name()));

        switch (currentCriteria) {
            case NAME_ASC:
                return getAlias().compareToIgnoreCase(other.getAlias());
            case NAME_DESC:
                return other.getAlias().compareToIgnoreCase(getAlias());
            case CAPACITY_ASC:
                return Long.compare(getCapacity(), other.getCapacity());
            case CAPACITY_DESC:
                return Long.compare(other.getCapacity(), getCapacity());
            case INBOUND_CAPACITY_ASC:
                return Long.compare(getRemoteBalance(), other.getRemoteBalance());
            case INBOUND_CAPACITY_DESC:
                return Long.compare(other.getRemoteBalance(), getRemoteBalance());
            case OUTBOUND_CAPACITY_ASC:
                return Long.compare(getLocalBalance(), other.getLocalBalance());
            case OUTBOUND_CAPACITY_DESC:
                return Long.compare(other.getLocalBalance(), getLocalBalance());
            case SYMMETRY_ASC:
                return Double.compare(getBalanceRatio(), other.getBalanceRatio());
            case SYMMETRY_DESC:
                return Double.compare(other.getBalanceRatio(), getBalanceRatio());
            default:
                return 0;
        }
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
