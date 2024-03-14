package app.michaelwuensch.bitbanana.wallet;


import android.os.Handler;

import com.github.lightningnetwork.lnd.lnrpc.ConnectPeerRequest;
import com.github.lightningnetwork.lnd.lnrpc.GetTransactionsRequest;
import com.github.lightningnetwork.lnd.lnrpc.Invoice;
import com.github.lightningnetwork.lnd.lnrpc.InvoiceSubscription;
import com.github.lightningnetwork.lnd.lnrpc.LightningAddress;
import com.github.lightningnetwork.lnd.lnrpc.ListInvoiceRequest;
import com.github.lightningnetwork.lnd.lnrpc.ListPaymentsRequest;
import com.github.lightningnetwork.lnd.lnrpc.NodeInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Payment;
import com.github.lightningnetwork.lnd.lnrpc.Transaction;
import com.github.lightningnetwork.lnd.walletrpc.UtxoLease;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.DebounceHandler;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Wallet_Components {

    private static final String LOG_TAG = Wallet_Components.class.getSimpleName();

    private static Wallet_Components mInstance = null;
    private final Set<HistoryListener> mHistoryListeners = new HashSet<>();
    private final Set<InvoiceSubscriptionListener> mInvoiceSubscriptionListeners = new HashSet<>();
    private final Set<TransactionSubscriptionListener> mTransactionSubscriptionListeners = new HashSet<>();
    private final Set<UtxoSubscriptionListener> mUtxoSubscriptionListeners = new HashSet<>();
    private final Set<PeerUpdateListener> mPeerUpdateListeners = new HashSet<>();

    public List<Transaction> mOnChainTransactionList;
    public List<Invoice> mInvoiceList;
    public List<Invoice> mTempInvoiceUpdateList;
    public List<Payment> mPaymentsList;
    public List<Utxo> mUTXOsList;
    public List<UtxoLease> mLockedUTXOsList;

    private boolean mTransactionUpdated = false;
    private boolean mInvoicesUpdated = false;
    private boolean mPaymentsUpdated = false;
    private boolean mUpdatingHistory = false;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Handler mHandler = new Handler();
    private final DebounceHandler mChannelsUpdateDebounceHandler = new DebounceHandler();

    private Wallet_Components() {
        ;
    }

    public static Wallet_Components getInstance() {

        if (mInstance == null) {
            mInstance = new Wallet_Components();
        }

        return mInstance;
    }

    /**
     * Use this to reset the wallet information when the wallet was switched.
     */
    public void reset() {
        mOnChainTransactionList = null;
        mInvoiceList = null;
        mTempInvoiceUpdateList = null;
        mPaymentsList = null;
        mUTXOsList = null;
        mLockedUTXOsList = null;
        mTransactionUpdated = false;
        mInvoicesUpdated = false;
        mPaymentsUpdated = false;
        mUpdatingHistory = false;

        compositeDisposable.clear();
        mHandler.removeCallbacksAndMessages(null);
        mChannelsUpdateDebounceHandler.shutdown();
    }


    /**
     * This will fetch all transaction history from LND.
     * After that the history is provided in lists that can be handled in a synchronized way.
     */
    public void fetchLNDTransactionHistory() {
        // Set all updated flags to false. This way we can determine later, when update is finished.

        if (!mUpdatingHistory) {
            mUpdatingHistory = true;
            mTransactionUpdated = false;
            mInvoicesUpdated = false;
            mPaymentsUpdated = false;

            fetchTransactionsFromLND();
            fetchInvoicesFromLND();
            fetchPaymentsFromLND();
        }
    }

    /**
     * This will fetch all lightning payment history from LND.
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

            fetchPaymentsFromLND();
        }
    }

    /**
     * This will fetch all on-chain transaction history from LND.
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

            fetchTransactionsFromLND();
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
     * This will fetch all On-Chain transactions involved with the current wallet from LND.
     */
    public void fetchTransactionsFromLND() {
        // fetch on-chain transactions
        compositeDisposable.add(LndConnection.getInstance().getLightningService().getTransactions(GetTransactionsRequest.newBuilder().build())
                .subscribe(transactionDetails -> {
                    mOnChainTransactionList = Lists.reverse(transactionDetails.getTransactionsList());

                    mTransactionUpdated = true;
                    isHistoryUpdateFinished();
                }, throwable -> BBLog.e(LOG_TAG, "Exception in transaction request task: " + throwable.getMessage())));
    }

    /**
     * This will fetch all lightning invoices from LND.
     */
    public void fetchInvoicesFromLND() {

        mTempInvoiceUpdateList = new LinkedList<>();

        fetchInvoicesFromLND(100);
    }

    private void fetchInvoicesFromLND(long lastIndex) {
        // Fetch lightning invoices
        ListInvoiceRequest invoiceRequest = ListInvoiceRequest.newBuilder()
                .setNumMaxInvoices(lastIndex)
                .build();

        compositeDisposable.add(LndConnection.getInstance().getLightningService().listInvoices(invoiceRequest)
                .subscribe(listInvoiceResponse -> {
                    mTempInvoiceUpdateList.addAll(listInvoiceResponse.getInvoicesList());

                    if (listInvoiceResponse.getLastIndexOffset() < lastIndex) {
                        // we have fetched all available invoices!
                        mInvoiceList = Lists.reverse(mTempInvoiceUpdateList);
                        mTempInvoiceUpdateList = null;
                        mInvoicesUpdated = true;
                        isHistoryUpdateFinished();
                    } else {
                        // there are still invoices to fetch, get the next batch!
                        fetchInvoicesFromLND(lastIndex + 100);
                    }
                }, throwable -> BBLog.e(LOG_TAG, "Exception in invoice request task: " + throwable.getMessage())));
    }

    /**
     * This will fetch lightning payments from LND.
     */
    public void fetchPaymentsFromLND() {
        // Fetch lightning payments
        ListPaymentsRequest paymentsRequest = ListPaymentsRequest.newBuilder()
                .setIncludeIncomplete(false)
                .build();

        compositeDisposable.add(LndConnection.getInstance().getLightningService().listPayments(paymentsRequest)
                .subscribe(listPaymentsResponse -> {
                    mPaymentsList = Lists.reverse(listPaymentsResponse.getPaymentsList());
                    mPaymentsUpdated = true;
                    isHistoryUpdateFinished();
                }, throwable -> BBLog.e(LOG_TAG, "Exception in payment request task: " + throwable.getMessage())));
    }

    public void connectPeer(LightningNodeUri nodeUri, boolean openChannel, long amount, int targetConf, boolean isPrivate) {
        if (nodeUri.getHost() == null || nodeUri.getHost().isEmpty()) {
            BBLog.d(LOG_TAG, "Host info missing. Trying to fetch host info to connect peer...");
            fetchNodeInfoToConnectPeer(nodeUri, openChannel, amount, targetConf, isPrivate);
            return;
        }

        LightningAddress lightningAddress = LightningAddress.newBuilder()
                .setHostBytes(ByteString.copyFrom(nodeUri.getHost().getBytes(StandardCharsets.UTF_8)))
                .setPubkeyBytes(ByteString.copyFrom(nodeUri.getPubKey().getBytes(StandardCharsets.UTF_8))).build();
        ConnectPeerRequest connectPeerRequest = ConnectPeerRequest.newBuilder().setAddr(lightningAddress).build();

        compositeDisposable.add(LndConnection.getInstance().getLightningService().connectPeer(connectPeerRequest)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .subscribe(connectPeerResponse -> {
                    BBLog.d(LOG_TAG, "Successfully connected to peer, trying to open channel...");
                    broadcastPeerConnectedEvent();
                    if (openChannel) {
                        Wallet_Channels.getInstance().openChannelConnected(nodeUri, amount, targetConf, isPrivate);
                    }
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error connecting to peer: " + throwable.getMessage());

                    if (throwable.getMessage().toLowerCase().contains("refused")) {
                        Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_REFUSED, throwable.getMessage());
                    } else if (throwable.getMessage().toLowerCase().contains("self")) {
                        Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_SELF, throwable.getMessage());
                    } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_TIMEOUT, throwable.getMessage());
                    } else {
                        Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION, throwable.getMessage());
                    }
                }));
    }

    public void fetchNodeInfoToConnectPeer(LightningNodeUri nodeUri, boolean openChannel, long amount, int targetConf, boolean isPrivate) {
        NodeInfoRequest nodeInfoRequest = NodeInfoRequest.newBuilder()
                .setPubKey(nodeUri.getPubKey())
                .build();

        compositeDisposable.add(LndConnection.getInstance().getLightningService().getNodeInfo(nodeInfoRequest)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .subscribe(nodeInfo -> {
                    if (nodeInfo.getNode().getAddressesCount() > 0) {
                        String tempUri = nodeUri.getPubKey() + "@" + nodeInfo.getNode().getAddresses(0).getAddr();
                        LightningNodeUri nodeUriWithHost = LightningNodeUriParser.parseNodeUri(tempUri);
                        if (nodeUriWithHost != null) {
                            BBLog.d(LOG_TAG, "Host info successfully fetched. NodeUriWithHost: " + nodeUriWithHost.getAsString());
                            connectPeer(nodeUriWithHost, openChannel, amount, targetConf, isPrivate);
                        } else {
                            BBLog.d(LOG_TAG, "Failed to parse nodeUri");
                            Wallet_Channels.getInstance().broadcastChannelOpenUpdate(nodeUri, Wallet_Channels.ChannelOpenUpdateListener.ERROR_CONNECTION_NO_HOST, null);
                        }
                    } else {
                        BBLog.d(LOG_TAG, "Node Info does not contain any addresses.");
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
    public void fetchNodeInfoFromLND(String pubkey, boolean lastNode, boolean saveAliasToCache, NodeInfoFetchedListener listener) {
        compositeDisposable.add(BackendManager.api().getNodeInfo(pubkey)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
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
            compositeDisposable.add(BackendManager.getCurrentBackend().api().getUTXOs(Wallet.getInstance().getCurrentNodeInfo().getBlockHeight())
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
     * Use this to subscribe the wallet to transaction events that happen on LND.
     * The events will be captured and forwarded to the TransactionSubscriptionListener.
     * All parts of the App that want to react on transaction events have to subscribe to the
     * TransactionSubscriptionListener.
     */
    public void subscribeToTransactions() {
        compositeDisposable.add(LndConnection.getInstance().getLightningService().subscribeTransactions(GetTransactionsRequest.newBuilder().build())
                .subscribe(transaction -> {
                    BBLog.d(LOG_TAG, "Received transaction subscription event.");
                    fetchTransactionsFromLND(); // update internal transaction list
                    Wallet_Balance.getInstance().fetchBalancesWithDebounce(); // Always update balances if a transaction event occurs.
                    broadcastTransactionUpdate(transaction);
                }));
    }

    public void cancelSubscriptions() {
        compositeDisposable.clear();
    }

    /**
     * Use this to subscribe the wallet to invoice events that happen on LND.
     * The events will be captured and forwarded to the InvoiceSubscriptionListener.
     * All parts of the App that want to react on invoice events have to subscribe to the
     * InvoiceSubscriptionListener.
     */
    public void subscribeToInvoices() {
        compositeDisposable.add(LndConnection.getInstance().getLightningService().subscribeInvoices(InvoiceSubscription.newBuilder().build())
                .subscribe(invoice -> {
                    BBLog.d(LOG_TAG, "Received invoice subscription event.");

                    // is this a new invoice or is an old one updated?
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

                            // Inform all subscribers
                            broadcastInvoiceUpdated(invoice);
                        }
                    } else {
                        // this is a new one
                        mInvoiceList = new LinkedList<>();
                        mInvoiceList.add(invoice);
                        broadcastInvoiceAdded(invoice);
                    }
                }));
    }

    /**
     * Returns if the invoice has been payed already.
     *
     * @param invoice
     * @return
     */
    public boolean isInvoicePayed(Invoice invoice) {
        boolean payed;
        if (invoice.getValue() == 0) {
            payed = invoice.getAmtPaidSat() != 0;
        } else {
            payed = invoice.getValue() <= invoice.getAmtPaidSat();
        }
        return payed;
    }

    /**
     * Returns if the invoice has been expired. This function just checks if the expiration date is in the past.
     * It will also return expired for already payed invoices.
     *
     * @param invoice
     * @return
     */
    public boolean isInvoiceExpired(Invoice invoice) {
        return invoice.getCreationDate() + invoice.getExpiry() < System.currentTimeMillis() / 1000;
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
    private void broadcastInvoiceAdded(Invoice invoice) {
        for (InvoiceSubscriptionListener listener : mInvoiceSubscriptionListeners) {
            listener.onNewInvoiceAdded(invoice);
        }
    }

    /**
     * Notify all listeners about updated invoice.
     *
     * @param invoice the updated invoice
     */
    private void broadcastInvoiceUpdated(Invoice invoice) {
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
    private void broadcastTransactionUpdate(Transaction transaction) {
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
        void onNewInvoiceAdded(Invoice invoice);

        void onExistingInvoiceUpdated(Invoice invoice);
    }

    public interface TransactionSubscriptionListener {
        void onTransactionEvent(Transaction transaction);
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