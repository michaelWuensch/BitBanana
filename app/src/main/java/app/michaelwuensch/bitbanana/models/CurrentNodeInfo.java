package app.michaelwuensch.bitbanana.models;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.util.Version;

public class CurrentNodeInfo {

    private final String Alias;
    private final Version Version;
    private final String FullVersionString;
    private final String PubKey;
    private final LightningNodeUri[] LightningNodeUris;
    private final int BlockHeight;
    private final BackendConfig.Network Network;
    private final boolean Synced;

    public static Builder newBuilder() {
        return new Builder();
    }

    private CurrentNodeInfo(Builder builder) {
        this.Alias = builder.Alias;
        this.Version = builder.Version;
        this.PubKey = builder.PubKey;
        this.LightningNodeUris = builder.LightningNodeUris;
        this.BlockHeight = builder.BlockHeight;
        this.Network = builder.Network;
        this.FullVersionString = builder.FullVersionString;
        this.Synced = builder.Synced;
    }

    public String getAlias() {
        return Alias;
    }

    public Version getVersion() {
        return Version;
    }

    public String getFullVersionString() {
        return FullVersionString;
    }

    public String getPubKey() {
        return PubKey;
    }

    public LightningNodeUri[] getLightningNodeUris() {
        return LightningNodeUris;
    }

    public int getBlockHeight() {
        return BlockHeight;
    }

    public BackendConfig.Network getNetwork() {
        return Network;
    }

    public boolean isSynced() {
        return Synced;
    }

    //Builder Class
    public static class Builder {

        private String Alias;
        private Version Version;
        private String FullVersionString;
        private String PubKey;
        private LightningNodeUri[] LightningNodeUris;
        private int BlockHeight;
        private BackendConfig.Network Network;
        private boolean Synced;

        private Builder() {
            // required parameters
        }

        public CurrentNodeInfo build() {
            return new CurrentNodeInfo(this);
        }

        public Builder setAlias(String alias) {
            this.Alias = alias;
            return this;
        }

        public Builder setVersion(Version version) {
            this.Version = version;
            return this;
        }

        public Builder setFullVersionString(String fullVersionString) {
            FullVersionString = fullVersionString;
            return this;
        }

        public Builder setPubKey(String pubKey) {
            this.PubKey = pubKey;
            return this;
        }

        public Builder setLightningNodeUris(LightningNodeUri[] lightningNodeUris) {
            this.LightningNodeUris = lightningNodeUris;
            return this;
        }

        public Builder setBlockHeight(int blockHeight) {
            this.BlockHeight = blockHeight;
            return this;
        }

        public Builder setNetwork(BackendConfig.Network network) {
            Network = network;
            return this;
        }

        public Builder setSynced(boolean synced) {
            Synced = synced;
            return this;
        }
    }
}