package app.michaelwuensch.bitbanana.models;

public class NodeInfo {

    private final String Alias;
    private final String PubKey;
    private final int NumChannels;
    private final boolean hasNumChannels;
    private final long TotalCapacity;
    private final boolean hasTotalCapacity;

    public static Builder newBuilder() {
        return new Builder();
    }

    private NodeInfo(Builder builder) {
        this.Alias = builder.Alias;
        this.PubKey = builder.PubKey;
        this.NumChannels = builder.NumChannels;
        this.hasNumChannels = builder.hasNumChannels;
        this.TotalCapacity = builder.TotalCapacity;
        this.hasTotalCapacity = builder.hasTotalCapacity;
    }

    public String getAlias() {
        return Alias;
    }

    public String getPubKey() {
        return PubKey;
    }

    public int getNumChannels() {
        return NumChannels;
    }

    public boolean hasNumChannels() {
        return hasNumChannels;
    }

    /**
     * Total capacity in msats (sum of all channel capacities)
     */
    public long getTotalCapacity() {
        return TotalCapacity;
    }

    public boolean hasTotalCapacity() {
        return hasTotalCapacity;
    }


    //Builder Class
    public static class Builder {
        private String Alias;
        private String PubKey;
        private int NumChannels;
        private boolean hasNumChannels;
        private long TotalCapacity;
        private boolean hasTotalCapacity;

        private Builder() {
            // required parameters
        }

        public NodeInfo build() {
            return new NodeInfo(this);
        }

        public Builder setAlias(String alias) {
            this.Alias = alias;
            return this;
        }

        public Builder setPubKey(String pubKey) {
            this.PubKey = pubKey;
            return this;
        }

        public Builder setNumChannels(int numChannels) {
            NumChannels = numChannels;
            hasNumChannels = true;
            return this;
        }

        /**
         * Total capacity in msats (sum of all channel capacities)
         */
        public Builder setTotalCapacity(long totalCapacity) {
            TotalCapacity = totalCapacity;
            hasTotalCapacity = true;
            return this;
        }
    }
}