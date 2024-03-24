package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class CustomRecord implements Serializable {

    private final long FieldNumber;
    private final String Value;

    public static Builder newBuilder() {
        return new Builder();
    }

    private CustomRecord(Builder builder) {
        this.FieldNumber = builder.FieldNumber;
        this.Value = builder.Value;
    }

    /**
     * The number of the record
     */
    public long getFieldNumber() {
        return FieldNumber;
    }

    /**
     * The value as hex string
     */
    public String getValue() {
        return Value;
    }


    //Builder Class
    public static class Builder {


        private long FieldNumber;
        private String Value;


        private Builder() {
            // required parameters
        }

        public CustomRecord build() {
            return new CustomRecord(this);
        }


        /**
         * The number of the record
         */
        public Builder setFieldNumber(long fieldNumber) {
            this.FieldNumber = fieldNumber;
            return this;
        }

        /**
         * The value as hex string
         */
        public Builder setValue(String value) {
            Value = value;
            return this;
        }
    }
}