package app.michaelwuensch.bitbanana.listViews.peers;

import com.google.protobuf.ByteString;

public interface PeerSelectListener {
    void onPeerSelect(ByteString peer);
}
