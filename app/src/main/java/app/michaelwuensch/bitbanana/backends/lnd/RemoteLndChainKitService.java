package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.lightningnetwork.lnd.chainrpc.ChainKitGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndChainKitService implements LndChainKitService {

    private final ChainKitGrpc.ChainKitStub asyncStub;

    public RemoteLndChainKitService(Channel channel, CallCredentials callCredentials) {
        asyncStub = ChainKitGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.chainrpc.GetBlockResponse> getBlock(com.github.lightningnetwork.lnd.chainrpc.GetBlockRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getBlock(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.chainrpc.GetBestBlockResponse> getBestBlock(com.github.lightningnetwork.lnd.chainrpc.GetBestBlockRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getBestBlock(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.chainrpc.GetBlockHashResponse> getBlockHash(com.github.lightningnetwork.lnd.chainrpc.GetBlockHashRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getBlockHash(request, new RemoteSingleObserver<>(emitter)));
    }

}