package app.michaelwuensch.bitbanana.listViews.utxos;

import java.io.Serializable;

public interface UTXOSelectListener {
    void onUtxoSelect(Serializable utxo);
}
