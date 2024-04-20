package app.michaelwuensch.bitbanana.models.Channels;

import java.io.Serializable;

public class ChannelConstraints implements Serializable {

    private final int SelfDelay;
    private final long ChannelReserve;


    private ChannelConstraints(Builder builder) {
        SelfDelay = builder.SelfDelay;
        ChannelReserve = builder.ChannelReserve;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Self delay expressed in relative blocks. If the channel is force closed by this node, the node will need to wait for this many blocks before it can regain its funds.
     */
    public int getSelfDelay() {
        return SelfDelay;
    }

    /**
     * The minimum amount in msats this node is required to reserve in its balance.
     * This makes sure this node always has something to lose if it tries to cheat.
     */
    public long getChannelReserve() {
        return ChannelReserve;
    }

    public static class Builder {
        private int SelfDelay;
        private long ChannelReserve;

        private Builder() {
            // required parameters
        }

        public ChannelConstraints build() {
            return new ChannelConstraints(this);
        }

        /**
         * Self delay expressed in relative blocks. If the channel is force closed by this node, the node will need to wait for this many blocks before it can regain its funds.
         */
        public Builder setSelfDelay(int selfDelay) {
            this.SelfDelay = selfDelay;
            return this;
        }

        /**
         * The minimum amount in msats this node is required to reserve in its balance.
         * This makes sure this node always has something to lose if it tries to cheat.
         */
        public Builder setChannelReserve(long channelReserve) {
            ChannelReserve = channelReserve;
            return this;
        }
    }
}
