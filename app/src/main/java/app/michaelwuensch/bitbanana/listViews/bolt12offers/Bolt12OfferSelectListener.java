package app.michaelwuensch.bitbanana.listViews.bolt12offers;

import java.io.Serializable;

public interface Bolt12OfferSelectListener {
    void onOfferSelect(Serializable bolt12Offer);

    void onQrCodeSelect(Serializable bolt12Offer);
}
