package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.List;

public class OnChainTransaction implements Serializable {

    private final String TransactionId;
    private final long Amount;
    private final int BlockHeight;
    private final int Confirmations;
    private final long Fee;
    private final long TimeStamp;
    private final String Label;
    private final boolean hasLabel;
    private final List<Outpoint> Inputs;

    public static Builder newBuilder() {
        return new Builder();
    }

    private OnChainTransaction(Builder builder) {
        this.TransactionId = builder.TransactionId;
        this.Amount = builder.Amount;
        this.BlockHeight = builder.BlockHeight;
        this.Confirmations = builder.Confirmations;
        this.Fee = builder.Fee;
        this.TimeStamp = builder.TimeStamp;
        this.Label = builder.Label;
        this.hasLabel = builder.hasLabel;
        this.Inputs = builder.Inputs;
    }

    public String getTransactionId() {
        return TransactionId;
    }

    /**
     * The amount in msat
     */
    public long getAmount() {
        return Amount;
    }

    /**
     * Whether or not the invoice as already been expired.
     */
    public boolean isConfirmed() {
        return getBlockHeight() != 0;
    }

    public boolean hasRequestAmountSpecified() {
        return Amount != 0;
    }

    /**
     * The height of the block this transaction was included in. 0 if unconfirmed.
     */
    public int getBlockHeight() {
        return BlockHeight;
    }

    public int getConfirmations() {
        return Confirmations;
    }

    /**
     * Transaction miner fee in msat
     */
    public long getFee() {
        return Fee;
    }

    /**
     * UNIX timestamp of when the Transaction was created or confirmed in seconds since the unix epoch.??? ToDo: check what it actually is.
     */
    public long getTimeStamp() {
        return TimeStamp;
    }

    public String getLabel() {
        return Label;
    }

    public boolean hasLabel() {
        return hasLabel;
    }

    public List<Outpoint> getInputs() {
        return Inputs;
    }


    //Builder Class
    public static class Builder {

        private String TransactionId;
        private long Amount;
        private int BlockHeight;
        private int Confirmations;
        private long Fee;
        private long TimeStamp;
        private String Label;
        private boolean hasLabel;
        private List<Outpoint> Inputs;

        private Builder() {
            // required parameters
        }

        public OnChainTransaction build() {
            return new OnChainTransaction(this);
        }

        public Builder setTransactionId(String transactionId) {
            this.TransactionId = transactionId;
            return this;
        }

        /**
         * The amount in msat
         */
        public Builder setAmount(long amount) {
            this.Amount = amount;
            return this;
        }

        /**
         * The height of the block this transaction was included in. 0 if unconfirmed.
         */
        public Builder setBlockHeight(int blockHeight) {
            BlockHeight = blockHeight;
            return this;
        }

        public Builder setConfirmations(int confirmations) {
            Confirmations = confirmations;
            return this;
        }

        /**
         * Transaction miner fee in msat
         */
        public Builder setFee(long fee) {
            Fee = fee;
            return this;
        }

        /**
         * UNIX timestamp of when the Transaction was created or confirmed in seconds since the unix epoch.??? ToDo: check what it actually is.
         */
        public Builder setTimeStamp(long timeStamp) {
            TimeStamp = timeStamp;
            return this;
        }

        public Builder setLabel(String label) {
            Label = label;
            hasLabel = label != null && !label.isEmpty();
            return this;
        }

        public Builder setInputs(List<Outpoint> inputs) {
            Inputs = inputs;
            return this;
        }
    }
}