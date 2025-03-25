package app.michaelwuensch.bitbanana.wallet;


import android.os.Handler;

import com.github.lightningnetwork.lnd.lnrpc.ChannelEventSubscription;
import com.github.lightningnetwork.lnd.lnrpc.ChannelEventUpdate;
import com.github.lightningnetwork.lnd.routerrpc.HtlcEvent;
import com.github.lightningnetwork.lnd.routerrpc.SubscribeHtlcEventsRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.models.Channels.CloseChannelRequest;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannelRequest;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.Peer;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.DebounceHandler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Wallet_Channels {

    private static final String LOG_TAG = Wallet_Channels.class.getSimpleName();

    private static Wallet_Channels mInstance = null;

    private final Set<ChannelEventSubscriptionListener> mChannelEventSubscriptionListeners = new HashSet<>();
    private final Set<ChannelsUpdatedSubscriptionListener> mChannelsUpdatedSubscriptionListeners = new HashSet<>();
    private final Set<ChannelCloseUpdateListener> mChannelCloseUpdateListeners = new HashSet<>();
    private final Set<ChannelOpenUpdateListener> mChannelOpenUpdateListeners = new HashSet<>();
    private final Set<HtlcSubscriptionListener> mHtlcSubscriptionListeners = new HashSet<>();


    private List<OpenChannel> mOpenChannelsList;
    private List<PendingChannel> mPendingChannelsList;
    private List<ClosedChannel> mClosedChannelsList;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Handler mHandler = new Handler();
    private final DebounceHandler mChannelsUpdateDebounceHandler = new DebounceHandler();

    private Wallet_Channels() {
        ;
    }

    public static Wallet_Channels getInstance() {

        if (mInstance == null) {
            mInstance = new Wallet_Channels();
        }

        return mInstance;
    }

    /**
     * Use this to reset the wallet_channels component.
     */
    public void reset() {
        mOpenChannelsList = null;
        mPendingChannelsList = null;
        mClosedChannelsList = null;

        compositeDisposable.clear();
        mHandler.removeCallbacksAndMessages(null);
        mChannelsUpdateDebounceHandler.shutdown();
    }

    public List<OpenChannel> getOpenChannelsList() {
        return mOpenChannelsList;
    }

    public List<PendingChannel> getPendingChannelsList() {
        return mPendingChannelsList;
    }

    public List<ClosedChannel> getClosedChannelsList() {
        return mClosedChannelsList;
    }

    public void openChannel(LightningNodeUri nodeUri, OpenChannelRequest openChannelRequest) {
        compositeDisposable.add(BackendManager.api().listPeers()
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(response -> {
                    boolean connected = false;
                    for (Peer node : response) {
                        if (node.getPubKey().equals(openChannelRequest.getNodePubKey())) {
                            connected = true;
                            break;
                        }
                    }

                    if (connected) {
                        BBLog.d(LOG_TAG, "Already connected to peer, trying to open channel...");
                        openChannelConnected(openChannelRequest);
                    } else {
                        BBLog.d(LOG_TAG, "Not connected to peer, trying to connect...");
                        Wallet_NodesAndPeers.getInstance().connectPeer(nodeUri, openChannelRequest);
                    }
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error listing peers request: " + throwable.getMessage());
                    if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        broadcastChannelOpenUpdate(openChannelRequest.getNodePubKey(), ChannelOpenUpdateListener.ERROR_GET_PEERS_TIMEOUT, throwable.getMessage());
                    } else {
                        broadcastChannelOpenUpdate(openChannelRequest.getNodePubKey(), ChannelOpenUpdateListener.ERROR_GET_PEERS, throwable.getMessage());
                    }
                }));
    }

    public void openChannelConnected(OpenChannelRequest openChannelRequest) {
        compositeDisposable.add(BackendManager.api().openChannel(openChannelRequest)
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(() -> {
                    BBLog.d(LOG_TAG, "Channel open successfully initiated!");
                    broadcastChannelOpenUpdate(openChannelRequest.getNodePubKey(), ChannelOpenUpdateListener.SUCCESS, null);
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error opening channel: " + throwable.getMessage());

                    if (throwable.getMessage().toLowerCase().contains("pending channels exceed maximum")) {
                        broadcastChannelOpenUpdate(openChannelRequest.getNodePubKey(), ChannelOpenUpdateListener.ERROR_CHANNEL_PENDING_MAX, throwable.getMessage());
                    } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        broadcastChannelOpenUpdate(openChannelRequest.getNodePubKey(), ChannelOpenUpdateListener.ERROR_CHANNEL_TIMEOUT, throwable.getMessage());
                    } else {
                        broadcastChannelOpenUpdate(openChannelRequest.getNodePubKey(), ChannelOpenUpdateListener.ERROR_CHANNEL_OPEN, throwable.getMessage());
                    }
                }));
    }

    public void closeChannel(OpenChannel channel, boolean force) {
        // On Core Lightning we do not get a response for Channel Closes if the peer is not online as it waits for 2 days (default)
        // for the peer to come online before falling back to a force close.
        // Therefore we reduce the timeout and fake the thrown error to be successful.

        long timeout;
        boolean fakeSuccess = (force && BackendManager.getCurrentBackendType() == BackendConfig.BackendType.CORE_LIGHTNING_GRPC);
        if (fakeSuccess)
            timeout = 5;
        else
            timeout = ApiUtil.getBackendTimeout();

        CloseChannelRequest closeChannelRequest = CloseChannelRequest.newBuilder()
                .setShortChannelId(channel.getShortChannelId())
                .setFundingOutpoint(channel.getFundingOutpoint())
                .setForceClose(force)
                .build();

        compositeDisposable.add(BackendManager.api().closeChannel(closeChannelRequest)
                .timeout(timeout, TimeUnit.SECONDS)
                .subscribe(() -> {
                    BBLog.d(LOG_TAG, "Channel close successfully initiated!");
                    broadcastChannelCloseUpdate(channel.getFundingOutpoint().toString(), ChannelCloseUpdateListener.SUCCESS, null);
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error closing channel: " + throwable.getMessage());
                    if (throwable.getMessage().toLowerCase().contains("offline")) {
                        broadcastChannelCloseUpdate(channel.getFundingOutpoint().toString(), ChannelCloseUpdateListener.ERROR_PEER_OFFLINE, throwable.getMessage());
                    } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        if (fakeSuccess)
                            broadcastChannelCloseUpdate(channel.getFundingOutpoint().toString(), ChannelCloseUpdateListener.SUCCESS, null);
                        else
                            broadcastChannelCloseUpdate(channel.getFundingOutpoint().toString(), ChannelCloseUpdateListener.ERROR_CHANNEL_TIMEOUT, throwable.getMessage());
                    } else {
                        broadcastChannelCloseUpdate(channel.getFundingOutpoint().toString(), ChannelCloseUpdateListener.ERROR_CHANNEL_CLOSE, throwable.getMessage());
                    }
                }));
    }

    public void fetchChannels() {
        compositeDisposable.add(fetchChannelsSingle()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(aBoolean -> {

                }, throwable -> BBLog.e(LOG_TAG, "Exception in get channels info request task: " + throwable.getMessage())));
    }

    public Single<Boolean> fetchChannelsSingle() {
        Single<List<OpenChannel>> openChannelsObservable = BackendManager.api().listOpenChannels();
        Single<List<PendingChannel>> pendingChannelsObservable = BackendManager.api().listPendingChannels();
        Single<List<ClosedChannel>> closedChannelsObservable = BackendManager.api().listClosedChannels();

        return Single.zip(openChannelsObservable, pendingChannelsObservable, closedChannelsObservable, (openChannelsResponse, pendingChannelsResponse, closedChannelsResponse) -> {
            mOpenChannelsList = openChannelsResponse;
            mClosedChannelsList = closedChannelsResponse;
            mPendingChannelsList = pendingChannelsResponse;

            BBLog.d(LOG_TAG, "Channels fetched!");
            fetchNodeInfos();
            return true;
        });
    }

    private void fetchNodeInfos() {
        // Load NodeInfos for all involved nodes. This allows us to display aliases later.
        Set<String> channelNodes = new HashSet<>();

        for (OpenChannel c : mOpenChannelsList) {
            if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getRemotePubKey()))
                channelNodes.add(c.getRemotePubKey());
        }
        for (PendingChannel c : mPendingChannelsList) {
            if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getRemotePubKey()))
                channelNodes.add(c.getRemotePubKey());
        }

        for (ClosedChannel c : mClosedChannelsList) {
            if (!AliasManager.getInstance().hasUpToDateAliasInfo(c.getRemotePubKey()))
                channelNodes.add(c.getRemotePubKey());
        }

        // Delay each NodeInfo request for 100ms to not stress the node
        ArrayList<String> channelNodesList = new ArrayList<>(channelNodes);
        if (!channelNodesList.isEmpty())
            BBLog.d(LOG_TAG, "Fetching node info for " + channelNodesList.size() + " nodes.");

        compositeDisposable.add(Observable.range(0, channelNodesList.size())
                .concatMap(i -> Observable.just(i).delay(100, TimeUnit.MILLISECONDS))
                .doOnNext(integer -> Wallet_NodesAndPeers.getInstance().fetchNodeInfo(channelNodesList.get(integer), integer == channelNodesList.size() - 1, true, null))
                .subscribe());

        if (channelNodesList.size() == 0) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    broadcastChannelsUpdated();
                }
            }, 100);
        }
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
                    BBLog.v(LOG_TAG, htlcEvent.toString());
                    if (htlcEvent.hasSettleEvent() || (htlcEvent.hasFinalHtlcEvent() && htlcEvent.getFinalHtlcEvent().getSettled())) {
                        Wallet_Balance.getInstance().fetchBalancesWithDebounce(); // Always update balances if a htlc event occurs.
                        updateLNDChannelsWithDebounce(); // Always update channels if a htlc event occurs.
                    }
                    broadcastHtlcEvent(htlcEvent);
                }));
    }

    public void cancelSubscriptions() {
        compositeDisposable.clear();
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
                    switch (channelEventUpdate.getChannelCase()) {
                        case OPEN_CHANNEL:
                            BBLog.d(LOG_TAG, "ChannelEvent: Channel has been opened");
                            break;
                        case CLOSED_CHANNEL:
                            BBLog.d(LOG_TAG, "ChannelEvent: Channel has been closed");
                            break;
                        case ACTIVE_CHANNEL:
                            BBLog.d(LOG_TAG, "ChannelEvent: Channel went active");
                            break;
                        case INACTIVE_CHANNEL:
                            BBLog.d(LOG_TAG, "ChannelEvent: Open channel went to inactive");
                            break;
                        case CHANNEL_NOT_SET:
                            BBLog.d(LOG_TAG, "ChannelEvent: Channel not set");
                            break;
                        default:
                            BBLog.d(LOG_TAG, "ChannelEvent: Unknown channel event: " + channelEventUpdate.getChannelCase());
                            break;
                    }
                    BBLog.v(LOG_TAG, channelEventUpdate.toString());

                    updateLNDChannelsWithDebounce();
                    broadcastChannelEvent(channelEventUpdate);
                }));
    }

    public void updateLNDChannelsWithDebounce() {
        BBLog.d(LOG_TAG, "Fetch channels from LND. (debounce)");

        mChannelsUpdateDebounceHandler.attempt(this::fetchChannels, DebounceHandler.DEBOUNCE_1_SECOND);
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
    public void broadcastChannelsUpdated() {
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

    /**
     * Notify all listeners to channel open updates
     */
    public void broadcastChannelOpenUpdate(String nodePubKey, int status, String message) {
        for (ChannelOpenUpdateListener listener : mChannelOpenUpdateListeners) {
            listener.onChannelOpenUpdate(nodePubKey, status, message);
        }
    }

    public void registerChannelOpenUpdateListener(ChannelOpenUpdateListener listener) {
        mChannelOpenUpdateListeners.add(listener);
    }

    public void unregisterChannelOpenUpdateListener(ChannelOpenUpdateListener listener) {
        mChannelOpenUpdateListeners.remove(listener);
    }


    public interface HtlcSubscriptionListener {
        void onHtlcEvent(HtlcEvent htlcEvent);
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

    public interface ChannelsUpdatedSubscriptionListener {
        void onChannelsUpdated();
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

        void onChannelOpenUpdate(String nodePubKey, int status, String message);
    }
}