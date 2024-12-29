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
    private final String Memo;
    private final boolean hasMemo;
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
        this.Memo = builder.Memo;
        this.hasMemo = builder.hasMemo;
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

    public String getMemo() {
        return Memo;
    }

    public boolean hasMemo() {
        return hasMemo;
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
        private String Memo;
        private boolean hasMemo;
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
            return this;
        }

        public Builder setMemo(String memo) {
            Memo = memo;
            hasMemo = memo != null && !memo.isEmpty();
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