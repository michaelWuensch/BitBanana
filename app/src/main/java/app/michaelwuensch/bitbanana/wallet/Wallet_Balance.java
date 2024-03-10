package app.michaelwuensch.bitbanana.wallet;


import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.DebounceHandler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Wallet_Balance {

    private static final String LOG_TAG = Wallet_Balance.class.getSimpleName();

    private static Wallet_Balance mInstance = null;
    private final Set<BalanceListener> mBalanceListeners = new HashSet<>();

    public boolean mBalancesFetched;
    private Balances mBalances = Balances.newBuilder().build();

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private DebounceHandler mBalancesDebounceHandler = new DebounceHandler();

    private Wallet_Balance() {
        ;
    }

    public static Wallet_Balance getInstance() {

        if (mInstance == null) {
            mInstance = new Wallet_Balance();
        }

        return mInstance;
    }

    /**
     * Use this to reset the wallet_balance component.
     */
    public void reset() {
        mBalancesFetched = false;
        mBalances = Balances.newBuilder().build();

        compositeDisposable.clear();
        mBalancesDebounceHandler.shutdown();
    }


    /**
     * This will return a Balance object that contains all types of balances.
     * Please note that this might be different from the actual balance of the node.
     * To update what this function returns call fetchBalances()
     *
     * @return
     */
    public Balances getBalances() {
        return mBalances;
    }

    /**
     * This will return a Balance object that contains all types of balances.
     * Use this only when wallet is not yet setup. The balances are not real and will always be the same.
     * If desired, these values can be set to specific values for demonstration purposes.
     *
     * @return
     */
    public Balances getDemoBalances() {
        return Balances.newBuilder().build();
    }

    public void fetchBalancesWithDebounce() {
        BBLog.d(LOG_TAG, "Fetch balance from Node. (debounce)");

        mBalancesDebounceHandler.attempt(this::fetchBalances, DebounceHandler.DEBOUNCE_1_SECOND);
    }


    /**
     * This will fetch the current balance from the connect node.
     * All Listeners registered to BalanceListener will be informed about any changes.
     */
    public void fetchBalances() {
        compositeDisposable.add(BackendManager.api().getBalances()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                    BBLog.d(LOG_TAG, "Balances Fetched!");
                    mBalances = response;
                    mBalancesFetched = true;
                    broadcastBalanceUpdate();

                    if (!Wallet.getInstance().mIsWalletReady) {
                        mBalancesFetched = true;
                        if (Wallet_Components.getInstance().mChannelsFetched) {
                            Wallet.getInstance().mIsWalletReady = true;
                            Wallet.getInstance().setWalletLoadState(Wallet.WalletLoadState.WALLET_LOADED);
                        }
                    }
                }, throwable -> BBLog.e(LOG_TAG, "Exception in fetch balance task: " + throwable.getMessage())));
    }


    /**
     * Notify all listeners to balance updates.
     */
    private void broadcastBalanceUpdate() {
        for (BalanceListener listener : mBalanceListeners) {
            listener.onBalanceUpdated();
        }
    }

    public void registerBalanceListener(BalanceListener listener) {
        mBalanceListeners.add(listener);
    }

    public void unregisterBalanceListener(BalanceListener listener) {
        mBalanceListeners.remove(listener);
    }

    public interface BalanceListener {
        void onBalanceUpdated();
    }
}