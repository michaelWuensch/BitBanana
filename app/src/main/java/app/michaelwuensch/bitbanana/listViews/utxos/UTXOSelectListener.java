package app.michaelwuensch.bitbanana.listViews.utxos;

import com.google.protobuf.ByteString;

public interface UTXOSelectListener {
    void onUtxoSelect(ByteString utxo);
}
