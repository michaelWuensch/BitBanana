package app.michaelwuensch.bitbanana.listViews.watchtowers.items;


import app.michaelwuensch.bitbanana.models.Watchtower;
import app.michaelwuensch.bitbanana.util.AliasManager;

public class WatchtowerListItem implements Comparable<WatchtowerListItem> {

    private String mAlias;
    private Watchtower mWatchtower;

    public WatchtowerListItem(Watchtower watchtower) {
        mWatchtower = watchtower;
        mAlias = AliasManager.getInstance().getAlias(watchtower.getPubKey());
    }

    public String getAlias() {
        return mAlias;
    }

    public Watchtower getWatchtower() {
        return mWatchtower;
    }

    public boolean equalsWithSameContent(Object o) {
        if (!equals(o)) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(WatchtowerListItem o) {
        return this.mAlias.toLowerCase().compareTo(o.mAlias.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchtowerListItem that = (WatchtowerListItem) o;

        return mWatchtower.getPubKey().equals(that.getWatchtower().getPubKey());
    }

    @Override
    public int hashCode() {
        return mWatchtower.getPubKey().hashCode();
    }
}
