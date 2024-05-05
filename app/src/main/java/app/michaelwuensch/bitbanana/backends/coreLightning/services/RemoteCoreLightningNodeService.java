package app.michaelwuensch.bitbanana.backends.coreLightning.services;

import com.github.ElementsProject.lightning.cln.NodeGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
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
    public Single<com.github.ElementsProject.lightning.cln.AutocleaninvoiceResponse> autoCleanInvoice(com.github.ElementsProject.lightning.cln.AutocleaninvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.autoCleanInvoice(request, new RemoteSingleObserver<>(emitter)));
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
    public Single<com.github.ElementsProject.lightning.cln.DelexpiredinvoiceResponse> delExpiredInvoice(com.github.ElementsProject.lightning.cln.DelexpiredinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.delExpiredInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.DelinvoiceResponse> delInvoice(com.github.ElementsProject.lightning.cln.DelinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.delInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.InvoiceResponse> invoice(com.github.ElementsProject.lightning.cln.InvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.invoice(request, new RemoteSingleObserver<>(emitter)));
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
    public Single<com.github.ElementsProject.lightning.cln.DisconnectResponse> disconnect(com.github.ElementsProject.lightning.cln.DisconnectRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.disconnect(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FeeratesResponse> feerates(com.github.ElementsProject.lightning.cln.FeeratesRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.feerates(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FetchinvoiceResponse> fetchInvoice(com.github.ElementsProject.lightning.cln.FetchinvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fetchInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.FundchannelResponse> fundChannel(com.github.ElementsProject.lightning.cln.FundchannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.fundChannel(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.GetrouteResponse> getRoute(com.github.ElementsProject.lightning.cln.GetrouteRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getRoute(request, new RemoteSingleObserver<>(emitter)));
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
    public Single<com.github.ElementsProject.lightning.cln.OfferResponse> offer(com.github.ElementsProject.lightning.cln.OfferRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.offer(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.PingResponse> ping(com.github.ElementsProject.lightning.cln.PingRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.ping(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SendcustommsgResponse> sendCustomMsg(com.github.ElementsProject.lightning.cln.SendcustommsgRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendCustomMsg(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.SetchannelResponse> setChannel(com.github.ElementsProject.lightning.cln.SetchannelRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.setChannel(request, new RemoteSingleObserver<>(emitter)));
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
    public Single<com.github.ElementsProject.lightning.cln.WaitblockheightResponse> waitBlockHeight(com.github.ElementsProject.lightning.cln.WaitblockheightRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.waitBlockHeight(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.WaitResponse> wait(com.github.ElementsProject.lightning.cln.WaitRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.wait(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.ElementsProject.lightning.cln.StopResponse> stop(com.github.ElementsProject.lightning.cln.StopRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.stop(request, new RemoteSingleObserver<>(emitter)));
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
    public Single<com.github.ElementsProject.lightning.cln.BkprlistincomeResponse> bkprListIncome(com.github.ElementsProject.lightning.cln.BkprlistincomeRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.bkprListIncome(request, new RemoteSingleObserver<>(emitter)));
    }

}