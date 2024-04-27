package app.michaelwuensch.bitbanana.backends.lndHub.models;

public class LndHubBalanceResponse {
    private BTC BTC;

    public BTC getBTC() {
        return BTC;
    }

    public class BTC {
        private int AvailableBalance;

        public int getAvailableBalance() {
            return AvailableBalance;
        }
    }
}
