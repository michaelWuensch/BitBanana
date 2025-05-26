package app.michaelwuensch.bitbanana.models.Channels;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Used for channels selected in BitBanana. This will always be channels where your node is involved.
 */
public class SelectedChannel implements Serializable {

    private final ShortChannelId ShortChannelId;
    private final String RemotePubKey;


    public static Builder newBuilder() {
        return new Builder();
    }

    private SelectedChannel(Builder builder) {
        this.ShortChannelId = builder.ShortChannelId;
        this.RemotePubKey = builder.RemotePubKey;
    }

    public ShortChannelId getShortChannelId() {
        return ShortChannelId;
    }

    public String getRemotePubKey() {
        return RemotePubKey;
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        SelectedChannel that = (SelectedChannel) obj;
        return this.getShortChannelId().equals(that.getShortChannelId());
    }

    //Builder Class
    public static class Builder {
        private ShortChannelId ShortChannelId;
        private String RemotePubKey;

        private Builder() {
            // required parameters
        }

        public SelectedChannel build() {
            return new SelectedChannel(this);
        }

        public Builder setShortChannelId(ShortChannelId shortChannelId) {
            this.ShortChannelId = shortChannelId;
            return this;
        }

        public Builder setRemotePubKey(String remotePubKey) {
            this.RemotePubKey = remotePubKey;
            return this;
        }
    }
}