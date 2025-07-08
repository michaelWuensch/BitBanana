package app.michaelwuensch.bitbanana.labels;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class WalletLabelsCollection {

    @SerializedName("wallets")
    private List<WalletLabelsEntry> walletLabelsList;

    int version;

    public WalletLabelsCollection() {
        walletLabelsList = new ArrayList<>();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<WalletLabelsEntry> getWalletLabelsList() {
        return walletLabelsList;
    }

    public void setWalletLabelsList(List<WalletLabelsEntry> walletLabelsList) {
        this.walletLabelsList = walletLabelsList;
    }

    public void addWalletLabelsEntry(WalletLabelsEntry entry) {
        walletLabelsList.add(entry);
    }
}
