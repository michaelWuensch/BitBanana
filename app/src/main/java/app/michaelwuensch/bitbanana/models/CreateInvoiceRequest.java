package app.michaelwuensch.bitbanana.models;

import com.github.ElementsProject.lightning.cln.AmountOrAny;

import java.io.Serializable;

public class CreateInvoiceRequest implements Serializable {

    private final long Amount;
    private final long Expiry;
    private final String Description;
    private final boolean IncludeRouteHints;


    public static Builder newBuilder() {
        return new Builder();
    }

    private CreateInvoiceRequest(Builder builder) {
        this.Amount = builder.Amount;
        this.Expiry = builder.Expiry;
        this.Description = builder.Description;
        this.IncludeRouteHints = builder.IncludeRouteHints;
    }


    /**
     * The requested amount in msat
     */
    public long getAmount() {
        return Amount;
    }

    /**
     * The time the invoice is valid for, in seconds.
     */
    public long getExpiry() {
        return Expiry;
    }

    public String getDescription() {
        return Description;
    }

    public boolean getIncludeRouteHints() {
        return IncludeRouteHints;
    }


    //Builder Class
    public static class Builder {
        private long Amount;
        private long Expiry;
        private String Description;
        private boolean IncludeRouteHints;


        private Builder() {
            // required parameters
        }

        public CreateInvoiceRequest build() {
            return new CreateInvoiceRequest(this);
        }


        /**
         * The requested amount in msat
         */
        public Builder setAmount(long amount) {
            Amount = amount;
            return this;
        }

        /**
         * The time the invoice is valid for, in seconds.
         */
        public Builder setExpiry(long expiry) {
            Expiry = expiry;
            return this;
        }

        public Builder setDescription(String description) {
            Description = description;
            return this;
        }

        public Builder setIncludeRouteHints(boolean includeRouteHints) {
            IncludeRouteHints = includeRouteHints;
            return this;
        }
    }
}