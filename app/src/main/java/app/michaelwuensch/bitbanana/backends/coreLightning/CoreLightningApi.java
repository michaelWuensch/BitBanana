package app.michaelwuensch.bitbanana.backends.coreLightning;

import com.github.ElementsProject.lightning.cln.ChannelSide;
import com.github.ElementsProject.lightning.cln.CheckmessageRequest;
import com.github.ElementsProject.lightning.cln.GetinfoRequest;
import com.github.ElementsProject.lightning.cln.ListchannelsRequest;
import com.github.ElementsProject.lightning.cln.ListclosedchannelsClosedchannels;
import com.github.ElementsProject.lightning.cln.ListclosedchannelsRequest;
import com.github.ElementsProject.lightning.cln.ListclosedchannelsResponse;
import com.github.ElementsProject.lightning.cln.ListfundsChannels;
import com.github.ElementsProject.lightning.cln.ListfundsOutputs;
import com.github.ElementsProject.lightning.cln.ListfundsRequest;
import com.github.ElementsProject.lightning.cln.ListnodesRequest;
import com.github.ElementsProject.lightning.cln.ListpeerchannelsChannels;
import com.github.ElementsProject.lightning.cln.ListpeerchannelsRequest;
import com.github.ElementsProject.lightning.cln.ListpeerchannelsResponse;
import com.github.ElementsProject.lightning.cln.SignmessageRequest;

import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.coreLightning.connection.CoreLightningConnection;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.Channels.ChannelConstraints;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.PublicChannelInfo;
import app.michaelwuensch.bitbanana.models.Channels.RoutingPolicy;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.NodeInfo;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.models.Utxo;
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
    public Single<NodeInfo> getNodeInfo(String pubKey) {
        ListnodesRequest listnodesRequest = ListnodesRequest.newBuilder()
                .setId(ApiUtil.ByteStringFromHexString(pubKey))
                .build();

        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().listNodes(listnodesRequest)
                .map(response -> {
                    return NodeInfo.newBuilder()
                            .setPubKey(ApiUtil.StringFromHexByteString(response.getNodes(0).getNodeid()))
                            .setAlias(response.getNodes(0).getAlias())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "CoreLightning getNodeInfo failed! " + throwable.toString()));
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

    public Single<List<Utxo>> getUTXOs(long currentBlockHeight) {
        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().listFunds(ListfundsRequest.newBuilder().build())
                .map(response -> {
                    List<Utxo> utxoList = new ArrayList<>();
                    for (ListfundsOutputs output : response.getOutputsList()) {
                        if (output.getStatus() == ListfundsOutputs.ListfundsOutputsStatus.CONFIRMED || output.getStatus() == ListfundsOutputs.ListfundsOutputsStatus.UNCONFIRMED) {
                            Utxo.Builder builder = Utxo.newBuilder()
                                    .setAddress(output.getAddress())
                                    .setAmount(output.getAmountMsat().getMsat())
                                    .setOutpoint(Outpoint.newBuilder()
                                            .setTransactionID(ApiUtil.StringFromHexByteString(output.getTxid()))
                                            .setOutputIndex(output.getOutput())
                                            .build())
                                    .setBlockHeight(output.getBlockheight());
                            if (output.getBlockheight() == 0) {
                                builder.setConfirmations(0);
                            } else {
                                builder.setConfirmations(currentBlockHeight - output.getBlockheight() + 1);
                            }
                            utxoList.add(builder.build());
                        }
                    }

                    return utxoList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching utxo list failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<OpenChannel>> getOpenChannels() {
        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().listPeerChannels(ListpeerchannelsRequest.newBuilder().build())
                .map(response -> {
                    List<OpenChannel> openChannelsList = new ArrayList<>();
                    for (ListpeerchannelsChannels channel : response.getChannelsList())
                        if (channel.getState() == ListpeerchannelsChannels.ListpeerchannelsChannelsState.CHANNELD_NORMAL)
                            openChannelsList.add(OpenChannel.newBuilder()
                                    .setActive(channel.getPeerConnected())
                                    .setRemotePubKey(ApiUtil.StringFromHexByteString(channel.getPeerId()))
                                    .setShortChannelId(ApiUtil.ScidFromString(channel.getShortChannelId()))
                                    //.setChannelType(???)
                                    .setInitiator(channel.getOpener() == ChannelSide.LOCAL)
                                    .setPrivate(channel.getPrivate())
                                    .setCapacity(channel.getTotalMsat().getMsat())
                                    .setLocalBalance(channel.getToUsMsat().getMsat())
                                    .setRemoteBalance(channel.getTotalMsat().getMsat() - channel.getToUsMsat().getMsat())
                                    .setLocalChannelConstraints(ChannelConstraints.newBuilder()
                                            .setSelfDelay(channel.getOurToSelfDelay())
                                            .setChannelReserve(channel.getOurReserveMsat().getMsat())
                                            .build())
                                    .setRemoteChannelConstraints(ChannelConstraints.newBuilder()
                                            .setSelfDelay(channel.getTheirToSelfDelay())
                                            .setChannelReserve(channel.getTheirReserveMsat().getMsat())
                                            .build())
                                    .setFundingOutpoint(Outpoint.newBuilder()
                                            .setTransactionID(ApiUtil.StringFromHexByteString(channel.getFundingTxid()))
                                            .setOutputIndex(channel.getFundingOutnum())
                                            .build())
                                    //.setCommitFee(???)
                                    .setTotalSent(channel.getOutFulfilledMsat().getMsat())
                                    .setTotalReceived(channel.getInFulfilledMsat().getMsat())
                                    .build());
                    return openChannelsList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "List open channels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<PendingChannel>> getPendingChannels() {
        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().listPeerChannels(ListpeerchannelsRequest.newBuilder().build())
                .map(response -> {
                            List<PendingChannel> pendingChannelsList = new ArrayList<>();
                            for (ListpeerchannelsChannels channel : response.getChannelsList())
                                if (channel.getState() != ListpeerchannelsChannels.ListpeerchannelsChannelsState.CHANNELD_NORMAL
                                        && channel.getState() != ListpeerchannelsChannels.ListpeerchannelsChannelsState.ONCHAIN) {
                                    PendingChannel.PendingType pendingType;
                                    switch (channel.getState()) {
                                        case OPENINGD:
                                        case CHANNELD_AWAITING_LOCKIN:
                                        case DUALOPEND_OPEN_INIT:
                                        case DUALOPEND_OPEN_COMMITTED:
                                        case DUALOPEND_AWAITING_LOCKIN:
                                            pendingType = PendingChannel.PendingType.PENDING_OPEN;
                                            break;
                                        case CHANNELD_SHUTTING_DOWN:
                                        case CLOSINGD_SIGEXCHANGE:
                                        case CLOSINGD_COMPLETE:
                                            pendingType = PendingChannel.PendingType.PENDING_CLOSE;
                                            break;
                                        case AWAITING_UNILATERAL:
                                        case FUNDING_SPEND_SEEN:
                                            pendingType = PendingChannel.PendingType.PENDING_FORCE_CLOSE;
                                            break;
                                        default:
                                            pendingType = PendingChannel.PendingType.UNKNOWN;
                                    }
                                    pendingChannelsList.add(PendingChannel.newBuilder()
                                            .setRemotePubKey(ApiUtil.StringFromHexByteString(channel.getPeerId()))
                                            .setShortChannelId(ApiUtil.ScidFromString(channel.getShortChannelId()))
                                            //.setChannelType(???)
                                            .setPendingType(pendingType)
                                            .setInitiator(channel.getOpener() == ChannelSide.LOCAL)
                                            .setPrivate(channel.getPrivate())
                                            .setCapacity(channel.getTotalMsat().getMsat())
                                            .setLocalBalance(channel.getToUsMsat().getMsat())
                                            .setRemoteBalance(channel.getTotalMsat().getMsat() - channel.getToUsMsat().getMsat())
                                            .setFundingOutpoint(Outpoint.newBuilder()
                                                    .setTransactionID(ApiUtil.StringFromHexByteString(channel.getFundingTxid()))
                                                    .setOutputIndex(channel.getFundingOutnum())
                                                    .build())
                                            //.setCloseTransactionId(???)
                                            //.setBlocksTilMaturity(???)
                                            //.setCommitFee(???)
                                            .setTotalSent(channel.getOutFulfilledMsat().getMsat())
                                            .setTotalReceived(channel.getInFulfilledMsat().getMsat())
                                            .build());
                                }
                            return pendingChannelsList;
                        }
                )
                .doOnError(throwable -> BBLog.w(LOG_TAG, "List pending channels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<ClosedChannel>> getClosedChannels() {
        // closed channels only returns channels that are closed for more than 100 blocks, therefore we also need to get channels from listpeerchannels with state ONCHAIN
        Single<ListclosedchannelsResponse> clnClosedChannelsList = CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().listClosedChannels(ListclosedchannelsRequest.newBuilder().build());
        Single<ListpeerchannelsResponse> clnPeerChannelsList = CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().listPeerChannels(ListpeerchannelsRequest.newBuilder().build());

        return Single.zip(clnClosedChannelsList, clnPeerChannelsList, (closedListResponse, peerListResponse) -> {
            List<ClosedChannel> closedChannelsList = new ArrayList<>();
            for (ListclosedchannelsClosedchannels channel : closedListResponse.getClosedchannelsList())
                closedChannelsList.add(ClosedChannel.newBuilder()
                        .setRemotePubKey(ApiUtil.StringFromHexByteString(channel.getPeerId()))
                        .setShortChannelId(ApiUtil.ScidFromString(channel.getShortChannelId()))
                        .setCloseTransactionId(ApiUtil.StringFromHexByteString(channel.getLastCommitmentTxid()))
                        //.setChannelType(???)
                        .setOpenInitiator(channel.getOpener() == ChannelSide.LOCAL)
                        .setCloseInitiator(channel.getCloser() == ChannelSide.LOCAL)
                        //.setCloseType(???)
                        //.setCloseHeight(???)
                        .setPrivate(channel.getPrivate())
                        .setCapacity(channel.getTotalMsat().getMsat())
                        .setLocalBalance(channel.getFinalToUsMsat().getMsat())
                        .setRemoteBalance(channel.getTotalMsat().getMsat() - channel.getFinalToUsMsat().getMsat())
                        .setFundingOutpoint(Outpoint.newBuilder()
                                .setTransactionID(ApiUtil.StringFromHexByteString(channel.getFundingTxid()))
                                .setOutputIndex(channel.getFundingOutnum())
                                .build())
                        //.setSweepTransactionIds(???)
                        .build());
            for (ListpeerchannelsChannels channel : peerListResponse.getChannelsList())
                if (channel.getState() == ListpeerchannelsChannels.ListpeerchannelsChannelsState.ONCHAIN)
                    closedChannelsList.add(ClosedChannel.newBuilder()
                            .setRemotePubKey(ApiUtil.StringFromHexByteString(channel.getPeerId()))
                            .setShortChannelId(ApiUtil.ScidFromString(channel.getShortChannelId()))
                            //.setCloseTransactionId(ApiUtil.StringFromHexByteString(channel.getScratchTxid())) // correct ?
                            //.setChannelType(???)
                            .setOpenInitiator(channel.getOpener() == ChannelSide.LOCAL)
                            .setCloseInitiator(channel.getCloser() == ChannelSide.LOCAL)
                            //.setCloseType(???)
                            .setPrivate(channel.getPrivate())
                            .setCapacity(channel.getTotalMsat().getMsat())
                            .setLocalBalance(channel.getToUsMsat().getMsat())
                            .setRemoteBalance(channel.getTotalMsat().getMsat() - channel.getToUsMsat().getMsat())
                            .setFundingOutpoint(Outpoint.newBuilder()
                                    .setTransactionID(ApiUtil.StringFromHexByteString(channel.getFundingTxid()))
                                    .setOutputIndex(channel.getFundingOutnum())
                                    .build())
                            .build());
            return closedChannelsList;
        });
    }

    @Override
    public Single<PublicChannelInfo> getPublicChannelInfo(ShortChannelId shortChannelId) {
        ListchannelsRequest request = ListchannelsRequest.newBuilder()
                .setShortChannelId(shortChannelId.toString())
                .build();

        return CoreLightningConnection.getInstance().getCoreLightningNodeServiceService().listChannels(request)
                .map(response -> {
                    return PublicChannelInfo.newBuilder()
                            .setShortChannelId(ApiUtil.ScidFromString(response.getChannels(0).getShortChannelId()))
                            //.setFundingOutpoint(???)
                            .setNode1PubKey(ApiUtil.StringFromHexByteString(response.getChannels(0).getSource()))
                            .setNode2PubKey(ApiUtil.StringFromHexByteString(response.getChannels(1).getSource()))
                            .setNode1RoutingPolicy(RoutingPolicy.newBuilder()
                                    .setFeeBase(response.getChannels(0).getBaseFeeMillisatoshi())
                                    .setFeeRate(response.getChannels(0).getFeePerMillionth())
                                    .setDelay(response.getChannels(0).getDelay())
                                    .setMinHTLC(response.getChannels(0).getHtlcMinimumMsat().getMsat())
                                    .setMaxHTLC(response.getChannels(0).getHtlcMaximumMsat().getMsat())
                                    .build())
                            .setNode2RoutingPolicy(RoutingPolicy.newBuilder()
                                    .setFeeBase(response.getChannels(1).getBaseFeeMillisatoshi())
                                    .setFeeRate(response.getChannels(1).getFeePerMillionth())
                                    .setDelay(response.getChannels(1).getDelay())
                                    .setMinHTLC(response.getChannels(1).getHtlcMinimumMsat().getMsat())
                                    .setMaxHTLC(response.getChannels(1).getHtlcMaximumMsat().getMsat())
                                    .build())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetch public channel info failed: " + throwable.fillInStackTrace()));
    }
}
