package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class LightningNodeUri implements Serializable {

    private final String mPubKey;
    private final String mHost;
    private final int mPort;


    private LightningNodeUri(@NonNull String pubKey, String host, int port) {
        mPubKey = pubKey;
        mHost = host;
        mPort = port;
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
        if (mHost != null)
            uri = uri + "@" + mHost;
        if (mPort != 0)
            uri = uri + ":" + mPort;
        return uri;
    }

    public boolean isTorUri() {
        if (getHost() == null) {
            return false;
        }
        return getHost().toLowerCase().contains(".onion");
    }

    public static class Builder {
        private String mPubKey;
        private String mHost;
        private int mPort;

        public Builder setPubKey(@NonNull String pubKey) {
            this.mPubKey = pubKey;

            return this;
        }

        public Builder setHost(String host) {
            this.mHost = host;

            return this;
        }

        public Builder setPort(int port) {
            this.mPort = port;

            return this;
        }

        public LightningNodeUri build() {
            return new LightningNodeUri(mPubKey, mHost, mPort);
        }
    }
}
