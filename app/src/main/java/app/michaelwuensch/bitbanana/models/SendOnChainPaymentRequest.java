package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class SendOnChainPaymentRequest implements Serializable {

    private final String Address;
    private final long Amount;
    private final long SatPerVByte;
    private final boolean SendAll;

    public static Builder newBuilder() {
        return new Builder();
    }

    private SendOnChainPaymentRequest(Builder builder) {
        this.SatPerVByte = builder.SatPerVByte;
        this.Amount = builder.Amount;
        this.Address = builder.Address;
        this.SendAll = builder.SendAll;
    }

    public String getAddress() {
        return Address;
    }

    /**
     * Amount in msat.
     */
    public long getAmount() {
        return Amount;
    }

    /**
     * Number of Blocks the payment should confirm in.
     */
    public long getSatPerVByte() {
        return SatPerVByte;
    }

    /**
     * If set to true all available funds will be sent ignoring the set amount. This is basically sweeping.
     */
    public boolean isSendAll() {
        return SendAll;
    }


    //Builder Class
    public static class Builder {

        private String Address;
        private long Amount;
        private long SatPerVByte;
        private boolean SendAll;


        private Builder() {
            // required parameters
        }

        public SendOnChainPaymentRequest build() {
            return new SendOnChainPaymentRequest(this);
        }

        public Builder setAddress(String address) {
            Address = address;
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
         * Number of Blocks the payment should confirm in.
         */
        public Builder setSatPerVByte(long satPerVByte) {
            SatPerVByte = satPerVByte;
            return this;
        }

        /**
         * If set to true all available funds will be sent ignoring the set amount. This is basically sweeping.
         */
        public Builder setSendAll(boolean sendAll) {
            SendAll = sendAll;
            return this;
        }
    }
}