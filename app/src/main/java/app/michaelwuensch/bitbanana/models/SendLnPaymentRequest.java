package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.List;

public class SendLnPaymentRequest implements Serializable {

    private final PaymentType PaymentType;
    private final DecodedBolt11 Bolt11;
    private final long Amount;
    private final long MaxFee;
    private final String DestinationPubKey;
    private final String Preimage;
    private final String PaymentHash;
    private final List<CustomRecord> CustomRecords;

    public static Builder newBuilder() {
        return new Builder();
    }

    private SendLnPaymentRequest(Builder builder) {
        this.PaymentType = builder.PaymentType;
        this.Bolt11 = builder.Bolt11;
        this.MaxFee = builder.MaxFee;
        this.Amount = builder.Amount;
        this.DestinationPubKey = builder.DestinationPubKey;
        this.Preimage = builder.Preimage;
        this.PaymentHash = builder.PaymentHash;
        this.CustomRecords = builder.CustomRecords;
    }

    public SendLnPaymentRequest.PaymentType getPaymentType() {
        return PaymentType;
    }

    public DecodedBolt11 getBolt11() {
        return Bolt11;
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

    public List<CustomRecord> getCustomRecords() {
        return CustomRecords;
    }


    //Builder Class
    public static class Builder {

        private PaymentType PaymentType;
        private DecodedBolt11 Bolt11;
        private long Amount;
        private long MaxFee;
        private String DestinationPubKey;
        private String Preimage;
        private String PaymentHash;
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

        public Builder setCustomRecords(List<CustomRecord> customRecords) {
            CustomRecords = customRecords;
            return this;
        }
    }

    public enum PaymentType {
        BOLT11_INVOICE,
        KEYSEND;
    }
}