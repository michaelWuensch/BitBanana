package app.michaelwuensch.bitbanana.listViews.watchtowerSessions.items;


import app.michaelwuensch.bitbanana.models.WatchtowerSession;

public class WatchtowerSessionListItem implements Comparable<WatchtowerSessionListItem> {


    private WatchtowerSession mSession;

    public WatchtowerSessionListItem(WatchtowerSession session) {
        mSession = session;
    }


    public WatchtowerSession getWatchtowerSession() {
        return mSession;
    }

    public boolean equalsWithSameContent(Object o) {
        if (!equals(o)) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(WatchtowerSessionListItem o) {
        // First, compare by isExhausted
        if (this.getWatchtowerSession().getIsExhausted() != o.getWatchtowerSession().getIsExhausted()) {
            return Boolean.compare(this.getWatchtowerSession().getIsExhausted(), o.getWatchtowerSession().getIsExhausted());
        }

        // Second, compare by NumBackups
        int numBackupsComparison = Long.compare(this.getWatchtowerSession().getNumBackups(), o.getWatchtowerSession().getNumBackups());

        // If NumBackups are equal, compare by hexId
        if (numBackupsComparison == 0) {
            return this.getWatchtowerSession().getId().compareTo(o.getWatchtowerSession().getId());
        }

        // Otherwise, return the result of the NumBackups comparison
        return numBackupsComparison;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchtowerSessionListItem that = (WatchtowerSessionListItem) o;

        return mSession.getId().equals(that.getWatchtowerSession().getId()) && mSession.getNumBackups() == that.getWatchtowerSession().getNumBackups();
    }

    @Override
    public int hashCode() {
        return mSession.getId().hashCode();
    }
}
