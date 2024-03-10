package app.michaelwuensch.bitbanana.listViews.utxos;

import com.google.protobuf.ByteString;

import java.io.Serializable;

public interface UTXOSelectListener {
    void onUtxoSelect(Serializable utxo);
}
