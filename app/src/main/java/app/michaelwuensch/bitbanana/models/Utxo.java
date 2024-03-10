package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class Utxo implements Serializable {

    private final String Address;
    private final long AmountMsat;
    private final long BlockHeight;
    private final long Confirmations;
    private final String TransactionId;
    private final int OutputIndex;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Utxo(Builder builder) {
        this.Address = builder.Address;
        this.AmountMsat = builder.AmountMsat;
        this.TransactionId = builder.TransactionID;
        this.OutputIndex = builder.OutputIndex;
        this.BlockHeight = builder.BlockHeight;
        this.Confirmations = builder.Confirmations;
    }

    public String getAddress() {
        return Address;
    }

    public long getAmountMsat() {
        return AmountMsat;
    }

    public long getBlockHeight() {
        return BlockHeight;
    }

    public String getTransactionId() {
        return TransactionId;
    }


    public int getOutputIndex() {
        return OutputIndex;
    }

    public long getConfirmations() {
        return Confirmations;
    }


    //Builder Class
    public static class Builder {

        private String Address;
        private long AmountMsat;
        private long BlockHeight;
        private long Confirmations;
        private String TransactionID;
        private int OutputIndex;

        private Builder() {
            // required parameters
        }

        public Utxo build() {
            return new Utxo(this);
        }

        public Builder setAddress(String address) {
            this.Address = address;
            return this;
        }

        public Builder setAmountMsat(long amountMsat) {
            this.AmountMsat = amountMsat;
            return this;
        }

        public Builder setBlockHeight(long blockHeight) {
            BlockHeight = blockHeight;
            return this;
        }

        public Builder setConfirmations(long confirmations) {
            Confirmations = confirmations;
            return this;
        }

        public Builder setTransactionID(String transactionID) {
            this.TransactionID = transactionID;
            return this;
        }

        public Builder setOutputIndex(int outputIndex) {
            this.OutputIndex = outputIndex;
            return this;
        }
    }
}