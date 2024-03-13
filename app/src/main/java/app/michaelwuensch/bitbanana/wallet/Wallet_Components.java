package app.michaelwuensch.bitbanana.wallet;


import android.os.Handler;

import com.github.lightningnetwork.lnd.lnrpc.ChanBackupSnapshot;
import com.github.lightningnetwork.lnd.lnrpc.Channel;
import com.github.lightningnetwork.lnd.lnrpc.ChannelBackupSubscription;
import com.github.lightningnetwork.lnd.lnrpc.ChannelCloseSummary;
import com.github.lightningnetwork.lnd.lnrpc.ChannelEventSubscription;
import com.github.lightningnetwork.lnd.lnrpc.ChannelEventUpdate;
import com.github.lightningnetwork.lnd.lnrpc.ChannelPoint;
import com.github.lightningnetwork.lnd.lnrpc.CloseChannelRequest;
import com.github.lightningnetwork.lnd.lnrpc.ClosedChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.ClosedChannelsResponse;
import com.github.lightningnetwork.lnd.lnrpc.ConnectPeerRequest;
import com.github.lightningnetwork.lnd.lnrpc.GetTransactionsRequest;
import com.github.lightningnetwork.lnd.lnrpc.Invoice;
import com.github.lightningnetwork.lnd.lnrpc.InvoiceSubscription;
import com.github.lightningnetwork.lnd.lnrpc.LightningAddress;
import com.github.lightningnetwork.lnd.lnrpc.ListChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.ListChannelsResponse;
import com.github.lightningnetwork.lnd.lnrpc.ListInvoiceRequest;
import com.github.lightningnetwork.lnd.lnrpc.ListPaymentsRequest;
import com.github.lightningnetwork.lnd.lnrpc.ListPeersRequest;
import com.github.lightningnetwork.lnd.lnrpc.NodeInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.OpenChannelRequest;
import com.github.lightningnetwork.lnd.lnrpc.Payment;
import com.github.lightningnetwork.lnd.lnrpc.Peer;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse;
import com.github.lightningnetwork.lnd.lnrpc.PreviousOutPoint;
import com.github.lightningnetwork.lnd.lnrpc.Resolution;
import com.github.lightningnetwork.lnd.lnrpc.Transaction;
import com.github.lightningnetwork.lnd.routerrpc.HtlcEvent;
import com.github.lightningnetwork.lnd.routerrpc.SubscribeHtlcEventsRequest;
import com.github.lightningnetwork.lnd.walletrpc.UtxoLease;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.DebounceHandler;
import app.michaelwuensch.bitbanana.util.HexUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Wallet_Components {

    private static final String LOG_TAG = Wallet_Components.class.getSimpleName();

    private static Wallet_Components mInstance = null;
    private final Set<HistoryListener> mHistoryListeners = new HashSet<>();
    private final Set<InvoiceSubscriptionListener> mInvoiceSubscriptionListeners = new HashSet<>();
    private final Set<TransactionSubscriptionListener> mTransactionSubscriptionListeners = new HashSet<>();
    private final Set<ChannelEventSubscriptionListener> mChannelEventSubscriptionListeners = new HashSet<>();
    private final Set<ChannelsUpdatedSubscriptionListener> mChannelsUpdatedSubscriptionListeners = new HashSet<>();
    private final Set<ChannelBackupSubscriptionListener> mChannelBackupSubscriptionListeners = new HashSet<>();
    private final Set<ChannelCloseUpdateListener> mChannelCloseUpdateListeners = new HashSet<>();
    private final Set<ChannelOpenUpdateListener> mChannelOpenUpdateListeners = new HashSet<>();
    private final Set<HtlcSubscriptionListener> mHtlcSubscriptionListeners = new HashSet<>();
    private final Set<UtxoSubscriptionListener> mUtxoSubscriptionListeners = new HashSet<>();
    private final Set<PeerUpdateListener> mPeerUpdateListeners = new HashSet<>();

    public List<Transaction> mOnChainTransactionList;
    public List<Invoice> mInvoiceList;
    public List<Invoice> mTempInvoiceUpdateList;
    public List<Payment> mPaymentsList;
    public List<Channel> mOpenChannelsList;
    public List<PendingChannelsResponse.PendingOpenChannel> mPendingOpenChannelsList;
    public List<PendingChannelsResponse.ClosedChannel> mPendingClosedChannelsList;
    public List<PendingChannelsResponse.ForceClosedChannel> mPendingForceClosedChannelsList;
    public List<PendingChannelsResponse.WaitingCloseChannel> mPendingWaitingCloseChannelsList;
    public List<ChannelCloseSummary> mClosedChannelsList;
    public List<Utxo> mUTXOsList;
    public List<UtxoLease> mLockedUTXOsList;


    public boolean mChannelsFetched = false;
    private boolean mTransactionUpdated = false;
    private boolean mInvoicesUpdated = false;
    private boolean mPaymentsUpdated = false;
    private boolean mUpdatingHistory = false;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Handler mHandler = new Handler();
    private DebounceHandler mChannelsUpdateDebounceHandler = new DebounceHandler();

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
        compositeDisposable.clear();

        mOnChainTransactionList = null;
        mInvoiceList = null;
        mTempInvoiceUpdateList = null;
        mPaymentsList = null;
        mOpenChannelsList = null;
        mPendingOpenChannelsList = null;
        mPendingClosedChannelsList = null;
        mPendingForceClosedChannelsList = null;
        mPendingWaitingCloseChannelsList = null;
        mUTXOsList = null;
        mLockedUTXOsList = null;
        mTransactionUpdated = false;
        mInvoicesUpdated = false;
        mPaymentsUpdated = false;
        mUpdatingHistory = false;
        mChannelsFetched = false;

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

    public void openChannel(LightningNodeUri nodeUri, long amount, int targetConf, boolean isPrivate) {
        compositeDisposable.add(LndConnection.getInstance().getLightningService().listPeers(ListPeersRequest.newBuilder().build())
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .subscribe(listPeersResponse -> {
                    boolean connected = false;
                    for (Peer node : listPeersResponse.getPeersList()) {
                        if (node.getPubKey().equals(nodeUri.getPubKey())) {
                            connected = true;
                            break;
                        }
                    }

                    if (connected) {
                        BBLog.d(LOG_TAG, "Already connected to peer, trying to open channel...");
                        openChannelConnected(nodeUri, amount, targetConf, isPrivate);
                    } else {
                        BBLog.d(LOG_TAG, "Not connected to peer, trying to connect...");
                        connectPeer(nodeUri, true, amount, targetConf, isPrivate);
                    }
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error listing peers request: " + throwable.getMessage());
                    if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_GET_PEERS_TIMEOUT, throwable.getMessage());
                    } else {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_GET_PEERS, throwable.getMessage());
                    }
                }));
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
                        openChannelConnected(nodeUri, amount, targetConf, isPrivate);
                    }
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error connecting to peer: " + throwable.getMessage());

                    if (throwable.getMessage().toLowerCase().contains("refused")) {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CONNECTION_REFUSED, throwable.getMessage());
                    } else if (throwable.getMessage().toLowerCase().contains("self")) {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CONNECTION_SELF, throwable.getMessage());
                    } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CONNECTION_TIMEOUT, throwable.getMessage());
                    } else {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CONNECTION, throwable.getMessage());
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
                            broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CONNECTION_NO_HOST, null);
                        }
                    } else {
                        BBLog.d(LOG_TAG, "Node Info does not contain any addresses.");
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CONNECTION_NO_HOST, null);
                    }
                }, throwable -> {
                    BBLog.w(LOG_TAG, "Fetching host info failed. Exception in get node info (" + nodeUri.getPubKey() + ") request task: " + throwable.getMessage());
                    broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CONNECTION_NO_HOST, null);
                }));
    }

    private void openChannelConnected(LightningNodeUri nodeUri, long amount, int targetConf, boolean isPrivate) {
        byte[] nodeKeyBytes = HexUtil.hexToBytes(nodeUri.getPubKey());
        OpenChannelRequest openChannelRequest = OpenChannelRequest.newBuilder()
                .setNodePubkey(ByteString.copyFrom(nodeKeyBytes))
                .setTargetConf(targetConf)
                .setPrivate(isPrivate)
                .setLocalFundingAmount(amount)
                .build();

        compositeDisposable.add(LndConnection.getInstance().getLightningService().openChannel(openChannelRequest)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .firstOrError()
                .subscribe(openStatusUpdate -> {
                    BBLog.d(LOG_TAG, "Open channel update: " + openStatusUpdate.getUpdateCase().getNumber());
                    broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.SUCCESS, null);
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error opening channel: " + throwable.getMessage());

                    if (throwable.getMessage().toLowerCase().contains("pending channels exceed maximum")) {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CHANNEL_PENDING_MAX, throwable.getMessage());
                    } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CHANNEL_TIMEOUT, throwable.getMessage());
                    } else {
                        broadcastChannelOpenUpdate(nodeUri, ChannelOpenUpdateListener.ERROR_CHANNEL_OPEN, throwable.getMessage());
                    }
                }));
    }

    public void closeChannel(String channelPoint, boolean force) {
        ChannelPoint point = ChannelPoint.newBuilder()
                .setFundingTxidStr(channelPoint.substring(0, channelPoint.indexOf(':')))
                .setOutputIndex(Character.getNumericValue(channelPoint.charAt(channelPoint.length() - 1)))
                .build();

        CloseChannelRequest closeChannelRequest = CloseChannelRequest.newBuilder()
                .setChannelPoint(point)
                .setForce(force)
                .build();

        compositeDisposable.add(LndConnection.getInstance().getLightningService().closeChannel(closeChannelRequest)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .firstOrError()
                .subscribe(closeStatusUpdate -> {
                    BBLog.d(LOG_TAG, "Closing channel update: " + closeStatusUpdate.getUpdateCase().getNumber());
                    broadcastChannelCloseUpdate(channelPoint, ChannelCloseUpdateListener.SUCCESS, null);
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error closing channel: " + throwable.getMessage());
                    if (throwable.getMessage().toLowerCase().contains("offline")) {
                        broadcastChannelCloseUpdate(channelPoint, ChannelCloseUpdateListener.ERROR_PEER_OFFLINE, throwable.getMessage());
                    } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        broadcastChannelCloseUpdate(channelPoint, ChannelCloseUpdateListener.ERROR_CHANNEL_TIMEOUT, throwable.getMessage());
                    } else {
                        broadcastChannelCloseUpdate(channelPoint, ChannelCloseUpdateListener.ERROR_CHANNEL_CLOSE, throwable.getMessage());
                    }
                }));
    }

    public void fetchChannelsFromLND() {
        Single<ListChannelsResponse> listChannelsObservable = LndConnection.getInstance().getLightningService().listChannels(ListChannelsRequest.newBuilder().build());
        Single<PendingChannelsResponse> pendingChannelsObservable = LndConnection.getInstance().getLightningService().pendingChannels(PendingChannelsRequest.newBuilder().build());
        Single<ClosedChannelsResponse> closedChannelsObservable = LndConnection.getInstance().getLightningService().closedChannels(ClosedChannelsRequest.newBuilder().build());

        compositeDisposable.add(Single.zip(listChannelsObservable, pendingChannelsObservable, closedChannelsObservable, (listChannelsResponse, pendingChannelsResponse, closedChannelsResponse) -> {

            mOpenChannelsList = listChannelsResponse.getChannelsList();
            mClosedChannelsList = closedChannelsResponse.getChannelsList();
            mPendingOpenChannelsList = pendingChannelsResponse.getPendingOpenChannelsList();
            mPendingClosedChannelsList = pendingChannelsResponse.getPendingClosingChannelsList();
            mPendingForceClosedChannelsList = pendingChannelsResponse.getPendingForceClosingChannelsList();
            mPendingWaitingCloseChannelsList = pendingChannelsResponse.getWaitingCloseChannelsList();

            // Load NodeInfos for all involved nodes. This allows us to display aliases later.
            Set<String> channelNodes = new HashSet<>();

            for (Channel c : mOpenChannelsList) {
                if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getRemotePubkey()))
                    channelNodes.add(c.getRemotePubkey());
            }
            for (PendingChannelsResponse.PendingOpenChannel c : mPendingOpenChannelsList) {
                if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getChannel().getRemoteNodePub()))
                    channelNodes.add(c.getChannel().getRemoteNodePub());
            }
            for (PendingChannelsResponse.ClosedChannel c : mPendingClosedChannelsList) {
                if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getChannel().getRemoteNodePub()))
                    channelNodes.add(c.getChannel().getRemoteNodePub());
            }
            for (PendingChannelsResponse.ForceClosedChannel c : mPendingForceClosedChannelsList) {
                if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getChannel().getRemoteNodePub()))
                    channelNodes.add(c.getChannel().getRemoteNodePub());
            }
            for (PendingChannelsResponse.WaitingCloseChannel c : mPendingWaitingCloseChannelsList) {
                if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getChannel().getRemoteNodePub()))
                    channelNodes.add(c.getChannel().getRemoteNodePub());
            }

            for (ChannelCloseSummary c : mClosedChannelsList) {
                if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getRemotePubkey()))
                    channelNodes.add(c.getRemotePubkey());
            }

            // Delay each NodeInfo request for 100ms to not stress LND
            ArrayList<String> channelNodesList = new ArrayList<>(channelNodes);
            BBLog.d(LOG_TAG, "Fetching node info for " + channelNodesList.size() + " nodes.");

            compositeDisposable.add(Observable.range(0, channelNodesList.size())
                    .concatMap(i -> Observable.just(i).delay(100, TimeUnit.MILLISECONDS))
                    .doOnNext(integer -> fetchNodeInfoFromLND(channelNodesList.get(integer), integer == channelNodesList.size() - 1, true, null))
                    .subscribe());

            if (channelNodesList.size() == 0) {
                broadcastChannelsUpdated();
            }

            return true;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(aBoolean -> {
            // Zip executed without error
            BBLog.d(LOG_TAG, "Channels fetched!");
            if (!Wallet.getInstance().mIsWalletReady) {
                mChannelsFetched = true;
                if (Wallet_Balance.getInstance().mBalancesFetched) {
                    Wallet.getInstance().mIsWalletReady = true;
                    Wallet.getInstance().setWalletLoadState(Wallet.WalletLoadState.WALLET_LOADED);
                }
            }
        }, throwable -> BBLog.e(LOG_TAG, "Exception in get channels info request task: " + throwable.getMessage())));
    }

    /**
     * This will fetch the NodeInfo according to the supplied pubkey.
     * The NodeInfo will then be added to the mNodeInfos list (no duplicates) which can then
     * be used for non async tasks, such as getting the aliases for channels.
     *
     * @param pubkey
     */
    public void fetchNodeInfoFromLND(String pubkey, boolean lastNode, boolean saveAliasToCache, NodeInfoFetchedListener listener) {
        NodeInfoRequest nodeInfoRequest = NodeInfoRequest.newBuilder()
                .setPubKey(pubkey)
                .build();

        compositeDisposable.add(LndConnection.getInstance().getLightningService().getNodeInfo(nodeInfoRequest)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .subscribe(nodeInfo -> {
                    BBLog.v(LOG_TAG, "Fetched Node info from " + nodeInfo.getNode().getAlias());
                    AliasManager.getInstance().saveAlias(nodeInfo.getNode().getPubKey(), nodeInfo.getNode().getAlias());

                    if (lastNode) {
                        AliasManager.getInstance().saveAliasesToCache();
                        broadcastChannelsUpdated();
                    } else {
                        if (saveAliasToCache)
                            AliasManager.getInstance().saveAliasesToCache();
                    }
                    if (listener != null) {
                        listener.onNodeInfoFetched(nodeInfo.getNode().getPubKey());
                    }
                }, throwable -> {
                    if (AliasManager.getInstance().hasAliasInfo(pubkey)) {
                        // Prevents requesting nodInfo for the unavailable node to often
                        AliasManager.getInstance().updateTimestampForAlias(pubkey);
                    } else {
                        // Prevents requesting nodInfo for the unavailable node to often
                        AliasManager.getInstance().saveAlias(pubkey, pubkey);
                    }
                    if (lastNode) {
                        AliasManager.getInstance().saveAliasesToCache();
                        broadcastChannelsUpdated();
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
     * Get the remote pubkey from a channel Id.
     * This will only work for currently opened channels. If the id does not match with any open channel, null will be returned.
     *
     * @return remote pub key
     */
    public String getRemotePubKeyFromChannelId(long chanId) {
        if (mOpenChannelsList != null) {
            for (Channel channel : mOpenChannelsList) {
                if (channel.getChanId() == chanId) {
                    return channel.getRemotePubkey();
                }
            }
        }
        // ToDo: Add pending channels
        if (mClosedChannelsList != null) {
            for (ChannelCloseSummary channelCloseSummary : mClosedChannelsList) {
                if (channelCloseSummary.getChanId() == chanId)
                    return channelCloseSummary.getRemotePubkey();
            }
        }

        return null;
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

    /**
     * Use this to subscribe the wallet to htlc events that happen on LND.
     * The events will be captured and forwarded to the HtlcSubscriptionListener.
     * All parts of the App that want to react on transaction events have to subscribe to the
     * HtlcSubscriptionListener.
     */
    public void subscribeToHtlcEvents() {
        compositeDisposable.add(LndConnection.getInstance().getRouterService().subscribeHtlcEvents(SubscribeHtlcEventsRequest.newBuilder().build())
                .subscribe(htlcEvent -> {
                    BBLog.d(LOG_TAG, "Received htlc subscription event. Type: " + htlcEvent.getEventType().toString());
                    Wallet_Balance.getInstance().fetchBalancesWithDebounce(); // Always update balances if a htlc event occurs.
                    updateLNDChannelsWithDebounce(); // Always update channels if a htlc event occurs.
                    broadcastHtlcEvent(htlcEvent);
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
     * Use this to subscribe the wallet to channel events that happen on LND.
     * The events will be captured and forwarded to the ChannelEventSubscriptionListener.
     * All parts of the App that want to react on channel events have to subscribe to the
     * ChannelEventSubscriptionListener.
     */
    public void subscribeToChannelEvents() {
        compositeDisposable.add(LndConnection.getInstance().getLightningService().subscribeChannelEvents(ChannelEventSubscription.newBuilder().build())
                .subscribe(channelEventUpdate -> {
                    BBLog.d(LOG_TAG, "Received channel update event");
                    switch (channelEventUpdate.getChannelCase()) {
                        case OPEN_CHANNEL:
                            BBLog.d(LOG_TAG, "Channel has been opened");
                            break;
                        case CLOSED_CHANNEL:
                            BBLog.d(LOG_TAG, "Channel has been closed");
                            break;
                        case ACTIVE_CHANNEL:
                            BBLog.d(LOG_TAG, "Channel went active");
                            break;
                        case INACTIVE_CHANNEL:
                            BBLog.d(LOG_TAG, "Open channel went to inactive");
                            break;
                        case CHANNEL_NOT_SET:
                            BBLog.d(LOG_TAG, "Received channel event update case: not set Channel");
                            break;
                        default:
                            BBLog.d(LOG_TAG, "Unknown channel event: " + channelEventUpdate.getChannelCase());
                            break;
                    }

                    updateLNDChannelsWithDebounce();
                    broadcastChannelEvent(channelEventUpdate);
                }));
    }

    public void updateLNDChannelsWithDebounce() {
        BBLog.d(LOG_TAG, "Fetch channels from LND. (debounce)");

        mChannelsUpdateDebounceHandler.attempt(this::fetchChannelsFromLND, DebounceHandler.DEBOUNCE_1_SECOND);
    }

    /**
     * Use this to subscribe the wallet to channel backup events that happen on LND.
     * The events will be captured and forwarded to the ChannelBackupSubscriptionListener.
     * All parts of the App that want to react on channel backups have to subscribe to the
     * ChannelBackupSubscriptionListener.
     */
    public void subscribeToChannelBackup() {
        compositeDisposable.add(LndConnection.getInstance().getLightningService().subscribeChannelBackups(ChannelBackupSubscription.newBuilder().build())
                .subscribe(chanBackupSnapshot -> {
                    BBLog.d(LOG_TAG, "Received channel backup event.");
                    broadcastChannelBackup(chanBackupSnapshot);
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
     * This function determines if the given transaction is is associated with a channel operation.
     *
     * @param transaction
     * @return
     */
    public boolean isChannelTransaction(Transaction transaction) {

        // This is faster especially for nodes with lots of channels
        if (hasChannelTransactionLabel(transaction)) {
            return true;
        }

        // ToDo: looping through all channels can be removed later. But this is still necessary for Nodes that run since LND versions prior to LND 0.11
        // open channels
        if (mOpenChannelsList != null) {
            for (Channel c : mOpenChannelsList) {
                String[] parts = c.getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return true;
                }
            }
        }

        // pending open channels
        if (mPendingOpenChannelsList != null) {
            for (PendingChannelsResponse.PendingOpenChannel c : mPendingOpenChannelsList) {
                String[] parts = c.getChannel().getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return true;
                }
            }
        }

        // pending closed channels
        if (mPendingClosedChannelsList != null) {
            for (PendingChannelsResponse.ClosedChannel c : mPendingClosedChannelsList) {
                String[] parts = c.getChannel().getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return true;
                }
            }
        }

        // pending force closed channels
        if (mPendingForceClosedChannelsList != null) {
            for (PendingChannelsResponse.ForceClosedChannel c : mPendingForceClosedChannelsList) {
                String[] parts = c.getChannel().getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return true;
                }
            }
        }

        // pending waiting for close channels
        if (mPendingWaitingCloseChannelsList != null) {
            for (PendingChannelsResponse.WaitingCloseChannel c : mPendingWaitingCloseChannelsList) {
                String[] parts = c.getChannel().getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return true;
                }
            }
        }

        // closed channels
        if (mClosedChannelsList != null) {
            for (ChannelCloseSummary c : mClosedChannelsList) {
                String[] parts = c.getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0]) || transaction.getTxHash().equals(c.getClosingTxHash())) {
                    return true;
                }
                for (Resolution res : c.getResolutionsList()) {
                    if (transaction.getTxHash().equals(res.getSweepTxid())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * This function determines if according to the label, that gets applied automatically by lnd, this is a chanel transaction.
     * The labelTypes are derived from: https://github.com/lightningnetwork/lnd/blob/master/labels/labels.go
     *
     * @param transaction
     * @return
     */
    public boolean hasChannelTransactionLabel(Transaction transaction) {
        String[] labelType = {":openchannel", ":closechannel", ":justicetx"};
        if (transaction.getLabel() != null && !transaction.getLabel().isEmpty()) {
            for (String label : labelType) {
                if (transaction.getLabel().toLowerCase().contains(label)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This functions helps us to link on-chain channel transaction with the corresponding channel's public node alias.
     *
     * @return pubKey of the Node the channel is linked to
     */
    public String getNodePubKeyFromChannelTransaction(Transaction transaction) {

        // open channels
        if (mOpenChannelsList != null) {
            for (Channel c : mOpenChannelsList) {
                String[] parts = c.getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return c.getRemotePubkey();
                }
            }
        }

        // pending open channels
        if (mPendingOpenChannelsList != null) {
            for (PendingChannelsResponse.PendingOpenChannel c : mPendingOpenChannelsList) {
                String[] parts = c.getChannel().getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return c.getChannel().getRemoteNodePub();
                }
            }
        }

        // pending closed channels
        if (mPendingClosedChannelsList != null) {
            for (PendingChannelsResponse.ClosedChannel c : mPendingClosedChannelsList) {
                String[] parts = c.getChannel().getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return c.getChannel().getRemoteNodePub();
                }
            }
        }

        // pending force closed channels
        if (mPendingForceClosedChannelsList != null) {
            for (PendingChannelsResponse.ForceClosedChannel c : mPendingForceClosedChannelsList) {
                String[] parts = c.getChannel().getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return c.getChannel().getRemoteNodePub();
                } else if (transaction.getLabel().toLowerCase().contains("sweep")) {
                    // force closes are marked with a "sweep" label in lnd
                    List<PreviousOutPoint> previousOutPoints = transaction.getPreviousOutpointsList();
                    for (PreviousOutPoint op : previousOutPoints) {
                        if (op.getOutpoint().split(":")[0].equals(c.getClosingTxid()))
                            return c.getChannel().getRemoteNodePub();
                    }
                }
            }
        }

        // pending waiting for close channels
        if (mPendingWaitingCloseChannelsList != null) {
            for (PendingChannelsResponse.WaitingCloseChannel c : mPendingWaitingCloseChannelsList) {
                String[] parts = c.getChannel().getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0])) {
                    return c.getChannel().getRemoteNodePub();
                }
            }
        }

        // closed channels
        if (mClosedChannelsList != null) {
            for (ChannelCloseSummary c : mClosedChannelsList) {
                String[] parts = c.getChannelPoint().split(":");
                if (transaction.getTxHash().equals(parts[0]) || transaction.getTxHash().equals(c.getClosingTxHash())) {
                    return c.getRemotePubkey();
                }
                for (Resolution res : c.getResolutionsList()) {
                    if (transaction.getTxHash().equals(res.getSweepTxid())) {
                        return c.getRemotePubkey();
                    }
                }
            }
        }
        return "";
    }

    /**
     * Returns if the wallet has at least one online channel.
     *
     * @return
     */
    public boolean hasOpenActiveChannels() {
        if (mOpenChannelsList != null) {
            if (mOpenChannelsList.size() != 0) {
                for (Channel c : mOpenChannelsList) {
                    if (c.getActive()) {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns the block height of a channel opening transaction.
     * This only works if we are the initiator of the channel as LND will not store the transaction otherwhise.
     *
     * @param channelPoint channel point as string
     * @return block height. -1 if not available.
     */
    public int getChannelOpenBlockHeight(String channelPoint) {
        String txId = channelPoint.split(":")[0];
        for (Transaction tx : mOnChainTransactionList) {
            if (tx.getTxHash().equals(txId))
                return tx.getBlockHeight();
        }
        return -1;
    }

    /**
     * Get the maximum amount that can be received over Lightning Channels.
     *
     * @return amount in satoshis
     */
    public long getMaxLightningReceiveAmount() {

        if (!BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            return 0;
        }

        // Mpp is supported. Use the sum of the remote balances of all channels as maximum.
        long tempMax = 0L;
        if (mOpenChannelsList != null) {
            for (Channel c : mOpenChannelsList) {
                if (c.getActive()) {
                    tempMax = tempMax + Math.max(c.getRemoteBalance() - c.getRemoteConstraints().getChanReserveSat(), 0);
                }
            }
        }
        return tempMax;
    }

    /**
     * Get the maximum amount that can be send over Lightning Channels.
     *
     * @return amount in satoshis
     */
    public long getMaxLightningSendAmount() {

        if (!BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            return 0;
        }

        // Mpp is supported. Use the sum of the local balances of all channels as maximum.
        long tempMax = 0L;
        if (mOpenChannelsList != null) {
            for (Channel c : mOpenChannelsList) {
                if (c.getActive()) {
                    tempMax = tempMax + Math.max(c.getLocalBalance() - c.getLocalConstraints().getChanReserveSat(), 0);
                }
            }
        }
        return tempMax;
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
     * Notify all listeners to htlc update.
     *
     * @param htlcEvent the htlc event that occured
     */
    private void broadcastHtlcEvent(HtlcEvent htlcEvent) {
        for (HtlcSubscriptionListener listener : mHtlcSubscriptionListeners) {
            listener.onHtlcEvent(htlcEvent);
        }
    }

    public void registerHtlcSubscriptionListener(HtlcSubscriptionListener listener) {
        mHtlcSubscriptionListeners.add(listener);
    }

    public void unregisterHtlcSubscriptionListener(HtlcSubscriptionListener listener) {
        mHtlcSubscriptionListeners.remove(listener);
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

    /**
     * Notify all listeners to channel event updates.
     *
     * @param channelEventUpdate the channel update
     */
    private void broadcastChannelEvent(ChannelEventUpdate channelEventUpdate) {
        for (ChannelEventSubscriptionListener listener : mChannelEventSubscriptionListeners) {
            listener.onChannelEvent(channelEventUpdate);
        }
    }

    public void registerChannelEventSubscriptionListener(ChannelEventSubscriptionListener listener) {
        mChannelEventSubscriptionListeners.add(listener);
    }

    public void unregisterChannelEventSubscriptionListener(ChannelEventSubscriptionListener listener) {
        mChannelEventSubscriptionListeners.remove(listener);
    }


    /**
     * Notify all listeners that channels have been updated.
     */
    private void broadcastChannelsUpdated() {
        for (ChannelsUpdatedSubscriptionListener listener : mChannelsUpdatedSubscriptionListeners) {
            listener.onChannelsUpdated();
        }
    }

    public void registerChannelsUpdatedSubscriptionListener(ChannelsUpdatedSubscriptionListener listener) {
        mChannelsUpdatedSubscriptionListeners.add(listener);
    }

    public void unregisterChannelsUpdatedSubscriptionListener(ChannelsUpdatedSubscriptionListener listener) {
        mChannelsUpdatedSubscriptionListeners.remove(listener);
    }

    /**
     * Notify all listeners to channel backup updates.
     *
     * @param chanBackupSnapshot snapshot of channel backup
     */
    private void broadcastChannelBackup(ChanBackupSnapshot chanBackupSnapshot) {
        for (ChannelBackupSubscriptionListener listener : mChannelBackupSubscriptionListeners) {
            listener.onChannelBackupEvent(chanBackupSnapshot);
        }
    }

    public void registerChannelBackupSubscriptionListener(ChannelBackupSubscriptionListener listener) {
        mChannelBackupSubscriptionListeners.add(listener);
    }

    public void unregisterChannelBackuptSubscriptionListener(ChannelBackupSubscriptionListener listener) {
        mChannelBackupSubscriptionListeners.remove(listener);
    }

    /**
     * Notify all listeners to channel close updates
     */
    private void broadcastChannelCloseUpdate(String channelPoint, int status, String message) {
        for (ChannelCloseUpdateListener listener : mChannelCloseUpdateListeners) {
            listener.onChannelCloseUpdate(channelPoint, status, message);
        }
    }

    public void registerChannelCloseUpdateListener(ChannelCloseUpdateListener listener) {
        mChannelCloseUpdateListeners.add(listener);
    }

    public void unregisterChannelCloseUpdateListener(ChannelCloseUpdateListener listener) {
        mChannelCloseUpdateListeners.remove(listener);
    }

    private void broadcastChannelOpenUpdate(LightningNodeUri lightningNodeUri, int status, String message) {
        for (ChannelOpenUpdateListener listener : mChannelOpenUpdateListeners) {
            listener.onChannelOpenUpdate(lightningNodeUri, status, message);
        }
    }

    public void registerChannelOpenUpdateListener(ChannelOpenUpdateListener listener) {
        mChannelOpenUpdateListeners.add(listener);
    }

    public void unregisterChannelOpenUpdateListener(ChannelOpenUpdateListener listener) {
        mChannelOpenUpdateListeners.remove(listener);
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

    public interface HtlcSubscriptionListener {
        void onHtlcEvent(HtlcEvent htlcEvent);
    }

    public interface UtxoSubscriptionListener {
        void onUtxoListUpdated();
    }

    public interface ChannelEventSubscriptionListener {
        void onChannelEvent(ChannelEventUpdate channelEventUpdate);
    }

    public interface ChannelCloseUpdateListener {

        int SUCCESS = -1;
        int ERROR_PEER_OFFLINE = 0;
        int ERROR_CHANNEL_TIMEOUT = 1;
        int ERROR_CHANNEL_CLOSE = 2;

        void onChannelCloseUpdate(String channelPoint, int status, String message);
    }

    public interface ChannelBackupSubscriptionListener {
        void onChannelBackupEvent(ChanBackupSnapshot chanBackupSnapshot);
    }

    public interface ChannelsUpdatedSubscriptionListener {
        void onChannelsUpdated();
    }

    public interface NodeInfoFetchedListener {
        void onNodeInfoFetched(String pubkey);
    }

    public interface PeerUpdateListener {
        void onConnectedToPeer();
    }

    public interface ChannelOpenUpdateListener {

        int SUCCESS = -1;
        int ERROR_GET_PEERS_TIMEOUT = 0;
        int ERROR_GET_PEERS = 1;
        int ERROR_CONNECTION_TIMEOUT = 2;
        int ERROR_CONNECTION_REFUSED = 3;
        int ERROR_CONNECTION_SELF = 4;
        int ERROR_CONNECTION_NO_HOST = 5;
        int ERROR_CONNECTION = 6;
        int ERROR_CHANNEL_TIMEOUT = 7;
        int ERROR_CHANNEL_PENDING_MAX = 8;
        int ERROR_CHANNEL_OPEN = 9;

        void onChannelOpenUpdate(LightningNodeUri lightningNodeUri, int status, String message);
    }
}