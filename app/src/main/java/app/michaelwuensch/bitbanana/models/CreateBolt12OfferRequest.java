package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class CreateBolt12OfferRequest implements Serializable {

    private final long Amount;
    private final String Description;
    private final String InternalLabel;
    private final boolean SingleUse;


    public static Builder newBuilder() {
        return new Builder();
    }

    private CreateBolt12OfferRequest(Builder builder) {
        this.Amount = builder.Amount;
        this.Description = builder.Description;
        this.InternalLabel = builder.InternalLabel;
        this.SingleUse = builder.SingleUse;
    }


    /**
     * The requested amount in msat
     */
    public long getAmount() {
        return Amount;
    }

    public String getDescription() {
        return Description;
    }

    public String getInternalLabel() {
        return InternalLabel;
    }

    public boolean getSingleUse() {
        return SingleUse;
    }


    //Builder Class
    public static class Builder {
        private long Amount;
        private String Description;
        private String InternalLabel;
        private boolean SingleUse;


        private Builder() {
            // required parameters
        }

        public CreateBolt12OfferRequest build() {
            return new CreateBolt12OfferRequest(this);
        }


        /**
         * The requested amount in msat
         */
        public Builder setAmount(long amount) {
            Amount = amount;
            return this;
        }

        public Builder setDescription(String description) {
            Description = description;
            return this;
        }

        public Builder setInternalLabel(String internalLabel) {
            InternalLabel = internalLabel;
            return this;
        }

        public Builder setSingleUse(boolean singleUse) {
            SingleUse = singleUse;
            return this;
        }
    }
}