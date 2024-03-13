package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.lightningnetwork.lnd.lnrpc.ChannelBalanceRequest;
import com.github.lightningnetwork.lnd.lnrpc.ChannelBalanceResponse;
import com.github.lightningnetwork.lnd.lnrpc.GetInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse;
import com.github.lightningnetwork.lnd.lnrpc.SignMessageRequest;
import com.github.lightningnetwork.lnd.lnrpc.Utxo;
import com.github.lightningnetwork.lnd.lnrpc.VerifyMessageRequest;
import com.github.lightningnetwork.lnd.lnrpc.WalletBalanceRequest;
import com.github.lightningnetwork.lnd.lnrpc.WalletBalanceResponse;
import com.github.lightningnetwork.lnd.walletrpc.ListUnspentRequest;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.Version;
import io.reactivex.rxjava3.core.Single;

public class LndApi extends Api {
    private static final String LOG_TAG = LndApi.class.getSimpleName();

    public LndApi() {

    }

    @Override
    public Single<CurrentNodeInfo> getCurrentNodeInfo() {
        return LndConnection.getInstance().getLightningService().getInfo(GetInfoRequest.newBuilder().build())
                .map(response -> {
                    LightningNodeUri[] lnUris;
                    if (response.getUrisCount() != 0) {
                        lnUris = new LightningNodeUri[response.getUrisCount()];
                        for (int i = 0; i < response.getUrisCount(); i++) {
                            lnUris[i] = LightningNodeUriParser.parseNodeUri(response.getUris(i));
                        }
                    } else {
                        lnUris = new LightningNodeUri[1];
                        lnUris[0] = LightningNodeUriParser.parseNodeUri(response.getIdentityPubkey());
                    }

                    return CurrentNodeInfo.newBuilder()
                            .setAlias(response.getAlias())
                            .setVersion(new Version(response.getVersion().split("-")[0]))
                            .setFullVersionString(response.getVersion())
                            .setPubKey(response.getIdentityPubkey())
                            .setBlockHeight(response.getBlockHeight())
                            .setLightningNodeUris(lnUris)
                            .setNetwork(BaseBackendConfig.Network.parseFromString(response.getChains(0).getNetwork()))
                            .setSynced(response.getSyncedToChain())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "LND getInfo failed: " + throwable.toString()));
    }

    @Override
    public Single<Balances> getBalances() {
        Single<WalletBalanceResponse> walletBalanceSingle = LndConnection.getInstance().getLightningService().walletBalance(WalletBalanceRequest.newBuilder().build());
        Single<ChannelBalanceResponse> channelBalanceSingle = LndConnection.getInstance().getLightningService().channelBalance(ChannelBalanceRequest.newBuilder().build());
        Single<PendingChannelsResponse> pendingChannelsSingle = LndConnection.getInstance().getLightningService().pendingChannels(PendingChannelsRequest.newBuilder().build());

        return Single.zip(walletBalanceSingle, channelBalanceSingle, pendingChannelsSingle, (walletBalanceResponse, channelBalanceResponse, pendingChannelsResponse) -> {

            Balances balances = Balances.newBuilder()
                    .setOnChainConfirmed(walletBalanceResponse.getConfirmedBalance())
                    .setOnChainUnconfirmed(walletBalanceResponse.getUnconfirmedBalance())
                    .setChannelBalance(channelBalanceResponse.getLocalBalance().getSat())
                    .setChannelBalancePendingOpen(channelBalanceResponse.getPendingOpenLocalBalance().getSat())
                    .setChannelBalanceLimbo(pendingChannelsResponse.getTotalLimboBalance())
                    .build();

            return balances;
        });
    }

    @Override
    public Single<String> signMessageWithNode(String message) {
        SignMessageRequest signMessageRequest = SignMessageRequest.newBuilder()
                .setMsg(ByteString.copyFrom(message, StandardCharsets.UTF_8))
                .build();

        return LndConnection.getInstance().getLightningService().signMessage(signMessageRequest)
                .map(response -> {
                    return response.getSignature();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Sign message failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<VerifyMessageResponse> verifyMessageWithNode(String message, String signature) {
        VerifyMessageRequest verifyMessageRequest = VerifyMessageRequest.newBuilder()
                .setMsg(ByteString.copyFrom(message, StandardCharsets.UTF_8))
                .setSignatureBytes(ByteString.copyFrom(signature, StandardCharsets.UTF_8))
                .build();

        return LndConnection.getInstance().getLightningService().verifyMessage(verifyMessageRequest)
                .map(response -> {
                    return VerifyMessageResponse.newBuilder()
                            .setIsValid(response.getValid())
                            .setPubKey(response.getPubkey())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Verify message failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<app.michaelwuensch.bitbanana.models.Utxo>> getUTXOs(long currentBlockHeight) {
        ListUnspentRequest listUnspentRequest = ListUnspentRequest.newBuilder()
                .setMaxConfs(999999999) // default is 0
                .build();

        return LndConnection.getInstance().getWalletKitService().listUnspent(listUnspentRequest)
                .map(response -> {
                    List<app.michaelwuensch.bitbanana.models.Utxo> utxoList = new ArrayList<>();
                    for (Utxo utxo : response.getUtxosList()) {
                        utxoList.add(app.michaelwuensch.bitbanana.models.Utxo.newBuilder()
                                .setAddress(utxo.getAddress())
                                .setAmount(utxo.getAmountSat() * 1000)
                                .setBlockHeight(currentBlockHeight - utxo.getConfirmations())
                                .setConfirmations(utxo.getConfirmations())
                                .setOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(utxo.getOutpoint().getTxidStr())
                                        .setOutputIndex(utxo.getOutpoint().getOutputIndex())
                                        .build())
                                .build());
                    }
                    return utxoList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching utxo list failed: " + throwable.fillInStackTrace()));
    }
}
