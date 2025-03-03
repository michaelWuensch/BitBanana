package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.Random;

public class BBLogItem implements Serializable {

    private String Message;
    private final String Tag;
    private final long Timestamp;
    private final Verbosity Verbosity;
    private final long RandomID;
    private final boolean AllInfoInMessage;

    public static Builder newBuilder() {
        return new Builder();
    }

    private BBLogItem(Builder builder) {
        this.Message = builder.Message;
        this.Tag = builder.Tag;
        this.Timestamp = builder.Timestamp;
        this.Verbosity = builder.Verbosity;
        this.AllInfoInMessage = builder.AllInfoInMessage;

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

    public boolean hasTag() {
        return Tag != null && !Tag.isEmpty();
    }

    public boolean isAllInfoInMessage() {
        return AllInfoInMessage;
    }

    public void setMessage(String message) {
        Message = message;
    }


    //Builder Class
    public static class Builder {

        private String Message;
        private String Tag;
        private long Timestamp;
        private Verbosity Verbosity;
        private boolean AllInfoInMessage;

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

        public Builder setIseAllInfoInMessage(boolean allInfoInMessage) {
            AllInfoInMessage = allInfoInMessage;
            return this;
        }
    }

    public enum Verbosity {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR;

        public String getDisplayName() {
            switch (this) {
                case VERBOSE:
                    return "Verbose";
                case DEBUG:
                    return "Debug";
                case INFO:
                    return "Info";
                case WARNING:
                    return "Warning";
                case ERROR:
                    return "Error";
                default:
                    return "Debug";
            }
        }
    }
}