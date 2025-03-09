package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.List;

import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;

public class SendLnPaymentRequest implements Serializable {

    private final PaymentType PaymentType;
    private final DecodedBolt11 Bolt11;
    private final String Bolt12InvoiceString;
    private final long Amount;
    private final long MaxFee;
    private final String DestinationPubKey;
    private final String Preimage;
    private final String PaymentHash;
    private final ShortChannelId FirstHop;
    private final boolean HasFirstHop;
    private final String LastHop;
    private final boolean HasLastHop;
    private final List<CustomRecord> CustomRecords;

    public static Builder newBuilder() {
        return new Builder();
    }

    private SendLnPaymentRequest(Builder builder) {
        this.PaymentType = builder.PaymentType;
        this.Bolt11 = builder.Bolt11;
        this.Bolt12InvoiceString = builder.Bolt12InvoiceString;
        this.MaxFee = builder.MaxFee;
        this.Amount = builder.Amount;
        this.DestinationPubKey = builder.DestinationPubKey;
        this.Preimage = builder.Preimage;
        this.PaymentHash = builder.PaymentHash;
        this.FirstHop = builder.FirstHop;
        this.HasFirstHop = builder.HasFirstHop;
        this.LastHop = builder.LastHop;
        this.HasLastHop = builder.HasLastHop;
        this.CustomRecords = builder.CustomRecords;
    }

    public SendLnPaymentRequest.PaymentType getPaymentType() {
        return PaymentType;
    }

    public DecodedBolt11 getBolt11() {
        return Bolt11;
    }

    /**
     * This is used for bolt 12 payments
     */
    public String getBolt12InvoiceString() {
        return Bolt12InvoiceString;
    }

    /**
     * Amount in msat.
     */
    public long getAmount() {
        return Amount;
    }

    /**
     * Max fee in msat to be paid.
     */
    public long getMaxFee() {
        return MaxFee;
    }

    public String getDestinationPubKey() {
        return DestinationPubKey;
    }

    public String getPreimage() {
        return Preimage;
    }

    public String getPaymentHash() {
        return PaymentHash;
    }

    /**
     * The short channel id of the first hop.
     */
    public ShortChannelId getFirstHop() {
        return FirstHop;
    }

    public boolean hasFirstHop() {
        return HasFirstHop;
    }

    /**
     * The pub key of the last hop.
     */
    public String getLastHop() {
        return LastHop;
    }

    public boolean hasLastHop() {
        return HasLastHop;
    }

    public List<CustomRecord> getCustomRecords() {
        return CustomRecords;
    }


    //Builder Class
    public static class Builder {

        private PaymentType PaymentType;
        private DecodedBolt11 Bolt11;
        private String Bolt12InvoiceString;
        private long Amount;
        private long MaxFee;
        private String DestinationPubKey;
        private String Preimage;
        private String PaymentHash;
        private ShortChannelId FirstHop;
        private boolean HasFirstHop;
        private String LastHop;
        private boolean HasLastHop;
        private List<CustomRecord> CustomRecords;

        private Builder() {
            // required parameters
        }

        public SendLnPaymentRequest build() {
            return new SendLnPaymentRequest(this);
        }

        public Builder setPaymentType(SendLnPaymentRequest.PaymentType paymentType) {
            PaymentType = paymentType;
            return this;
        }

        public Builder setBolt11(DecodedBolt11 bolt11) {
            this.Bolt11 = bolt11;
            return this;
        }

        /**
         * This is used for bolt 12 payments
         */
        public Builder setBolt12InvoiceString(String bolt12InvoiceString) {
            this.Bolt12InvoiceString = bolt12InvoiceString;
            return this;
        }

        /**
         * Amount in msat.
         */
        public Builder setAmount(long amount) {
            Amount = amount;
            return this;
        }

        /**
         * Max fee in msat to be paid.
         */
        public Builder setMaxFee(long maxFee) {
            MaxFee = maxFee;
            return this;
        }

        public Builder setDestinationPubKey(String destinationPubKey) {
            DestinationPubKey = destinationPubKey;
            return this;
        }

        public Builder setPreimage(String preimage) {
            Preimage = preimage;
            return this;
        }

        public Builder setPaymentHash(String paymentHash) {
            PaymentHash = paymentHash;
            return this;
        }

        public Builder setFirstHop(ShortChannelId firstHop) {
            FirstHop = firstHop;
            HasFirstHop = firstHop != null;
            return this;
        }

        public Builder setLastHop(String lastHopPubKey) {
            LastHop = lastHopPubKey;
            HasLastHop = lastHopPubKey != null;
            return this;
        }

        public Builder setCustomRecords(List<CustomRecord> customRecords) {
            CustomRecords = customRecords;
            return this;
        }
    }

    public enum PaymentType {
        BOLT11_INVOICE,
        BOLT12_INVOICE,
        KEYSEND;
    }
}