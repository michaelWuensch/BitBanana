package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class DecodedBolt12 implements Serializable {

    private final String Bolt12String;
    private final String OfferId;
    private final long Amount;
    private final String Description;
    private final long ExpiresAt;

    public static Builder newBuilder() {
        return new Builder();
    }

    private DecodedBolt12(Builder builder) {
        this.Bolt12String = builder.Bolt12String;
        this.OfferId = builder.OfferId;
        this.Amount = builder.Amount;
        this.Description = builder.Description;
        this.ExpiresAt = builder.ExpiresAt;
    }

    public String getBolt12String() {
        return Bolt12String;
    }

    public String getOfferId() {
        return OfferId;
    }

    public long getAmount() {
        return Amount;
    }

    public String getDescription() {
        return Description;
    }


    /**
     * UNIX timestamp of when the offer will expire in seconds since the unix epoch.
     */
    public long getExpiresAt() {
        return ExpiresAt;
    }


    //Builder Class
    public static class Builder {

        private String Bolt12String;
        private String OfferId;
        private long Amount;
        private String Description;
        private long ExpiresAt;

        private Builder() {
            // required parameters
        }

        public DecodedBolt12 build() {
            return new DecodedBolt12(this);
        }

        public Builder setBolt12String(String bolt12String) {
            this.Bolt12String = bolt12String;
            return this;
        }

        public Builder setOfferId(String offerId) {
            OfferId = offerId;
            return this;
        }

        /**
         * Amount in msat. If this is 0 no specific amount is requested.
         */
        public Builder setAmount(long amount) {
            Amount = amount;
            return this;
        }

        public Builder setDescription(String description) {
            Description = description;
            return this;
        }

        /**
         * UNIX timestamp of when the offer will expire in seconds since the unix epoch.
         */
        public Builder setExpiresAt(long expiresAt) {
            ExpiresAt = expiresAt;
            return this;
        }
    }
}