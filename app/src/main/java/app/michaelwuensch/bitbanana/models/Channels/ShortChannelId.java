package app.michaelwuensch.bitbanana.models.Channels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * ShortChannelID (SCID)
 * See: https://github.com/lightning/bolts/blob/master/07-routing-gossip.md#definition-of-short_channel_id
 */
public class ShortChannelId implements Serializable {

    private final int BlockHeight;
    private final int Index;
    private final int OutputIndex;

    public static Builder newBuilder() {
        return new Builder();
    }

    private ShortChannelId(Builder builder) {
        this.BlockHeight = builder.BlockHeight;
        this.Index = builder.Index;
        this.OutputIndex = builder.OutputIndex;
    }

    /**
     * The block height of the funding transaction
     */
    public int getBlockHeight() {
        return BlockHeight;
    }

    /**
     * The transaction index of the funding transaction within the block
     */
    public int getIndex() {
        return Index;
    }

    /**
     * The output index of the funding transaction that pays to the channel.
     */
    public int getOutputIndex() {
        return OutputIndex;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ShortChannelId that = (ShortChannelId) obj;
        return this.toString().equals(that.toString());
    }

    @NonNull
    @Override
    public String toString() {
        return BlockHeight + "x" + Index + "x" + OutputIndex;
    }

    //Builder Class
    public static class Builder {
        private int BlockHeight;
        private int Index;
        private int OutputIndex;

        private Builder() {
            // required parameters
        }

        public ShortChannelId build() {
            return new ShortChannelId(this);
        }

        /**
         * The block height of the funding transaction
         */
        public Builder setBlockHeight(int blockHeight) {
            this.BlockHeight = blockHeight;
            return this;
        }

        /**
         * The transaction index of the funding transaction within the block
         */
        public Builder setIndex(int index) {
            this.Index = index;
            return this;
        }

        /**
         * The output index of the funding transaction that pays to the channel.
         */
        public Builder setOutputIndex(int outputIndex) {
            this.OutputIndex = outputIndex;
            return this;
        }
    }
}