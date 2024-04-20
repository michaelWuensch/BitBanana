package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class NewOnChainAddressRequest implements Serializable {

    private final Type Type;
    private final boolean Unused;


    public static Builder newBuilder() {
        return new Builder();
    }

    private NewOnChainAddressRequest(Builder builder) {
        this.Type = builder.Type;
        this.Unused = builder.Unused;
    }


    public Type getType() {
        return Type;
    }

    /**
     * If this is true, the next unused address will be returned.
     * If it is false, a new address will be returned, even if the one before was never used.
     */
    public boolean getUnused() {
        return Unused;
    }


    //Builder Class
    public static class Builder {
        private Type Type;
        private boolean Unused;


        private Builder() {
            // required parameters
        }

        public NewOnChainAddressRequest build() {
            return new NewOnChainAddressRequest(this);
        }

        public Builder setType(Type type) {
            Type = type;
            return this;
        }

        /**
         * If this is true, the next unused address will be returned.
         * If it is false, a new address will be returned, even if the one before was never used.
         */
        public Builder setUnused(boolean unused) {
            Unused = unused;
            return this;
        }
    }

    public enum Type {
        SEGWIT_COMPATIBILITY,
        SEGWIT,
        TAPROOT;
    }
}