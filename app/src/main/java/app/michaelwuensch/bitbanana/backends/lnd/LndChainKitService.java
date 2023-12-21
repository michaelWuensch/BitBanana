package app.michaelwuensch.bitbanana.backends.lnd;

import io.reactivex.rxjava3.core.Single;

public interface LndChainKitService {

    Single<com.github.lightningnetwork.lnd.chainrpc.GetBlockResponse> getBlock(com.github.lightningnetwork.lnd.chainrpc.GetBlockRequest request);

    Single<com.github.lightningnetwork.lnd.chainrpc.GetBestBlockResponse> getBestBlock(com.github.lightningnetwork.lnd.chainrpc.GetBestBlockRequest request);

    Single<com.github.lightningnetwork.lnd.chainrpc.GetBlockHashResponse> getBlockHash(com.github.lightningnetwork.lnd.chainrpc.GetBlockHashRequest request);
}