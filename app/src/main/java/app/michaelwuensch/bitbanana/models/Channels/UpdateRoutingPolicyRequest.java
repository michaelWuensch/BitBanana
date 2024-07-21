package app.michaelwuensch.bitbanana.models.Channels;

import java.io.Serializable;

public class UpdateRoutingPolicyRequest implements Serializable {

    private final OpenChannel Channel;
    private final boolean hasChannel;
    private final long FeeBase;
    private final boolean hasFeeBase;
    private final long FeeRate;
    private final boolean hasFeeRate;
    private final long InboundFeeBase;
    private final boolean hasInboundFeeBase;
    private final long InboundFeeRate;
    private final boolean hasInboundFeeRate;
    private final int Delay;
    private final boolean hasDelay;
    private final long MinHTLC;
    private final boolean hasMinHTLC;
    private final long MaxHTLC;
    private final boolean hasMaxHTLC;

    public static Builder newBuilder() {
        return new Builder();
    }

    private UpdateRoutingPolicyRequest(Builder builder) {
        this.Channel = builder.Channel;
        this.hasChannel = builder.hasChannel;
        this.FeeBase = builder.FeeBase;
        this.hasFeeBase = builder.hasFeeBase;
        this.FeeRate = builder.FeeRate;
        this.hasFeeRate = builder.hasFeeRate;
        this.InboundFeeBase = builder.InboundFeeBase;
        this.hasInboundFeeBase = builder.hasInboundFeeBase;
        this.InboundFeeRate = builder.InboundFeeRate;
        this.hasInboundFeeRate = builder.hasInboundFeeRate;
        this.Delay = builder.Delay;
        this.hasDelay = builder.hasDelay;
        this.MinHTLC = builder.MinHTLC;
        this.hasMinHTLC = builder.hasMinHTLC;
        this.MaxHTLC = builder.MaxHTLC;
        this.hasMaxHTLC = builder.hasMaxHTLC;
    }

    public OpenChannel getChannel() {
        return Channel;
    }

    public boolean hasChannel() {
        return hasChannel;
    }

    public long getFeeBase() {
        return FeeBase;
    }

    public boolean hasFeeBase() {
        return hasFeeBase;
    }

    public long getFeeRate() {
        return FeeRate;
    }

    public boolean hasFeeRate() {
        return hasFeeRate;
    }

    public long getInboundFeeBase() {
        return InboundFeeBase;
    }

    public boolean hasInboundFeeBase() {
        return hasInboundFeeBase;
    }

    public long getInboundFeeRate() {
        return InboundFeeRate;
    }

    public boolean hasInboundFeeRate() {
        return hasInboundFeeRate;
    }

    public int getDelay() {
        return Delay;
    }

    public boolean hasDelay() {
        return hasDelay;
    }

    public long getMinHTLC() {
        return MinHTLC;
    }

    public boolean hasMinHTLC() {
        return hasMinHTLC;
    }

    public long getMaxHTLC() {
        return MaxHTLC;
    }

    public boolean hasMaxHTLC() {
        return hasMaxHTLC;
    }


    //Builder Class
    public static class Builder {
        private OpenChannel Channel;
        private boolean hasChannel;
        private long FeeBase;
        private boolean hasFeeBase;
        private long FeeRate;
        private boolean hasFeeRate;
        private long InboundFeeBase;
        private boolean hasInboundFeeBase;
        private long InboundFeeRate;
        private boolean hasInboundFeeRate;
        private int Delay;
        private boolean hasDelay;
        private long MinHTLC;
        private boolean hasMinHTLC;
        private long MaxHTLC;
        private boolean hasMaxHTLC;

        private Builder() {
            // required parameters
        }

        public UpdateRoutingPolicyRequest build() {
            return new UpdateRoutingPolicyRequest(this);
        }

        public Builder setChannel(OpenChannel channel) {
            Channel = channel;
            hasChannel = true;
            return this;
        }

        public Builder setFeeBase(long feeBase) {
            this.FeeBase = feeBase;
            this.hasFeeBase = true;
            return this;
        }

        public Builder setFeeRate(long feeRate) {
            this.FeeRate = feeRate;
            this.hasFeeRate = true;
            return this;
        }

        public Builder setInboundFeeBase(long feeBase) {
            this.InboundFeeBase = feeBase;
            this.hasInboundFeeBase = true;
            return this;
        }

        public Builder setInboundFeeRate(long feeRate) {
            this.InboundFeeRate = feeRate;
            this.hasInboundFeeRate = true;
            return this;
        }

        public Builder setDelay(int delay) {
            Delay = delay;
            this.hasDelay = true;
            return this;
        }

        public Builder setMinHTLC(long minHTLC) {
            MinHTLC = minHTLC;
            this.hasMinHTLC = true;
            return this;
        }

        public Builder setMaxHTLC(long maxHTLC) {
            MaxHTLC = maxHTLC;
            this.hasMaxHTLC = true;
            return this;
        }
    }
}