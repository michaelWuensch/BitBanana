package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.lightningnetwork.lnd.autopilotrpc.AutopilotGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndAutopilotService implements LndAutopilotService {

    private final AutopilotGrpc.AutopilotStub asyncStub;

    public RemoteLndAutopilotService(Channel channel, CallCredentials callCredentials) {
        asyncStub = AutopilotGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.autopilotrpc.StatusResponse> status(com.github.lightningnetwork.lnd.autopilotrpc.StatusRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.status(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.autopilotrpc.ModifyStatusResponse> modifyStatus(com.github.lightningnetwork.lnd.autopilotrpc.ModifyStatusRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.modifyStatus(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.autopilotrpc.QueryScoresResponse> queryScores(com.github.lightningnetwork.lnd.autopilotrpc.QueryScoresRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.queryScores(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.autopilotrpc.SetScoresResponse> setScores(com.github.lightningnetwork.lnd.autopilotrpc.SetScoresRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.setScores(request, new RemoteSingleObserver<>(emitter)));
    }

}