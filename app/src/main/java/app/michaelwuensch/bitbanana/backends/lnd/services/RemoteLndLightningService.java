package app.michaelwuensch.bitbanana.backends.lnd.services;

import com.github.lightningnetwork.lnd.lnrpc.LightningGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultObservable;
import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import app.michaelwuensch.bitbanana.backends.RemoteStreamObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndLightningService implements LndLightningService {

    private final LightningGrpc.LightningStub asyncStub;

    public RemoteLndLightningService(Channel channel, CallCredentials callCredentials) {
        asyncStub = LightningGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.WalletBalanceResponse> walletBalance(com.github.lightningnetwork.lnd.lnrpc.WalletBalanceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.walletBalance(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ChannelBalanceResponse> channelBalance(com.github.lightningnetwork.lnd.lnrpc.ChannelBalanceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.channelBalance(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.TransactionDetails> getTransactions(com.github.lightningnetwork.lnd.lnrpc.GetTransactionsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getTransactions(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.EstimateFeeResponse> estimateFee(com.github.lightningnetwork.lnd.lnrpc.EstimateFeeRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.estimateFee(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.SendCoinsResponse> sendCoins(com.github.lightningnetwork.lnd.lnrpc.SendCoinsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendCoins(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ListUnspentResponse> listUnspent(com.github.lightningnetwork.lnd.lnrpc.ListUnspentRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listUnspent(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.Transaction> subscribeTransactions(com.github.lightningnetwork.lnd.lnrpc.GetTransactionsRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeTransactions(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.SendManyResponse> sendMany(com.github.lightningnetwork.lnd.lnrpc.SendManyRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendMany(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.NewAddressResponse> newAddress(com.github.lightningnetwork.lnd.lnrpc.NewAddressRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.newAddress(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.SignMessageResponse> signMessage(com.github.lightningnetwork.lnd.lnrpc.SignMessageRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.signMessage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.VerifyMessageResponse> verifyMessage(com.github.lightningnetwork.lnd.lnrpc.VerifyMessageRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.verifyMessage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ConnectPeerResponse> connectPeer(com.github.lightningnetwork.lnd.lnrpc.ConnectPeerRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.connectPeer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.DisconnectPeerResponse> disconnectPeer(com.github.lightningnetwork.lnd.lnrpc.DisconnectPeerRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.disconnectPeer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ListPeersResponse> listPeers(com.github.lightningnetwork.lnd.lnrpc.ListPeersRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listPeers(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.PeerEvent> subscribePeerEvents(com.github.lightningnetwork.lnd.lnrpc.PeerEventSubscription request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribePeerEvents(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.GetInfoResponse> getInfo(com.github.lightningnetwork.lnd.lnrpc.GetInfoRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getInfo(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.GetDebugInfoResponse> getDebugInfo(com.github.lightningnetwork.lnd.lnrpc.GetDebugInfoRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getDebugInfo(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.GetRecoveryInfoResponse> getRecoveryInfo(com.github.lightningnetwork.lnd.lnrpc.GetRecoveryInfoRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getRecoveryInfo(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse> pendingChannels(com.github.lightningnetwork.lnd.lnrpc.PendingChannelsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.pendingChannels(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ListChannelsResponse> listChannels(com.github.lightningnetwork.lnd.lnrpc.ListChannelsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listChannels(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.ChannelEventUpdate> subscribeChannelEvents(com.github.lightningnetwork.lnd.lnrpc.ChannelEventSubscription request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeChannelEvents(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ClosedChannelsResponse> closedChannels(com.github.lightningnetwork.lnd.lnrpc.ClosedChannelsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.closedChannels(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ChannelPoint> openChannelSync(com.github.lightningnetwork.lnd.lnrpc.OpenChannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.openChannelSync(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.OpenStatusUpdate> openChannel(com.github.lightningnetwork.lnd.lnrpc.OpenChannelRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.openChannel(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.BatchOpenChannelResponse> batchOpenChannel(com.github.lightningnetwork.lnd.lnrpc.BatchOpenChannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.batchOpenChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.FundingStateStepResp> fundingStateStep(com.github.lightningnetwork.lnd.lnrpc.FundingTransitionMsg request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fundingStateStep(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.CloseStatusUpdate> closeChannel(com.github.lightningnetwork.lnd.lnrpc.CloseChannelRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.closeChannel(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.AbandonChannelResponse> abandonChannel(com.github.lightningnetwork.lnd.lnrpc.AbandonChannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.abandonChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.SendResponse> sendPaymentSync(com.github.lightningnetwork.lnd.lnrpc.SendRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendPaymentSync(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.SendResponse> sendToRouteSync(com.github.lightningnetwork.lnd.lnrpc.SendToRouteRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendToRouteSync(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.AddInvoiceResponse> addInvoice(com.github.lightningnetwork.lnd.lnrpc.Invoice request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.addInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ListInvoiceResponse> listInvoices(com.github.lightningnetwork.lnd.lnrpc.ListInvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listInvoices(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.Invoice> lookupInvoice(com.github.lightningnetwork.lnd.lnrpc.PaymentHash request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.lookupInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.Invoice> subscribeInvoices(com.github.lightningnetwork.lnd.lnrpc.InvoiceSubscription request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeInvoices(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.PayReq> decodePayReq(com.github.lightningnetwork.lnd.lnrpc.PayReqString request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.decodePayReq(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ListPaymentsResponse> listPayments(com.github.lightningnetwork.lnd.lnrpc.ListPaymentsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listPayments(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.DeletePaymentResponse> deletePayment(com.github.lightningnetwork.lnd.lnrpc.DeletePaymentRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.deletePayment(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.DeleteAllPaymentsResponse> deleteAllPayments(com.github.lightningnetwork.lnd.lnrpc.DeleteAllPaymentsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.deleteAllPayments(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ChannelGraph> describeGraph(com.github.lightningnetwork.lnd.lnrpc.ChannelGraphRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.describeGraph(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.NodeMetricsResponse> getNodeMetrics(com.github.lightningnetwork.lnd.lnrpc.NodeMetricsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getNodeMetrics(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ChannelEdge> getChanInfo(com.github.lightningnetwork.lnd.lnrpc.ChanInfoRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getChanInfo(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.NodeInfo> getNodeInfo(com.github.lightningnetwork.lnd.lnrpc.NodeInfoRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getNodeInfo(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.QueryRoutesResponse> queryRoutes(com.github.lightningnetwork.lnd.lnrpc.QueryRoutesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.queryRoutes(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.NetworkInfo> getNetworkInfo(com.github.lightningnetwork.lnd.lnrpc.NetworkInfoRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getNetworkInfo(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.StopResponse> stopDaemon(com.github.lightningnetwork.lnd.lnrpc.StopRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.stopDaemon(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.GraphTopologyUpdate> subscribeChannelGraph(com.github.lightningnetwork.lnd.lnrpc.GraphTopologySubscription request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeChannelGraph(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.DebugLevelResponse> debugLevel(com.github.lightningnetwork.lnd.lnrpc.DebugLevelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.debugLevel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.FeeReportResponse> feeReport(com.github.lightningnetwork.lnd.lnrpc.FeeReportRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.feeReport(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.PolicyUpdateResponse> updateChannelPolicy(com.github.lightningnetwork.lnd.lnrpc.PolicyUpdateRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.updateChannelPolicy(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ForwardingHistoryResponse> forwardingHistory(com.github.lightningnetwork.lnd.lnrpc.ForwardingHistoryRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.forwardingHistory(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ChannelBackup> exportChannelBackup(com.github.lightningnetwork.lnd.lnrpc.ExportChannelBackupRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.exportChannelBackup(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ChanBackupSnapshot> exportAllChannelBackups(com.github.lightningnetwork.lnd.lnrpc.ChanBackupExportRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.exportAllChannelBackups(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.VerifyChanBackupResponse> verifyChanBackup(com.github.lightningnetwork.lnd.lnrpc.ChanBackupSnapshot request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.verifyChanBackup(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.RestoreBackupResponse> restoreChannelBackups(com.github.lightningnetwork.lnd.lnrpc.RestoreChanBackupRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.restoreChannelBackups(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.ChanBackupSnapshot> subscribeChannelBackups(com.github.lightningnetwork.lnd.lnrpc.ChannelBackupSubscription request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeChannelBackups(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.BakeMacaroonResponse> bakeMacaroon(com.github.lightningnetwork.lnd.lnrpc.BakeMacaroonRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bakeMacaroon(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ListMacaroonIDsResponse> listMacaroonIDs(com.github.lightningnetwork.lnd.lnrpc.ListMacaroonIDsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listMacaroonIDs(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.DeleteMacaroonIDResponse> deleteMacaroonID(com.github.lightningnetwork.lnd.lnrpc.DeleteMacaroonIDRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.deleteMacaroonID(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ListPermissionsResponse> listPermissions(com.github.lightningnetwork.lnd.lnrpc.ListPermissionsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listPermissions(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.CheckMacPermResponse> checkMacaroonPermissions(com.github.lightningnetwork.lnd.lnrpc.CheckMacPermRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.checkMacaroonPermissions(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.SendCustomMessageResponse> sendCustomMessage(com.github.lightningnetwork.lnd.lnrpc.SendCustomMessageRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendCustomMessage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.CustomMessage> subscribeCustomMessages(com.github.lightningnetwork.lnd.lnrpc.SubscribeCustomMessagesRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeCustomMessages(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.ListAliasesResponse> listAliases(com.github.lightningnetwork.lnd.lnrpc.ListAliasesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listAliases(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.LookupHtlcResolutionResponse> lookupHtlcResolution(com.github.lightningnetwork.lnd.lnrpc.LookupHtlcResolutionRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.lookupHtlcResolution(request, new RemoteSingleObserver<>(emitter)));
    }

}