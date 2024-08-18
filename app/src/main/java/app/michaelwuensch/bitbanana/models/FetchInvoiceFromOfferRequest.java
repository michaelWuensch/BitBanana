package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class FetchInvoiceFromOfferRequest implements Serializable {

    private final DecodedBolt12 DecodedBolt12;
    private final long Amount;
    private final String Comment;

    public static Builder newBuilder() {
        return new Builder();
    }

    private FetchInvoiceFromOfferRequest(Builder builder) {
        this.DecodedBolt12 = builder.DecodedBolt12;
        this.Amount = builder.Amount;
        this.Comment = builder.Comment;
    }

    public DecodedBolt12 getDecodedBolt12() {
        return DecodedBolt12;
    }

    /**
     * Amount in msat.
     */
    public long getAmount() {
        return Amount;
    }

    public String getComment() {
        return Comment;
    }


    //Builder Class
    public static class Builder {

        private DecodedBolt12 DecodedBolt12;
        private long Amount;
        private String Comment;

        private Builder() {
            // required parameters
        }

        public FetchInvoiceFromOfferRequest build() {
            return new FetchInvoiceFromOfferRequest(this);
        }

        public Builder setDecodedBolt12(DecodedBolt12 decodedBolt12) {
            DecodedBolt12 = decodedBolt12;
            return this;
        }

        public Builder setComment(String comment) {
            Comment = comment;
            return this;
        }

        /**
         * Amount in msat.
         */
        public Builder setAmount(long amount) {
            Amount = amount;
            return this;
        }
    }
}