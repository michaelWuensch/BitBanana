package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class CreateInvoiceResponse implements Serializable {

    private final String Bolt11;
    private final long AddIndex;

    public static Builder newBuilder() {
        return new Builder();
    }

    private CreateInvoiceResponse(Builder builder) {
        this.Bolt11 = builder.Bolt11;
        this.AddIndex = builder.AddIndex;
    }

    public String getBolt11() {
        return Bolt11;
    }


    /**
     * The "add" index of this invoice. Each newly created invoice will increment this index making it monotonically increasing.
     */
    public long getAddIndex() {
        return AddIndex;
    }


    //Builder Class
    public static class Builder {

        private String Bolt11;
        private long AddIndex;

        private Builder() {
            // required parameters
        }

        public CreateInvoiceResponse build() {
            return new CreateInvoiceResponse(this);
        }

        public Builder setBolt11(String bolt11) {
            this.Bolt11 = bolt11;
            return this;
        }

        public Builder setAddIndex(long addIndex) {
            AddIndex = addIndex;
            return this;
        }
    }
}