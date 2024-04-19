package app.michaelwuensch.bitbanana.wallet;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Wallet_NodesAndPeers {

    private static final String LOG_TAG = Wallet_NodesAndPeers.class.getSimpleName();

    private static Wallet_NodesAndPeers mInstance = null;
    private final Set<PeerUpdateListener> mPeerUpdateListeners = new HashSet<>();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private Wallet_NodesAndPeers() {
        ;
    }

    public static Wallet_NodesAndPeers getInstance() {

        if (mInstance == null) {
            mInstance = new Wallet_NodesAndPeers();
        }

        return mInstance;
    }

    /**
     * Use this to reset the wallet information when the wallet was switched.
     */
    public void reset() {
        compositeDisposable.clear();
    }

    public void connectPeer(LightningNodeUri nodeUri, boolean openChannel, long amount, long satPerVByte, boolean isPrivate) {
        if (nodeUri.getHost() == null || nodeUri.getHost().isEmpty()) {
            BBLog.d(LOG_TAG, "Host info missing. Trying to fetch host info to connect peer...");
            fetchNodeInfoToConnectPeer(nodeUri, openChannel, amount, satPerVByte, isPrivate);
            return;
        }

        compositeDisposable.add(BackendManager.api().connectPeer(nodeUri)
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribe(() -> {
                    BBLog.d(LOG_TAG, "Successfully connected to peer.");
                    broadcastPeerConnectedSuccess();
                    if (openChannel) {
                        BBLog.d(LOG_TAG, "Now that we are connected to peer, trying to open channel...");
                        Wallet_Channels.getInstance().openChannelConnected(nodeUri, amount, satPerVByte, isPrivate);
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
                        broadcastPeerConnectedError(throwable.getMessage());
                    }
                }));
    }

    private void fetchNodeInfoToConnectPeer(LightningNodeUri nodeUri, boolean openChannel, long amount, long satPerVByte, boolean isPrivate) {
        compositeDisposable.add(BackendManager.api().getNodeInfo(nodeUri.getPubKey())
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribe(response -> {
                    if (response.getAddresses().size() > 0) {
                        String tempUri = nodeUri.getPubKey() + "@" + response.getAddresses().get(0);
                        LightningNodeUri nodeUriWithHost = LightningNodeUriParser.parseNodeUri(tempUri);
                        if (nodeUriWithHost != null) {
                            BBLog.d(LOG_TAG, "Host info successfully fetched. NodeUriWithHost: " + nodeUriWithHost.getAsString());
                            connectPeer(nodeUriWithHost, openChannel, amount, satPerVByte, isPrivate);
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

    public void cancelSubscriptions() {
        compositeDisposable.clear();
    }


    private void broadcastPeerConnectedSuccess() {
        for (PeerUpdateListener listener : mPeerUpdateListeners) {
            listener.onConnectedToPeer();
        }
    }

    private void broadcastPeerConnectedError(String message) {
        for (PeerUpdateListener listener : mPeerUpdateListeners) {
            listener.onError(message);
        }
    }

    public void registerPeerUpdateListener(PeerUpdateListener listener) {
        mPeerUpdateListeners.add(listener);
    }

    public void unregisterPeerUpdateListener(PeerUpdateListener listener) {
        mPeerUpdateListeners.remove(listener);
    }

    public interface NodeInfoFetchedListener {
        void onNodeInfoFetched(String pubkey);
    }

    public interface PeerUpdateListener {
        void onConnectedToPeer();

        void onError(String message);
    }
}