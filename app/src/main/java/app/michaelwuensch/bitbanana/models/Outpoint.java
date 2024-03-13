package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Outpoint implements Serializable {

    private String TransactionID;
    private int OutputIndex;


    private Outpoint(Builder builder) {
        TransactionID = builder.TransactionID;
        OutputIndex = builder.OutputIndex;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getTransactionID() {
        return TransactionID;
    }

    public int getOutputIndex() {
        return OutputIndex;
    }

    @NonNull
    @Override
    public String toString() {
        return TransactionID + ":" + OutputIndex;
    }

    public static class Builder {
        private String TransactionID;
        private int OutputIndex;

        private Builder() {
        }

        public Outpoint build() {
            return new Outpoint(this);
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
