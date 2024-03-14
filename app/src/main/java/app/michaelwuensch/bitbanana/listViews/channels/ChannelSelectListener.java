package app.michaelwuensch.bitbanana.listViews.channels;

import com.google.protobuf.ByteString;

import java.io.Serializable;

public interface ChannelSelectListener {

    void onChannelSelect(Serializable channel, int type);
}
