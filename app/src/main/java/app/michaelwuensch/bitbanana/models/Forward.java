package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;

public class Forward implements Serializable {

    private final ShortChannelId ChannelIdIn;
    private final ShortChannelId ChannelIdOut;
    private final long AmountIn;
    private final long AmountOut;
    private final long Fee;
    private final long TimestampNs;


    public static Builder newBuilder() {
        return new Builder();
    }

    private Forward(Builder builder) {
        this.ChannelIdIn = builder.ChannelIdIn;
        this.ChannelIdOut = builder.ChannelIdOut;
        this.AmountIn = builder.AmountIn;
        this.AmountOut = builder.AmountOut;
        this.Fee = builder.Fee;
        this.TimestampNs = builder.TimestampNs;
    }

    public ShortChannelId getChannelIdIn() {
        return ChannelIdIn;
    }

    public ShortChannelId getChannelIdOut() {
        return ChannelIdOut;
    }


    /**
     * Amount in in msat
     */
    public long getAmountIn() {
        return AmountIn;
    }

    /**
     * Amount out in msat
     */
    public long getAmountOut() {
        return AmountOut;
    }

    /**
     * The paid fee in msat
     */
    public long getFee() {
        return Fee;
    }

    /**
     * UNIX timestamp of when the forwarding was resolved in nano seconds since the unix epoch.
     */
    public long getTimestampNs() {
        return TimestampNs;
    }


    //Builder Class
    public static class Builder {

        private ShortChannelId ChannelIdIn;
        private ShortChannelId ChannelIdOut;
        private long AmountIn;
        private long AmountOut;
        private long Fee;
        private long TimestampNs;


        private Builder() {
            // required parameters
        }

        public Forward build() {
            return new Forward(this);
        }

        public Builder setChannelIdIn(ShortChannelId channelIdIn) {
            ChannelIdIn = channelIdIn;
            return this;
        }

        public Builder setChannelIdOut(ShortChannelId channelIdOut) {
            ChannelIdOut = channelIdOut;
            return this;
        }

        /**
         * Amount in in msat
         */
        public Builder setAmountIn(long amountIn) {
            AmountIn = amountIn;
            return this;
        }

        /**
         * Amount out in msat
         */
        public Builder setAmountOut(long amountOut) {
            AmountOut = amountOut;
            return this;
        }

        /**
         * The paid fee in msat
         */
        public Builder setFee(long fee) {
            Fee = fee;
            return this;
        }

        /**
         * UNIX timestamp of when the payment was created in nano seconds since the unix epoch.
         */
        public Builder setTimestampNs(long timestampNs) {
            this.TimestampNs = timestampNs;
            return this;
        }
    }
}