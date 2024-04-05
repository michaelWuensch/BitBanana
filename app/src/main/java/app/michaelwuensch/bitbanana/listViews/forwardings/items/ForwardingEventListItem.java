package app.michaelwuensch.bitbanana.listViews.forwardings.items;

import app.michaelwuensch.bitbanana.models.Forward;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;

public class ForwardingEventListItem extends ForwardingListItem {


    private final Forward mForwardingEvent;

    public ForwardingEventListItem(Forward forwardingEvent) {
        mForwardingEvent = forwardingEvent;
        mTimestampNS = forwardingEvent.getTimestampNs();
        mTimestampMS = TimeFormatUtil.NStoMS(mTimestampNS);
    }

    public Forward getForwardingEvent() {
        return mForwardingEvent;
    }

    @Override
    public int getType() {
        return TYPE_FORWARDING_EVENT;
    }
}
