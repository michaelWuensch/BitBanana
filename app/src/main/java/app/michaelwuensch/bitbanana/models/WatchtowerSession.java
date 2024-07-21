package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class WatchtowerSession implements Serializable {

    private final String Id;
    private final long SweepSatPerVByte;
    private final long NumBackups;
    private final long NumPendingBackups;
    private final long NumMaxBackups;
    private final boolean IsTerminated;
    private final SessionType Type;

    public static Builder newBuilder() {
        return new Builder();
    }

    private WatchtowerSession(Builder builder) {
        Id = builder.Id;
        SweepSatPerVByte = builder.SweepSatPerVByte;
        NumBackups = builder.NumBackups;
        NumPendingBackups = builder.NumPendingBackups;
        NumMaxBackups = builder.NumMaxBackups;
        IsTerminated = builder.IsTerminated;
        Type = builder.Type;
    }

    @NonNull
    public String getId() {
        return Id;
    }

    public long getSweepSatPerVByte() {
        return SweepSatPerVByte;
    }

    public long getNumBackups() {
        return NumBackups;
    }

    public long getNumPendingBackups() {
        return NumPendingBackups;
    }

    public long getNumMaxBackups() {
        return NumMaxBackups;
    }

    // A session is considered exhausted only if it has no un-acked updates and the
    // sequence number of the session is equal to the max updates of the session
    // policy.
    public boolean getIsExhausted() {
        return !(NumBackups < NumMaxBackups || NumPendingBackups > 0);
    }

    public boolean getIsTerminated() {
        return IsTerminated;
    }

    public SessionType getType() {
        return Type;
    }

    public static class Builder {
        private String Id;
        private long SweepSatPerVByte;
        private long NumBackups;
        private long NumPendingBackups;
        private long NumMaxBackups;
        private boolean IsTerminated;
        private SessionType Type;

        public WatchtowerSession build() {
            return new WatchtowerSession(this);
        }

        public Builder setId(@NonNull String id) {
            this.Id = id;
            return this;
        }

        public Builder setSweepSatPerVByte(long sweepSatPerVByte) {
            this.SweepSatPerVByte = sweepSatPerVByte;
            return this;
        }

        public Builder setNumBackups(long numBackups) {
            this.NumBackups = numBackups;
            return this;
        }

        public Builder setNumPendingBackups(long numPendingBackups) {
            this.NumPendingBackups = numPendingBackups;
            return this;
        }

        public Builder setNumMaxBackups(long numMaxBackups) {
            this.NumMaxBackups = numMaxBackups;
            return this;
        }

        public Builder setIsTerminated(boolean isTerminated) {
            this.IsTerminated = isTerminated;
            return this;
        }

        public Builder setType(SessionType type) {
            this.Type = type;
            return this;
        }
    }

    public enum SessionType {
        UNKNOWN,
        LEGACY,
        ANCHOR,
        TAPROOT;
    }
}
