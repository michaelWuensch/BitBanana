package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.List;

public class LnRoute implements Serializable {

    private final List<LnHop> Hops;
    private final long Amount;
    private final long Fee;

    public static Builder newBuilder() {
        return new Builder();
    }

    private LnRoute(Builder builder) {
        this.Hops = builder.Hops;
        this.Amount = builder.Amount;
        this.Fee = builder.Fee;
    }

    public List<LnHop> getHops() {
        return Hops;
    }

    public long getAmount() {
        return Amount;
    }

    public long getFee() {
        return Fee;
    }


    //Builder Class
    public static class Builder {

        private List<LnHop> Hops;
        private long Amount;
        private long Fee;

        private Builder() {
            // required parameters
        }

        public LnRoute build() {
            return new LnRoute(this);
        }

        public Builder setHops(List<LnHop> hops) {
            this.Hops = hops;
            return this;
        }

        public Builder setAmount(long amount) {
            this.Amount = amount;
            return this;
        }

        public Builder setFee(long fee) {
            this.Fee = fee;
            return this;
        }
    }
}