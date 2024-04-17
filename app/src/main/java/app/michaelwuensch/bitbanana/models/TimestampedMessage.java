package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class TimestampedMessage implements Serializable {

    private final String Message;
    private final long Timestamp;


    public static Builder newBuilder() {
        return new Builder();
    }

    private TimestampedMessage(Builder builder) {
        Message = builder.Message;
        Timestamp = builder.Timestamp;
    }

    @NonNull
    public String getMessage() {
        return Message;
    }

    public long getTimestamp() {
        return Timestamp;
    }


    public static class Builder {
        private String Message;

        private long Timestamp;


        public TimestampedMessage build() {
            return new TimestampedMessage(this);
        }

        public Builder setMessage(@NonNull String message) {
            this.Message = message;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            Timestamp = timestamp;
            return this;
        }
    }
}
