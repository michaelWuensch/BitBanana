package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class LnInvoice implements Serializable {

    private final InvoiceType Type;
    private final String Bolt11;
    private final String Bolt12;
    private final String PaymentHash;
    private final long AmountRequested;
    private final long AmountPaid;
    private final long CreatedAt;
    private final long PaidAt;
    private final long ExpiresAt;
    private final long AddIndex;
    private final String Memo;
    private final boolean hasMemo;
    private final String Bolt12PayerNote;
    private final boolean hasBolt12PayerNote;
    private final String KeysendMessage;
    private final boolean hasKeysendMessage;

    public static Builder newBuilder() {
        return new Builder();
    }

    private LnInvoice(Builder builder) {
        this.Type = builder.Type;
        this.Bolt11 = builder.Bolt11;
        this.Bolt12 = builder.Bolt12;
        this.PaymentHash = builder.PaymentHash;
        this.AmountRequested = builder.AmountRequested;
        this.AmountPaid = builder.AmountPaid;
        this.CreatedAt = builder.CreatedAt;
        this.PaidAt = builder.PaidAt;
        this.ExpiresAt = builder.ExpiresAt;
        this.AddIndex = builder.AddIndex;
        this.Memo = builder.Memo;
        this.hasMemo = builder.hasMemo;
        this.Bolt12PayerNote = builder.Bolt12PayerNote;
        this.hasBolt12PayerNote = builder.hasBolt12PayerNote;
        this.KeysendMessage = builder.KeysendMessage;
        this.hasKeysendMessage = builder.hasKeysendMessage;
    }

    public InvoiceType getType() {
        return Type;
    }

    public String getBolt11() {
        return Bolt11;
    }

    public String getBolt12() {
        return Bolt12;
    }

    /**
     * The hash of the payment preimage which will prove payment.
     */
    public String getPaymentHash() {
        return PaymentHash;
    }

    /**
     * The requested amount in msat
     */
    public long getAmountRequested() {
        return AmountRequested;
    }

    /**
     * The paid amount in msat
     */
    public long getAmountPaid() {
        return AmountPaid;
    }

    /**
     * Whether or not this invoice has already been paid.
     */
    public boolean isPaid() {
        if (hasRequestAmountSpecified())
            return AmountPaid >= AmountRequested;
        else
            return AmountPaid > 0;
    }

    /**
     * Whether or not the invoice as already been expired.
     */
    public boolean isExpired() {
        return ExpiresAt < System.currentTimeMillis() / 1000;
    }

    public boolean hasRequestAmountSpecified() {
        return AmountRequested != 0;
    }

    /**
     * UNIX timestamp of when the invoice was created in seconds since the unix epoch.
     */
    public long getCreatedAt() {
        return CreatedAt;
    }

    /**
     * UNIX timestamp of when the invoice was paid in seconds since the unix epoch.
     */
    public long getPaidAt() {
        return PaidAt;
    }

    /**
     * UNIX timestamp of when it will become / became unpayable in seconds since the unix epoch.
     */
    public long getExpiresAt() {
        return ExpiresAt;
    }

    /**
     * The "add" index of this invoice. Each newly created invoice will increment this index making it monotonically increasing.
     */
    public long getAddIndex() {
        return AddIndex;
    }

    public String getMemo() {
        return Memo;
    }

    public boolean hasMemo() {
        return hasMemo;
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


    //Builder Class
    public static class Builder {

        private InvoiceType Type;
        private String Bolt11;
        private String Bolt12;
        private String PaymentHash;
        private long AmountRequested;
        private long AmountPaid;
        private long CreatedAt;
        private long PaidAt;
        private long ExpiresAt;
        private long AddIndex;
        private String Memo;
        private boolean hasMemo;
        private String Bolt12PayerNote;
        private boolean hasBolt12PayerNote;
        private String KeysendMessage;
        private boolean hasKeysendMessage;

        private Builder() {
            // required parameters
        }

        public LnInvoice build() {
            return new LnInvoice(this);
        }

        public Builder setType(InvoiceType type) {
            this.Type = type;
            return this;
        }

        public Builder setBolt11(String bolt11) {
            this.Bolt11 = bolt11;
            return this;
        }

        public Builder setBolt12(String bolt12) {
            this.Bolt12 = bolt12;
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
         * The requested amount in msat
         */
        public Builder setAmountRequested(long amountRequested) {
            this.AmountRequested = amountRequested;
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
         * UNIX timestamp of when the invoice was created in seconds since the unix epoch.
         */
        public Builder setCreatedAt(long createdAt) {
            CreatedAt = createdAt;
            return this;
        }

        /**
         * UNIX timestamp of when the invoice was paid in seconds since the unix epoch.
         */
        public Builder setPaidAt(long paidAt) {
            PaidAt = paidAt;
            return this;
        }

        /**
         * UNIX timestamp of when it will become / became unpayable in seconds since the unix epoch.
         */
        public Builder setExpiresAt(long expiresAt) {
            ExpiresAt = expiresAt;
            return this;
        }

        public Builder setAddIndex(long addIndex) {
            AddIndex = addIndex;
            return this;
        }

        public Builder setMemo(String memo) {
            Memo = memo;
            hasMemo = memo != null && !memo.isEmpty();
            return this;
        }

        public Builder setBolt12PayerNote(String note) {
            Bolt12PayerNote = note;
            hasBolt12PayerNote = note != null && !note.isEmpty();
            return this;
        }

        public Builder setKeysendMessage(String keysendMessage) {
            KeysendMessage = keysendMessage;
            hasKeysendMessage = keysendMessage != null && !keysendMessage.isEmpty();
            return this;
        }
    }

    public enum InvoiceType {
        BOLT11_INVOICE,
        BOLT12_INVOICE;
    }
}