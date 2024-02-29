package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.lightningnetwork.lnd.signrpc.SignerGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndSignerService implements LndSignerService {

    private final SignerGrpc.SignerStub asyncStub;

    public RemoteLndSignerService(Channel channel, CallCredentials callCredentials) {
        asyncStub = SignerGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.SignResp> signOutputRaw(com.github.lightningnetwork.lnd.signrpc.SignReq request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.signOutputRaw(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.InputScriptResp> computeInputScript(com.github.lightningnetwork.lnd.signrpc.SignReq request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.computeInputScript(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.SignMessageResp> signMessage(com.github.lightningnetwork.lnd.signrpc.SignMessageReq request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.signMessage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.VerifyMessageResp> verifyMessage(com.github.lightningnetwork.lnd.signrpc.VerifyMessageReq request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.verifyMessage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.SharedKeyResponse> deriveSharedKey(com.github.lightningnetwork.lnd.signrpc.SharedKeyRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.deriveSharedKey(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.MuSig2CombineKeysResponse> muSig2CombineKeys(com.github.lightningnetwork.lnd.signrpc.MuSig2CombineKeysRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.muSig2CombineKeys(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.MuSig2SessionResponse> muSig2CreateSession(com.github.lightningnetwork.lnd.signrpc.MuSig2SessionRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.muSig2CreateSession(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.MuSig2RegisterNoncesResponse> muSig2RegisterNonces(com.github.lightningnetwork.lnd.signrpc.MuSig2RegisterNoncesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.muSig2RegisterNonces(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.MuSig2SignResponse> muSig2Sign(com.github.lightningnetwork.lnd.signrpc.MuSig2SignRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.muSig2Sign(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.MuSig2CombineSigResponse> muSig2CombineSig(com.github.lightningnetwork.lnd.signrpc.MuSig2CombineSigRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.muSig2CombineSig(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.signrpc.MuSig2CleanupResponse> muSig2Cleanup(com.github.lightningnetwork.lnd.signrpc.MuSig2CleanupRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.muSig2Cleanup(request, new RemoteSingleObserver<>(emitter)));
    }

}