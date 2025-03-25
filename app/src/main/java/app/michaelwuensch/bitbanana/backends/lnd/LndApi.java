package app.michaelwuensch.bitbanana.backends.lnd;

import static app.michaelwuensch.bitbanana.util.PaymentUtil.KEYSEND_MESSAGE_RECORD;

import com.github.lightningnetwork.lnd.invoicesrpc.LookupInvoiceMsg;
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
import com.github.lightningnetwork.lnd.lnrpc.GetDebugInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.GetInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.GetTransactionsRequest;
import com.github.lightningnetwork.lnd.lnrpc.HTLCAttempt;
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
import com.github.lightningnetwork.lnd.lnrpc.OutPoint;
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
import com.github.lightningnetwork.lnd.walletrpc.LeaseOutputRequest;
import com.github.lightningnetwork.lnd.walletrpc.ListLeasesRequest;
import com.github.lightningnetwork.lnd.walletrpc.ListLeasesResponse;
import com.github.lightningnetwork.lnd.walletrpc.ListUnspentRequest;
import com.github.lightningnetwork.lnd.walletrpc.ListUnspentResponse;
import com.github.lightningnetwork.lnd.walletrpc.ReleaseOutputRequest;
import com.github.lightningnetwork.lnd.walletrpc.UtxoLease;
import com.github.lightningnetwork.lnd.wtclientrpc.AddTowerRequest;
import com.github.lightningnetwork.lnd.wtclientrpc.DeactivateTowerRequest;
import com.github.lightningnetwork.lnd.wtclientrpc.GetTowerInfoRequest;
import com.github.lightningnetwork.lnd.wtclientrpc.ListTowersRequest;
import com.github.lightningnetwork.lnd.wtclientrpc.RemoveTowerRequest;
import com.github.lightningnetwork.lnd.wtclientrpc.Tower;
import com.github.lightningnetwork.lnd.wtclientrpc.TowerSession;
import com.github.lightningnetwork.lnd.wtclientrpc.TowerSessionInfo;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.models.BBLogItem;
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
import app.michaelwuensch.bitbanana.models.Lease;
import app.michaelwuensch.bitbanana.models.LeaseUTXORequest;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnFeature;
import app.michaelwuensch.bitbanana.models.LnHop;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.LnRoute;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.models.NodeInfo;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.models.PagedResponse;
import app.michaelwuensch.bitbanana.models.Peer;
import app.michaelwuensch.bitbanana.models.ReleaseUTXORequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.models.SendOnChainPaymentRequest;
import app.michaelwuensch.bitbanana.models.SignMessageResponse;
import app.michaelwuensch.bitbanana.models.TimestampedMessage;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
import app.michaelwuensch.bitbanana.models.Watchtower;
import app.michaelwuensch.bitbanana.models.WatchtowerSession;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
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
        BBLog.d(LOG_TAG, "getCurrentNodeInfo called.");
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

                    String avatarMaterial;
                    if (((LndBackend) BackendManager.getCurrentBackend()).getIsAccountRestricted())
                        avatarMaterial = ((LndBackend) BackendManager.getCurrentBackend()).getAccount();
                    else
                        avatarMaterial = response.getIdentityPubkey();

                    return CurrentNodeInfo.newBuilder()
                            .setAlias(response.getAlias())
                            .setVersion(new Version(response.getVersion().split("-")[0]))
                            .setFullVersionString(response.getVersion())
                            .setPubKey(response.getIdentityPubkey())
                            .setBlockHeight(response.getBlockHeight())
                            .setLightningNodeUris(lnUris)
                            .setNetwork(BackendConfig.Network.parseFromString(response.getChains(0).getNetwork()))
                            .setSynced(response.getSyncedToChain())
                            .setAvatarMaterial(avatarMaterial)
                            .build();
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getCurrentNodeInfo success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getCurrentNodeInfo failed: " + throwable.toString()));
    }

    @Override
    public Single<NodeInfo> getNodeInfo(String pubKey) {
        BBLog.d(LOG_TAG, "getNodeInfo called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getNodeInfo success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getNodeInfo failed: " + throwable.toString()));
    }

    @Override
    public Single<Balances> getBalances() {
        BBLog.d(LOG_TAG, "getBalances called.");
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
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getBalances success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getBalances failed: " + throwable.toString()));
    }

    @Override
    public Single<SignMessageResponse> signMessageWithNode(String message) {
        BBLog.d(LOG_TAG, "signMessageWithNode called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "signMessageWithNode success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "signMessageWithNode failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<VerifyMessageResponse> verifyMessageWithNode(String message, String signature) {
        BBLog.d(LOG_TAG, "verifyMessageWithNode called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "verifyMessageWithNode success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "verifyMessageWithNode failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<app.michaelwuensch.bitbanana.models.Utxo>> listUTXOs(long currentBlockHeight) {
        BBLog.d(LOG_TAG, "listUTXOs called.");
        ListUnspentRequest listUnspentRequest = ListUnspentRequest.newBuilder()
                .setMaxConfs(999999999) // default is 0
                .build();

        ListLeasesRequest listLeasesRequest = ListLeasesRequest.newBuilder().build();

        Single<ListUnspentResponse> listUnspentObservable = LndConnection.getInstance().getWalletKitService().listUnspent(listUnspentRequest);
        Single<ListLeasesResponse> listLeasesObservable = LndConnection.getInstance().getWalletKitService().listLeases(listLeasesRequest);

        return Single.zip(listUnspentObservable, listLeasesObservable, (listUnspentResponse, listLeasesResponse) -> {
                    List<app.michaelwuensch.bitbanana.models.Utxo> utxoList = new ArrayList<>();
                    for (Utxo utxo : listUnspentResponse.getUtxosList()) {
                        utxoList.add(app.michaelwuensch.bitbanana.models.Utxo.newBuilder()
                                .setAddress(utxo.getAddress())
                                .setAmount(utxo.getAmountSat() * 1000)
                                .setBlockHeight(currentBlockHeight - utxo.getConfirmations())
                                .setConfirmations(utxo.getConfirmations())
                                .setOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(utxo.getOutpoint().getTxidStr())
                                        .setOutputIndex(utxo.getOutpoint().getOutputIndex())
                                        .build())
                                .setLease(null)
                                .build());
                    }
                    for (UtxoLease utxoLease : listLeasesResponse.getLockedUtxosList()) {
                        Lease lease = Lease.newBuilder()
                                .setId(ApiUtil.StringFromHexByteString(utxoLease.getId()))
                                .setExpiration(utxoLease.getExpiration())
                                .setOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(utxoLease.getOutpoint().getTxidStr())
                                        .setOutputIndex(utxoLease.getOutpoint().getOutputIndex())
                                        .build())
                                .build();
                        utxoList.add(app.michaelwuensch.bitbanana.models.Utxo.newBuilder()
                                //.setAddress(???)
                                .setAmount(utxoLease.getValue() * 1000)
                                .setOutpoint(Outpoint.newBuilder()
                                        .setTransactionID(utxoLease.getOutpoint().getTxidStr())
                                        .setOutputIndex(utxoLease.getOutpoint().getOutputIndex())
                                        .build())
                                .setLease(lease)
                                .build());
                    }
                    return utxoList;
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "listUTXOs success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "listUTXOs failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<OpenChannel>> listOpenChannels() {
        BBLog.d(LOG_TAG, "listOpenChannels called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "listOpenChannels success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "listOpenChannels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<PendingChannel>> listPendingChannels() {
        BBLog.d(LOG_TAG, "listPendingChannels called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "listPendingChannels success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "listPendingChannels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<ClosedChannel>> listClosedChannels() {
        BBLog.d(LOG_TAG, "listClosedChannels called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "listClosedChannels success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "listClosedChannels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<PublicChannelInfo> getPublicChannelInfo(ShortChannelId shortChannelId) {
        BBLog.d(LOG_TAG, "getPublicChannelInfo called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getPublicChannelInfo success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getPublicChannelInfo failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<String>> updateRoutingPolicy(UpdateRoutingPolicyRequest updateRoutingPolicyRequest) {
        BBLog.d(LOG_TAG, "updateRoutingPolicy called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "updateRoutingPolicy success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "updateRoutingPolicy failed: " + throwable.fillInStackTrace()));
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
                .setType(LnInvoice.InvoiceType.BOLT11_INVOICE)
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

    private Single<PagedResponse<LnInvoice>> getInvoicesPage(long firstIndexOffset, int pageSize) {
        BBLog.v(LOG_TAG, "Fetching invoices page, offset:  " + (firstIndexOffset) + ", PageSize: " + pageSize);
        ListInvoiceRequest invoiceRequest = ListInvoiceRequest.newBuilder()
                .setNumMaxInvoices(pageSize)
                .setIndexOffset(firstIndexOffset)
                .build();

        return LndConnection.getInstance().getLightningService().listInvoices(invoiceRequest)
                .map(response -> {
                    List<LnInvoice> invoicesList = new ArrayList<>();
                    for (Invoice invoice : response.getInvoicesList()) {
                        invoicesList.add(getInvoiceFromLNDInvoice(invoice));
                    }
                    PagedResponse<LnInvoice> page = PagedResponse.<LnInvoice>newBuilder()
                            .setPage(invoicesList)
                            .setPageSize(response.getInvoicesCount())
                            .setFirstIndexOffset(response.getFirstIndexOffset())
                            .setLastIndexOffset(response.getLastIndexOffset())
                            .build();
                    return page;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching invoice page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<LnInvoice> getInvoice(String paymentHash) {
        BBLog.d(LOG_TAG, "getInvoice called.");
        LookupInvoiceMsg request = LookupInvoiceMsg.newBuilder()
                .setPaymentHash(ApiUtil.ByteStringFromHexString(paymentHash))
                .build();

        return LndConnection.getInstance().getInvoicesService().lookupInvoiceV2(request)
                .map(response -> {
                    return getInvoiceFromLNDInvoice(response);
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getInvoice success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getInvoice failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<LnInvoice>> listInvoices(long firstIndexOffset, int pageSize) {
        BBLog.d(LOG_TAG, "listInvoices called.");
        return getInvoicesPage(firstIndexOffset, pageSize)
                .flatMap(data -> {
                    if (data == null || data.getPage().isEmpty()) {
                        // No more pages, return an empty list
                        return Single.just(Collections.emptyList());
                    } else if (data.getPageSize() < pageSize) {
                        // Current page has fewer items than pageSize, no more data to fetch
                        return Single.just(data.getPage());
                    } else {
                        // Fetch the next page and concatenate results
                        return listInvoices(data.getLastIndexOffset(), pageSize)
                                .map(nextPageData -> {
                                    List<LnInvoice> combinedList = new ArrayList<>(data.getPage());
                                    combinedList.addAll(nextPageData);
                                    return combinedList;
                                });
                    }
                });
    }

    @Override
    public Observable<LnInvoice> subscribeToInvoices() {
        BBLog.d(LOG_TAG, "subscribeToInvoices called.");
        return LndConnection.getInstance().getLightningService().subscribeInvoices(InvoiceSubscription.newBuilder().build())
                .map(invoice -> {
                    return getInvoiceFromLNDInvoice(invoice);
                })
                .doOnSubscribe(response -> BBLog.d(LOG_TAG, "subscribeToInvoices success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "subscribeToInvoices failed: " + throwable.fillInStackTrace()));
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
        BBLog.d(LOG_TAG, "listOnChainTransactions called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "listOnChainTransactions success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "listOnChainTransactions failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Observable<OnChainTransaction> subscribeToOnChainTransactions() {
        BBLog.d(LOG_TAG, "subscribeToOnChainTransactions called.");
        GetTransactionsRequest request = GetTransactionsRequest.newBuilder()
                .setEndHeight(-1) //include unconfirmed
                .build();
        return LndConnection.getInstance().getLightningService().subscribeTransactions(request)
                .map(transaction -> {
                    return getOnChainTransactionFromLNDTransaction(transaction);
                })
                .doOnSubscribe(response -> BBLog.d(LOG_TAG, "subscribeToOnChainTransactions success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "subscribeToOnChainTransactions failed: " + throwable.fillInStackTrace()));
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

        LnPayment.Builder lnPaymentBuilder = LnPayment.newBuilder()
                .setPaymentHash(lndPayment.getPaymentHash())
                .setPaymentPreimage(lndPayment.getPaymentPreimage())
                .setDestinationPubKey(lastHop.getPubKey())
                .setStatus(paymentStatus)
                .setAmountPaid(lndPayment.getValueMsat())
                .setFee(lndPayment.getFeeMsat())
                .setCreatedAt(lndPayment.getCreationTimeNs() / 1000000000)
                .setBolt11(lndPayment.getPaymentRequest())
                //.setDescription() This information is contained in the bolt11 string and will only be extracted when it needs to be displayed to improve performance.
                .setKeysendMessage(keysendMessage);

        if (paymentStatus == LnPayment.Status.SUCCEEDED) {
            // Check for routes (payment paths)
            List<LnRoute> routes = new ArrayList<>();
            for (HTLCAttempt htlcAttempt : lndPayment.getHtlcsList()) {
                if (htlcAttempt.getStatus() == HTLCAttempt.HTLCStatus.SUCCEEDED && htlcAttempt.hasRoute()) {
                    List<LnHop> hops = new ArrayList<>();
                    int id = 1;
                    for (Hop hop : htlcAttempt.getRoute().getHopsList()) {
                        hops.add(LnHop.newBuilder()
                                .setIdInRoute(id)
                                .setShortChannelId(ApiUtil.ScidFromLong(hop.getChanId()))
                                .setPubKey(hop.getPubKey())
                                .setAmount(hop.getAmtToForwardMsat())
                                .setFee(hop.getFeeMsat())
                                .setIsLastHop(id == htlcAttempt.getRoute().getHopsList().size())
                                .build());
                        id++;
                    }
                    routes.add(LnRoute.newBuilder()
                            .setHops(hops)
                            .setAmount(htlcAttempt.getRoute().getTotalAmtMsat())
                            .setFee(htlcAttempt.getRoute().getTotalFeesMsat())
                            .build());
                }
            }
            if (!routes.isEmpty())
                lnPaymentBuilder.setRoutes(routes);
        }

        return lnPaymentBuilder.build();
    }

    private Single<PagedResponse<LnPayment>> getLnPaymentPage(long firstIndexOffset, int pageSize) {
        BBLog.v(LOG_TAG, "Fetching payments page, offset:  " + (firstIndexOffset) + ", PageSize: " + pageSize);
        ListPaymentsRequest request = ListPaymentsRequest.newBuilder()
                .setIncludeIncomplete(false)
                .setMaxPayments(pageSize)
                .setIndexOffset(firstIndexOffset)
                .build();

        return LndConnection.getInstance().getLightningService().listPayments(request)
                .map(response -> {
                    List<LnPayment> paymentsList = new ArrayList<>();
                    for (Payment payment : response.getPaymentsList()) {
                        paymentsList.add(getLnPaymentFromLNDPayment(payment));
                    }
                    PagedResponse<LnPayment> page = PagedResponse.<LnPayment>newBuilder()
                            .setPage(paymentsList)
                            .setPageSize(response.getPaymentsCount())
                            .setFirstIndexOffset(response.getFirstIndexOffset())
                            .setLastIndexOffset(response.getLastIndexOffset())
                            .build();
                    return page;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching payment page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<LnPayment>> listLnPayments(long firstIndexOffset, int pageSize) {
        BBLog.d(LOG_TAG, "listLnPayments called.");
        return getLnPaymentPage(firstIndexOffset, pageSize)
                .flatMap(data -> {
                    if (data == null || data.getPage().isEmpty()) {
                        // No more pages, return an empty list
                        return Single.just(Collections.emptyList());
                    } else if (data.getPageSize() < pageSize) {
                        // Current page has fewer items than pageSize, no more data to fetch
                        return Single.just(data.getPage());
                    } else {
                        // Fetch the next page and concatenate results
                        return listLnPayments(data.getLastIndexOffset(), pageSize)
                                .map(nextPageData -> {
                                    List<LnPayment> combinedList = new ArrayList<>(data.getPage());
                                    combinedList.addAll(nextPageData);
                                    return combinedList;
                                });
                    }
                });
    }

    private Single<PagedResponse<Forward>> getForwardPage(long firstIndexOffset, int pageSize, long startTime) {
        BBLog.v(LOG_TAG, "Fetching forwards page, offset:  " + (firstIndexOffset) + ", PageSize: " + pageSize);
        ForwardingHistoryRequest request = ForwardingHistoryRequest.newBuilder()
                .setStartTime(startTime)
                .setNumMaxEvents(pageSize)
                .setIndexOffset((int) firstIndexOffset)
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
                    PagedResponse<Forward> page = PagedResponse.<Forward>newBuilder()
                            .setPage(forwardList)
                            .setPageSize(response.getForwardingEventsCount())
                            .setLastIndexOffset(firstIndexOffset + pageSize)
                            .build();
                    return page;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching forwarding events page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<Forward>> listForwards(long firstIndexOffset, int pageSize, long startTime) {
        BBLog.d(LOG_TAG, "listForwards called.");
        return getForwardPage(firstIndexOffset, pageSize, startTime)
                .flatMap(data -> {
                    if (data == null || data.getPage().isEmpty()) {
                        // No more pages, return an empty list
                        return Single.just(Collections.emptyList());
                    } else if (data.getPageSize() < pageSize) {
                        // Current page has fewer items than pageSize, no more data to fetch
                        return Single.just(data.getPage());
                    } else {
                        // Fetch the next page and concatenate results
                        return listForwards(data.getLastIndexOffset(), pageSize, startTime)
                                .map(nextPageData -> {
                                    List<Forward> combinedList = new ArrayList<>(data.getPage());
                                    combinedList.addAll(nextPageData);
                                    return combinedList;
                                });
                    }
                });
    }

    @Override
    public Single<List<Peer>> listPeers() {
        BBLog.d(LOG_TAG, "listPeers called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "listPeers success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "listPeers failed: " + throwable.fillInStackTrace()));
    }


    private Watchtower LndWatchtowerToBBWatchtower(Tower tower) {
        boolean isActive = false;
        for (TowerSessionInfo tsi : tower.getSessionInfoList()) {
            if (tsi.getActiveSessionCandidate())
                isActive = true;
        }
        List<WatchtowerSession> sessions = new ArrayList<>();
        for (TowerSessionInfo tsi : tower.getSessionInfoList()) {
            WatchtowerSession.SessionType sessionType;
            switch (tsi.getPolicyType()) {
                case LEGACY:
                    sessionType = WatchtowerSession.SessionType.LEGACY;
                    break;
                case ANCHOR:
                    sessionType = WatchtowerSession.SessionType.ANCHOR;
                    break;
                case TAPROOT:
                    sessionType = WatchtowerSession.SessionType.TAPROOT;
                    break;
                default:
                    sessionType = WatchtowerSession.SessionType.UNKNOWN;
            }
            for (TowerSession ts : tsi.getSessionsList()) {
                sessions.add(WatchtowerSession.newBuilder()
                        .setId(ApiUtil.StringFromHexByteString(ts.getId()))
                        .setNumBackups(ts.getNumBackups())
                        .setNumPendingBackups(ts.getNumPendingBackups())
                        .setNumMaxBackups(ts.getMaxBackups())
                        .setSweepSatPerVByte(ts.getSweepSatPerVbyte())
                        .setType(sessionType)
                        .build());
            }
        }

        return Watchtower.newBuilder()
                .setPubKey(ApiUtil.StringFromHexByteString(tower.getPubkey()))
                .setAddresses(tower.getAddressesList())
                .setIsActive(isActive)
                .setSessions(sessions)
                .build();
    }

    @Override
    public Single<List<Watchtower>> listWatchtowers() {
        BBLog.d(LOG_TAG, "listWatchtowers called.");
        ListTowersRequest request = ListTowersRequest.newBuilder()
                .setIncludeSessions(true)
                .build();

        return LndConnection.getInstance().getWatchtowerClientService().listTowers(request)
                .map(response -> {
                    List<Watchtower> watchtowerList = new ArrayList<>();
                    for (Tower tower : response.getTowersList()) {
                        watchtowerList.add(LndWatchtowerToBBWatchtower(tower));
                    }
                    return watchtowerList;
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "listWatchtowers success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "listWatchtowers failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<Watchtower> getWatchtower(String pubKey) {
        BBLog.d(LOG_TAG, "getWatchtower called.");
        GetTowerInfoRequest request = GetTowerInfoRequest.newBuilder()
                .setPubkey(ApiUtil.ByteStringFromHexString(pubKey))
                .setIncludeSessions(true)
                .build();

        return LndConnection.getInstance().getWatchtowerClientService().getTowerInfo(request)
                .map(response -> {
                    return LndWatchtowerToBBWatchtower(response);
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getWatchtower success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getWatchtower failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable addWatchtower(String pubKey, String address) {
        BBLog.d(LOG_TAG, "addWatchtower called.");
        AddTowerRequest request = AddTowerRequest.newBuilder()
                .setPubkey(ApiUtil.ByteStringFromHexString(pubKey))
                .setAddress(address)
                .build();

        return LndConnection.getInstance().getWatchtowerClientService().addTower(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnComplete(() -> BBLog.d(LOG_TAG, "addWatchtower success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "addWatchtower failed: " + throwable.getMessage()));
    }

    @Override
    public Single<String> deactivateWatchtower(String pubKey) {
        BBLog.d(LOG_TAG, "deactivateWatchtower called.");
        DeactivateTowerRequest request = DeactivateTowerRequest.newBuilder()
                .setPubkey(ApiUtil.ByteStringFromHexString(pubKey))
                .build();

        return LndConnection.getInstance().getWatchtowerClientService().deactivateTower(request)
                .map(response -> {
                    return response.getStatus();
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "deactivateWatchtower success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "deactivateWatchtower failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable removeWatchtower(String pubKey) {
        BBLog.d(LOG_TAG, "removeWatchtower called.");
        RemoveTowerRequest request = RemoveTowerRequest.newBuilder()
                .setPubkey(ApiUtil.ByteStringFromHexString(pubKey))
                .build();

        return LndConnection.getInstance().getWatchtowerClientService().removeTower(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnComplete(() -> BBLog.d(LOG_TAG, "removeWatchtower success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "removeWatchtower failed: " + throwable.getMessage()));
    }

    @Override
    public Single<LightningNodeUri> getOwnWatchtowerInfo() {
        BBLog.d(LOG_TAG, "getOwnWatchtowerInfo called.");
        com.github.lightningnetwork.lnd.watchtowerrpc.GetInfoRequest request = com.github.lightningnetwork.lnd.watchtowerrpc.GetInfoRequest.newBuilder()
                .build();

        return LndConnection.getInstance().getWatchtowerService().getInfo(request)
                .flatMap(response -> {
                    if (!response.getUrisList().isEmpty()) {
                        return Single.just(LightningNodeUriParser.parseNodeUri(response.getUris(0)));
                    } else
                        return Single.error(new IllegalStateException("URIs list is empty"));
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getOwnWatchtowerInfo success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getOwnWatchtowerInfo failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<CreateInvoiceResponse> createInvoice(CreateInvoiceRequest createInvoiceRequest) {
        BBLog.d(LOG_TAG, "createInvoice called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "createInvoice success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "createInvoice failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<String> getNewOnchainAddress(NewOnChainAddressRequest newOnChainAddressRequest) {
        BBLog.d(LOG_TAG, "getNewOnchainAddress called.");
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getNewOnchainAddress success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getNewOnchainAddress failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<SendLnPaymentResponse> sendLnPayment(SendLnPaymentRequest sendLnPaymentRequest) {
        BBLog.d(LOG_TAG, "sendLnPayment called.");
        SendPaymentRequest request = null;
        switch (sendLnPaymentRequest.getPaymentType()) {
            case BOLT11_INVOICE:
                SendPaymentRequest.Builder requestBuilder = SendPaymentRequest.newBuilder()
                        .setPaymentRequest(sendLnPaymentRequest.getBolt11().getBolt11String())
                        .setFeeLimitMsat(sendLnPaymentRequest.getMaxFee())
                        .setNoInflightUpdates(true)
                        .setTimeoutSeconds(ApiUtil.getPaymentTimeout())
                        .setMaxParts(10);

                if (sendLnPaymentRequest.getBolt11().hasNoAmountSpecified())
                    requestBuilder.setAmtMsat(sendLnPaymentRequest.getAmount());

                if (sendLnPaymentRequest.hasFirstHop())
                    requestBuilder.addOutgoingChanIds(ApiUtil.LongFromScid(sendLnPaymentRequest.getFirstHop()));

                if (sendLnPaymentRequest.hasLastHop()) {
                    requestBuilder.setLastHopPubkey(ApiUtil.ByteStringFromHexString(sendLnPaymentRequest.getLastHop()));
                    requestBuilder.setAllowSelfPayment(true);
                }

                request = requestBuilder.build();
                break;
            case KEYSEND:
                Map<Long, ByteString> customRecords = new HashMap<>();
                for (CustomRecord record : sendLnPaymentRequest.getCustomRecords())
                    customRecords.put(record.getFieldNumber(), ApiUtil.ByteStringFromHexString(record.getValue()));

                SendPaymentRequest.Builder requestBuilderKeysend = SendPaymentRequest.newBuilder()
                        .setDest(ApiUtil.ByteStringFromHexString(sendLnPaymentRequest.getDestinationPubKey()))
                        .setAmtMsat(sendLnPaymentRequest.getAmount())
                        .setFeeLimitMsat(sendLnPaymentRequest.getMaxFee())
                        .setPaymentHash(ApiUtil.ByteStringFromHexString(sendLnPaymentRequest.getPaymentHash()))
                        .setNoInflightUpdates(true)
                        .putAllDestCustomRecords(customRecords)
                        .setTimeoutSeconds(ApiUtil.getPaymentTimeout())
                        .setMaxParts(1); // KeySend does not support multi path payments

                if (sendLnPaymentRequest.hasFirstHop())
                    requestBuilderKeysend.addOutgoingChanIds(ApiUtil.LongFromScid(sendLnPaymentRequest.getFirstHop()));

                request = requestBuilderKeysend.build();
                break;
        }

        return LndConnection.getInstance().getRouterService().sendPaymentV2(request)
                .map(response -> {
                    switch (response.getStatus()) {
                        case SUCCEEDED:
                            return SendLnPaymentResponse.newBuilder()
                                    .setAmount(response.getValueMsat())
                                    .setFee(response.getFeeMsat())
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
                                case FAILURE_REASON_CANCELED:
                                    failureReason = SendLnPaymentResponse.FailureReason.CANCELED;
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
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "sendLnPayment success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "sendLnPayment failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable sendOnChainPayment(SendOnChainPaymentRequest sendOnChainPaymentRequest) {
        BBLog.d(LOG_TAG, "sendOnChainPayment called.");
        SendCoinsRequest.Builder requestBuilder = SendCoinsRequest.newBuilder()
                .setAddr(sendOnChainPaymentRequest.getAddress())
                .setSendAll(sendOnChainPaymentRequest.isSendAll())
                .setSatPerVbyte(sendOnChainPaymentRequest.getSatPerVByte());

        if (sendOnChainPaymentRequest.isSendAll())
            requestBuilder.setSendAll(true);
        else
            requestBuilder.setAmount(sendOnChainPaymentRequest.getAmount() / 1000);

        if (sendOnChainPaymentRequest.hasUTXOs()) {
            for (Outpoint outpoint : sendOnChainPaymentRequest.getUTXOs()) {
                requestBuilder.addOutpoints(OutPoint.newBuilder()
                        .setOutputIndex(outpoint.getOutputIndex())
                        .setTxidStr(outpoint.getTransactionID())
                        .build());
            }
        }

        SendCoinsRequest request = requestBuilder.build();

        return LndConnection.getInstance().getLightningService().sendCoins(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnComplete(() -> BBLog.d(LOG_TAG, "sendOnChainPayment success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "sendOnChainPayment failed: " + throwable.getMessage()));
    }

    @Override
    public Completable connectPeer(LightningNodeUri lightningNodeUri) {
        BBLog.d(LOG_TAG, "connectPeer called.");
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
                .doOnComplete(() -> BBLog.d(LOG_TAG, "connectPeer success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "connectPeer failed: " + throwable.getMessage()));
    }

    @Override
    public Completable disconnectPeer(String pubKey) {
        BBLog.d(LOG_TAG, "disconnectPeer called.");
        DisconnectPeerRequest request = DisconnectPeerRequest.newBuilder()
                .setPubKey(pubKey)
                .build();

        return LndConnection.getInstance().getLightningService().disconnectPeer(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnComplete(() -> BBLog.d(LOG_TAG, "disconnectPeer success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "disconnectPeer failed: " + throwable.getMessage()));
    }

    @Override
    public Single<FeeEstimateResponse> getFeeEstimates() {
        BBLog.d(LOG_TAG, "getFeeEstimates called.");
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
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getFeeEstimates success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getFeeEstimates failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<Double> getTransactionSizeVByte(String address, long amount) {
        BBLog.d(LOG_TAG, "getTransactionSizeVByte called.");
        com.github.lightningnetwork.lnd.lnrpc.EstimateFeeRequest request = com.github.lightningnetwork.lnd.lnrpc.EstimateFeeRequest.newBuilder()
                .setTargetConf(2)
                .putAddrToAmount(address, amount / 1000)
                .build();
        return LndConnection.getInstance().getLightningService().estimateFee(request)
                .map(response -> {
                    return (double) response.getFeeSat() / (double) response.getSatPerVbyte();
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "getTransactionSizeVByte success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "getTransactionSizeVByte failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable openChannel(OpenChannelRequest openChannelRequest) {
        BBLog.d(LOG_TAG, "openChannel called.");
        com.github.lightningnetwork.lnd.lnrpc.OpenChannelRequest.Builder requestBuilder = com.github.lightningnetwork.lnd.lnrpc.OpenChannelRequest.newBuilder()
                .setNodePubkey(ApiUtil.ByteStringFromHexString(openChannelRequest.getNodePubKey()))
                .setSatPerVbyte(openChannelRequest.getSatPerVByte())
                .setPrivate(openChannelRequest.isPrivate());

        if (openChannelRequest.isUseAllFunds())
            requestBuilder.setFundMax(true);
        else
            requestBuilder.setLocalFundingAmount(openChannelRequest.getAmount() / 1000);

        if (openChannelRequest.hasUTXOs())
            for (Outpoint outpoint : openChannelRequest.getUTXOs()) {
                requestBuilder.addOutpoints(OutPoint.newBuilder()
                        .setOutputIndex(outpoint.getOutputIndex())
                        .setTxidStr(outpoint.getTransactionID())
                        .build());
            }

        com.github.lightningnetwork.lnd.lnrpc.OpenChannelRequest request = requestBuilder.build();

        return LndConnection.getInstance().getLightningService().openChannel(request)
                .firstOrError()
                .ignoreElement()
                .doOnComplete(() -> BBLog.d(LOG_TAG, "openChannel success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "openChannel failed: " + throwable.getMessage()));
    }

    @Override
    public Completable closeChannel(CloseChannelRequest closeChannelRequest) {
        BBLog.d(LOG_TAG, "closeChannel called.");
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
                .doOnComplete(() -> BBLog.d(LOG_TAG, "closeChannel success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "closeChannel failed: " + throwable.getMessage()));
    }

    @Override
    public Single<Long> estimateRoutingFee(String PubKey, long amount) {
        BBLog.d(LOG_TAG, "estimateRoutingFee called.");
        RouteFeeRequest request = RouteFeeRequest.newBuilder()
                .setDest(ApiUtil.ByteStringFromHexString(PubKey))
                .setAmtSat(Math.max(amount / 1000L, 1))
                .build();
        return LndConnection.getInstance().getRouterService().estimateRouteFee(request)
                .map(response -> {
                    return response.getRoutingFeeMsat();
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "estimateRoutingFee success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "estimateRoutingFee failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable leaseUTXO(LeaseUTXORequest leaseUTXORequest) {
        BBLog.d(LOG_TAG, "leaseUTXO called.");
        LeaseOutputRequest request = LeaseOutputRequest.newBuilder()
                .setOutpoint(OutPoint.newBuilder()
                        .setTxidStr(leaseUTXORequest.getOutpoint().getTransactionID())
                        .setOutputIndex(leaseUTXORequest.getOutpoint().getOutputIndex())
                        .build())
                .setId(ApiUtil.ByteStringFromHexString(leaseUTXORequest.getId()))
                .setExpirationSeconds(leaseUTXORequest.getExpiration())
                .build();

        return LndConnection.getInstance().getWalletKitService().leaseOutput(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnComplete(() -> BBLog.d(LOG_TAG, "leaseUTXO success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "leaseUTXO failed: " + throwable.getMessage()));
    }


    @Override
    public Completable releaseUTXO(ReleaseUTXORequest releaseUTXORequest) {
        BBLog.d(LOG_TAG, "releaseUTXO called.");
        ReleaseOutputRequest request = ReleaseOutputRequest.newBuilder()
                .setOutpoint(OutPoint.newBuilder()
                        .setTxidStr(releaseUTXORequest.getOutpoint().getTransactionID())
                        .setOutputIndex(releaseUTXORequest.getOutpoint().getOutputIndex())
                        .build())
                .setId(ApiUtil.ByteStringFromHexString(releaseUTXORequest.getId()))
                .build();

        return LndConnection.getInstance().getWalletKitService().releaseOutput(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnComplete(() -> BBLog.d(LOG_TAG, "releaseUTXO success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "releaseUTXO failed: " + throwable.getMessage()));
    }

    @Override
    public Single<List<BBLogItem>> listBackendLogs() {
        BBLog.d(LOG_TAG, "listBackendLogs called.");
        GetDebugInfoRequest request = GetDebugInfoRequest.newBuilder().build();

        return LndConnection.getInstance().getLightningService().getDebugInfo(request)
                .map(response -> {
                    List<BBLogItem> logList = new ArrayList<>();

                    long timestamp = 0;
                    for (String log : response.getLogList()) {
                        timestamp++;
                        BBLogItem currLogItem = lndLogToBBLogItem(log, timestamp);
                        if (!logList.isEmpty() && currLogItem.hasTag()) {
                            int lastElementIndex = logList.size() - 1;
                            BBLogItem tempLastElement = logList.get(lastElementIndex);
                            tempLastElement.setMessage(tempLastElement.getMessage() + "\n" + currLogItem.getMessage());
                            logList.set(lastElementIndex, tempLastElement);
                        } else {
                            logList.add(currLogItem);
                        }
                    }
                    return logList;
                })
                .doOnSuccess(response -> BBLog.d(LOG_TAG, "listBackendLogs success."))
                .doOnError(throwable -> BBLog.w(LOG_TAG, "listBackendLogs failed: " + throwable.fillInStackTrace()));
    }

    private BBLogItem lndLogToBBLogItem(String log, long timestamp) {
        String[] parts = log.split(" ");

        BBLogItem.Verbosity verbosity = BBLogItem.Verbosity.VERBOSE;
        if (parts.length > 2) {
            switch (parts[2]) {
                case "[DBG]":
                    verbosity = BBLogItem.Verbosity.DEBUG;
                    break;
                case "[INF]":
                    verbosity = BBLogItem.Verbosity.INFO;
                    break;
                case "[WRN]":
                    verbosity = BBLogItem.Verbosity.WARNING;
                    break;
                case "[ERR]":
                    verbosity = BBLogItem.Verbosity.ERROR;
                    break;
            }
        }

        // LND returns some logs in multiple String Array items. We want to combine them. As a dummy we set the tag which we do not actually use. This way we can see if it is just an additional line or a new log.
        if (parts.length < 3 || (!parts[2].equals("[DBG]") && !parts[2].equals("[INF]") && !parts[2].equals("[WRN]") && !parts[2].equals("[ERR]")))
            return BBLogItem.newBuilder()
                    .setVerbosity(verbosity)
                    .setMessage(log)
                    .setTag("additionalLine")
                    .setTimestamp(timestamp)
                    .build();

        return BBLogItem.newBuilder()
                .setVerbosity(verbosity)
                .setMessage(log)
                .setTimestamp(timestamp)
                .setIseAllInfoInMessage(true)
                .build();
    }
}
