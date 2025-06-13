package app.michaelwuensch.bitbanana.wallet;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Wallet_Bolt12Offers {

    private static final String LOG_TAG = Wallet_Bolt12Offers.class.getSimpleName();

    private static Wallet_Bolt12Offers mInstance = null;

    private List<Bolt12Offer> mOffersList;
    private final Set<Bolt12OffersSubscriptionListener> mBolt12OffersSubscriptionListeners = new HashSet<>();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private Wallet_Bolt12Offers() {
    }

    public static Wallet_Bolt12Offers getInstance() {

        if (mInstance == null) {
            mInstance = new Wallet_Bolt12Offers();
        }

        return mInstance;
    }

    /**
     * Use this to reset the wallet information when the wallet was switched.
     */
    public void reset() {
        mOffersList = null;
        compositeDisposable.clear();
    }

    public List<Bolt12Offer> getBolt12OffersList() {
        return mOffersList;
    }

    public Bolt12Offer getBolt12OfferById(String id) {
        if (mOffersList != null) {
            for (Bolt12Offer offer : mOffersList) {
                if (offer.getOfferId().equals(id)) {
                    return offer;
                }
            }
        }
        return null;
    }

    public void fetchBolt12Offers() {
        compositeDisposable.add(BackendManager.api().listBolt12Offers()
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(response -> {
                            mOffersList = response;
                            broadcastBolt12OffersListUpdated();
                        }
                        , throwable -> {
                            BBLog.w(LOG_TAG, "Fetching bolt12 offers list failed: " + throwable.getMessage());
                        }));
    }

    public Single<Boolean> fetchBolt12OffersSingle() {
        return BackendManager.api().listBolt12Offers()
                .map(response -> {
                    mOffersList = response;
                    BBLog.d(LOG_TAG, "bolt12 offers fetched!");
                    broadcastBolt12OffersListUpdated();
                    return true;
                });
    }


    public void cancelSubscriptions() {
        compositeDisposable.clear();
    }

    /**
     * Notify all listeners to offers list update.
     */
    private void broadcastBolt12OffersListUpdated() {
        for (Bolt12OffersSubscriptionListener listener : mBolt12OffersSubscriptionListeners) {
            listener.onBolt12OffersListUpdated();
        }
    }

    public void registerBolt12OffersSubscriptionListener(Bolt12OffersSubscriptionListener listener) {
        mBolt12OffersSubscriptionListeners.add(listener);
    }

    public void unregisterBolt12OffersSubscriptionListener(Bolt12OffersSubscriptionListener listener) {
        mBolt12OffersSubscriptionListeners.remove(listener);
    }

    public interface Bolt12OffersSubscriptionListener {
        void onBolt12OffersListUpdated();
    }

}