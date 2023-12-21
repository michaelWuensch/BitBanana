package app.michaelwuensch.bitbanana.listViews.forwardings;

import com.google.protobuf.ByteString;

public interface ForwardingEventSelectListener {
    void onForwardingEventSelect(ByteString forwardingEvent);
}
