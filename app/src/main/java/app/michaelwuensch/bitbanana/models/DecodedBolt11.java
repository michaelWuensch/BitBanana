package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class DecodedBolt11 implements Serializable {

    private final String Bolt11String;
    private final String PaymentHash;
    private final String PaymentSecret;
    private final String DestinationPubKey;
    private final long AmountRequested;
    private final long Timestamp;
    private final long Expiry;
    private final String Description;
    private final boolean hasDescription;
    private final String DescriptionHash;

    public static Builder newBuilder() {
        return new Builder();
    }

    private DecodedBolt11(Builder builder) {
        this.Bolt11String = builder.Bolt11String;
        this.PaymentHash = builder.PaymentHash;
        this.PaymentSecret = builder.PaymentSecret;
        this.DestinationPubKey = builder.DestinationPubKey;
        this.AmountRequested = builder.AmountRequested;
        this.Timestamp = builder.Timestamp;
        this.Expiry = builder.Expiry;
        this.Description = builder.Description;
        this.hasDescription = builder.hasDescription;
        this.DescriptionHash = builder.DescriptionHash;
    }

    public String getBolt11String() {
        return Bolt11String;
    }

    /**
     * The hash of the payment preimage which will prove payment.
     */
    public String getPaymentHash() {
        return PaymentHash;
    }

    /**
     * Also sometimes called PaymentAddress
     */
    public String getPaymentSecret() {
        return PaymentSecret;
    }

    public String getDestinationPubKey() {
        return DestinationPubKey;
    }

    /**
     * The requested amount in msat
     */
    public long getAmountRequested() {
        return AmountRequested;
    }

    /**
     * Whether or not the invoice as already been expired.
     */
    public boolean isExpired() {
        return getExpiresAt() < System.currentTimeMillis() / 1000;
    }

    public boolean hasAmountSpecified() {
        return AmountRequested != 0;
    }

    public boolean hasNoAmountSpecified() {
        return !hasAmountSpecified();
    }

    /**
     * UNIX timestamp of when the invoice was created in seconds since the unix epoch.
     */
    public long getTimestamp() {
        return Timestamp;
    }

    /**
     * Duration in seconds this invoice is valid for.
     */
    public long getExpiry() {
        return Expiry;
    }

    /**
     * UNIX timestamp of when it will become / became unpayable in seconds since the unix epoch.
     */
    public long getExpiresAt() {
        return Timestamp + Expiry;
    }

    public String getDescription() {
        return Description;
    }

    public boolean hasDescription() {
        return hasDescription;
    }

    public boolean hasNoDescription() {
        return !hasDescription;
    }

    public String getDescriptionHash() {
        return DescriptionHash;
    }


    //Builder Class
    public static class Builder {

        private String Bolt11String;
        private String PaymentHash;
        private String PaymentSecret;
        private String DestinationPubKey;
        private long AmountRequested;
        private long Timestamp;
        private long Expiry;
        private String Description;
        private boolean hasDescription;
        private String DescriptionHash;

        private Builder() {
            // required parameters
        }

        public DecodedBolt11 build() {
            return new DecodedBolt11(this);
        }

        public Builder setBolt11String(String bolt11String) {
            this.Bolt11String = bolt11String;
            return this;
        }

        /**
         * The hash of the payment preimage which will prove payment.
         */
        public Builder setPaymentHash(String paymentHash) {
            PaymentHash = paymentHash;
            return this;
        }

        /**
         * Also sometimes called PaymentAddress
         */
        public Builder setPaymentSecret(String paymentSecret) {
            PaymentSecret = paymentSecret;
            return this;
        }

        public Builder setDestinationPubKey(String destinationPubKey) {
            DestinationPubKey = destinationPubKey;
            return this;
        }

        /**
         * The requested amount in msat
         */
        public Builder setAmountRequested(long amountRequested) {
            this.AmountRequested = amountRequested;
            return this;
        }

        /**
         * UNIX timestamp of when the invoice was created in seconds since the unix epoch.
         */
        public Builder setTimestamp(long timestamp) {
            Timestamp = timestamp;
            return this;
        }

        /**
         * Duration in seconds this invoice is valid for.
         */
        public Builder setExpiry(long expiry) {
            Expiry = expiry;
            return this;
        }

        public Builder setDescription(String description) {
            Description = description;
            hasDescription = description != null && !description.isEmpty();
            return this;
        }

        public Builder setDescriptionHash(String descriptionHash) {
            DescriptionHash = descriptionHash;
            return this;
        }
    }
}