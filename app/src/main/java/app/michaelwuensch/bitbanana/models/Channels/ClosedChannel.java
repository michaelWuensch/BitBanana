package app.michaelwuensch.bitbanana.models.Channels;

import java.io.Serializable;
import java.util.List;

import app.michaelwuensch.bitbanana.models.Outpoint;

public class ClosedChannel implements Serializable {

    private final String RemotePubKey;
    private final ShortChannelId ShortChannelId;
    private final boolean hasShortChannelId;
    private final String CloseTransactionId;
    private final boolean hasCloseTransactionId;
    private final String ChannelType;
    private final boolean OpenInitiator;
    private final boolean CloseInitiator;
    private final CloseType CloseType;
    private final boolean hasCloseType;
    private final int CloseHeight;
    private final boolean hasCloseHeight;
    private final boolean Private;
    private final boolean hasPrivate;
    private final long Capacity;
    private final long LocalBalance;
    private final long RemoteBalance;
    private final Outpoint FundingOutpoint;
    private final List<String> SweepTransactionIds;
    private final boolean hasSweepTransactionIds;

    public static Builder newBuilder() {
        return new Builder();
    }

    private ClosedChannel(Builder builder) {
        this.RemotePubKey = builder.RemotePubKey;
        this.ShortChannelId = builder.ShortChannelId;
        this.hasShortChannelId = builder.hasShortChannelId;
        this.CloseTransactionId = builder.CloseTransactionId;
        this.hasCloseTransactionId = builder.hasCloseTransactionId;
        this.ChannelType = builder.ChannelType;
        this.OpenInitiator = builder.OpenInitiator;
        this.CloseInitiator = builder.CloseInitiator;
        this.CloseType = builder.CloseType;
        this.hasCloseType = builder.hasCloseType;
        this.CloseHeight = builder.CloseHeight;
        this.hasCloseHeight = builder.hasCloseHeight;
        this.Private = builder.Private;
        this.hasPrivate = builder.hasPrivate;
        this.Capacity = builder.Capacity;
        this.LocalBalance = builder.LocalBalance;
        this.RemoteBalance = builder.RemoteBalance;
        this.FundingOutpoint = builder.FundingOutpoint;
        this.SweepTransactionIds = builder.SweepTransactionIds;
        this.hasSweepTransactionIds = builder.hasSweepTransactionIds;
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

    public String getCloseTransactionId() {
        return CloseTransactionId;
    }

    public boolean hasCloseTransactionId() {
        return hasCloseTransactionId;
    }

    public String getChannelType() {
        return ChannelType;
    }

    /**
     * True if we were the ones that created the channel.
     */
    public boolean isOpenInitiator() {
        return OpenInitiator;
    }

    /**
     * True if we were the ones that closed the channel.
     */
    public boolean isCloseInitiator() {
        return CloseInitiator;
    }

    public ClosedChannel.CloseType getCloseType() {
        return CloseType;
    }

    public boolean isHasCloseType() {
        return hasCloseType;
    }

    /**
     * Block Height at which the funding transaction was spent.
     */
    public int getCloseHeight() {
        return CloseHeight;
    }

    public boolean hasCloseHeight() {
        return hasCloseHeight;
    }

    public boolean isPrivate() {
        return Private;
    }

    public boolean hasPrivate() {
        return hasPrivate;
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

    public List<String> getSweepTransactionIds() {
        return SweepTransactionIds;
    }

    public boolean hasSweepTransactionIds() {
        return hasSweepTransactionIds;
    }


    //Builder Class
    public static class Builder {

        private String RemotePubKey;
        private ShortChannelId ShortChannelId;
        private boolean hasShortChannelId;
        private String CloseTransactionId;
        private boolean hasCloseTransactionId;
        private String ChannelType;
        private boolean OpenInitiator;
        private boolean CloseInitiator;
        private CloseType CloseType;
        private boolean hasCloseType;
        private int CloseHeight;
        private boolean hasCloseHeight;
        private boolean Private;
        private boolean hasPrivate;
        private long Capacity;
        private long LocalBalance;
        private long RemoteBalance;
        private Outpoint FundingOutpoint;
        private List<String> SweepTransactionIds;
        private boolean hasSweepTransactionIds;

        private Builder() {
            // required parameters
        }

        public ClosedChannel build() {
            return new ClosedChannel(this);
        }

        public Builder setRemotePubKey(String remotePubKey) {
            this.RemotePubKey = remotePubKey;
            return this;
        }

        public Builder setChannelType(String channelType) {
            ChannelType = channelType;
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

        public Builder setCloseTransactionId(String closeTransactionId) {
            CloseTransactionId = closeTransactionId;
            hasCloseTransactionId = true;
            return this;
        }

        /**
         * Set to true if we were the ones that created the channel.
         */
        public Builder setOpenInitiator(boolean openInitiator) {
            OpenInitiator = openInitiator;
            return this;
        }

        /**
         * Set to true if we were the ones that closed the channel.
         */
        public Builder setCloseInitiator(boolean closeInitiator) {
            CloseInitiator = closeInitiator;
            return this;
        }

        public Builder setCloseType(CloseType closeType) {
            CloseType = closeType;
            hasCloseType = true;
            return this;
        }

        public Builder setCloseHeight(int closeHeight) {
            CloseHeight = closeHeight;
            hasCloseHeight = true;
            return this;
        }

        public Builder setPrivate(boolean isPrivate) {
            Private = isPrivate;
            hasPrivate = true;
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

        public Builder setSweepTransactionIds(List<String> sweepTransactionIds) {
            SweepTransactionIds = sweepTransactionIds;
            hasSweepTransactionIds = true;
            return this;
        }
    }

    public enum CloseType {
        UNKNOWN,
        COOPERATIVE_CLOSE,
        FORCE_CLOSE,
        BREACH_CLOSE;

        public static CloseType parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return UNKNOWN;
            }
        }
    }
}