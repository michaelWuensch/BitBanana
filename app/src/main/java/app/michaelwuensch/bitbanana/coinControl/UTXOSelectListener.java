package app.michaelwuensch.bitbanana.coinControl;

import com.google.protobuf.ByteString;

public interface UTXOSelectListener {
    void onUtxoSelect(ByteString utxo);
}
