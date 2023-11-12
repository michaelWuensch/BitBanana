package app.michaelwuensch.bitbanana.peers;

import com.google.protobuf.ByteString;

public interface PeerSelectListener {
    void onPeerSelect(ByteString peer);
}
