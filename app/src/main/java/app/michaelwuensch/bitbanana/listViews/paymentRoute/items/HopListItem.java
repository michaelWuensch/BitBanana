package app.michaelwuensch.bitbanana.listViews.paymentRoute.items;

import app.michaelwuensch.bitbanana.models.LnHop;

public class HopListItem implements Comparable<HopListItem> {

    private LnHop mHop;

    public HopListItem(LnHop hop) {
        mHop = hop;
    }

    public LnHop getHop() {
        return mHop;
    }

    public boolean equalsWithSameContent(Object o) {
        if (!equals(o)) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(HopListItem o) {
        HopListItem other = (HopListItem) o;
        return Long.compare(this.mHop.getIdInRoute(), other.mHop.getIdInRoute());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HopListItem that = (HopListItem) o;

        return mHop.getIdInRoute() == that.mHop.getIdInRoute();
    }

    @Override
    public int hashCode() {
        return Long.valueOf(mHop.getIdInRoute()).hashCode();
    }
}
