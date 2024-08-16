package app.michaelwuensch.bitbanana.listViews.bolt12offers.items;


import app.michaelwuensch.bitbanana.models.Bolt12Offer;

public class Bolt12OfferListItem implements Comparable<Bolt12OfferListItem> {


    private Bolt12Offer mBolt12Offer;

    public Bolt12OfferListItem(Bolt12Offer bolt12Offer) {
        mBolt12Offer = bolt12Offer;
    }

    public Bolt12Offer getBolt12Offer() {
        return mBolt12Offer;
    }

    public boolean equalsWithSameContent(Object o) {
        if (!equals(o)) {
            return false;
        }

        Bolt12OfferListItem that = ((Bolt12OfferListItem) o);

        return this.getBolt12Offer().getIsActive() == that.getBolt12Offer().getIsActive()
                && this.getBolt12Offer().getWasAlreadyUsed() == that.getBolt12Offer().getWasAlreadyUsed();
    }

    @Override
    public int compareTo(Bolt12OfferListItem o) {
        // First, compare by active state
        if (this.getBolt12Offer().getIsActive() != o.getBolt12Offer().getIsActive()) {
            return Boolean.compare(o.getBolt12Offer().getIsActive(), this.getBolt12Offer().getIsActive());
        }

        // Second, compare by Label
        return this.getBolt12Offer().getLabel().toLowerCase().compareTo(o.getBolt12Offer().getLabel().toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bolt12OfferListItem that = (Bolt12OfferListItem) o;

        return mBolt12Offer.getOfferId().equals(that.getBolt12Offer().getOfferId());
    }

    @Override
    public int hashCode() {
        return mBolt12Offer.getOfferId().hashCode();
    }
}
