package app.michaelwuensch.bitbanana.backends.lnd.services;

import com.github.lightningnetwork.lnd.watchtowerrpc.WatchtowerGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndWatchtowerService implements LndWatchtowerService {

    private final WatchtowerGrpc.WatchtowerStub asyncStub;

    public RemoteLndWatchtowerService(Channel channel, CallCredentials callCredentials) {
        asyncStub = WatchtowerGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.watchtowerrpc.GetInfoResponse> getInfo(com.github.lightningnetwork.lnd.watchtowerrpc.GetInfoRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getInfo(request, new RemoteSingleObserver<>(emitter)));
    }

}