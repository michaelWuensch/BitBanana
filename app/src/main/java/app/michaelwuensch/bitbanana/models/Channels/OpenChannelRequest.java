package app.michaelwuensch.bitbanana.models.Channels;

import java.io.Serializable;

public class OpenChannelRequest implements Serializable {

    private final String NodePubKey;
    private final long Amount;
    private final long SatPerVByte;
    private final boolean Private;
    private final boolean UseAllFunds;

    public static Builder newBuilder() {
        return new Builder();
    }

    private OpenChannelRequest(Builder builder) {
        this.SatPerVByte = builder.SatPerVByte;
        this.Amount = builder.Amount;
        this.NodePubKey = builder.NodePubKey;
        this.Private = builder.Private;
        this.UseAllFunds = builder.UseAllFunds;
    }

    public String getNodePubKey() {
        return NodePubKey;
    }

    /**
     * Amount in msat.
     */
    public long getAmount() {
        return Amount;
    }

    /**
     * Fee rate to use to open the channel in sat/vB.
     */
    public long getSatPerVByte() {
        return SatPerVByte;
    }

    /**
     * Whether or not the channel is private (invisible to the network)
     */
    public boolean isPrivate() {
        return Private;
    }

    /**
     * If set to true all available funds will be used to open the channel ignoring the set amount.
     */
    public boolean isUseAllFunds() {
        return UseAllFunds;
    }


    //Builder Class
    public static class Builder {

        private String NodePubKey;
        private long Amount;
        private long SatPerVByte;
        private boolean Private;
        private boolean UseAllFunds;


        private Builder() {
            // required parameters
        }

        public OpenChannelRequest build() {
            return new OpenChannelRequest(this);
        }

        public Builder setNodePubKey(String nodePubKey) {
            NodePubKey = nodePubKey;
            return this;
        }

        /**
         * Amount in msat.
         */
        public Builder setAmount(long amount) {
            Amount = amount;
            return this;
        }

        /**
         * Fee rate to use to open the channel in sat/vB.
         */
        public Builder setSatPerVByte(long satPerVByte) {
            SatPerVByte = satPerVByte;
            return this;
        }

        /**
         * Whether or not the channel is private (invisible to the network)
         */
        public Builder setPrivate(boolean isPrivate) {
            Private = isPrivate;
            return this;
        }

        /**
         * If set to true all available funds will be used to open the channel ignoring the set amount.
         */
        public Builder setUseAllFunds(boolean useAllFunds) {
            UseAllFunds = useAllFunds;
            return this;
        }
    }
}