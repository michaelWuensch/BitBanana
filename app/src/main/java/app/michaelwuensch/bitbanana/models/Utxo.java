package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class Utxo implements Serializable {

    private final String Address;
    private final long Amount;
    private final long BlockHeight;
    private final long Confirmations;
    private final Outpoint Outpoint;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Utxo(Builder builder) {
        this.Address = builder.Address;
        this.Amount = builder.Amount;
        this.Outpoint = builder.Outpoint;
        this.BlockHeight = builder.BlockHeight;
        this.Confirmations = builder.Confirmations;
    }

    public String getAddress() {
        return Address;
    }

    /**
     * Amount in msat
     */
    public long getAmount() {
        return Amount;
    }

    /**
     * Block height where it was confirmed
     */
    public long getBlockHeight() {
        return BlockHeight;
    }

    public Outpoint getOutpoint() {
        return Outpoint;
    }

    public long getConfirmations() {
        return Confirmations;
    }


    //Builder Class
    public static class Builder {

        private String Address;
        private long Amount;
        private long BlockHeight;
        private long Confirmations;
        private Outpoint Outpoint;

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

        /**
         * Amount in msat
         */
        public Builder setAmount(long amount) {
            this.Amount = amount;
            return this;
        }

        /**
         * Block height where it was confirmed
         */
        public Builder setBlockHeight(long blockHeight) {
            BlockHeight = blockHeight;
            return this;
        }

        public Builder setConfirmations(long confirmations) {
            Confirmations = confirmations;
            return this;
        }

        public Builder setOutpoint(Outpoint outpoint) {
            this.Outpoint = outpoint;
            return this;
        }
    }
}