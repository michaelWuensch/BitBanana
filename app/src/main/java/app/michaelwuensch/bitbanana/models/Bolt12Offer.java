package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class Bolt12Offer implements Serializable {

    private final DecodedBolt12 decodedBolt12;
    private final String OfferId;
    private final String Label;
    private boolean IsActive;
    private final boolean IsSingleUse;
    private final boolean WasAlreadyUsed;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Bolt12Offer(Builder builder) {
        this.decodedBolt12 = builder.decodedBolt12;
        this.OfferId = builder.OfferId;
        this.Label = builder.Label;
        this.IsActive = builder.IsActive;
        this.IsSingleUse = builder.IsSingleUse;
        this.WasAlreadyUsed = builder.WasAlreadyUsed;
    }

    public DecodedBolt12 getDecodedBolt12() {
        return decodedBolt12;
    }

    public String getOfferId() {
        return OfferId;
    }

    public String getLabel() {
        return Label;
    }

    public boolean getIsActive() {
        return IsActive;
    }

    public boolean getIsSingleUse() {
        return IsSingleUse;
    }

    public boolean getWasAlreadyUsed() {
        return WasAlreadyUsed;
    }

    public void updateActiveState(boolean isActive) {
        IsActive = isActive;
    }

    //Builder Class
    public static class Builder {

        private DecodedBolt12 decodedBolt12;
        private String OfferId;
        private String Label;
        private boolean IsActive;
        private boolean IsSingleUse;
        private boolean WasAlreadyUsed;

        private Builder() {
            // required parameters
        }

        public Bolt12Offer build() {
            return new Bolt12Offer(this);
        }

        public Builder setDecodedBolt12(DecodedBolt12 decodedBolt12) {
            this.decodedBolt12 = decodedBolt12;
            return this;
        }

        public Builder setOfferId(String offerId) {
            OfferId = offerId;
            return this;
        }

        public Builder setLabel(String label) {
            Label = label;
            return this;
        }

        public Builder setIsActive(boolean isActive) {
            this.IsActive = isActive;
            return this;
        }

        public Builder setIsSingleUse(boolean isSingleUse) {
            this.IsSingleUse = isSingleUse;
            return this;
        }

        public Builder setWasAlreadyUsed(boolean wasAlreadyUsed) {
            this.WasAlreadyUsed = wasAlreadyUsed;
            return this;
        }
    }
}