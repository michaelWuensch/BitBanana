package app.michaelwuensch.bitbanana.listViews.utxos.items;

import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.PrefsUtil;

public class UTXOListItem implements Comparable<UTXOListItem> {

    private Utxo mUtxo;

    public UTXOListItem(Utxo utxo) {
        mUtxo = utxo;
    }

    public Utxo getUtxo() {
        return mUtxo;
    }

    public enum SortCriteria {
        AGE_ASC,
        AGE_DESC,
        VALUE_ASC,
        VALUE_DESC,
    }

    public boolean equalsWithSameContent(Object o) {
        if (!equals(o)) {
            return false;
        }

        UTXOListItem other = (UTXOListItem) o;

        return this.getUtxo().isLeased() == other.getUtxo().isLeased() && this.getUtxo().getConfirmations() == other.getUtxo().getConfirmations();
    }

    @Override
    public int compareTo(UTXOListItem other) {
        SortCriteria currentCriteria = SortCriteria.valueOf(PrefsUtil.getPrefs().getString(PrefsUtil.UTXO_SORT_CRITERIA, SortCriteria.AGE_ASC.name()));

        switch (currentCriteria) {
            case AGE_ASC:
                return Long.compare(other.getUtxo().getBlockHeight(), this.getUtxo().getBlockHeight());
            case AGE_DESC:
                return Long.compare(this.getUtxo().getBlockHeight(), other.getUtxo().getBlockHeight());
            case VALUE_ASC:
                return Long.compare(this.mUtxo.getAmount(), other.mUtxo.getAmount());
            case VALUE_DESC:
                return Long.compare(other.mUtxo.getAmount(), this.mUtxo.getAmount());
            default:
                return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UTXOListItem that = (UTXOListItem) o;

        return this.getUtxo().getOutpoint().toString().equals(that.getUtxo().getOutpoint().toString());
    }

    @Override
    public int hashCode() {
        return getUtxo().getOutpoint().toString().hashCode();
    }
}
