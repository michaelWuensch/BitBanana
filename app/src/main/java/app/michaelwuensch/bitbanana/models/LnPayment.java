package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.List;

public class LnPayment implements Serializable {

    private final String PaymentHash;
    private final String PaymentPreimage;
    private final String DestinationPubKey;
    private final boolean hasDestinationPubKey;
    private final Status Status;
    private final long AmountPaid;
    private final long Fee;
    private final long CreatedAt;
    private final String Bolt11;
    private final boolean hasBolt11;
    private final String Bolt12;
    private final boolean hasBolt12;
    private final String Description;
    private final boolean hasDescription;
    private final String Bolt12PayerNote;
    private final boolean hasBolt12PayerNote;
    private final String KeysendMessage;
    private final boolean hasKeysendMessage;
    private final List<LnRoute> Routes;
    private final boolean hasRoutes;

    public static Builder newBuilder() {
        return new Builder();
    }

    private LnPayment(Builder builder) {
        this.PaymentHash = builder.PaymentHash;
        this.PaymentPreimage = builder.PaymentPreimage;
        this.DestinationPubKey = builder.DestinationPubKey;
        this.hasDestinationPubKey = builder.hasDestinationPubKey;
        this.Status = builder.Status;
        this.AmountPaid = builder.AmountPaid;
        this.Fee = builder.Fee;
        this.CreatedAt = builder.CreatedAt;
        this.Bolt11 = builder.Bolt11;
        this.hasBolt11 = builder.hasBolt11;
        this.Bolt12 = builder.Bolt12;
        this.hasBolt12 = builder.hasBolt12;
        this.Description = builder.Description;
        this.hasDescription = builder.hasDescription;
        this.Bolt12PayerNote = builder.Bolt12PayerNote;
        this.hasBolt12PayerNote = builder.hasBolt12PayerNote;
        this.KeysendMessage = builder.KeysendMessage;
        this.hasKeysendMessage = builder.hasKeysendMessage;
        this.Routes = builder.Routes;
        this.hasRoutes = builder.hasRoutes;
    }

    public String getPaymentHash() {
        return PaymentHash;
    }

    public String getPaymentPreimage() {
        return PaymentPreimage;
    }

    public String getDestinationPubKey() {
        return DestinationPubKey;
    }

    public boolean hasDestinationPubKey() {
        return hasDestinationPubKey;
    }

    public LnPayment.Status getStatus() {
        return Status;
    }

    /**
     * The paid amount in msat
     */
    public long getAmountPaid() {
        return AmountPaid;
    }

    /**
     * The paid fee in msat
     */
    public long getFee() {
        return Fee;
    }

    /**
     * UNIX timestamp of when the payment was created in seconds since the unix epoch.
     */
    public long getCreatedAt() {
        return CreatedAt;
    }

    public String getBolt11() {
        return Bolt11;
    }

    public boolean hasBolt11() {
        return hasBolt11;
    }

    public String getBolt12() {
        return Bolt12;
    }

    public boolean hasBolt12() {
        return hasBolt12;
    }

    public String getDescription() {
        return Description;
    }

    public boolean hasDescription() {
        return hasDescription;
    }

    public String getBolt12PayerNote() {
        return Bolt12PayerNote;
    }

    public boolean hasBolt12PayerNote() {
        return hasBolt12PayerNote;
    }

    public String getKeysendMessage() {
        return KeysendMessage;
    }

    public boolean hasKeysendMessage() {
        return hasKeysendMessage;
    }

    public List<LnRoute> getRoutes() {
        return Routes;
    }

    public boolean hasRoutes() {
        return hasRoutes;
    }


    //Builder Class
    public static class Builder {

        private String PaymentHash;
        private String PaymentPreimage;
        private String DestinationPubKey;
        private boolean hasDestinationPubKey;
        private Status Status;
        private long AmountPaid;
        private long Fee;
        private long CreatedAt;
        private String Bolt11;
        private boolean hasBolt11;
        private String Bolt12;
        private boolean hasBolt12;
        private String Description;
        private boolean hasDescription;
        private String Bolt12PayerNote;
        private boolean hasBolt12PayerNote;
        private String KeysendMessage;
        private boolean hasKeysendMessage;
        private List<LnRoute> Routes;
        private boolean hasRoutes;

        private Builder() {
            // required parameters
        }

        public LnPayment build() {
            return new LnPayment(this);
        }

        public Builder setPaymentHash(String paymentHash) {
            PaymentHash = paymentHash;
            return this;
        }

        public Builder setPaymentPreimage(String paymentPreimage) {
            PaymentPreimage = paymentPreimage;
            return this;
        }

        public Builder setDestinationPubKey(String destinationPubKey) {
            this.DestinationPubKey = destinationPubKey;
            hasDestinationPubKey = destinationPubKey != null && !destinationPubKey.isEmpty();
            return this;
        }

        public Builder setStatus(LnPayment.Status status) {
            Status = status;
            return this;
        }

        /**
         * The paid amount in msat
         */
        public Builder setAmountPaid(long amountPaid) {
            AmountPaid = amountPaid;
            return this;
        }

        /**
         * The paid fee in msat
         */
        public Builder setFee(long fee) {
            Fee = fee;
            return this;
        }

        /**
         * UNIX timestamp of when the payment was created in seconds since the unix epoch.
         */
        public Builder setCreatedAt(long createdAt) {
            CreatedAt = createdAt;
            return this;
        }

        public Builder setBolt11(String bolt11) {
            Bolt11 = bolt11;
            hasBolt11 = bolt11 != null && !bolt11.isEmpty();
            return this;
        }

        public Builder setBolt12(String bolt12) {
            Bolt12 = bolt12;
            hasBolt12 = bolt12 != null && !bolt12.isEmpty();
            return this;
        }

        public Builder setDescription(String description) {
            Description = description;
            hasDescription = description != null && !description.isEmpty();
            return this;
        }

        public Builder setBolt12PayerNote(String bolt12PayerNote) {
            Bolt12PayerNote = bolt12PayerNote;
            hasBolt12PayerNote = bolt12PayerNote != null && !bolt12PayerNote.isEmpty();
            return this;
        }

        public Builder setKeysendMessage(String keysendMessage) {
            KeysendMessage = keysendMessage;
            hasKeysendMessage = keysendMessage != null && !keysendMessage.isEmpty();
            return this;
        }

        public Builder setRoutes(List<LnRoute> routes) {
            Routes = routes;
            hasRoutes = true;
            return this;
        }
    }

    public enum Status {
        PENDING,
        SUCCEEDED,
        FAILED;
    }
}