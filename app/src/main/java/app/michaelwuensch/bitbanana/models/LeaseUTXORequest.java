package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class LeaseUTXORequest implements Serializable {

    private final Outpoint Outpoint;
    private final String Id;
    private final long Expiration;

    public static Builder newBuilder() {
        return new Builder();
    }

    private LeaseUTXORequest(Builder builder) {
        this.Outpoint = builder.Outpoint;
        this.Id = builder.Id;
        this.Expiration = builder.Expiration;
    }

    /**
     * The outpoint that will be leased.
     */
    public Outpoint getOutpoint() {
        return Outpoint;
    }

    /**
     * An ID of 32 random bytes that must be unique for each distinct application using this RPC which will be used to bound the output lease to.
     * The bytes will be stored in HEX format here.
     */
    public String getId() {
        return Id;
    }

    /**
     * Expiration in seconds until the lease is automatically ended.
     */
    public long getExpiration() {
        return Expiration;
    }


    //Builder Class
    public static class Builder {

        private Outpoint Outpoint;
        private String Id;
        private long Expiration;


        private Builder() {
            // required parameters
        }

        public LeaseUTXORequest build() {
            return new LeaseUTXORequest(this);
        }

        /**
         * The outpoint that will be leased.
         */
        public Builder setOutpoint(Outpoint outpoint) {
            Outpoint = outpoint;
            return this;
        }

        /**
         * An ID of 32 random bytes that must be unique for each distinct application using this RPC which will be used to bound the output lease to.
         * The bytes will be stored in HEX format here.
         */
        public Builder setId(String id) {
            Id = id;
            return this;
        }

        /**
         * Expiration in seconds until the lease is automatically ended.
         */
        public Builder setExpiration(long expiration) {
            Expiration = expiration;
            return this;
        }
    }
}