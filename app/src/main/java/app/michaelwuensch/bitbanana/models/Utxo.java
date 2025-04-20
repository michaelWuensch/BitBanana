package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class Utxo implements Serializable {

    private final String Address;
    private final boolean hasAddress;
    private final long Amount;
    private final long BlockHeight;
    private final long Confirmations;
    private final Outpoint Outpoint;
    private final Lease Lease;
    private final boolean hasLease;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Utxo(Builder builder) {
        this.Address = builder.Address;
        this.hasAddress = builder.hasAddress;
        this.Amount = builder.Amount;
        this.Outpoint = builder.Outpoint;
        this.BlockHeight = builder.BlockHeight;
        this.Confirmations = builder.Confirmations;
        this.Lease = builder.Lease;
        this.hasLease = builder.hasLease;
    }

    public String getAddress() {
        return Address;
    }

    public boolean hasAddress() {
        return hasAddress;
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

    public Lease getLease() {
        return Lease;
    }

    public boolean isLeased() {
        return hasLease;
    }

    //Builder Class
    public static class Builder {

        private String Address;
        private boolean hasAddress;
        private long Amount;
        private long BlockHeight;
        private long Confirmations;
        private Outpoint Outpoint;
        private Lease Lease;
        private boolean hasLease;

        private Builder() {
            // required parameters
        }

        public Utxo build() {
            return new Utxo(this);
        }

        public Builder setAddress(String address) {
            this.Address = address;
            this.hasAddress = address != null && !address.isEmpty();
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

        public Builder setLease(Lease lease) {
            this.Lease = lease;
            this.hasLease = lease != null;
            return this;
        }
    }
}