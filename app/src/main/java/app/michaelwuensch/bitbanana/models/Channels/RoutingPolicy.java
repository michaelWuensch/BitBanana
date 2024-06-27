package app.michaelwuensch.bitbanana.models.Channels;

import java.io.Serializable;

public class RoutingPolicy implements Serializable {

    private final long FeeBase;
    private final long FeeRate;
    private final long InboundFeeBase;
    private final long InboundFeeRate;
    private final int Delay;
    private final long MinHTLC;
    private final long MaxHTLC;

    public static Builder newBuilder() {
        return new Builder();
    }

    private RoutingPolicy(Builder builder) {
        this.FeeBase = builder.FeeBase;
        this.FeeRate = builder.FeeRate;
        this.Delay = builder.Delay;
        this.MinHTLC = builder.MinHTLC;
        this.MaxHTLC = builder.MaxHTLC;
        this.InboundFeeBase = builder.InboundFeeBase;
        this.InboundFeeRate = builder.InboundFeeRate;
    }

    public long getFeeBase() {
        return FeeBase;
    }

    public long getFeeRate() {
        return FeeRate;
    }

    public long getInboundFeeBase() {
        return InboundFeeBase;
    }

    public long getInboundFeeRate() {
        return InboundFeeRate;
    }

    public int getDelay() {
        return Delay;
    }

    public long getMinHTLC() {
        return MinHTLC;
    }

    public long getMaxHTLC() {
        return MaxHTLC;
    }


    //Builder Class
    public static class Builder {
        private long FeeBase;
        private long FeeRate;
        private long InboundFeeBase;
        private long InboundFeeRate;
        private int Delay;
        private long MinHTLC;
        private long MaxHTLC;

        private Builder() {
            // required parameters
        }

        public RoutingPolicy build() {
            return new RoutingPolicy(this);
        }

        public Builder setFeeBase(long feeBase) {
            this.FeeBase = feeBase;
            return this;
        }

        public Builder setFeeRate(long feeRate) {
            this.FeeRate = feeRate;
            return this;
        }

        public Builder setInboundFeeBase(long feeBase) {
            this.InboundFeeBase = feeBase;
            return this;
        }

        public Builder setInboundFeeRate(long feeRate) {
            this.InboundFeeRate = feeRate;
            return this;
        }

        public Builder setDelay(int delay) {
            Delay = delay;
            return this;
        }

        public Builder setMinHTLC(long minHTLC) {
            MinHTLC = minHTLC;
            return this;
        }

        public Builder setMaxHTLC(long maxHTLC) {
            MaxHTLC = maxHTLC;
            return this;
        }
    }
}