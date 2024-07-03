package app.michaelwuensch.bitbanana.backends.lnd;

import static app.michaelwuensch.bitbanana.util.PaymentUtil.KEYSEND_MESSAGE_RECORD;

import com.github.lightningnetwork.lnd.lnrpc.ChanInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Channel;
import com.github.lightningnetwork.lnd.lnrpc.ChannelBalanceRequest;
import com.github.lightningnetwork.lnd.lnrpc.ChannelBalanceResponse;
import com.github.lightningnetwork.lnd.lnrpc.ChannelCloseSummary;
import com.github.lightningnetwork.lnd.lnrpc.ChannelPoint;
import com.github.lightningnetwork.lnd.lnrpc.ClosedChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.ConnectPeerRequest;
import com.github.lightningnetwork.lnd.lnrpc.DisconnectPeerRequest;
import com.github.lightningnetwork.lnd.lnrpc.FailedUpdate;
import com.github.lightningnetwork.lnd.lnrpc.Feature;
import com.github.lightningnetwork.lnd.lnrpc.ForwardingEvent;
import com.github.lightningnetwork.lnd.lnrpc.ForwardingHistoryRequest;
import com.github.lightningnetwork.lnd.lnrpc.GetInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.GetTransactionsRequest;
import com.github.lightningnetwork.lnd.lnrpc.Hop;
import com.github.lightningnetwork.lnd.lnrpc.InboundFee;
import com.github.lightningnetwork.lnd.lnrpc.Initiator;
import com.github.lightningnetwork.lnd.lnrpc.Invoice;
import com.github.lightningnetwork.lnd.lnrpc.InvoiceSubscription;
import com.github.lightningnetwork.lnd.lnrpc.LightningAddress;
import com.github.lightningnetwork.lnd.lnrpc.ListChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.ListInvoiceRequest;
import com.github.lightningnetwork.lnd.lnrpc.ListPaymentsRequest;
import com.github.lightningnetwork.lnd.lnrpc.ListPeersRequest;
import com.github.lightningnetwork.lnd.lnrpc.NewAddressRequest;
import com.github.lightningnetwork.lnd.lnrpc.NodeAddress;
import com.github.lightningnetwork.lnd.lnrpc.NodeInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Payment;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsRequest;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse;
import com.github.lightningnetwork.lnd.lnrpc.PolicyUpdateRequest;
import com.github.lightningnetwork.lnd.lnrpc.Resolution;
import com.github.lightningnetwork.lnd.lnrpc.SendCoinsRequest;
import com.github.lightningnetwork.lnd.lnrpc.SignMessageRequest;
import com.github.lightningnetwork.lnd.lnrpc.TimestampedError;
import com.github.lightningnetwork.lnd.lnrpc.Transaction;
import com.github.lightningnetwork.lnd.lnrpc.Utxo;
import com.github.lightningnetwork.lnd.lnrpc.VerifyMessageRequest;
import com.github.lightningnetwork.lnd.lnrpc.WalletBalanceRequest;
import com.github.lightningnetwork.lnd.lnrpc.WalletBalanceResponse;
import com.github.lightningnetwork.lnd.routerrpc.RouteFeeRequest;
import com.github.lightningnetwork.lnd.routerrpc.SendPaymentRequest;
import com.github.lightningnetwork.lnd.walletrpc.EstimateFeeRequest;
import com.github.lightningnetwork.lnd.walletrpc.EstimateFeeResponse;
import com.github.lightningnetwork.lnd.walletrpc.ListUnspentRequest;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.Channels.ChannelConstraints;
import app.michaelwuensch.bitbanana.models.Channels.CloseChannelRequest;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannelRequest;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.PublicChannelInfo;
import app.michaelwuensch.bitbanana.models.Channels.RoutingPolicy;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.Channels.UpdateRoutingPolicyRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceResponse;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.CustomRecord;
import app.michaelwuensch.bitbanana.models.FeeEstimateResponse;
import app.michaelwuensch.bitbanana.models.Forward;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnFeature;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.models.NodeInfo;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.models.Peer;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.models.SendOnChainPaymentRequest;
import app.michaelwuensch.bitbanana.models.SignMessageResponse;
import app.michaelwuensch.bitbanana.models.TimestampedMessage;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.PaymentRequestUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UtilFunctions;
import app.michaelwuensch.bitbanana.util.Version;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Class that translates BitBanana backend interactions into LND API specific interactions.
 * <p>
 * You can find the LND API documentation here:
 * https://lightning.engineering/api-docs/api/lnd/
 */
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
                            .setNetwork(BackendConfig.Network.parseFromString(response.getChains(0).getNetwork()))
                            .setSynced(response.getSyncedToChain())
                            .setAvatarMaterial(response.getIdentityPubkey())
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
                    List<String> addresses = new ArrayList<>();
                    for (NodeAddress address : response.getNode().getAddressesList())
                        addresses.add(address.getAddr());
                    return NodeInfo.newBuilder()
                            .setPubKey(response.getNode().getPubKey())
                            .setAlias(response.getNode().getAlias())
                            .setAddresses(addresses)
                            .setNumChannels(response.getNumChannels())
                            .setTotalCapacity(response.getTotalCapacity() * 1000L)
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
                    .setOnChainConfirmed(walletBalanceResponse.getConfirmedBalance() * 1000L)
                    .setOnChainUnconfirmed(walletBalanceResponse.getUnconfirmedBalance() * 1000L)
                    .setChannelBalance(channelBalanceResponse.getLocalBalance().getMsat())
                    .setChannelBalancePendingOpen(channelBalanceResponse.getPendingOpenLocalBalance().getMsat())
                    .setChannelBalanceLimbo(pendingChannelsResponse.getTotalLimboBalance() * 1000L)
                    .build();

            return balances;
        });
    }

    @Override
    public Single<SignMessageResponse> signMessageWithNode(String message) {
        SignMessageRequest signMessageRequest = SignMessageRequest.newBuilder()
                .setMsg(ByteString.copyFrom(message, StandardCharsets.UTF_8))
                .build();

        return LndConnection.getInstance().getLightningService().signMessage(signMessageRequest)
                .map(response -> {
                    return SignMessageResponse.newBuilder()
                            .setSignature(response.getSignature()) //This is not really correct as we use zbase encoded here, but needed for lnurl Auth to work.
                            .setZBase(response.getSignature())
                            .build();
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
    public Single<List<app.michaelwuensch.bitbanana.models.Utxo>> listUTXOs(long currentBlockHeight) {
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
    public Single<List<OpenChannel>> listOpenChannels() {
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
    public Single<List<PendingChannel>> listPendingChannels() {
        return LndConnection.getInstance().getLightningService().pendingChannels(PendingChannelsRequest.newBuilder().build())
                .map(response -> {
                    List<PendingChannel> pendingChannelsList = new ArrayList<>();
                    for (PendingChannelsResponse.PendingOpenChannel channel : response.getPendingOpenChannelsList())
                        pendingChannelsList.add(PendingChannel.newBuilder()
                                .setRemotePubKey(channel.getChannel().getRemoteNodePub())
                                //.setShortChannelId() NEVER AVAILABLE AT THIS STATE
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
    public Single<List<ClosedChannel>> listClosedChannels() {
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
                    RoutingPolicy.Builder Node1Policy = RoutingPolicy.newBuilder();
                    Node1Policy.setFeeBase(response.getNode1Policy().getFeeBaseMsat())
                            .setFeeRate(response.getNode1Policy().getFeeRateMilliMsat())
                            .setDelay(response.getNode1Policy().getTimeLockDelta())
                            .setMinHTLC(response.getNode1Policy().getMinHtlc())
                            .setMaxHTLC(response.getNode1Policy().getMaxHtlcMsat());

                    if (Wallet.getInstance().getCurrentNodeInfo().getVersion().compareTo(new Version("0.18.0")) >= 0)  //ToDo: Remove when support for LND 0.17.x is removed
                        Node1Policy.setInboundFeeBase(response.getNode1Policy().getInboundFeeBaseMsat())
                                .setInboundFeeRate(response.getNode1Policy().getInboundFeeRateMilliMsat());

                    RoutingPolicy.Builder Node2Policy = RoutingPolicy.newBuilder();
                    Node2Policy.setFeeBase(response.getNode2Policy().getFeeBaseMsat())
                            .setFeeRate(response.getNode2Policy().getFeeRateMilliMsat())
                            .setDelay(response.getNode2Policy().getTimeLockDelta())
                            .setMinHTLC(response.getNode2Policy().getMinHtlc())
                            .setMaxHTLC(response.getNode2Policy().getMaxHtlcMsat());

                    if (Wallet.getInstance().getCurrentNodeInfo().getVersion().compareTo(new Version("0.18.0")) >= 0)  //ToDo: Remove when support for LND 0.17.x is removed
                        Node2Policy.setInboundFeeBase(response.getNode2Policy().getInboundFeeBaseMsat())
                                .setInboundFeeRate(response.getNode2Policy().getInboundFeeRateMilliMsat());


                    return PublicChannelInfo.newBuilder()
                            .setShortChannelId(ApiUtil.ScidFromLong(response.getChannelId()))
                            .setFundingOutpoint(ApiUtil.OutpointFromString(response.getChanPoint()))
                            .setNode1PubKey(response.getNode1Pub())
                            .setNode2PubKey(response.getNode2Pub())
                            .setNode1RoutingPolicy(Node1Policy.build())
                            .setNode2RoutingPolicy(Node2Policy.build())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetch public channel info failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<String>> updateRoutingPolicy(UpdateRoutingPolicyRequest updateRoutingPolicyRequest) {
        PolicyUpdateRequest.Builder LNDRequest = PolicyUpdateRequest.newBuilder();
        if (updateRoutingPolicyRequest.hasChannel()) {
            LNDRequest.setChanPoint(ChannelPoint.newBuilder()
                    .setFundingTxidStr(updateRoutingPolicyRequest.getChannel().getFundingOutpoint().getTransactionID())
                    .setOutputIndex(updateRoutingPolicyRequest.getChannel().getFundingOutpoint().getOutputIndex())
                    .build());
        } else {
            LNDRequest.setGlobal(true);
        }
        if (updateRoutingPolicyRequest.hasFeeBase())
            LNDRequest.setBaseFeeMsat(updateRoutingPolicyRequest.getFeeBase());
        if (updateRoutingPolicyRequest.hasFeeRate())
            LNDRequest.setFeeRatePpm((int) updateRoutingPolicyRequest.getFeeRate());
        if (updateRoutingPolicyRequest.hasDelay())
            LNDRequest.setTimeLockDelta(updateRoutingPolicyRequest.getDelay());
        if (updateRoutingPolicyRequest.hasMinHTLC()) {
            LNDRequest.setMinHtlcMsat(updateRoutingPolicyRequest.getMinHTLC());
            LNDRequest.setMinHtlcMsatSpecified(true);
        }
        if (updateRoutingPolicyRequest.hasInboundFeeRate() || updateRoutingPolicyRequest.hasInboundFeeBase()) {
            InboundFee.Builder ifb = InboundFee.newBuilder();
            if (updateRoutingPolicyRequest.hasInboundFeeBase())
                ifb.setBaseFeeMsat((int) updateRoutingPolicyRequest.getInboundFeeBase());
            if (updateRoutingPolicyRequest.hasInboundFeeRate())
                ifb.setFeeRatePpm((int) updateRoutingPolicyRequest.getInboundFeeRate());
            LNDRequest.setInboundFee(ifb);
        }


        if (updateRoutingPolicyRequest.hasMaxHTLC())
            LNDRequest.setMaxHtlcMsat(updateRoutingPolicyRequest.getMaxHTLC());

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

    private LnInvoice getInvoiceFromLNDInvoice(Invoice lndInvoice) {
        String keysendMessage = null;
        try {
            Map<Long, ByteString> customRecords = lndInvoice.getHtlcs(0).getCustomRecordsMap();
            for (Long key : customRecords.keySet()) {
                if (key == KEYSEND_MESSAGE_RECORD) {
                    keysendMessage = customRecords.get(key).toString(StandardCharsets.UTF_8);
                    break;
                }
            }
        } catch (Exception ignored) {

        }

        return LnInvoice.newBuilder()
                .setBolt11(lndInvoice.getPaymentRequest())
                .setPaymentHash(ApiUtil.StringFromHexByteString(lndInvoice.getRHash()))
                .setAmountRequested(lndInvoice.getValueMsat())
                .setAmountPaid(lndInvoice.getAmtPaidMsat())
                .setCreatedAt(lndInvoice.getCreationDate())
                .setPaidAt(lndInvoice.getSettleDate())
                .setExpiresAt(lndInvoice.getCreationDate() + lndInvoice.getExpiry())
                .setAddIndex(lndInvoice.getAddIndex())
                .setMemo(lndInvoice.getMemo())
                .setKeysendMessage(keysendMessage)
                .build();
    }

    private Single<List<LnInvoice>> getInvoicesPage(int page, int pageSize) {
        ListInvoiceRequest invoiceRequest = ListInvoiceRequest.newBuilder()
                .setNumMaxInvoices(pageSize)
                .setIndexOffset((long) page * pageSize)
                .build();

        return LndConnection.getInstance().getLightningService().listInvoices(invoiceRequest)
                .map(response -> {
                    List<LnInvoice> invoicesList = new ArrayList<>();
                    for (Invoice invoice : response.getInvoicesList()) {
                        invoicesList.add(getInvoiceFromLNDInvoice(invoice));
                    }
                    return invoicesList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching Invoice page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<LnInvoice>> listInvoices(int page, int pageSize) {
        return getInvoicesPage(page, pageSize)
                .flatMap(data -> {
                    if (data.isEmpty()) {
                        return Single.just(Collections.emptyList()); // No more pages, return an empty list
                    } else if (data.size() < pageSize) {
                        return Single.just(data);
                    } else {
                        return listInvoices(page + 1, pageSize)
                                .flatMap(nextPageData -> {
                                    data.addAll(nextPageData); // Combine current page data with next page data
                                    return Single.just(data);
                                });
                    }
                });
    }

    @Override
    public Observable<LnInvoice> subscribeToInvoices() {
        return LndConnection.getInstance().getLightningService().subscribeInvoices(InvoiceSubscription.newBuilder().build())
                .map(invoice -> {
                    return getInvoiceFromLNDInvoice(invoice);
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Invoice subscription failed: " + throwable.fillInStackTrace()));
    }

    private OnChainTransaction getOnChainTransactionFromLNDTransaction(Transaction lndTransaction) {
        return OnChainTransaction.newBuilder()
                .setTransactionId(lndTransaction.getTxHash())
                .setAmount(lndTransaction.getAmount() * 1000)
                .setBlockHeight(lndTransaction.getBlockHeight())
                .setFee(lndTransaction.getTotalFees() * 1000)
                .setTimeStamp(lndTransaction.getTimeStamp())
                //.setLabel(lndTransaction.getLabel())
                .build();
    }

    @Override
    public Single<List<OnChainTransaction>> listOnChainTransactions() {
        GetTransactionsRequest request = GetTransactionsRequest.newBuilder()
                .setEndHeight(-1) //include unconfirmed
                .build();

        return LndConnection.getInstance().getLightningService().getTransactions(request)
                .map(response -> {
                    List<OnChainTransaction> transactionList = new ArrayList<>();
                    for (Transaction transaction : response.getTransactionsList())
                        transactionList.add(getOnChainTransactionFromLNDTransaction(transaction));
                    return transactionList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching OnChainTransactions failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Observable<OnChainTransaction> subscribeToOnChainTransactions() {
        GetTransactionsRequest request = GetTransactionsRequest.newBuilder()
                .setEndHeight(-1) //include unconfirmed
                .build();
        return LndConnection.getInstance().getLightningService().subscribeTransactions(request)
                .map(transaction -> {
                    return getOnChainTransactionFromLNDTransaction(transaction);
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "OnChainTransaction subscription failed: " + throwable.fillInStackTrace()));
    }

    private LnPayment getLnPaymentFromLNDPayment(Payment lndPayment) {
        Hop lastHop = lndPayment.getHtlcs(0).getRoute().getHops(lndPayment.getHtlcs(0).getRoute().getHopsCount() - 1);
        String keysendMessage = null;
        try {
            Map<Long, ByteString> customRecords = lastHop.getCustomRecordsMap();
            for (Long key : customRecords.keySet()) {
                if (key == KEYSEND_MESSAGE_RECORD) {
                    keysendMessage = customRecords.get(key).toString(StandardCharsets.UTF_8);
                    break;
                }
            }
        } catch (Exception ignored) {

        }

        LnPayment.Status paymentStatus;
        switch (lndPayment.getStatus()) {
            case SUCCEEDED:
                paymentStatus = LnPayment.Status.SUCCEEDED;
                break;
            case FAILED:
                paymentStatus = LnPayment.Status.FAILED;
                break;
            default:
                paymentStatus = LnPayment.Status.PENDING;
        }

        return LnPayment.newBuilder()
                .setPaymentHash(lndPayment.getPaymentHash())
                .setPaymentPreimage(lndPayment.getPaymentPreimage())
                .setDestinationPubKey(lastHop.getPubKey())
                .setStatus(paymentStatus)
                .setAmountPaid(lndPayment.getValueMsat())
                .setFee(lndPayment.getFeeMsat())
                .setCreatedAt(lndPayment.getCreationTimeNs() / 1000000000)
                .setBolt11(lndPayment.getPaymentRequest())
                .setMemo(PaymentRequestUtil.getMemo(lndPayment.getPaymentRequest()))
                .setKeysendMessage(keysendMessage)
                .build();
    }

    private Single<List<LnPayment>> getLnPaymentPage(int page, int pageSize) {
        ListPaymentsRequest request = ListPaymentsRequest.newBuilder()
                .setIncludeIncomplete(false)
                .setMaxPayments(pageSize)
                .setIndexOffset((long) page * pageSize)
                .build();

        return LndConnection.getInstance().getLightningService().listPayments(request)
                .map(response -> {
                    List<LnPayment> paymentsList = new ArrayList<>();
                    for (Payment payment : response.getPaymentsList()) {
                        paymentsList.add(getLnPaymentFromLNDPayment(payment));
                    }
                    return paymentsList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching payment page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<LnPayment>> listLnPayments(int page, int pageSize) {
        return getLnPaymentPage(page, pageSize)
                .flatMap(data -> {
                    if (data.isEmpty()) {
                        return Single.just(Collections.emptyList()); // No more pages, return an empty list
                    } else if (data.size() < pageSize) {
                        return Single.just(data);
                    } else {
                        return listLnPayments(page + 1, pageSize)
                                .flatMap(nextPageData -> {
                                    data.addAll(nextPageData); // Combine current page data with next page data
                                    return Single.just(data);
                                });
                    }
                });
    }

    private Single<List<Forward>> getForwardPage(int page, int pageSize, long startTime) {
        ForwardingHistoryRequest request = ForwardingHistoryRequest.newBuilder()
                .setStartTime(startTime)
                .setNumMaxEvents(pageSize)
                .setIndexOffset(page * pageSize)
                .build();

        return LndConnection.getInstance().getLightningService().forwardingHistory(request)
                .map(response -> {
                    List<Forward> forwardList = new ArrayList<>();
                    for (ForwardingEvent forwardingEvent : response.getForwardingEventsList()) {
                        forwardList.add(Forward.newBuilder()
                                .setAmountIn(forwardingEvent.getAmtInMsat())
                                .setAmountOut(forwardingEvent.getAmtOutMsat())
                                .setChannelIdIn(ApiUtil.ScidFromLong(forwardingEvent.getChanIdIn()))
                                .setChannelIdOut(ApiUtil.ScidFromLong(forwardingEvent.getChanIdOut()))
                                .setFee(forwardingEvent.getFeeMsat())
                                .setTimestampNs(forwardingEvent.getTimestampNs())
                                .build());
                    }
                    return forwardList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching forwarding events page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<Forward>> listForwards(int page, int pageSize, long startTime) {
        return getForwardPage(page, pageSize, startTime)
                .flatMap(data -> {
                    if (data.isEmpty()) {
                        return Single.just(Collections.emptyList()); // No more pages, return an empty list
                    } else if (data.size() < pageSize) {
                        return Single.just(data);
                    } else {
                        return listForwards(page + 1, pageSize, startTime)
                                .flatMap(nextPageData -> {
                                    data.addAll(nextPageData); // Combine current page data with next page data
                                    return Single.just(data);
                                });
                    }
                });
    }

    @Override
    public Single<List<Peer>> listPeers() {
        ListPeersRequest request = ListPeersRequest.newBuilder()
                .build();

        return LndConnection.getInstance().getLightningService().listPeers(request)
                .map(response -> {
                    List<Peer> peerList = new ArrayList<>();
                    for (com.github.lightningnetwork.lnd.lnrpc.Peer peer : response.getPeersList()) {
                        List<TimestampedMessage> errorMessages = new ArrayList<>();
                        for (TimestampedError e : peer.getErrorsList())
                            errorMessages.add(TimestampedMessage.newBuilder()
                                    .setMessage(e.getError())
                                    .setTimestamp(e.getTimestamp())
                                    .build());
                        List<LnFeature> features = new ArrayList<>();
                        for (Map.Entry<Integer, Feature> feature : peer.getFeaturesMap().entrySet()) {
                            features.add(LnFeature.newBuilder()
                                    .setFeatureNumber(feature.getKey())
                                    .setName(feature.getValue().getName())
                                    .build());
                        }
                        peerList.add(Peer.newBuilder()
                                .setPubKey(peer.getPubKey())
                                .setAddress(peer.getAddress())
                                .setPing(peer.getPingTime())
                                .setFlapCount(peer.getFlapCount())
                                .setLastFlapTimestamp(peer.getLastFlapNs())
                                .setErrorMessages(errorMessages)
                                .setFeatures(features)
                                .build());
                    }
                    return peerList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching peers failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<CreateInvoiceResponse> createInvoice(CreateInvoiceRequest createInvoiceRequest) {
        Invoice request = Invoice.newBuilder()
                .setValueMsat(createInvoiceRequest.getAmount())
                .setMemo(createInvoiceRequest.getDescription())
                .setExpiry(createInvoiceRequest.getExpiry())
                .setPrivate(createInvoiceRequest.getIncludeRouteHints())
                .build();

        return LndConnection.getInstance().getLightningService().addInvoice(request)
                .map(response -> {
                    return CreateInvoiceResponse.newBuilder()
                            .setBolt11(response.getPaymentRequest())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Creating invoice failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<String> getNewOnchainAddress(NewOnChainAddressRequest newOnChainAddressRequest) {
        int addressType = 5;
        switch (newOnChainAddressRequest.getType()) {
            case SEGWIT_COMPATIBILITY:
                addressType = newOnChainAddressRequest.getUnused() ? 3 : 1;
                break;
            case SEGWIT:
                addressType = newOnChainAddressRequest.getUnused() ? 2 : 0;
                break;
            case TAPROOT:
                addressType = newOnChainAddressRequest.getUnused() ? 5 : 4;
        }
        NewAddressRequest request = NewAddressRequest.newBuilder()
                .setTypeValue(addressType) // 2 = unused bech32 (native segwit) , 3 = unused Segwit compatibility address, 5 = unused Taproot (bech32m)
                .build();

        return LndConnection.getInstance().getLightningService().newAddress(request)
                .map(response -> {
                    return response.getAddress();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Creating new OnChainAddress failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<SendLnPaymentResponse> sendLnPayment(SendLnPaymentRequest sendLnPaymentRequest) {
        SendPaymentRequest request = null;
        switch (sendLnPaymentRequest.getPaymentType()) {
            case BOLT11_INVOICE:
                SendPaymentRequest.Builder requestBuilder = SendPaymentRequest.newBuilder()
                        .setPaymentRequest(sendLnPaymentRequest.getBolt11().getBolt11String())
                        .setFeeLimitMsat(sendLnPaymentRequest.getMaxFee())
                        .setNoInflightUpdates(true)
                        .setTimeoutSeconds(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier())
                        .setMaxParts(10);

                if (sendLnPaymentRequest.getBolt11().hasNoAmountSpecified())
                    requestBuilder.setAmtMsat(sendLnPaymentRequest.getAmount());

                request = requestBuilder.build();
                break;
            case KEYSEND:
                Map<Long, ByteString> customRecords = new HashMap<>();
                for (CustomRecord record : sendLnPaymentRequest.getCustomRecords())
                    customRecords.put(record.getFieldNumber(), ApiUtil.ByteStringFromHexString(record.getValue()));

                request = SendPaymentRequest.newBuilder()
                        .setDest(ApiUtil.ByteStringFromHexString(sendLnPaymentRequest.getDestinationPubKey()))
                        .setAmtMsat(sendLnPaymentRequest.getAmount())
                        .setFeeLimitSat(sendLnPaymentRequest.getMaxFee())
                        .setPaymentHash(ApiUtil.ByteStringFromHexString(sendLnPaymentRequest.getPaymentHash()))
                        .setNoInflightUpdates(true)
                        .putAllDestCustomRecords(customRecords)
                        .setTimeoutSeconds(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier())
                        .setMaxParts(1) // KeySend does not support multi path payments
                        .build();
                break;
        }

        return LndConnection.getInstance().getRouterService().sendPaymentV2(request)
                .map(response -> {
                    switch (response.getStatus()) {
                        case SUCCEEDED:
                            return SendLnPaymentResponse.newBuilder()
                                    .setPaymentPreimage(response.getPaymentPreimage())
                                    .build();
                        case FAILED:
                            app.michaelwuensch.bitbanana.models.SendLnPaymentResponse.FailureReason failureReason = null;
                            switch (response.getFailureReason()) {
                                case FAILURE_REASON_TIMEOUT:
                                    failureReason = SendLnPaymentResponse.FailureReason.TIMEOUT;
                                    break;
                                case FAILURE_REASON_NO_ROUTE:
                                    failureReason = SendLnPaymentResponse.FailureReason.NO_ROUTE;
                                    break;
                                case FAILURE_REASON_INSUFFICIENT_BALANCE:
                                    failureReason = SendLnPaymentResponse.FailureReason.INSUFFICIENT_FUNDS;
                                    break;
                                case FAILURE_REASON_INCORRECT_PAYMENT_DETAILS:
                                    failureReason = SendLnPaymentResponse.FailureReason.INCORRECT_PAYMENT_DETAILS;
                                    break;
                                default:
                                    failureReason = SendLnPaymentResponse.FailureReason.UNKNOWN;
                            }
                            return SendLnPaymentResponse.newBuilder()
                                    .setFailureReason(failureReason)
                                    .setAmount(response.getValueMsat())
                                    .build();
                        default:
                            return SendLnPaymentResponse.newBuilder()
                                    .setFailureReason(SendLnPaymentResponse.FailureReason.UNKNOWN)
                                    .setAmount(response.getValueMsat())
                                    .build();
                    }
                })
                .firstOrError()
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Error sending lightning payment: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable sendOnChainPayment(SendOnChainPaymentRequest sendOnChainPaymentRequest) {

        SendCoinsRequest request = SendCoinsRequest.newBuilder()
                .setAddr(sendOnChainPaymentRequest.getAddress())
                .setAmount(sendOnChainPaymentRequest.getAmount() / 1000)
                .setSatPerVbyte(sendOnChainPaymentRequest.getSatPerVByte())
                .build();

        return LndConnection.getInstance().getLightningService().sendCoins(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Sending on chain payment failed: " + throwable.getMessage()));
    }

    @Override
    public Completable connectPeer(LightningNodeUri lightningNodeUri) {
        LightningAddress.Builder lightningAddressBuilder = LightningAddress.newBuilder()
                .setPubkey(lightningNodeUri.getPubKey());

        if (lightningNodeUri.hasHost())
            lightningAddressBuilder.setHost(lightningNodeUri.getHost());

        LightningAddress lightningAddress = lightningAddressBuilder.build();

        ConnectPeerRequest request = ConnectPeerRequest.newBuilder()
                .setAddr(lightningAddress)
                .build();

        return LndConnection.getInstance().getLightningService().connectPeer(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Connecting to peer failed: " + throwable.getMessage()));
    }

    @Override
    public Completable disconnectPeer(String pubKey) {
        DisconnectPeerRequest request = DisconnectPeerRequest.newBuilder()
                .setPubKey(pubKey)
                .build();

        return LndConnection.getInstance().getLightningService().disconnectPeer(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Disconnecting from peer failed: " + throwable.getMessage()));
    }

    @Override
    public Single<FeeEstimateResponse> getFeeEstimates() {
        Single<EstimateFeeResponse> nextBlockSingle = LndConnection.getInstance().getWalletKitService().estimateFee(EstimateFeeRequest.newBuilder().setConfTarget(2).build());
        Single<EstimateFeeResponse> hourSingle = LndConnection.getInstance().getWalletKitService().estimateFee(EstimateFeeRequest.newBuilder().setConfTarget(6).build());
        Single<EstimateFeeResponse> daySingle = LndConnection.getInstance().getWalletKitService().estimateFee(EstimateFeeRequest.newBuilder().setConfTarget(144).build());
        Single<EstimateFeeResponse> minimumSingle = LndConnection.getInstance().getWalletKitService().estimateFee(EstimateFeeRequest.newBuilder().setConfTarget(1008).build());
        return Single.zip(nextBlockSingle, hourSingle, daySingle, minimumSingle, (nextBlockResponse, hourResponse, dayResponse, minimumResponse) -> {
            return FeeEstimateResponse.newBuilder()
                    .setNextBlockFee((int) (UtilFunctions.satPerKwToSatPerVByte(nextBlockResponse.getSatPerKw())))
                    .setHourFee((int) (UtilFunctions.satPerKwToSatPerVByte(hourResponse.getSatPerKw())))
                    .setDayFee((int) (UtilFunctions.satPerKwToSatPerVByte(dayResponse.getSatPerKw())))
                    .setMinimumFee((int) (UtilFunctions.satPerKwToSatPerVByte(minimumResponse.getSatPerKw())))
                    .build();
        });
    }

    @Override
    public Single<Double> getTransactionSizeVByte(String address, long amount) {
        com.github.lightningnetwork.lnd.lnrpc.EstimateFeeRequest request = com.github.lightningnetwork.lnd.lnrpc.EstimateFeeRequest.newBuilder()
                .setTargetConf(2)
                .putAddrToAmount(address, amount / 1000)
                .build();
        return LndConnection.getInstance().getLightningService().estimateFee(request)
                .map(response -> {
                    return (double) response.getFeeSat() / (double) response.getSatPerVbyte();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Getting transaction size failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable openChannel(OpenChannelRequest openChannelRequest) {
        com.github.lightningnetwork.lnd.lnrpc.OpenChannelRequest request = com.github.lightningnetwork.lnd.lnrpc.OpenChannelRequest.newBuilder()
                .setNodePubkey(ApiUtil.ByteStringFromHexString(openChannelRequest.getNodePubKey()))
                .setSatPerVbyte(openChannelRequest.getSatPerVByte())
                .setPrivate(openChannelRequest.isPrivate())
                .setLocalFundingAmount(openChannelRequest.getAmount() / 1000)
                .build();

        return LndConnection.getInstance().getLightningService().openChannel(request)
                .firstOrError()
                .ignoreElement()
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Error opening channel: " + throwable.getMessage()));
    }

    @Override
    public Completable closeChannel(CloseChannelRequest closeChannelRequest) {
        com.github.lightningnetwork.lnd.lnrpc.CloseChannelRequest request = com.github.lightningnetwork.lnd.lnrpc.CloseChannelRequest.newBuilder()
                .setChannelPoint(ChannelPoint.newBuilder()
                        .setFundingTxidStr(closeChannelRequest.getFundingOutpoint().getTransactionID())
                        .setOutputIndex(closeChannelRequest.getFundingOutpoint().getOutputIndex())
                        .build())
                .setForce(closeChannelRequest.isForceClose())
                .build();

        return LndConnection.getInstance().getLightningService().closeChannel(request)
                .firstOrError()
                .ignoreElement()
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Error closing channel: " + throwable.getMessage()));
    }

    @Override
    public Single<Long> estimateRoutingFee(String PubKey, long amount) {
        RouteFeeRequest request = RouteFeeRequest.newBuilder()
                .setDest(ApiUtil.ByteStringFromHexString(PubKey))
                .setAmtSat(Math.max(amount / 1000L, 1))
                .build();
        return LndConnection.getInstance().getRouterService().estimateRouteFee(request)
                .map(response -> {
                    return response.getRoutingFeeMsat();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Routing fee estimation failed: " + throwable.fillInStackTrace()));
    }
}
