package app.michaelwuensch.bitbanana.labels;

/**
 * Class representing the labels associated with a specific wallet.
 */
public class WalletLabelsEntry {

    private String walletId;
    private Labels labels;

    public WalletLabelsEntry() {
        // Default constructor for JSON deserialization
    }

    public WalletLabelsEntry(String walletId, Labels labels) {
        this.walletId = walletId;
        this.labels = labels;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public Labels getLabels() {
        return labels;
    }

    public void setLabels(Labels labels) {
        this.labels = labels;
    }
}