package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class Lease implements Serializable {

    private final Outpoint Outpoint;
    private final String Id;
    private final long Expiration;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Lease(Builder builder) {
        this.Outpoint = builder.Outpoint;
        this.Id = builder.Id;
        this.Expiration = builder.Expiration;
    }

    /**
     * The outpoint that is leased.
     */
    public Outpoint getOutpoint() {
        return Outpoint;
    }

    /**
     * The unique ID that was used to lock the output.
     */
    public String getId() {
        return Id;
    }

    /**
     * The absolute expiration of the output lease represented as a unix timestamp
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

        public Lease build() {
            return new Lease(this);
        }

        /**
         * The outpoint that is leased.
         */
        public Builder setOutpoint(Outpoint outpoint) {
            Outpoint = outpoint;
            return this;
        }

        /**
         * The unique ID that was used to lock the output.
         */
        public Builder setId(String id) {
            Id = id;
            return this;
        }

        /**
         * The absolute expiration of the output lease represented as a unix timestamp
         */
        public Builder setExpiration(long expiration) {
            Expiration = expiration;
            return this;
        }
    }
}