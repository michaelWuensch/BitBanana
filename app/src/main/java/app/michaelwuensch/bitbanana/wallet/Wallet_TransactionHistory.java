package app.michaelwuensch.bitbanana.wallet;


import android.widget.Toast;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Wallet_TransactionHistory {

    private static final String LOG_TAG = Wallet_TransactionHistory.class.getSimpleName();

    private static Wallet_TransactionHistory mInstance = null;
    private final Set<HistoryListener> mHistoryListeners = new HashSet<>();
    private final Set<InvoiceSubscriptionListener> mInvoiceSubscriptionListeners = new HashSet<>();
    private final Set<TransactionSubscriptionListener> mTransactionSubscriptionListeners = new HashSet<>();
    private final Set<UtxoSubscriptionListener> mUtxoSubscriptionListeners = new HashSet<>();
    private final Set<PeerUpdateListener> mPeerUpdateListeners = new HashSet<>();

    private List<OnChainTransaction> mOnChainTransactionList;
    private List<LnInvoice> mInvoiceList;
    private List<LnPayment> mPaymentsList;
    public List<Utxo> mUTXOsList;


    private boolean mTransactionUpdated = false;
    private boolean mInvoicesUpdated = false;
    private boolean mPaymentsUpdated = false;
    private boolean mUpdatingHistory = false;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private Wallet_TransactionHistory() {
        ;
    }

    public static Wallet_TransactionHistory getInstance() {

        if (mInstance == null) {
            mInstance = new Wallet_TransactionHistory();
        }

        return mInstance;
    }

    /**
     * Use this to reset the wallet information when the wallet was switched.
     */
    public void reset() {
        mOnChainTransactionList = null;
        mInvoiceList = null;
        mPaymentsList = null;
        mUTXOsList = null;
        mTransactionUpdated = false;
        mInvoicesUpdated = false;
        mPaymentsUpdated = false;
        mUpdatingHistory = false;

        compositeDisposable.clear();
    }


    /**
     * This will fetch all transaction history from the node.
     * After that the history is provided in lists that can be handled in a synchronized way.
     */
    public void fetchTransactionHistory() {
        // Set all updated flags to false. This way we can determine later, when update is finished.

        if (!mUpdatingHistory) {
            mUpdatingHistory = true;
            mTransactionUpdated = false;
            mInvoicesUpdated = false;
            mPaymentsUpdated = false;

            fetchOnChainTransactions();
            fetchInvoicesList();
            fetchPayments();
        }
    }

    /**
     * This will fetch all lightning payment history from the node.
     * After that the history is provided in lists that can be handled in a synchronized way.
     * <p>
     * This will need less bandwidth than updating all history and can be called when a lightning
     * payment was successful.
     */
    public void updateLightningPaymentHistory() {
        // Set payment update flags to false. This way we can determine later, when update is finished.

        if (!mUpdatingHistory) {
            mUpdatingHistory = true;
            mPaymentsUpdated = false;

            fetchPayments();
        }
    }

    public List<LnInvoice> getInvoiceList() {
        return mInvoiceList;
    }

    public List<LnPayment> getPaymentsList() {
        return mPaymentsList;
    }

    public List<OnChainTransaction> getOnChainTransactionList() {
        return mOnChainTransactionList;
    }

    /**
     * This will fetch all on-chain transaction history from the node.
     * After that the history is provided in lists that can be handled in a synchronized way.
     * <p>
     * This will need less bandwidth than updating all history and can be called when a lightning
     * payment was successful.
     */
    public void updateOnChainTransactionHistory() {
        // Set payment update flags to false. This way we can determine later, when update is finished.

        if (!mUpdatingHistory) {
            mUpdatingHistory = true;
            mTransactionUpdated = false;

            fetchOnChainTransactions();
        }
    }

    /**
     * checks if the history update is finished and then broadcast an update to all registered classes.
     */
    private void isHistoryUpdateFinished() {
        if (mTransactionUpdated && mInvoicesUpdated && mPaymentsUpdated) {
            mUpdatingHistory = false;
            broadcastHistoryUpdate();
        }
    }

    /**
     * This will fetch all On-Chain transactions involved with the current wallet from the node.
     */
    public void fetchOnChainTransactions() {
        compositeDisposable.add(BackendManager.api().listOnChainTransactions()
                .subscribe(response -> {
                    mOnChainTransactionList = response;
                    mTransactionUpdated = true;
                    isHistoryUpdateFinished();
                }, throwable -> BBLog.e(LOG_TAG, "Exception in transaction request task: " + throwable.getMessage())));
    }

    /**
     * This will fetch all lightning invoices from the node.
     */
    public void fetchInvoicesList() {
        compositeDisposable.add(BackendManager.api().listInvoices(0, 500)
                .subscribe(response -> {
                    mInvoiceList = Lists.reverse(response); // we want most recent on top.
                    mInvoicesUpdated = true;
                    isHistoryUpdateFinished();
                }, throwable -> BBLog.e(LOG_TAG, "Exception in invoice request task: " + throwable.getMessage())));
    }

    /**
     * This will fetch lightning payments from the node.
     */
    public void fetchPayments() {
        compositeDisposable.add(BackendManager.api().listLnPayments(0, 500)
                .subscribe(response -> {
                    mPaymentsList = Lists.reverse(response); // we want most recent on top.
                    mPaymentsUpdated = true;
                    isHistoryUpdateFinished();
                }, throwable -> BBLog.e(LOG_TAG, "Exception in fetch payments task: " + throwable.getMessage())));
    }

    public void connectPeer(LightningNodeUri nodeUri, boolean openChannel, long amount, int targetConf, boolean isPrivate) {
        if (nodeUri.getHost() == null || nodeUri.getHost().isEmpty()) {
            BBLog.d(LOG_TAG, "Host info missing. Trying to fetch host info to connect peer...");
            fetchNodeInfoToConnectPeer(nodeUri, openChannel, amount, targetConf, isPrivate);
            return;
        }

        compositeDisposable.add(BackendManager.api().connectPeer(nodeUri)
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribe(() -> {
                    BBLog.d(LOG_TAG, "Successfully connected to peer.");
                    broadcastPeerConnectedEvent();
                    if (openChannel) {
                        BBLog.d(LOG_TAG, "Now that we are connected to peer, trying to open channel...");
                        Wallet_Channels.getInstance().openChannelConnected(nodeUri, amount, targetConf, isPrivate);
                    }
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error connecting to peer: " + throwable.getMessage());

                    if (openChannel) {
                        if (throwable.getMessage().toLowerCase().contains("refused")) {
                            Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_REFUSED, throwable.getMessage());
                        } else if (throwable.getMessage().toLowerCase().contains("self")) {
                            Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_SELF, throwable.getMessage());
                        } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
                            Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_TIMEOUT, throwable.getMessage());
                        } else {
                            Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION, throwable.getMessage());
                        }
                    } else {
                        Toast.makeText(App.getAppContext(), "Connecting peer failed!" + " " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
    }

    public void fetchNodeInfoToConnectPeer(LightningNodeUri nodeUri, boolean openChannel, long amount, int targetConf, boolean isPrivate) {
        compositeDisposable.add(BackendManager.api().getNodeInfo(nodeUri.getPubKey())
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribe(response -> {
                    if (response.getAddresses().size() > 0) {
                        String tempUri = nodeUri.getPubKey() + "@" + response.getAddresses().get(0);
                        LightningNodeUri nodeUriWithHost = LightningNodeUriParser.parseNodeUri(tempUri);
                        if (nodeUriWithHost != null) {
                            BBLog.d(LOG_TAG, "Host info successfully fetched. NodeUriWithHost: " + nodeUriWithHost.getAsString());
                            connectPeer(nodeUriWithHost, openChannel, amount, targetConf, isPrivate);
                        } else {
                            BBLog.d(LOG_TAG, "Failed to parse nodeUri");
                            Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_NO_HOST, null);
                        }
                    } else {
                        BBLog.w(LOG_TAG, "Node Info does not contain any addresses.");
                        Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_NO_HOST, null);
                    }
                }, throwable -> {
                    BBLog.w(LOG_TAG, "Fetching host info failed. Exception in get node info (" + nodeUri.getPubKey() + ") request task: " + throwable.getMessage());
                    Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_NO_HOST, null);
                }));
    }

    /**
     * This will fetch the NodeInfo according to the supplied pubkey.
     * For now we are just interested in the alias.
     * The AliasManager takes care of the result so we can use aliases in a non async way.
     */
    public void fetchNodeInfo(String pubkey, boolean lastNode, boolean saveAliasToCache, NodeInfoFetchedListener listener) {
        compositeDisposable.add(BackendManager.api().getNodeInfo(pubkey)
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribe(response -> {
                    BBLog.v(LOG_TAG, "Fetched Node info from " + response.getAlias());
                    AliasManager.getInstance().saveAlias(response.getPubKey(), response.getAlias());

                    if (lastNode) {
                        AliasManager.getInstance().saveAliasesToCache();
                        Wallet_Channels.getInstance().broadcastChannelsUpdated();
                    } else {
                        if (saveAliasToCache)
                            AliasManager.getInstance().saveAliasesToCache();
                    }
                    if (listener != null) {
                        listener.onNodeInfoFetched(response.getPubKey());
                    }
                }, throwable -> {
                    if (AliasManager.getInstance().hasAliasInfo(pubkey)) {
                        // Prevents requesting nodeInfo for the unavailable node to often
                        AliasManager.getInstance().updateTimestampForAlias(pubkey);
                    } else {
                        // Prevents requesting nodeInfo for the unavailable node to often
                        AliasManager.getInstance().saveAlias(pubkey, pubkey);
                    }
                    if (lastNode) {
                        AliasManager.getInstance().saveAliasesToCache();
                        Wallet_Channels.getInstance().broadcastChannelsUpdated();
                    } else {
                        if (saveAliasToCache)
                            AliasManager.getInstance().saveAliasesToCache();
                    }
                    BBLog.w(LOG_TAG, "Exception in get node info (" + pubkey + ") request task: " + throwable.getMessage());
                }));
    }


    public void fetchUTXOs() {
        if (Wallet.getInstance().isInfoFetched()) {
            compositeDisposable.add(BackendManager.getCurrentBackend().api().listUTXOs(Wallet.getInstance().getCurrentNodeInfo().getBlockHeight())
                    .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                    .subscribe(response -> {
                                mUTXOsList = response;
                                broadcastUtxoListUpdated();
                            }
                            , throwable -> {
                                BBLog.w(LOG_TAG, "Fetching utxo list failed: " + throwable.getMessage());
                            }));
        } else {
            BBLog.w(LOG_TAG, "Fetching utxo list failed. Block height is not yet fetched.");
        }
    }

    /**
     * Use this to subscribe the wallet to transaction events that happen on the node.
     * The events will be captured and forwarded to the TransactionSubscriptionListener.
     * All parts of the App that want to react on transaction events have to subscribe to the
     * TransactionSubscriptionListener.
     */
    public void subscribeToTransactions() {
        compositeDisposable.add(BackendManager.api().subscribeToOnChainTransactions()
                .subscribe(transaction -> {
                    BBLog.d(LOG_TAG, "Received transaction subscription event.");
                    fetchOnChainTransactions(); // update internal transaction list
                    Wallet_Balance.getInstance().fetchBalancesWithDebounce(); // Always update balances if a transaction event occurs.
                    broadcastTransactionUpdate(transaction);
                }));
    }

    public void cancelSubscriptions() {
        compositeDisposable.clear();
    }

    /**
     * Use this to subscribe the wallet to invoice events that happen on the node.
     * The events will be captured and forwarded to the InvoiceSubscriptionListener.
     * All parts of the App that want to react on invoice events have to subscribe to the
     * InvoiceSubscriptionListener.
     */
    public void subscribeToInvoices() {
        compositeDisposable.add(BackendManager.api().subscribeToInvoices()
                .subscribe(invoice -> {
                    BBLog.d(LOG_TAG, "Received invoice subscription event.");

                    if (mInvoiceList != null) {
                        if (invoice.getAddIndex() > mInvoiceList.get(0).getAddIndex()) {
                            // this is a new one
                            mInvoiceList.add(0, invoice);
                            broadcastInvoiceAdded(invoice);
                        } else {
                            // this is an update
                            // Find out which element has to be replaced
                            int changeIndex = -1;
                            for (int i = 0; i < mInvoiceList.size() - 1; i++) {
                                if (mInvoiceList.get(i).getAddIndex() == invoice.getAddIndex()) {
                                    changeIndex = i;
                                    break;
                                }
                            }

                            // Replace it
                            if (changeIndex >= 0) {
                                mInvoiceList.set(changeIndex, invoice);
                            }

                            broadcastInvoiceUpdated(invoice);
                        }
                    } else {
                        mInvoiceList = new ArrayList<>();
                        mInvoiceList.add(invoice);
                        broadcastInvoiceAdded(invoice);
                    }
                }));
    }

    /**
     * Notify all listeners to history updates.
     */
    private void broadcastHistoryUpdate() {
        for (HistoryListener listener : mHistoryListeners) {
            listener.onHistoryUpdated();
        }
    }

    public void registerHistoryListener(HistoryListener listener) {
        mHistoryListeners.add(listener);
    }

    public void unregisterHistoryListener(HistoryListener listener) {
        mHistoryListeners.remove(listener);
    }


    /**
     * Notify all listeners about new invoice.
     *
     * @param invoice the new invoice
     */
    private void broadcastInvoiceAdded(LnInvoice invoice) {
        for (InvoiceSubscriptionListener listener : mInvoiceSubscriptionListeners) {
            listener.onNewInvoiceAdded(invoice);
        }
    }

    /**
     * Notify all listeners about updated invoice.
     *
     * @param invoice the updated invoice
     */
    private void broadcastInvoiceUpdated(LnInvoice invoice) {
        for (InvoiceSubscriptionListener listener : mInvoiceSubscriptionListeners) {
            listener.onExistingInvoiceUpdated(invoice);
        }
    }

    public void registerInvoiceSubscriptionListener(InvoiceSubscriptionListener listener) {
        mInvoiceSubscriptionListeners.add(listener);
    }

    public void unregisterInvoiceSubscriptionListener(InvoiceSubscriptionListener listener) {
        mInvoiceSubscriptionListeners.remove(listener);
    }


    /**
     * Notify all listeners to transaction update.
     *
     * @param transaction the details about the transaction update
     */
    private void broadcastTransactionUpdate(OnChainTransaction transaction) {
        for (TransactionSubscriptionListener listener : mTransactionSubscriptionListeners) {
            listener.onTransactionEvent(transaction);
        }
    }

    public void registerTransactionSubscriptionListener(TransactionSubscriptionListener listener) {
        mTransactionSubscriptionListeners.add(listener);
    }

    public void unregisterTransactionSubscriptionListener(TransactionSubscriptionListener listener) {
        mTransactionSubscriptionListeners.remove(listener);
    }

    /**
     * Notify all listeners to utxo list update.
     */
    private void broadcastUtxoListUpdated() {
        for (UtxoSubscriptionListener listener : mUtxoSubscriptionListeners) {
            listener.onUtxoListUpdated();
        }
    }

    public void registerUtxoSubscriptionListener(UtxoSubscriptionListener listener) {
        mUtxoSubscriptionListeners.add(listener);
    }

    public void unregisterUtxoSubscriptionListener(UtxoSubscriptionListener listener) {
        mUtxoSubscriptionListeners.remove(listener);
    }

    private void broadcastPeerConnectedEvent() {
        for (PeerUpdateListener listener : mPeerUpdateListeners) {
            listener.onConnectedToPeer();
        }
    }

    public void registerPeerUpdateListener(PeerUpdateListener listener) {
        mPeerUpdateListeners.add(listener);
    }

    public void unregisterPeerUpdateListener(PeerUpdateListener listener) {
        mPeerUpdateListeners.remove(listener);
    }

    public interface HistoryListener {
        void onHistoryUpdated();
    }

    public interface InvoiceSubscriptionListener {
        void onNewInvoiceAdded(LnInvoice invoice);

        void onExistingInvoiceUpdated(LnInvoice invoice);
    }

    public interface TransactionSubscriptionListener {
        void onTransactionEvent(OnChainTransaction transaction);
    }

    public interface UtxoSubscriptionListener {
        void onUtxoListUpdated();
    }

    public interface NodeInfoFetchedListener {
        void onNodeInfoFetched(String pubkey);
    }

    public interface PeerUpdateListener {
        void onConnectedToPeer();
    }
}