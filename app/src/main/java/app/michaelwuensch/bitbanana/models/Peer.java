package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

public class Peer implements Serializable {

    private final String PubKey;
    private final String Address;
    private final long Ping;
    private final boolean hasPing;
    private final int FlapCount;
    private final boolean hasFlapCount;
    private final long LastFlapTimestamp;
    private final boolean hasLastFlap;
    private final List<TimestampedMessage> ErrorMessages;
    private final boolean hasErrorMessages;
    private final List<LnFeature> Features;
    private final boolean hasFeatures;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Peer(Builder builder) {
        PubKey = builder.PubKey;
        Address = builder.Address;
        Ping = builder.Ping;
        hasPing = builder.hasPing;
        FlapCount = builder.FlapCount;
        hasFlapCount = builder.hasFlapCount;
        LastFlapTimestamp = builder.LastFlapTimestamp;
        hasLastFlap = builder.hasLastFlap;
        ErrorMessages = builder.ErrorMessages;
        hasErrorMessages = builder.hasErrorMessages;
        Features = builder.Features;
        hasFeatures = builder.hasFeatures;
    }

    @NonNull
    public String getPubKey() {
        return PubKey;
    }

    public String getAddress() {
        return Address;
    }

    public long getPing() {
        return Ping;
    }

    public boolean hasPing() {
        return hasPing;
    }

    public int getFlapCount() {
        return FlapCount;
    }

    public boolean hasFlapCount() {
        return hasFlapCount;
    }

    public long getLastFlapTimestamp() {
        return LastFlapTimestamp;
    }

    public boolean hasLastFlap() {
        return hasLastFlap;
    }

    public List<TimestampedMessage> getErrorMessages() {
        return ErrorMessages;
    }

    public boolean hasErrorMessages() {
        return hasErrorMessages;
    }

    public List<LnFeature> getFeatures() {
        return Features;
    }

    public boolean hasFeatures() {
        return hasFeatures;
    }

    public static class Builder {
        private String PubKey;
        private String Address;
        private long Ping;
        private boolean hasPing;
        private int FlapCount;
        private boolean hasFlapCount;
        private long LastFlapTimestamp;
        private boolean hasLastFlap;
        private List<TimestampedMessage> ErrorMessages;
        private boolean hasErrorMessages;
        private List<LnFeature> Features;
        private boolean hasFeatures;

        public Peer build() {
            return new Peer(this);
        }

        public Builder setPubKey(@NonNull String pubKey) {
            this.PubKey = pubKey;
            return this;
        }

        public Builder setAddress(String address) {
            this.Address = address;
            return this;
        }

        public Builder setPing(long ping) {
            Ping = ping;
            hasPing = true;
            return this;
        }

        public Builder setFlapCount(int flapCount) {
            FlapCount = flapCount;
            hasFlapCount = true;
            return this;
        }

        public Builder setLastFlapTimestamp(long lastFlapTimestamp) {
            LastFlapTimestamp = lastFlapTimestamp;
            hasLastFlap = true;
            return this;
        }

        public Builder setErrorMessages(List<TimestampedMessage> errorMessages) {
            ErrorMessages = errorMessages;
            hasErrorMessages = true;
            return this;
        }

        public Builder setFeatures(List<LnFeature> features) {
            Features = features;
            hasFeatures = true;
            return this;
        }
    }
}
