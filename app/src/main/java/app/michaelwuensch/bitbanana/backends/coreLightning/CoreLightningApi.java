package app.michaelwuensch.bitbanana.backends.coreLightning;

import com.github.ElementsProject.lightning.cln.CheckmessageRequest;
import com.github.ElementsProject.lightning.cln.GetinfoRequest;
import com.github.ElementsProject.lightning.cln.ListfundsChannels;
import com.github.ElementsProject.lightning.cln.ListfundsOutputs;
import com.github.ElementsProject.lightning.cln.ListfundsRequest;
import com.github.ElementsProject.lightning.cln.SignmessageRequest;

import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.coreLightning.connection.CoreLightningConnection;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.Version;
import io.reactivex.rxjava3.core.Single;

public class CoreLightningApi extends Api {
    private static final String LOG_TAG = CoreLightningApi.class.getSimpleName();

    public CoreLightningApi() {

    }

    @Override
    public Single<CurrentNodeInfo> getCurrentNodeInfo() {
        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().getinfo(GetinfoRequest.newBuilder().build())
                .map(response -> {
                    String pubkey = ApiUtil.StringFromHexByteString(response.getId());
                    LightningNodeUri[] lnUris;
                    if (response.getAddressCount() != 0) {
                        lnUris = new LightningNodeUri[response.getAddressCount()];
                        for (int i = 0; i < response.getAddressCount(); i++) {
                            lnUris[i] = LightningNodeUriParser.parseNodeUri(response.getAddress(i).getAddress());
                        }
                    } else {
                        lnUris = new LightningNodeUri[1];
                        lnUris[0] = LightningNodeUriParser.parseNodeUri(pubkey);
                    }
                    return CurrentNodeInfo.newBuilder()
                            .setAlias(String.valueOf(response.getAlias()))
                            .setVersion(new Version(response.getVersion().replace("v", "")))
                            .setFullVersionString(response.getVersion())
                            .setPubKey(pubkey)
                            .setBlockHeight(response.getBlockheight())
                            .setLightningNodeUris(lnUris)
                            .setNetwork(BaseBackendConfig.Network.parseFromString(response.getNetwork()))
                            .setSynced(!(response.hasWarningBitcoindSync() || response.hasWarningLightningdSync()))
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "CoreLightning getInfo failed! " + throwable.toString()));
    }

    @Override
    public Single<Balances> getBalances() {
        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().listFunds(ListfundsRequest.newBuilder().build())
                .map(response -> {
                    long onChainConfirmed = 0;
                    long onChainUnconfirmed = 0;
                    long channelBalance = 0;
                    long channelBalancePending = 0;
                    for (ListfundsOutputs output : response.getOutputsList()) {
                        switch (output.getStatus()) {
                            case CONFIRMED:
                                onChainConfirmed = onChainConfirmed + output.getAmountMsat().getMsat() / 1000;
                                break;
                            case UNCONFIRMED:
                                onChainUnconfirmed = onChainUnconfirmed + output.getAmountMsat().getMsat() / 1000;
                                break;
                        }
                    }
                    for (ListfundsChannels channel : response.getChannelsList()) {
                        switch (channel.getState()) {
                            case ChanneldNormal:
                                channelBalance = channelBalance + channel.getOurAmountMsat().getMsat() / 1000;
                                break;
                            case ChanneldAwaitingLockin:
                            case DualopendAwaitingLockin:
                                channelBalancePending = channelBalancePending + channel.getOurAmountMsat().getMsat() / 1000;
                                break;
                        }
                    }
                    Balances balances = Balances.newBuilder()
                            .setOnChainConfirmed(onChainConfirmed)
                            .setOnChainUnconfirmed(onChainUnconfirmed)
                            .setChannelBalance(channelBalance)
                            .setChannelBalancePendingOpen(channelBalancePending)
                            .build();
                    return balances;
                });
    }

    @Override
    public Single<String> signMessageWithNode(String message) {
        SignmessageRequest signMessageRequest = SignmessageRequest.newBuilder()
                .setMessage(message)
                .build();

        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().signMessage(signMessageRequest)
                .map(response -> {
                    return response.getZbase();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Sign message failed: " + throwable.fillInStackTrace()));
    }

    public Single<VerifyMessageResponse> verifyMessageWithNode(String message, String signature) {
        CheckmessageRequest checkMessageRequest = CheckmessageRequest.newBuilder()
                .setMessage(message)
                .setZbase(signature)
                .build();

        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().checkMessage(checkMessageRequest)
                .map(response -> {
                    return VerifyMessageResponse.newBuilder()
                            .setIsValid(response.getVerified())
                            .setPubKey(ApiUtil.StringFromHexByteString(response.getPubkey()))
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Verify message failed: " + throwable.fillInStackTrace()));
    }
}
