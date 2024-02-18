package app.michaelwuensch.bitbanana.listViews.channels;

import com.google.protobuf.ByteString;

public interface ChannelSelectListener {

    void onChannelSelect(ByteString channel, int type);
}
