package app.michaelwuensch.bitbanana.models.Channels;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Outpoint;

public class OpenChannel implements Serializable {

    private final boolean Active;
    private final String RemotePubKey;
    private final ShortChannelId ShortChannelId;
    private final String ChannelType;
    private final boolean hasChannelType;
    private final boolean Initiator;
    private final boolean Private;
    private final long Capacity;
    private final long LocalBalance;
    private final long RemoteBalance;
    private final ChannelConstraints LocalChannelConstraints;
    private final ChannelConstraints RemoteChannelConstraints;
    private final long CommitFee;
    private final boolean hasCommitFee;
    private final long TotalSent;
    private final long TotalReceived;
    private final Outpoint FundingOutpoint;

    public static Builder newBuilder() {
        return new Builder();
    }

    private OpenChannel(Builder builder) {
        this.Active = builder.Active;
        this.RemotePubKey = builder.RemotePubKey;
        this.ShortChannelId = builder.ShortChannelId;
        this.ChannelType = builder.ChannelType;
        this.hasChannelType = builder.hasChannelType;
        this.Initiator = builder.Initiator;
        this.Private = builder.Private;
        this.Capacity = builder.Capacity;
        this.LocalBalance = builder.LocalBalance;
        this.RemoteBalance = builder.RemoteBalance;
        this.LocalChannelConstraints = builder.LocalChannelConstraints;
        this.RemoteChannelConstraints = builder.RemoteChannelConstraints;
        this.FundingOutpoint = builder.FundingOutpoint;
        this.CommitFee = builder.CommitFee;
        this.hasCommitFee = builder.hasCommitFee;
        this.TotalSent = builder.TotalSent;
        this.TotalReceived = builder.TotalReceived;
    }

    public boolean isActive() {
        return Active;
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

    public String getChannelType() {
        return ChannelType;
    }

    public boolean hasChannelType() {
        return hasChannelType;
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

    public ChannelConstraints getLocalChannelConstraints() {
        return LocalChannelConstraints;
    }

    public ChannelConstraints getRemoteChannelConstraints() {
        return RemoteChannelConstraints;
    }

    public Outpoint getFundingOutpoint() {
        return FundingOutpoint;
    }

    /**
     * The amount in msat calculated to be paid in fees for the current set of commitment transactions.
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
        return ((TotalSent + TotalReceived) / (double) Capacity);
    }


    //Builder Class
    public static class Builder {

        private boolean Active;
        private String RemotePubKey;
        private ShortChannelId ShortChannelId;
        private String ChannelType;
        private boolean hasChannelType;
        private boolean Initiator;
        private boolean Private;
        private long Capacity;
        private long LocalBalance;
        private long RemoteBalance;
        private ChannelConstraints LocalChannelConstraints;
        private ChannelConstraints RemoteChannelConstraints;
        private Outpoint FundingOutpoint;
        private long CommitFee;
        private boolean hasCommitFee;
        private long TotalSent;
        private long TotalReceived;

        private Builder() {
            // required parameters
        }

        public OpenChannel build() {
            return new OpenChannel(this);
        }

        public Builder setActive(boolean active) {
            Active = active;
            return this;
        }

        public Builder setRemotePubKey(String remotePubKey) {
            this.RemotePubKey = remotePubKey;
            return this;
        }

        public Builder setChannelType(String channelType) {
            ChannelType = channelType;
            hasChannelType = true;
            return this;
        }

        /**
         * The ShortChannelId (SCID) is a compact representation of a channel within the Lightning Network.
         * It contains information about the funding transaction.
         */
        public Builder setShortChannelId(ShortChannelId shortChannelId) {
            ShortChannelId = shortChannelId;
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

        public Builder setLocalChannelConstraints(ChannelConstraints localChannelConstraints) {
            LocalChannelConstraints = localChannelConstraints;
            return this;
        }

        public Builder setRemoteChannelConstraints(ChannelConstraints remoteChannelConstraints) {
            RemoteChannelConstraints = remoteChannelConstraints;
            return this;
        }

        public Builder setFundingOutpoint(Outpoint fundingOutpoint) {
            FundingOutpoint = fundingOutpoint;
            return this;
        }

        /**
         * The amount in msat calculated to be paid in fees for the current set of commitment transactions.
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
    }
}