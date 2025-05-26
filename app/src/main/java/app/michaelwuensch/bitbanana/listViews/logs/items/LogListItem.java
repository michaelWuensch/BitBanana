package app.michaelwuensch.bitbanana.listViews.logs.items;

import app.michaelwuensch.bitbanana.models.BBLogItem;
import app.michaelwuensch.bitbanana.util.PrefsUtil;

public class LogListItem implements Comparable<LogListItem> {

    private BBLogItem mLogItem;

    public LogListItem(BBLogItem logItem) {
        mLogItem = logItem;
    }

    public BBLogItem getLogItem() {
        return mLogItem;
    }

    public enum SortCriteria {
        AGE_ASC,
        AGE_DESC,
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

        SortCriteria currentCriteria = SortCriteria.valueOf(PrefsUtil.getPrefs().getString(PrefsUtil.LOG_SORT_CRITERIA, SortCriteria.AGE_DESC.name()));
        switch (currentCriteria) {
            case AGE_ASC:
                return Long.compare(other.mLogItem.getTimestamp(), this.mLogItem.getTimestamp());
            case AGE_DESC:
                return Long.compare(this.mLogItem.getTimestamp(), other.mLogItem.getTimestamp());
            default:
                return 0;
        }
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
