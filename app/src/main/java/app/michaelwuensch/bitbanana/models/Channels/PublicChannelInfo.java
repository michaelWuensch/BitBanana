package app.michaelwuensch.bitbanana.models.Channels;

import app.michaelwuensch.bitbanana.models.Outpoint;

public class PublicChannelInfo {

    private final ShortChannelId ShortChannelId;
    private final Outpoint FundingOutpoint;
    private final String Node1PubKey;
    private final String Node2PubKey;
    private final RoutingPolicy Node1RoutingPolicy;
    private final RoutingPolicy Node2RoutingPolicy;

    public static Builder newBuilder() {
        return new Builder();
    }

    private PublicChannelInfo(Builder builder) {
        this.ShortChannelId = builder.ShortChannelId;
        this.FundingOutpoint = builder.FundingOutpoint;
        this.Node1PubKey = builder.Node1PubKey;
        this.Node2PubKey = builder.Node2PubKey;
        this.Node1RoutingPolicy = builder.Node1RoutingPolicy;
        this.Node2RoutingPolicy = builder.Node2RoutingPolicy;
    }

    public ShortChannelId getShortChannelId() {
        return ShortChannelId;
    }

    public Outpoint getFundingOutpoint() {
        return FundingOutpoint;
    }

    public String getNode1PubKey() {
        return Node1PubKey;
    }

    public String getNode2PubKey() {
        return Node2PubKey;
    }

    public RoutingPolicy getNode1RoutingPolicy() {
        return Node1RoutingPolicy;
    }

    public RoutingPolicy getNode2RoutingPolicy() {
        return Node2RoutingPolicy;
    }


    //Builder Class
    public static class Builder {
        private ShortChannelId ShortChannelId;
        private Outpoint FundingOutpoint;
        private String Node1PubKey;
        private String Node2PubKey;
        private RoutingPolicy Node1RoutingPolicy;
        private RoutingPolicy Node2RoutingPolicy;

        private Builder() {
            // required parameters
        }

        public PublicChannelInfo build() {
            return new PublicChannelInfo(this);
        }

        public Builder setShortChannelId(ShortChannelId shortChannelId) {
            this.ShortChannelId = shortChannelId;
            return this;
        }

        public Builder setFundingOutpoint(Outpoint fundingOutpoint) {
            this.FundingOutpoint = fundingOutpoint;
            return this;
        }

        public Builder setNode1PubKey(String node1PubKey) {
            Node1PubKey = node1PubKey;
            return this;
        }

        public Builder setNode2PubKey(String node2PubKey) {
            Node2PubKey = node2PubKey;
            return this;
        }

        public Builder setNode1RoutingPolicy(RoutingPolicy node1RoutingPolicy) {
            Node1RoutingPolicy = node1RoutingPolicy;
            return this;
        }

        public Builder setNode2RoutingPolicy(RoutingPolicy node2RoutingPolicy) {
            Node2RoutingPolicy = node2RoutingPolicy;
            return this;
        }
    }
}