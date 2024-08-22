package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class DecodedBolt12InvoiceRequest implements Serializable {

    private final String Bolt12String;
    private final String OfferId;
    private final long Amount;
    private final String Description;
    private final String Issuer;
    private final long ExpiresAt;

    public static Builder newBuilder() {
        return new Builder();
    }

    private DecodedBolt12InvoiceRequest(Builder builder) {
        this.Bolt12String = builder.Bolt12String;
        this.OfferId = builder.OfferId;
        this.Amount = builder.Amount;
        this.Description = builder.Description;
        this.Issuer = builder.Issuer;
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

    public String getIssuer() {
        return Issuer;
    }

    /**
     * UNIX timestamp of when the offer will expire in seconds since the unix epoch.
     */
    public long getExpiresAt() {
        return ExpiresAt;
    }

    /**
     * Whether or not the bolt 12 offer has already been expired.
     */
    public boolean isExpired() {
        if (getExpiresAt() == 0)
            return false;
        return getExpiresAt() < System.currentTimeMillis() / 1000;
    }

    public boolean hasAmountSpecified() {
        return Amount != 0;
    }

    public boolean hasNoAmountSpecified() {
        return !hasAmountSpecified();
    }


    //Builder Class
    public static class Builder {

        private String Bolt12String;
        private String OfferId;
        private long Amount;
        private String Description;
        private String Issuer;
        private long ExpiresAt;

        private Builder() {
            // required parameters
        }

        public DecodedBolt12InvoiceRequest build() {
            return new DecodedBolt12InvoiceRequest(this);
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

        public Builder setIssuer(String issuer) {
            Issuer = issuer;
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