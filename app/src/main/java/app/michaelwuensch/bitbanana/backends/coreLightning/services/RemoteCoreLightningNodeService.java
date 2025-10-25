package app.michaelwuensch.bitbanana.backends.coreLightning.services;

import com.github.ElementsProject.lightning.cln.NodeGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultObservable;
import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import app.michaelwuensch.bitbanana.backends.RemoteStreamObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class RemoteCoreLightningNodeService implements CoreLightningNodeService {

    private final NodeGrpc.NodeStub asyncStub;

    public RemoteCoreLightningNodeService(Channel channel, CallCredentials callCredentials) {
        asyncStub = NodeGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.GetinfoResponse> getinfo(com.github.ElementsProject.lightning.cln.GetinfoRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getinfo(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListpeersResponse> listPeers(com.github.ElementsProject.lightning.cln.ListpeersRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listPeers(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListfundsResponse> listFunds(com.github.ElementsProject.lightning.cln.ListfundsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listFunds(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SendpayResponse> sendPay(com.github.ElementsProject.lightning.cln.SendpayRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendPay(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListchannelsResponse> listChannels(com.github.ElementsProject.lightning.cln.ListchannelsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listChannels(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AddgossipResponse> addGossip(com.github.ElementsProject.lightning.cln.AddgossipRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.addGossip(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AddpsbtoutputResponse> addPsbtOutput(com.github.ElementsProject.lightning.cln.AddpsbtoutputRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.addPsbtOutput(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AutocleanonceResponse> autoCleanOnce(com.github.ElementsProject.lightning.cln.AutocleanonceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.autoCleanOnce(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AutocleanstatusResponse> autoCleanStatus(com.github.ElementsProject.lightning.cln.AutocleanstatusRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.autoCleanStatus(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.CheckmessageResponse> checkMessage(com.github.ElementsProject.lightning.cln.CheckmessageRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.checkMessage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.CloseResponse> close(com.github.ElementsProject.lightning.cln.CloseRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.close(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ConnectResponse> connectPeer(com.github.ElementsProject.lightning.cln.ConnectRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.connectPeer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.CreateinvoiceResponse> createInvoice(com.github.ElementsProject.lightning.cln.CreateinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.createInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DatastoreResponse> datastore(com.github.ElementsProject.lightning.cln.DatastoreRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.datastore(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DatastoreusageResponse> datastoreUsage(com.github.ElementsProject.lightning.cln.DatastoreusageRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.datastoreUsage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.CreateonionResponse> createOnion(com.github.ElementsProject.lightning.cln.CreateonionRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.createOnion(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DeldatastoreResponse> delDatastore(com.github.ElementsProject.lightning.cln.DeldatastoreRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.delDatastore(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DelinvoiceResponse> delInvoice(com.github.ElementsProject.lightning.cln.DelinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.delInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DevforgetchannelResponse> devForgetChannel(com.github.ElementsProject.lightning.cln.DevforgetchannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.devForgetChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.EmergencyrecoverResponse> emergencyRecover(com.github.ElementsProject.lightning.cln.EmergencyrecoverRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.emergencyRecover(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.GetemergencyrecoverdataResponse> getEmergencyRecoverData(com.github.ElementsProject.lightning.cln.GetemergencyrecoverdataRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getEmergencyRecoverData(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ExposesecretResponse> exposeSecret(com.github.ElementsProject.lightning.cln.ExposesecretRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.exposeSecret(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.RecoverResponse> recover(com.github.ElementsProject.lightning.cln.RecoverRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.recover(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.RecoverchannelResponse> recoverChannel(com.github.ElementsProject.lightning.cln.RecoverchannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.recoverChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.InvoiceResponse> invoice(com.github.ElementsProject.lightning.cln.InvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.invoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.InvoicerequestResponse> createInvoiceRequest(com.github.ElementsProject.lightning.cln.InvoicerequestRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.createInvoiceRequest(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DisableinvoicerequestResponse> disableInvoiceRequest(com.github.ElementsProject.lightning.cln.DisableinvoicerequestRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.disableInvoiceRequest(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListinvoicerequestsResponse> listInvoiceRequests(com.github.ElementsProject.lightning.cln.ListinvoicerequestsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listInvoiceRequests(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListdatastoreResponse> listDatastore(com.github.ElementsProject.lightning.cln.ListdatastoreRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listDatastore(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListinvoicesResponse> listInvoices(com.github.ElementsProject.lightning.cln.ListinvoicesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listInvoices(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SendonionResponse> sendOnion(com.github.ElementsProject.lightning.cln.SendonionRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendOnion(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListsendpaysResponse> listSendPays(com.github.ElementsProject.lightning.cln.ListsendpaysRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listSendPays(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListtransactionsResponse> listTransactions(com.github.ElementsProject.lightning.cln.ListtransactionsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listTransactions(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.MakesecretResponse> makeSecret(com.github.ElementsProject.lightning.cln.MakesecretRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.makeSecret(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.PayResponse> pay(com.github.ElementsProject.lightning.cln.PayRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.pay(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListnodesResponse> listNodes(com.github.ElementsProject.lightning.cln.ListnodesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listNodes(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.WaitanyinvoiceResponse> waitAnyInvoice(com.github.ElementsProject.lightning.cln.WaitanyinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.waitAnyInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.WaitinvoiceResponse> waitInvoice(com.github.ElementsProject.lightning.cln.WaitinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.waitInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.WaitsendpayResponse> waitSendPay(com.github.ElementsProject.lightning.cln.WaitsendpayRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.waitSendPay(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.NewaddrResponse> newAddr(com.github.ElementsProject.lightning.cln.NewaddrRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.newAddr(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.WithdrawResponse> withdraw(com.github.ElementsProject.lightning.cln.WithdrawRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.withdraw(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.KeysendResponse> keySend(com.github.ElementsProject.lightning.cln.KeysendRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.keySend(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FundpsbtResponse> fundPsbt(com.github.ElementsProject.lightning.cln.FundpsbtRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fundPsbt(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SendpsbtResponse> sendPsbt(com.github.ElementsProject.lightning.cln.SendpsbtRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendPsbt(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SignpsbtResponse> signPsbt(com.github.ElementsProject.lightning.cln.SignpsbtRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.signPsbt(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.UtxopsbtResponse> utxoPsbt(com.github.ElementsProject.lightning.cln.UtxopsbtRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.utxoPsbt(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.TxdiscardResponse> txDiscard(com.github.ElementsProject.lightning.cln.TxdiscardRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.txDiscard(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.TxprepareResponse> txPrepare(com.github.ElementsProject.lightning.cln.TxprepareRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.txPrepare(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.TxsendResponse> txSend(com.github.ElementsProject.lightning.cln.TxsendRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.txSend(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListpeerchannelsResponse> listPeerChannels(com.github.ElementsProject.lightning.cln.ListpeerchannelsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listPeerChannels(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListclosedchannelsResponse> listClosedChannels(com.github.ElementsProject.lightning.cln.ListclosedchannelsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listClosedChannels(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DecodepayResponse> decodePay(com.github.ElementsProject.lightning.cln.DecodepayRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.decodePay(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DecodeResponse> decode(com.github.ElementsProject.lightning.cln.DecodeRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.decode(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DelpayResponse> delPay(com.github.ElementsProject.lightning.cln.DelpayRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.delPay(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DelforwardResponse> delForward(com.github.ElementsProject.lightning.cln.DelforwardRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.delForward(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DisableofferResponse> disableOffer(com.github.ElementsProject.lightning.cln.DisableofferRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.disableOffer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.EnableofferResponse> enableOffer(com.github.ElementsProject.lightning.cln.EnableofferRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.enableOffer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DisconnectResponse> disconnect(com.github.ElementsProject.lightning.cln.DisconnectRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.disconnect(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FeeratesResponse> feerates(com.github.ElementsProject.lightning.cln.FeeratesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.feerates(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.Fetchbip353Response> fetchBip353(com.github.ElementsProject.lightning.cln.Fetchbip353Request request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fetchBip353(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FetchinvoiceResponse> fetchInvoice(com.github.ElementsProject.lightning.cln.FetchinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fetchInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FundchannelCancelResponse> fundChannelCancel(com.github.ElementsProject.lightning.cln.FundchannelCancelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fundChannelCancel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FundchannelCompleteResponse> fundChannelComplete(com.github.ElementsProject.lightning.cln.FundchannelCompleteRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fundChannelComplete(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FundchannelResponse> fundChannel(com.github.ElementsProject.lightning.cln.FundchannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fundChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FundchannelStartResponse> fundChannelStart(com.github.ElementsProject.lightning.cln.FundchannelStartRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fundChannelStart(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.GetlogResponse> getLog(com.github.ElementsProject.lightning.cln.GetlogRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getLog(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FunderupdateResponse> funderUpdate(com.github.ElementsProject.lightning.cln.FunderupdateRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.funderUpdate(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.GetrouteResponse> getRoute(com.github.ElementsProject.lightning.cln.GetrouteRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getRoute(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListaddressesResponse> listAddresses(com.github.ElementsProject.lightning.cln.ListaddressesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listAddresses(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListforwardsResponse> listForwards(com.github.ElementsProject.lightning.cln.ListforwardsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listForwards(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListoffersResponse> listOffers(com.github.ElementsProject.lightning.cln.ListoffersRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listOffers(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListpaysResponse> listPays(com.github.ElementsProject.lightning.cln.ListpaysRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listPays(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListhtlcsResponse> listHtlcs(com.github.ElementsProject.lightning.cln.ListhtlcsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listHtlcs(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.MultifundchannelResponse> multiFundChannel(com.github.ElementsProject.lightning.cln.MultifundchannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.multiFundChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.MultiwithdrawResponse> multiWithdraw(com.github.ElementsProject.lightning.cln.MultiwithdrawRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.multiWithdraw(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.OfferResponse> offer(com.github.ElementsProject.lightning.cln.OfferRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.offer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.OpenchannelAbortResponse> openChannelAbort(com.github.ElementsProject.lightning.cln.OpenchannelAbortRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.openChannelAbort(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.OpenchannelBumpResponse> openChannelBump(com.github.ElementsProject.lightning.cln.OpenchannelBumpRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.openChannelBump(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.OpenchannelInitResponse> openChannelInit(com.github.ElementsProject.lightning.cln.OpenchannelInitRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.openChannelInit(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.OpenchannelSignedResponse> openChannelSigned(com.github.ElementsProject.lightning.cln.OpenchannelSignedRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.openChannelSigned(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.OpenchannelUpdateResponse> openChannelUpdate(com.github.ElementsProject.lightning.cln.OpenchannelUpdateRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.openChannelUpdate(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.PingResponse> ping(com.github.ElementsProject.lightning.cln.PingRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.ping(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.PluginResponse> plugin(com.github.ElementsProject.lightning.cln.PluginRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.plugin(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.RenepaystatusResponse> renePayStatus(com.github.ElementsProject.lightning.cln.RenepaystatusRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.renePayStatus(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.RenepayResponse> renePay(com.github.ElementsProject.lightning.cln.RenepayRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.renePay(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ReserveinputsResponse> reserveInputs(com.github.ElementsProject.lightning.cln.ReserveinputsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.reserveInputs(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SendcustommsgResponse> sendCustomMsg(com.github.ElementsProject.lightning.cln.SendcustommsgRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendCustomMsg(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SendinvoiceResponse> sendInvoice(com.github.ElementsProject.lightning.cln.SendinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SetchannelResponse> setChannel(com.github.ElementsProject.lightning.cln.SetchannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.setChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SetconfigResponse> setConfig(com.github.ElementsProject.lightning.cln.SetconfigRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.setConfig(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SetpsbtversionResponse> setPsbtVersion(com.github.ElementsProject.lightning.cln.SetpsbtversionRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.setPsbtVersion(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SigninvoiceResponse> signInvoice(com.github.ElementsProject.lightning.cln.SigninvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.signInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SignmessageResponse> signMessage(com.github.ElementsProject.lightning.cln.SignmessageRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.signMessage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SpliceInitResponse> spliceInit(com.github.ElementsProject.lightning.cln.SpliceInitRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.spliceInit(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SpliceSignedResponse> spliceSigned(com.github.ElementsProject.lightning.cln.SpliceSignedRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.spliceSigned(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SpliceUpdateResponse> spliceUpdate(com.github.ElementsProject.lightning.cln.SpliceUpdateRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.spliceUpdate(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DevspliceResponse> devSplice(com.github.ElementsProject.lightning.cln.DevspliceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.devSplice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.UnreserveinputsResponse> unreserveInputs(com.github.ElementsProject.lightning.cln.UnreserveinputsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.unreserveInputs(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.UpgradewalletResponse> upgradeWallet(com.github.ElementsProject.lightning.cln.UpgradewalletRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.upgradeWallet(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.WaitblockheightResponse> waitBlockHeight(com.github.ElementsProject.lightning.cln.WaitblockheightRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.waitBlockHeight(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.WaitResponse> wait(com.github.ElementsProject.lightning.cln.WaitRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.wait(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ListconfigsResponse> listConfigs(com.github.ElementsProject.lightning.cln.ListconfigsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.listConfigs(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.StopResponse> stop(com.github.ElementsProject.lightning.cln.StopRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.stop(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.HelpResponse> help(com.github.ElementsProject.lightning.cln.HelpRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.help(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.PreapprovekeysendResponse> preApproveKeysend(com.github.ElementsProject.lightning.cln.PreapprovekeysendRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.preApproveKeysend(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.PreapproveinvoiceResponse> preApproveInvoice(com.github.ElementsProject.lightning.cln.PreapproveinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.preApproveInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.StaticbackupResponse> staticBackup(com.github.ElementsProject.lightning.cln.StaticbackupRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.staticBackup(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BkprchannelsapyResponse> bkprChannelsApy(com.github.ElementsProject.lightning.cln.BkprchannelsapyRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprChannelsApy(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BkprdumpincomecsvResponse> bkprDumpIncomeCsv(com.github.ElementsProject.lightning.cln.BkprdumpincomecsvRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprDumpIncomeCsv(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BkprinspectResponse> bkprInspect(com.github.ElementsProject.lightning.cln.BkprinspectRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprInspect(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BkprlistaccounteventsResponse> bkprListAccountEvents(com.github.ElementsProject.lightning.cln.BkprlistaccounteventsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprListAccountEvents(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BkprlistbalancesResponse> bkprListBalances(com.github.ElementsProject.lightning.cln.BkprlistbalancesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprListBalances(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BkprlistincomeResponse> bkprListIncome(com.github.ElementsProject.lightning.cln.BkprlistincomeRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprListIncome(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BkpreditdescriptionbypaymentidResponse> bkprEditDescriptionByPaymentId(com.github.ElementsProject.lightning.cln.BkpreditdescriptionbypaymentidRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprEditDescriptionByPaymentId(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BkpreditdescriptionbyoutpointResponse> bkprEditDescriptionByOutpoint(com.github.ElementsProject.lightning.cln.BkpreditdescriptionbyoutpointRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprEditDescriptionByOutpoint(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.BlacklistruneResponse> blacklistRune(com.github.ElementsProject.lightning.cln.BlacklistruneRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.blacklistRune(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.CheckruneResponse> checkRune(com.github.ElementsProject.lightning.cln.CheckruneRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.checkRune(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.CreateruneResponse> createRune(com.github.ElementsProject.lightning.cln.CreateruneRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.createRune(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.ShowrunesResponse> showRunes(com.github.ElementsProject.lightning.cln.ShowrunesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.showRunes(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskreneunreserveResponse> askReneUnreserve(com.github.ElementsProject.lightning.cln.AskreneunreserveRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneUnreserve(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskrenelistlayersResponse> askReneListLayers(com.github.ElementsProject.lightning.cln.AskrenelistlayersRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneListLayers(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskrenecreatelayerResponse> askReneCreateLayer(com.github.ElementsProject.lightning.cln.AskrenecreatelayerRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneCreateLayer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskreneremovelayerResponse> askReneRemoveLayer(com.github.ElementsProject.lightning.cln.AskreneremovelayerRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneRemoveLayer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskrenereserveResponse> askReneReserve(com.github.ElementsProject.lightning.cln.AskrenereserveRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneReserve(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskreneageResponse> askReneAge(com.github.ElementsProject.lightning.cln.AskreneageRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneAge(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.GetroutesResponse> getRoutes(com.github.ElementsProject.lightning.cln.GetroutesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getRoutes(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskrenedisablenodeResponse> askReneDisableNode(com.github.ElementsProject.lightning.cln.AskrenedisablenodeRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneDisableNode(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskreneinformchannelResponse> askReneInformChannel(com.github.ElementsProject.lightning.cln.AskreneinformchannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneInformChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskrenecreatechannelResponse> askReneCreateChannel(com.github.ElementsProject.lightning.cln.AskrenecreatechannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneCreateChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskreneupdatechannelResponse> askReneUpdateChannel(com.github.ElementsProject.lightning.cln.AskreneupdatechannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneUpdateChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskrenebiaschannelResponse> askReneBiasChannel(com.github.ElementsProject.lightning.cln.AskrenebiaschannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneBiasChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.AskrenelistreservationsResponse> askReneListReservations(com.github.ElementsProject.lightning.cln.AskrenelistreservationsRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.askReneListReservations(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.InjectpaymentonionResponse> injectPaymentOnion(com.github.ElementsProject.lightning.cln.InjectpaymentonionRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.injectPaymentOnion(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.InjectonionmessageResponse> injectOnionMessage(com.github.ElementsProject.lightning.cln.InjectonionmessageRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.injectOnionMessage(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.XpayResponse> xpay(com.github.ElementsProject.lightning.cln.XpayRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.xpay(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SignmessagewithkeyResponse> signMessageWithKey(com.github.ElementsProject.lightning.cln.SignmessagewithkeyRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.signMessageWithKey(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.ElementsProject.lightning.cln.BlockAddedNotification> subscribeBlockAdded(com.github.ElementsProject.lightning.cln.StreamBlockAddedRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeBlockAdded(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.ElementsProject.lightning.cln.ChannelOpenFailedNotification> subscribeChannelOpenFailed(com.github.ElementsProject.lightning.cln.StreamChannelOpenFailedRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeChannelOpenFailed(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.ElementsProject.lightning.cln.ChannelOpenedNotification> subscribeChannelOpened(com.github.ElementsProject.lightning.cln.StreamChannelOpenedRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeChannelOpened(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.ElementsProject.lightning.cln.PeerConnectNotification> subscribeConnect(com.github.ElementsProject.lightning.cln.StreamConnectRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeConnect(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.ElementsProject.lightning.cln.CustomMsgNotification> subscribeCustomMsg(com.github.ElementsProject.lightning.cln.StreamCustomMsgRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeCustomMsg(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.ElementsProject.lightning.cln.ChannelStateChangedNotification> subscribeChannelStateChanged(com.github.ElementsProject.lightning.cln.StreamChannelStateChangedRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeChannelStateChanged(request, new RemoteStreamObserver<>(emitter)));
    }

}