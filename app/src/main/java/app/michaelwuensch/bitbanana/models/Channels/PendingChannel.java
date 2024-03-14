package app.michaelwuensch.bitbanana.models.Channels;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Outpoint;

public class PendingChannel implements Serializable {

    private final String RemotePubKey;
    private final ShortChannelId ShortChannelId;
    private final boolean hasShortChannelId;
    private final String ChannelType;
    private final PendingType PendingType;
    private final boolean Initiator;
    private final boolean Private;
    private final long Capacity;
    private final long LocalBalance;
    private final long RemoteBalance;
    private final long CommitFee;
    private final boolean hasCommitFee;
    private final long TotalSent;
    private final long TotalReceived;
    private final Outpoint FundingOutpoint;
    private final String CloseTransactionId;
    private final boolean hasCloseTransactionId;
    private final int BlocksTilMaturity;
    private final boolean hasBlocksTilMaturity;

    public static Builder newBuilder() {
        return new Builder();
    }

    private PendingChannel(Builder builder) {
        this.RemotePubKey = builder.RemotePubKey;
        this.ShortChannelId = builder.ShortChannelId;
        this.hasShortChannelId = builder.hasShortChannelId;
        this.ChannelType = builder.ChannelType;
        this.PendingType = builder.PendingType;
        this.Initiator = builder.Initiator;
        this.Private = builder.Private;
        this.Capacity = builder.Capacity;
        this.LocalBalance = builder.LocalBalance;
        this.RemoteBalance = builder.RemoteBalance;
        this.FundingOutpoint = builder.FundingOutpoint;
        this.CommitFee = builder.CommitFee;
        this.hasCommitFee = builder.hasCommitFee;
        this.TotalSent = builder.TotalSent;
        this.TotalReceived = builder.TotalReceived;
        this.CloseTransactionId = builder.CloseTransactionId;
        this.hasCloseTransactionId = builder.hasClosingTxId;
        this.BlocksTilMaturity = builder.BlocksTilMaturity;
        this.hasBlocksTilMaturity = builder.hasBlocksTilMaturity;

    }

    public String getRemotePubKey() {
        return RemotePubKey;
    }

    /**
     * The ShortChannelId (SCID) is a compact representation of a channel within the Lightning Network.
     * It contains information about the funding transaction.
     */
    public ShortChannelId getShortChannelId() {
        return ShortChannelId;
    }

    public boolean hasShortChannelId() {
        return hasShortChannelId;
    }

    public String getChannelType() {
        return ChannelType;
    }

    public PendingType getPendingType() {
        return PendingType;
    }

    /**
     * True if we were the ones that created the channel.
     */
    public boolean isInitiator() {
        return Initiator;
    }

    public boolean isPrivate() {
        return Private;
    }

    /**
     * The total amount of funds held in this channel in msat
     */
    public long getCapacity() {
        return Capacity;
    }

    /**
     * This node's current balance in msat in this channel.
     */
    public long getLocalBalance() {
        return LocalBalance;
    }

    /**
     * The counterpart's current balance in msat in this channel.
     */
    public long getRemoteBalance() {
        return RemoteBalance;
    }

    public Outpoint getFundingOutpoint() {
        return FundingOutpoint;
    }

    /**
     * The amount calculated to be paid in fees for the current set of commitment transactions.
     */
    public long getCommitFee() {
        return CommitFee;
    }

    public boolean hasCommitFee() {
        return hasCommitFee;
    }

    /**
     * The total number of msats we've sent within this channel.
     */
    public long getTotalSent() {
        return TotalSent;
    }

    /**
     * The total number of msats we've received within this channel.
     */
    public long getTotalReceived() {
        return TotalReceived;
    }

    /**
     * The total of sent and received funds divided by the capacity of the channel.
     * A channel with an activity greater than 1 means that more funds traveled through the channel than it can hold.
     * This metric is designed to show how effective a given channel is.
     */
    public double getActivity() {
        return (double) ((TotalSent + TotalReceived) / Capacity);
    }

    public String getCloseTransactionId() {
        return CloseTransactionId;
    }

    public boolean hasCloseTransactionId() {
        return hasCloseTransactionId;
    }

    public int getBlocksTilMaturity() {
        return BlocksTilMaturity;
    }

    public boolean hasBlocksTilMaturity() {
        return hasBlocksTilMaturity;
    }


    //Builder Class
    public static class Builder {

        private String RemotePubKey;
        private ShortChannelId ShortChannelId;
        private boolean hasShortChannelId;
        private String ChannelType;
        private PendingType PendingType;
        private boolean Initiator;
        private boolean Private;
        private long Capacity;
        private long LocalBalance;
        private long RemoteBalance;
        private Outpoint FundingOutpoint;
        private long CommitFee;
        private boolean hasCommitFee;
        private long TotalSent;
        private long TotalReceived;
        private String CloseTransactionId;
        private boolean hasClosingTxId;
        private int BlocksTilMaturity;
        private boolean hasBlocksTilMaturity;

        private Builder() {
            // required parameters
        }

        public PendingChannel build() {
            return new PendingChannel(this);
        }

        public Builder setRemotePubKey(String remotePubKey) {
            this.RemotePubKey = remotePubKey;
            return this;
        }

        public Builder setChannelType(String channelType) {
            ChannelType = channelType;
            return this;
        }

        public Builder setPendingType(PendingType pendingType) {
            PendingType = pendingType;
            return this;
        }

        /**
         * The ShortChannelId (SCID) is a compact representation of a channel within the Lightning Network.
         * It contains information about the funding transaction.
         */
        public Builder setShortChannelId(ShortChannelId shortChannelId) {
            ShortChannelId = shortChannelId;
            hasShortChannelId = shortChannelId != null;
            return this;
        }

        /**
         * Set to true if we were the ones that created the channel.
         */
        public Builder setInitiator(boolean initiator) {
            Initiator = initiator;
            return this;
        }

        public Builder setPrivate(boolean aPrivate) {
            Private = aPrivate;
            return this;
        }

        /**
         * The total amount of funds held in this channel in msat
         */
        public Builder setCapacity(long capacity) {
            Capacity = capacity;
            return this;
        }

        /**
         * This node's current balance in msat in this channel.
         */
        public Builder setLocalBalance(long localBalance) {
            LocalBalance = localBalance;
            return this;
        }

        /**
         * The counterpart's current balance in msat in this channel.
         */
        public Builder setRemoteBalance(long remoteBalance) {
            RemoteBalance = remoteBalance;
            return this;
        }

        public Builder setFundingOutpoint(Outpoint fundingOutpoint) {
            FundingOutpoint = fundingOutpoint;
            return this;
        }

        /**
         * The amount calculated to be paid in fees for the current set of commitment transactions.
         */
        public Builder setCommitFee(long commitFee) {
            CommitFee = commitFee;
            hasCommitFee = true;
            return this;
        }

        /**
         * The total number of msats we've sent within this channel.
         */
        public Builder setTotalSent(long totalSent) {
            TotalSent = totalSent;
            return this;
        }

        /**
         * The total number of msats we've received within this channel.
         */
        public Builder setTotalReceived(long totalReceived) {
            TotalReceived = totalReceived;
            return this;
        }

        public Builder setCloseTransactionId(String closeTransactionId) {
            CloseTransactionId = closeTransactionId;
            hasClosingTxId = true;
            return this;
        }

        public Builder setBlocksTilMaturity(int blocksTilMaturity) {
            BlocksTilMaturity = blocksTilMaturity;
            hasBlocksTilMaturity = true;
            return this;
        }
    }

    public enum PendingType {
        UNKNOWN,
        PENDING_OPEN,
        PENDING_CLOSE,
        PENDING_FORCE_CLOSE;

        public static PendingType parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return UNKNOWN;
            }
        }
    }
}