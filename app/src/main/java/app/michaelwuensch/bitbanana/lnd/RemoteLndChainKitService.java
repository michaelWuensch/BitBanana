package app.michaelwuensch.bitbanana.lnd;

import com.github.lightningnetwork.lnd.chainrpc.ChainKitGrpc;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndChainKitService implements LndChainKitService {

    private ChainKitGrpc.ChainKitStub asyncStub;

    public RemoteLndChainKitService(Channel channel, CallCredentials callCredentials) {
        asyncStub = ChainKitGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.chainrpc.GetBlockResponse> getBlock(com.github.lightningnetwork.lnd.chainrpc.GetBlockRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getBlock(request, new RemoteLndSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.chainrpc.GetBestBlockResponse> getBestBlock(com.github.lightningnetwork.lnd.chainrpc.GetBestBlockRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getBestBlock(request, new RemoteLndSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.chainrpc.GetBlockHashResponse> getBlockHash(com.github.lightningnetwork.lnd.chainrpc.GetBlockHashRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getBlockHash(request, new RemoteLndSingleObserver<>(emitter)));
    }

}