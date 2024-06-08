package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.List;

import app.michaelwuensch.bitbanana.util.WalletUtil;

public class OnChainTransaction implements Serializable {

    private final String TransactionId;
    private final long Amount;
    private int BlockHeight;
    private long Fee;
    private final long TimeStamp;
    private final String Label;
    private final boolean hasLabel;
    private final List<Outpoint> Inputs;
    private final TransactionType Type;


    public static Builder newBuilder() {
        return new Builder();
    }

    private OnChainTransaction(Builder builder) {
        this.TransactionId = builder.TransactionId;
        this.Amount = builder.Amount;
        this.BlockHeight = builder.BlockHeight;
        this.Fee = builder.Fee;
        this.TimeStamp = builder.TimeStamp;
        this.Label = builder.Label;
        this.hasLabel = builder.hasLabel;
        this.Inputs = builder.Inputs;
        this.Type = builder.Type;
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
     * Whether or not the transaction is confirmed.
     */
    public boolean isConfirmed() {
        return getBlockHeight() != 0;
    }

    /**
     * The height of the block this transaction was included in. 0 if unconfirmed.
     */
    public int getBlockHeight() {
        return BlockHeight;
    }

    public void setBlockHeight(int blockHeight) {
        BlockHeight = blockHeight;
    }

    public int getConfirmations() {
        if (BlockHeight == 0)
            return 0;
        return WalletUtil.getBlockHeight() - getBlockHeight() + 1;
    }

    /**
     * Transaction miner fee in msat
     */
    public long getFee() {
        return Fee;
    }

    /**
     * Transaction miner fee in msat
     */
    public void setFee(long fee) {
        Fee = fee;
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

    public TransactionType getType() {
        return Type;
    }


    //Builder Class
    public static class Builder {

        private String TransactionId;
        private long Amount;
        private int BlockHeight;
        private long Fee;
        private long TimeStamp;
        private String Label;
        private boolean hasLabel;
        private List<Outpoint> Inputs;
        private TransactionType Type;

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

        public Builder setType(TransactionType type) {
            Type = type;
            return this;
        }
    }

    public enum TransactionType {
        UNKNOWN,
        WALLET_SEND,
        WALLET_RECEIVE,
        OPEN_CHANNEL,
        CLOSE_CHANNEL,
        FORCE_CLOSE_CHANNEL;
    }
}