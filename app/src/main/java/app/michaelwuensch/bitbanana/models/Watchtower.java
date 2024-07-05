package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

public class Watchtower implements Serializable {

    private final String PubKey;
    private final List<String> Addresses;


    public static Builder newBuilder() {
        return new Builder();
    }

    private Watchtower(Builder builder) {
        PubKey = builder.PubKey;
        Addresses = builder.Addresses;
    }

    @NonNull
    public String getPubKey() {
        return PubKey;
    }

    public List<String> getAddress() {
        return Addresses;
    }

    public static class Builder {
        private String PubKey;
        private List<String> Addresses;


        public Watchtower build() {
            return new Watchtower(this);
        }

        public Builder setPubKey(@NonNull String pubKey) {
            this.PubKey = pubKey;
            return this;
        }

        public Builder setAddresses(List<String> addresses) {
            this.Addresses = addresses;
            return this;
        }
    }
}
