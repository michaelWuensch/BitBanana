package app.michaelwuensch.bitbanana.models;

public class FeeEstimateResponse {

    private final int NextBlockFee;
    private final int HourFee;
    private final int DayFee;
    private final int MinimumFee;

    public static Builder newBuilder() {
        return new Builder();
    }

    private FeeEstimateResponse(Builder builder) {
        this.NextBlockFee = builder.NextBlockFee;
        this.HourFee = builder.HourFee;
        this.DayFee = builder.DayFee;
        this.MinimumFee = builder.MinimumFee;
    }

    /**
     * FeeEstimate in sat/vB
     */
    public int getNextBlockFee() {
        return NextBlockFee;
    }

    /**
     * FeeEstimate in sat/vB
     */
    public int getHourFee() {
        return HourFee;
    }

    /**
     * FeeEstimate in sat/vB
     */
    public int getDayFee() {
        return DayFee;
    }

    /**
     * FeeEstimate in sat/vB
     */
    public int getMinimumFee() {
        return MinimumFee;
    }

    //Builder Class
    public static class Builder {
        private int NextBlockFee;
        private int HourFee;
        private int DayFee;
        private int MinimumFee;

        private Builder() {
            // required parameters
        }

        public FeeEstimateResponse build() {
            return new FeeEstimateResponse(this);
        }

        /**
         * FeeEstimate in sat/vB
         */
        public Builder setNextBlockFee(int nextBlockFee) {
            this.NextBlockFee = nextBlockFee;
            return this;
        }

        /**
         * FeeEstimate in sat/vB
         */
        public Builder setHourFee(int hourFee) {
            this.HourFee = hourFee;
            return this;
        }

        /**
         * FeeEstimate in sat/vB
         */
        public Builder setDayFee(int dayFee) {
            DayFee = dayFee;
            return this;
        }

        /**
         * FeeEstimate in sat/vB
         */
        public Builder setMinimumFee(int minimumFee) {
            MinimumFee = minimumFee;
            return this;
        }
    }
}