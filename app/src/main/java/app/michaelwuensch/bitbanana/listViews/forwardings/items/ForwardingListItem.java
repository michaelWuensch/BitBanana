package app.michaelwuensch.bitbanana.listViews.forwardings.items;

public abstract class ForwardingListItem implements Comparable<ForwardingListItem> {

    public static final int TYPE_DATE = 0;
    public static final int TYPE_FORWARDING_EVENT = 1;

    protected long mTimestampNS;
    protected long mTimestampMS;

    abstract public int getType();

    public boolean equalsWithSameContent(Object o) {
        return equals(o);
    }

    public long getTimestampNS() {
        return mTimestampNS;
    }

    public long getTimestampMS() {
        return mTimestampMS;
    }

    @Override
    public int compareTo(ForwardingListItem o) {
        return Long.compare(o.mTimestampNS, this.mTimestampNS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForwardingListItem that = (ForwardingListItem) o;

        if (this.getType() != that.getType()) {
            return false;
        }
        return mTimestampNS == that.mTimestampNS;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(mTimestampNS).hashCode();
    }
}
