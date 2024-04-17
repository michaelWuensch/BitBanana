package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class LightningNodeUri implements Serializable {

    private final String mPubKey;
    private final String mHost;
    private final boolean hasHost;
    private final int mPort;
    private final boolean hasPort;


    private LightningNodeUri(Builder builder) {
        mPubKey = builder.mPubKey;
        mHost = builder.mHost;
        hasHost = builder.hasHost;
        mPort = builder.mPort;
        hasPort = builder.hasPort;
    }

    @NonNull
    public String getPubKey() {
        return mPubKey;
    }

    public String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public String getAsString() {
        String uri = mPubKey;
        if (hasHost()) {
            uri = uri + "@" + mHost;
            if (hasPort())
                uri = uri + ":" + mPort;
        }
        return uri;
    }

    public boolean isTorUri() {
        if (getHost() == null) {
            return false;
        }
        return getHost().toLowerCase().contains(".onion");
    }

    public boolean hasHost() {
        return hasHost;
    }

    public boolean hasPort() {
        return hasPort;
    }

    public static class Builder {
        private String mPubKey;
        private String mHost;
        private boolean hasHost;
        private int mPort;
        private boolean hasPort;

        public Builder setPubKey(@NonNull String pubKey) {
            this.mPubKey = pubKey;
            return this;
        }

        public Builder setHost(String host) {
            this.mHost = host;
            this.hasHost = true;
            return this;
        }

        public Builder setPort(int port) {
            this.mPort = port;
            this.hasPort = true;
            return this;
        }

        public LightningNodeUri build() {
            return new LightningNodeUri(this);
        }
    }
}
