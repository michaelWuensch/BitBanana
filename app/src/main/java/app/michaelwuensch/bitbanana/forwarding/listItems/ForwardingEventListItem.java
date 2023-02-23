package app.michaelwuensch.bitbanana.forwarding.listItems;

import com.github.lightningnetwork.lnd.lnrpc.ForwardingEvent;

import app.michaelwuensch.bitbanana.util.TimeFormatUtil;

public class ForwardingEventListItem extends ForwardingListItem {


    private final ForwardingEvent mForwardingEvent;

    public ForwardingEventListItem(ForwardingEvent forwardingEvent) {
        mForwardingEvent = forwardingEvent;
        mTimestampNS = forwardingEvent.getTimestampNs();
        mTimestampMS = TimeFormatUtil.NStoMS(mTimestampNS);
    }

    public ForwardingEvent getForwardingEvent() {
        return mForwardingEvent;
    }

    @Override
    public int getType() {
        return TYPE_FORWARDING_EVENT;
    }
}
