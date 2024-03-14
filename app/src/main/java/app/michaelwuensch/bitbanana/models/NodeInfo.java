package app.michaelwuensch.bitbanana.models;

public class NodeInfo {

    private final String Alias;
    private final String PubKey;

    public static Builder newBuilder() {
        return new Builder();
    }

    private NodeInfo(Builder builder) {
        this.Alias = builder.Alias;
        this.PubKey = builder.PubKey;
    }

    public String getAlias() {
        return Alias;
    }

    public String getPubKey() {
        return PubKey;
    }


    //Builder Class
    public static class Builder {
        private String Alias;
        private String PubKey;

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
    }
}