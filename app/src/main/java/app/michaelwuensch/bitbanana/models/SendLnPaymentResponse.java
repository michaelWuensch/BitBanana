package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class SendLnPaymentResponse implements Serializable {

    private final FailureReason FailureReason;
    private final String PaymentPreimage;
    private final long Amount;
    private final long Fee;

    public static Builder newBuilder() {
        return new Builder();
    }

    private SendLnPaymentResponse(Builder builder) {
        this.FailureReason = builder.FailureReason;
        this.PaymentPreimage = builder.PaymentPreimage;
        this.Amount = builder.Amount;
        this.Fee = builder.Fee;
    }

    public FailureReason getFailureReason() {
        return FailureReason;
    }

    public boolean didSucceed() {
        return FailureReason == null;
    }

    public String getPaymentPreimage() {
        return PaymentPreimage;
    }

    public long getAmount() {
        return Amount;
    }

    public long getFee() {
        return Fee;
    }


    //Builder Class
    public static class Builder {

        private FailureReason FailureReason;
        private String PaymentPreimage;
        private long Amount;
        private long Fee;


        private Builder() {
            // required parameters
        }

        public SendLnPaymentResponse build() {
            return new SendLnPaymentResponse(this);
        }

        public Builder setFailureReason(FailureReason failureReason) {
            FailureReason = failureReason;
            return this;
        }

        public Builder setPaymentPreimage(String paymentPreimage) {
            PaymentPreimage = paymentPreimage;
            return this;
        }

        public Builder setAmount(long amount) {
            Amount = amount;
            return this;
        }

        public Builder setFee(long fee) {
            Fee = fee;
            return this;
        }
    }

    public enum FailureReason {
        TIMEOUT,
        NO_ROUTE,
        INSUFFICIENT_FUNDS,
        INCORRECT_PAYMENT_DETAILS,
        CANCELED,
        UNKNOWN;
    }
}