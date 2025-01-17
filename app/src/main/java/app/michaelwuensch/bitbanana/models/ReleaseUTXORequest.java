package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;

public class ReleaseUTXORequest implements Serializable {

    private final Outpoint Outpoint;
    private final String Id;

    public static Builder newBuilder() {
        return new Builder();
    }

    private ReleaseUTXORequest(Builder builder) {
        this.Outpoint = builder.Outpoint;
        this.Id = builder.Id;
    }

    /**
     * The outpoint that will be released.
     */
    public Outpoint getOutpoint() {
        return Outpoint;
    }

    /**
     * The unique ID that was used to lock the output.
     */
    public String getId() {
        return Id;
    }


    //Builder Class
    public static class Builder {

        private Outpoint Outpoint;
        private String Id;


        private Builder() {
            // required parameters
        }

        public ReleaseUTXORequest build() {
            return new ReleaseUTXORequest(this);
        }

        /**
         * The outpoint that will be released.
         */
        public Builder setOutpoint(Outpoint outpoint) {
            Outpoint = outpoint;
            return this;
        }

        /**
         * The unique ID that was used to lock the output.
         */
        public Builder setId(String id) {
            Id = id;
            return this;
        }
    }
}