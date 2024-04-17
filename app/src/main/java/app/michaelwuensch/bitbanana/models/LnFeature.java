package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class LnFeature implements Serializable {

    private final long FeatureNumber;
    private final String Name;

    public static Builder newBuilder() {
        return new Builder();
    }

    private LnFeature(Builder builder) {
        this.FeatureNumber = builder.FeatureNumber;
        this.Name = builder.Name;
    }

    /**
     * The number of the feature
     */
    public long getFeatureNumber() {
        return FeatureNumber;
    }

    public String getName() {
        return Name;
    }


    //Builder Class
    public static class Builder {


        private long FeatureNumber;
        private String Name;


        private Builder() {
            // required parameters
        }

        public LnFeature build() {
            return new LnFeature(this);
        }


        /**
         * The number of the feature
         */
        public Builder setFeatureNumber(long featureNumber) {
            this.FeatureNumber = featureNumber;
            return this;
        }

        public Builder setName(String name) {
            Name = name;
            return this;
        }
    }
}