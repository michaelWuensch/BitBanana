package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Watchtower implements Serializable {

    private final String PubKey;
    private final List<String> Addresses;
    private final boolean IsActive;
    private final List<WatchtowerSession> Sessions;


    public static Builder newBuilder() {
        return new Builder();
    }

    private Watchtower(Builder builder) {
        PubKey = builder.PubKey;
        Addresses = builder.Addresses != null ? new ArrayList<>(builder.Addresses) : new ArrayList<>();
        IsActive = builder.IsActive;
        Sessions = builder.Sessions;
    }

    @NonNull
    public String getPubKey() {
        return PubKey;
    }

    public List<String> getAddress() {
        return Addresses;
    }

    public boolean getIsActive() {
        return IsActive;
    }

    public List<WatchtowerSession> getSessions() {
        return Sessions;
    }

    public boolean hasNonExhaustedSessions() {
        boolean hasNonExhaustedSessions = false;
        for (WatchtowerSession s : getSessions()) {
            if (!s.getIsExhausted()) {
                hasNonExhaustedSessions = true;
                break;
            }
        }
        return hasNonExhaustedSessions;
    }

    public static class Builder {
        private String PubKey;
        private List<String> Addresses;
        private boolean IsActive;
        private List<WatchtowerSession> Sessions;

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

        public Builder setIsActive(boolean isActive) {
            this.IsActive = isActive;
            return this;
        }

        public Builder setSessions(List<WatchtowerSession> sessions) {
            this.Sessions = sessions;
            return this;
        }
    }
}
