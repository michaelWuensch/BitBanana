package app.michaelwuensch.bitbanana.listViews.logs.items;

import app.michaelwuensch.bitbanana.models.BBLogItem;

public class LogListItem implements Comparable<LogListItem> {

    private BBLogItem mLogItem;

    public LogListItem(BBLogItem logItem) {
        mLogItem = logItem;
    }

    public BBLogItem getLogItem() {
        return mLogItem;
    }

    public boolean equalsWithSameContent(Object o) {
        if (!equals(o)) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(LogListItem o) {
        LogListItem other = (LogListItem) o;
        return Long.compare(this.mLogItem.getTimestamp(), other.mLogItem.getTimestamp());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogListItem that = (LogListItem) o;

        if (mLogItem.getTimestamp() != that.mLogItem.getTimestamp())
            return false;

        return mLogItem.getRandomID() == that.mLogItem.getRandomID();
    }

    @Override
    public int hashCode() {
        String hashString = mLogItem.getMessage() + mLogItem.getRandomID();
        return hashString.hashCode();
    }
}
