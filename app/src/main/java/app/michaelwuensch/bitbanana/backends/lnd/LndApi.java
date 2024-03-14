package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.lightningnetwork.lnd.lnrpc.ChanInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Channel;
import com.github.lightningnetwork.lnd.lnrpc.ChannelBalanceRequest;
import com.github.lightningnetwork.lnd.lnrpc.ChannelBalanceResponse;
import com.github.lightningnetwork.lnd.lnrpc.ChannelCloseSummary;
import com.github.lightningnetwork.lnd.lnrpc.ChannelPoint;
import com.github.lightningnetwork.lnd.lnrpc.ClosedChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.FailedUpdate;
import com.github.lightningnetwork.lnd.lnrpc.GetInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Initiator;
import com.github.lightningnetwork.lnd.lnrpc.ListChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.NodeInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse;
import com.github.lightningnetwork.lnd.lnrpc.PolicyUpdateRequest;
import com.github.lightningnetwork.lnd.lnrpc.Resolution;
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
import app.michaelwuensch.bitbanana.models.Channels.ChannelConstraints;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.PublicChannelInfo;
import app.michaelwuensch.bitbanana.models.Channels.RoutingPolicy;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.Channels.UpdateRoutingPolicyRequest;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.NodeInfo;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
import app.michaelwuensch.bitbanana.util.ApiUtil;
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
    public Single<NodeInfo> getNodeInfo(String pubKey) {
        NodeInfoRequest nodeInfoRequest = NodeInfoRequest.newBuilder()
                .setPubKey(pubKey)
                .build();

        return LndConnection.getInstance().getLightningService().getNodeInfo(nodeInfoRequest)
                .map(response -> {
                    return NodeInfo.newBuilder()
                            .setPubKey(response.getNode().getPubKey())
                            .setAlias(response.getNode().getAlias())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "LND getNodeInfo failed: " + throwable.toString()));
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

    @Override
    public Single<List<OpenChannel>> getOpenChannels() {
        return LndConnection.getInstance().getLightningService().listChannels(ListChannelsRequest.newBuilder().build())
                .map(response -> {
                    List<OpenChannel> openChannelsList = new ArrayList<>();
                    for (Channel channel : response.getChannelsList())
                        openChannelsList.add(OpenChannel.newBuilder()
                                .setActive(channel.getActive())
                                .setRemotePubKey(channel.getRemotePubkey())
                                .setShortChannelId(ApiUtil.ScidFromLong(channel.getChanId()))
                                .setChannelType(channel.getCommitmentType().name())
                                .setInitiator(channel.getInitiator())
                                .setPrivate(channel.getPrivate())
                                .setCapacity(channel.getCapacity() * 1000)
                                .setLocalBalance(channel.getLocalBalance() * 1000)
                                .setRemoteBalance(channel.getRemoteBalance() * 1000)
                                .setLocalChannelConstraints(ChannelConstraints.newBuilder()
                                        .setSelfDelay(channel.getLocalConstraints().getCsvDelay())
                                        .setChannelReserve(channel.getLocalConstraints().getChanReserveSat() * 1000)
                                        .build())
                                .setRemoteChannelConstraints(ChannelConstraints.newBuilder()
                                        .setSelfDelay(channel.getRemoteConstraints().getCsvDelay())
                                        .setChannelReserve(channel.getRemoteConstraints().getChanReserveSat() * 1000)
                                        .build())
                                .setFundingOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(channel.getChannelPoint().split(":")[0])
                                        .setOutputIndex(Integer.parseInt(channel.getChannelPoint().split(":")[1]))
                                        .build())
                                .setCommitFee(channel.getCommitFee() * 1000)
                                .setTotalSent(channel.getTotalSatoshisSent() * 1000)
                                .setTotalReceived(channel.getTotalSatoshisReceived() * 1000)
                                .build());
                    return openChannelsList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "List open channels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<PendingChannel>> getPendingChannels() {
        return LndConnection.getInstance().getLightningService().pendingChannels(PendingChannelsRequest.newBuilder().build())
                .map(response -> {
                    List<PendingChannel> pendingChannelsList = new ArrayList<>();
                    for (PendingChannelsResponse.PendingOpenChannel channel : response.getPendingOpenChannelsList())
                        pendingChannelsList.add(PendingChannel.newBuilder()
                                .setRemotePubKey(channel.getChannel().getRemoteNodePub())
                                //.setShortChannelId(???)
                                .setChannelType(channel.getChannel().getCommitmentType().name())
                                .setPendingType(PendingChannel.PendingType.PENDING_OPEN)
                                .setInitiator(channel.getChannel().getInitiator() == Initiator.INITIATOR_LOCAL)
                                .setPrivate(channel.getChannel().getPrivate())
                                .setCapacity(channel.getChannel().getCapacity() * 1000)
                                .setLocalBalance(channel.getChannel().getLocalBalance() * 1000)
                                .setRemoteBalance(channel.getChannel().getRemoteBalance() * 1000)
                                .setFundingOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(channel.getChannel().getChannelPoint().split(":")[0])
                                        .setOutputIndex(Integer.parseInt(channel.getChannel().getChannelPoint().split(":")[1]))
                                        .build())
                                //.setCloseTransactionId()  NEVER AVAILABLE AT THIS STATE
                                //.setBlocksTilMaturity()  NEVER AVAILABLE AT THIS STATE
                                .setCommitFee(channel.getCommitFee() * 1000)
                                //.setTotalSent(???)
                                //.setTotalReceived(???)
                                .build());
                    for (PendingChannelsResponse.WaitingCloseChannel channel : response.getWaitingCloseChannelsList())
                        pendingChannelsList.add(PendingChannel.newBuilder()
                                .setRemotePubKey(channel.getChannel().getRemoteNodePub())
                                //.setShortChannelId(???)
                                .setChannelType(channel.getChannel().getCommitmentType().name())
                                .setPendingType(PendingChannel.PendingType.PENDING_CLOSE)
                                .setInitiator(channel.getChannel().getInitiator() == Initiator.INITIATOR_LOCAL)
                                .setPrivate(channel.getChannel().getPrivate())
                                .setCapacity(channel.getChannel().getCapacity() * 1000)
                                .setLocalBalance(channel.getChannel().getLocalBalance() * 1000)
                                .setRemoteBalance(channel.getChannel().getRemoteBalance() * 1000)
                                .setFundingOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(channel.getChannel().getChannelPoint().split(":")[0])
                                        .setOutputIndex(Integer.parseInt(channel.getChannel().getChannelPoint().split(":")[1]))
                                        .build())
                                .setCloseTransactionId(channel.getClosingTxid())
                                //.setBlocksTilMaturity()  NEVER AVAILABLE AT THIS STATE
                                //.setCommitFee(???)
                                //.setTotalSent(???)
                                //.setTotalReceived(???)
                                .build());
                    for (PendingChannelsResponse.ForceClosedChannel channel : response.getPendingForceClosingChannelsList())
                        pendingChannelsList.add(PendingChannel.newBuilder()
                                .setRemotePubKey(channel.getChannel().getRemoteNodePub())
                                //.setShortChannelId(???)
                                .setChannelType(channel.getChannel().getCommitmentType().name())
                                .setPendingType(PendingChannel.PendingType.PENDING_FORCE_CLOSE)
                                .setInitiator(channel.getChannel().getInitiator() == Initiator.INITIATOR_LOCAL)
                                .setPrivate(channel.getChannel().getPrivate())
                                .setCapacity(channel.getChannel().getCapacity() * 1000)
                                .setLocalBalance(channel.getChannel().getLocalBalance() * 1000)
                                .setRemoteBalance(channel.getChannel().getRemoteBalance() * 1000)
                                .setFundingOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(channel.getChannel().getChannelPoint().split(":")[0])
                                        .setOutputIndex(Integer.parseInt(channel.getChannel().getChannelPoint().split(":")[1]))
                                        .build())
                                .setCloseTransactionId(channel.getClosingTxid())
                                .setBlocksTilMaturity(channel.getBlocksTilMaturity())
                                //.setCommitFee(???)
                                //.setTotalSent(???)
                                //.setTotalReceived(???)
                                .build());
                    return pendingChannelsList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "List pending channels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<ClosedChannel>> getClosedChannels() {
        return LndConnection.getInstance().getLightningService().closedChannels(ClosedChannelsRequest.newBuilder().build())
                .map(response -> {
                    List<ClosedChannel> closedChannelsList = new ArrayList<>();
                    for (ChannelCloseSummary channel : response.getChannelsList()) {
                        ClosedChannel.CloseType closeType;
                        switch (channel.getCloseType()) {
                            case COOPERATIVE_CLOSE:
                                closeType = ClosedChannel.CloseType.COOPERATIVE_CLOSE;
                                break;
                            case LOCAL_FORCE_CLOSE:
                            case REMOTE_FORCE_CLOSE:
                                closeType = ClosedChannel.CloseType.FORCE_CLOSE;
                                break;
                            case BREACH_CLOSE:
                                closeType = ClosedChannel.CloseType.BREACH_CLOSE;
                                break;
                            default:
                                closeType = ClosedChannel.CloseType.UNKNOWN;
                        }
                        List<String> sweepTxIds = new ArrayList<>();
                        for (Resolution res : channel.getResolutionsList()) {
                            if (res.getSweepTxid() != null && res.getSweepTxid() != "")
                                sweepTxIds.add(res.getSweepTxid());
                        }
                        closedChannelsList.add(ClosedChannel.newBuilder()
                                .setRemotePubKey(channel.getRemotePubkey())
                                .setShortChannelId(ApiUtil.ScidFromLong(channel.getChanId()))
                                .setCloseTransactionId(channel.getClosingTxHash())
                                //.setChannelType(???)
                                .setOpenInitiator(channel.getOpenInitiator() == Initiator.INITIATOR_LOCAL)
                                .setCloseInitiator(channel.getCloseInitiator() == Initiator.INITIATOR_LOCAL)
                                .setCloseType(closeType)
                                .setCloseHeight(channel.getCloseHeight())
                                //.setPrivate(???)
                                .setCapacity(channel.getCapacity() * 1000)
                                .setLocalBalance(channel.getSettledBalance() * 1000)
                                .setRemoteBalance((channel.getCapacity() - channel.getSettledBalance()) * 1000)
                                .setFundingOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(channel.getChannelPoint().split(":")[0])
                                        .setOutputIndex(Integer.parseInt(channel.getChannelPoint().split(":")[1]))
                                        .build())
                                .setSweepTransactionIds(sweepTxIds)
                                .build());
                    }
                    return closedChannelsList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "List closed channels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<PublicChannelInfo> getPublicChannelInfo(ShortChannelId shortChannelId) {
        ChanInfoRequest request = ChanInfoRequest.newBuilder()
                .setChanId(ApiUtil.LongFromScid(shortChannelId))
                .build();

        return LndConnection.getInstance().getLightningService().getChanInfo(request)
                .map(response -> {
                    return PublicChannelInfo.newBuilder()
                            .setShortChannelId(ApiUtil.ScidFromLong(response.getChannelId()))
                            .setFundingOutpoint(ApiUtil.OutpointFromString(response.getChanPoint()))
                            .setNode1PubKey(response.getNode1Pub())
                            .setNode2PubKey(response.getNode2Pub())
                            .setNode1RoutingPolicy(RoutingPolicy.newBuilder()
                                    .setFeeBase(response.getNode1Policy().getFeeBaseMsat())
                                    .setFeeRate(response.getNode1Policy().getFeeRateMilliMsat())
                                    .setDelay(response.getNode1Policy().getTimeLockDelta())
                                    .setMinHTLC(response.getNode1Policy().getMinHtlc())
                                    .setMaxHTLC(response.getNode1Policy().getMaxHtlcMsat())
                                    .build())
                            .setNode2RoutingPolicy(RoutingPolicy.newBuilder()
                                    .setFeeBase(response.getNode2Policy().getFeeBaseMsat())
                                    .setFeeRate(response.getNode2Policy().getFeeRateMilliMsat())
                                    .setDelay(response.getNode2Policy().getTimeLockDelta())
                                    .setMinHTLC(response.getNode2Policy().getMinHtlc())
                                    .setMaxHTLC(response.getNode2Policy().getMaxHtlcMsat())
                                    .build())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetch public channel info failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<String>> updateRoutingPolicy(UpdateRoutingPolicyRequest request) {
        PolicyUpdateRequest.Builder LNDRequest = PolicyUpdateRequest.newBuilder();
        if (request.hasChannel()) {
            LNDRequest.setChanPoint(ChannelPoint.newBuilder()
                    .setFundingTxidStr(request.getChannel().getFundingOutpoint().getTransactionID())
                    .setOutputIndex(request.getChannel().getFundingOutpoint().getOutputIndex())
                    .build());
        } else {
            LNDRequest.setGlobal(true);
        }
        if (request.hasFeeBase())
            LNDRequest.setBaseFeeMsat(request.getFeeBase());
        if (request.hasFeeRate())
            LNDRequest.setFeeRatePpm((int) request.getFeeRate());
        if (request.hasDelay())
            LNDRequest.setTimeLockDelta(request.getDelay());
        if (request.hasMaxHTLC()) {
            LNDRequest.setMinHtlcMsat(request.getMinHTLC());
            LNDRequest.setMinHtlcMsatSpecified(true);
        }

        if (request.hasMaxHTLC())
            LNDRequest.setMaxHtlcMsat(request.getMaxHTLC());

        return LndConnection.getInstance().getLightningService().updateChannelPolicy(LNDRequest.build())
                .map(response -> {
                    List<String> errorList = new ArrayList<>();
                    for (FailedUpdate failedUpdate : response.getFailedUpdatesList()) {
                        errorList.add(failedUpdate.getUpdateError());
                    }
                    return errorList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetch public channel info failed: " + throwable.fillInStackTrace()));
    }
}
