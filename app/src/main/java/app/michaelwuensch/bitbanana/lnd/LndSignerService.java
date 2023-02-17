package app.michaelwuensch.bitbanana.lnd;

import io.reactivex.rxjava3.core.Single;

public interface LndSignerService {

    Single<com.github.lightningnetwork.lnd.signrpc.SignResp> signOutputRaw(com.github.lightningnetwork.lnd.signrpc.SignReq request);

    Single<com.github.lightningnetwork.lnd.signrpc.InputScriptResp> computeInputScript(com.github.lightningnetwork.lnd.signrpc.SignReq request);

    Single<com.github.lightningnetwork.lnd.signrpc.SignMessageResp> signMessage(com.github.lightningnetwork.lnd.signrpc.SignMessageReq request);

    Single<com.github.lightningnetwork.lnd.signrpc.VerifyMessageResp> verifyMessage(com.github.lightningnetwork.lnd.signrpc.VerifyMessageReq request);

    Single<com.github.lightningnetwork.lnd.signrpc.SharedKeyResponse> deriveSharedKey(com.github.lightningnetwork.lnd.signrpc.SharedKeyRequest request);

    Single<com.github.lightningnetwork.lnd.signrpc.MuSig2CombineKeysResponse> muSig2CombineKeys(com.github.lightningnetwork.lnd.signrpc.MuSig2CombineKeysRequest request);

    Single<com.github.lightningnetwork.lnd.signrpc.MuSig2SessionResponse> muSig2CreateSession(com.github.lightningnetwork.lnd.signrpc.MuSig2SessionRequest request);

    Single<com.github.lightningnetwork.lnd.signrpc.MuSig2RegisterNoncesResponse> muSig2RegisterNonces(com.github.lightningnetwork.lnd.signrpc.MuSig2RegisterNoncesRequest request);

    Single<com.github.lightningnetwork.lnd.signrpc.MuSig2SignResponse> muSig2Sign(com.github.lightningnetwork.lnd.signrpc.MuSig2SignRequest request);

    Single<com.github.lightningnetwork.lnd.signrpc.MuSig2CombineSigResponse> muSig2CombineSig(com.github.lightningnetwork.lnd.signrpc.MuSig2CombineSigRequest request);

    Single<com.github.lightningnetwork.lnd.signrpc.MuSig2CleanupResponse> muSig2Cleanup(com.github.lightningnetwork.lnd.signrpc.MuSig2CleanupRequest request);
}