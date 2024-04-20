package app.michaelwuensch.bitbanana.models.Channels;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Outpoint;

public class CloseChannelRequest implements Serializable {

    private final ShortChannelId ShortChannelId;
    private final Outpoint FundingOutpoint;
    private final long SatPerVByte;
    private final boolean ForceClose;

    public static Builder newBuilder() {
        return new Builder();
    }

    private CloseChannelRequest(Builder builder) {
        this.ShortChannelId = builder.ShortChannelId;
        this.FundingOutpoint = builder.FundingOutpoint;
        this.SatPerVByte = builder.SatPerVByte;
        this.ForceClose = builder.ForceClose;
    }

    public ShortChannelId getShortChannelId() {
        return ShortChannelId;
    }

    public Outpoint getFundingOutpoint() {
        return FundingOutpoint;
    }

    /**
     * Fee rate to use to open the channel in sat/vB. (For cooperative closes)
     */
    public long getSatPerVByte() {
        return SatPerVByte;
    }

    /**
     * Whether or not the channel should be force closed. This means the current commitment transaction will be signed and broadcast.
     */
    public boolean isForceClose() {
        return ForceClose;
    }


    //Builder Class
    public static class Builder {

        private ShortChannelId ShortChannelId;
        private Outpoint FundingOutpoint;
        private long SatPerVByte;
        private boolean ForceClose;


        private Builder() {
            // required parameters
        }

        public CloseChannelRequest build() {
            return new CloseChannelRequest(this);
        }

        public Builder setShortChannelId(ShortChannelId shortChannelId) {
            ShortChannelId = shortChannelId;
            return this;
        }

        public Builder setFundingOutpoint(Outpoint fundingOutpoint) {
            FundingOutpoint = fundingOutpoint;
            return this;
        }

        /**
         * Fee rate to use to open the channel in sat/vB. (For cooperative closes)
         */
        public Builder setSatPerVByte(long satPerVByte) {
            SatPerVByte = satPerVByte;
            return this;
        }

        /**
         * Whether or not the channel should be force closed. This means the current commitment transaction will be signed and broadcast.
         */
        public Builder setForceClose(boolean isPrivate) {
            ForceClose = isPrivate;
            return this;
        }
    }
}