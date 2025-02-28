package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.Random;

public class BBLogItem implements Serializable {

    private final String Message;
    private final String Tag;
    private final long Timestamp;
    private final Verbosity Verbosity;
    private final long RandomID;

    public static Builder newBuilder() {
        return new Builder();
    }

    private BBLogItem(Builder builder) {
        this.Message = builder.Message;
        this.Tag = builder.Tag;
        this.Timestamp = builder.Timestamp;
        this.Verbosity = builder.Verbosity;

        Random random = new Random();
        this.RandomID = random.nextLong();
    }

    public String getMessage() {
        return Message;
    }

    public String getTag() {
        return Tag;
    }

    public long getTimestamp() {
        return Timestamp;
    }

    public Verbosity getVerbosity() {
        return Verbosity;
    }

    public long getRandomID() {
        return RandomID;
    }


    //Builder Class
    public static class Builder {

        private String Message;
        private String Tag;
        private long Timestamp;
        private Verbosity Verbosity;

        private Builder() {
            // required parameters
        }

        public BBLogItem build() {
            return new BBLogItem(this);
        }

        public Builder setMessage(String message) {
            this.Message = message;
            return this;
        }

        public Builder setTag(String tag) {
            this.Tag = tag;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.Timestamp = timestamp;
            return this;
        }

        public Builder setVerbosity(Verbosity verbosity) {
            Verbosity = verbosity;
            return this;
        }
    }

    public enum Verbosity {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR;
    }
}