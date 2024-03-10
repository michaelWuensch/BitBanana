package app.michaelwuensch.bitbanana.models;

public class VerifyMessageResponse {

    private final String PubKey;
    private final boolean isValid;

    public static Builder newBuilder() {
        return new Builder();
    }

    private VerifyMessageResponse(Builder builder) {
        PubKey = builder.PubKey;
        isValid = builder.isValid;
    }

    public String getPubKey() {
        return PubKey;
    }

    public boolean isValid() {
        return isValid;
    }

    public static class Builder {
        private String PubKey;
        private boolean isValid;


        private Builder() {
        }

        public VerifyMessageResponse build() {
            return new VerifyMessageResponse(this);
        }

        public Builder setPubKey(String pubKey) {
            this.PubKey = pubKey;
            return this;
        }

        public Builder setIsValid(boolean isValid) {
            this.isValid = isValid;
            return this;
        }
    }
}
