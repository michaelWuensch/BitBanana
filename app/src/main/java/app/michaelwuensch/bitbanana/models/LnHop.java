package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;

public class LnHop implements Serializable {

    private final int IdInRoute;
    private final ShortChannelId ShortChannelId;
    private final String PubKey;
    private final long Amount;
    private final long Fee;
    private final boolean BlindingEntryHop;
    private final boolean IsLastHop;

    public static Builder newBuilder() {
        return new Builder();
    }

    private LnHop(Builder builder) {
        this.IdInRoute = builder.IdInRoute;
        this.ShortChannelId = builder.ShortChannelId;
        this.PubKey = builder.PubKey;
        this.Amount = builder.Amount;
        this.Fee = builder.Fee;
        this.BlindingEntryHop = builder.BlindingEntryHop;
        this.IsLastHop = builder.IsLastHop;
    }

    public int getIdInRoute() {
        return IdInRoute;
    }

    public ShortChannelId getShortChannelId() {
        return ShortChannelId;
    }

    public String getPubKey() {
        return PubKey;
    }

    /**
     * Amount in msat
     */
    public long getAmount() {
        return Amount;
    }

    /**
     * Fee in msats
     */
    public long getFee() {
        return Fee;
    }

    public boolean getIsBlindingEntryHop() {
        return BlindingEntryHop;
    }

    public boolean getIsLastHop() {
        return IsLastHop;
    }


    //Builder Class
    public static class Builder {

        private int IdInRoute;
        private ShortChannelId ShortChannelId;
        private String PubKey;
        private long Amount;
        private long Fee;
        private boolean BlindingEntryHop;
        private boolean IsLastHop;

        private Builder() {
            // required parameters
        }

        public LnHop build() {
            return new LnHop(this);
        }

        public Builder setIdInRoute(int id) {
            this.IdInRoute = id;
            return this;
        }

        public Builder setShortChannelId(ShortChannelId shortChannelId) {
            this.ShortChannelId = shortChannelId;
            return this;
        }

        public Builder setPubKey(String pubKey) {
            this.PubKey = pubKey;
            return this;
        }

        /**
         * Amount in msat
         */
        public Builder setAmount(long amount) {
            this.Amount = amount;
            return this;
        }

        /**
         * Fee in msats
         */
        public Builder setFee(long fee) {
            this.Fee = fee;
            return this;
        }

        public Builder setBlindingEntryHop(boolean isBlindingEntryHop) {
            this.BlindingEntryHop = isBlindingEntryHop;
            return this;
        }

        public Builder setIsLastHop(boolean isLastHop) {
            this.IsLastHop = isLastHop;
            return this;
        }
    }
}