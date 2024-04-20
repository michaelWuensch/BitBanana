package app.michaelwuensch.bitbanana.models;

public class SignMessageResponse {

    private final String Signature;
    private final String RecId;
    private final String ZBase;

    public static Builder newBuilder() {
        return new Builder();
    }

    private SignMessageResponse(Builder builder) {
        Signature = builder.Signature;
        RecId = builder.RecId;
        ZBase = builder.ZBase;
    }

    public String getSignature() {
        return Signature;
    }

    public String getRecId() {
        return RecId;
    }

    public String getZBase() {
        return ZBase;
    }

    public static class Builder {
        private String Signature;
        private String RecId;
        private String ZBase;


        private Builder() {
        }

        public SignMessageResponse build() {
            return new SignMessageResponse(this);
        }

        public Builder setSignature(String signature) {
            this.Signature = signature;
            return this;
        }

        public Builder setRecId(String RecId) {
            this.RecId = RecId;
            return this;
        }

        public Builder setZBase(String ZBase) {
            this.ZBase = ZBase;
            return this;
        }
    }
}
