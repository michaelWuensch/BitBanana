package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.lightningnetwork.lnd.lnrpc.StateGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultObservable;
import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import app.michaelwuensch.bitbanana.backends.RemoteStreamObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndStateService implements LndStateService {

    private final StateGrpc.StateStub asyncStub;

    public RemoteLndStateService(Channel channel, CallCredentials callCredentials) {
        asyncStub = StateGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.SubscribeStateResponse> subscribeState(com.github.lightningnetwork.lnd.lnrpc.SubscribeStateRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeState(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.GetStateResponse> getState(com.github.lightningnetwork.lnd.lnrpc.GetStateRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getState(request, new RemoteSingleObserver<>(emitter)));
    }

}