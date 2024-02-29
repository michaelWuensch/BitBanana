package app.michaelwuensch.bitbanana.backends.coreLightning;

import io.reactivex.rxjava3.core.Single;

public interface CoreLightningNodeService {

    Single<com.github.ElementsProject.lightning.cln.GetinfoResponse> getinfo(com.github.ElementsProject.lightning.cln.GetinfoRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListpeersResponse> listPeers(com.github.ElementsProject.lightning.cln.ListpeersRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListfundsResponse> listFunds(com.github.ElementsProject.lightning.cln.ListfundsRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendpayResponse> sendPay(com.github.ElementsProject.lightning.cln.SendpayRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListchannelsResponse> listChannels(com.github.ElementsProject.lightning.cln.ListchannelsRequest request);

    Single<com.github.ElementsProject.lightning.cln.AddgossipResponse> addGossip(com.github.ElementsProject.lightning.cln.AddgossipRequest request);

    Single<com.github.ElementsProject.lightning.cln.AutocleaninvoiceResponse> autoCleanInvoice(com.github.ElementsProject.lightning.cln.AutocleaninvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.CheckmessageResponse> checkMessage(com.github.ElementsProject.lightning.cln.CheckmessageRequest request);

    Single<com.github.ElementsProject.lightning.cln.CloseResponse> close(com.github.ElementsProject.lightning.cln.CloseRequest request);

    Single<com.github.ElementsProject.lightning.cln.ConnectResponse> connectPeer(com.github.ElementsProject.lightning.cln.ConnectRequest request);

    Single<com.github.ElementsProject.lightning.cln.CreateinvoiceResponse> createInvoice(com.github.ElementsProject.lightning.cln.CreateinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.DatastoreResponse> datastore(com.github.ElementsProject.lightning.cln.DatastoreRequest request);

    Single<com.github.ElementsProject.lightning.cln.DatastoreusageResponse> datastoreUsage(com.github.ElementsProject.lightning.cln.DatastoreusageRequest request);

    Single<com.github.ElementsProject.lightning.cln.CreateonionResponse> createOnion(com.github.ElementsProject.lightning.cln.CreateonionRequest request);

    Single<com.github.ElementsProject.lightning.cln.DeldatastoreResponse> delDatastore(com.github.ElementsProject.lightning.cln.DeldatastoreRequest request);

    Single<com.github.ElementsProject.lightning.cln.DelexpiredinvoiceResponse> delExpiredInvoice(com.github.ElementsProject.lightning.cln.DelexpiredinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.DelinvoiceResponse> delInvoice(com.github.ElementsProject.lightning.cln.DelinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.InvoiceResponse> invoice(com.github.ElementsProject.lightning.cln.InvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListdatastoreResponse> listDatastore(com.github.ElementsProject.lightning.cln.ListdatastoreRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListinvoicesResponse> listInvoices(com.github.ElementsProject.lightning.cln.ListinvoicesRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendonionResponse> sendOnion(com.github.ElementsProject.lightning.cln.SendonionRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListsendpaysResponse> listSendPays(com.github.ElementsProject.lightning.cln.ListsendpaysRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListtransactionsResponse> listTransactions(com.github.ElementsProject.lightning.cln.ListtransactionsRequest request);

    Single<com.github.ElementsProject.lightning.cln.PayResponse> pay(com.github.ElementsProject.lightning.cln.PayRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListnodesResponse> listNodes(com.github.ElementsProject.lightning.cln.ListnodesRequest request);

    Single<com.github.ElementsProject.lightning.cln.WaitanyinvoiceResponse> waitAnyInvoice(com.github.ElementsProject.lightning.cln.WaitanyinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.WaitinvoiceResponse> waitInvoice(com.github.ElementsProject.lightning.cln.WaitinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.WaitsendpayResponse> waitSendPay(com.github.ElementsProject.lightning.cln.WaitsendpayRequest request);

    Single<com.github.ElementsProject.lightning.cln.NewaddrResponse> newAddr(com.github.ElementsProject.lightning.cln.NewaddrRequest request);

    Single<com.github.ElementsProject.lightning.cln.WithdrawResponse> withdraw(com.github.ElementsProject.lightning.cln.WithdrawRequest request);

    Single<com.github.ElementsProject.lightning.cln.KeysendResponse> keySend(com.github.ElementsProject.lightning.cln.KeysendRequest request);

    Single<com.github.ElementsProject.lightning.cln.FundpsbtResponse> fundPsbt(com.github.ElementsProject.lightning.cln.FundpsbtRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendpsbtResponse> sendPsbt(com.github.ElementsProject.lightning.cln.SendpsbtRequest request);

    Single<com.github.ElementsProject.lightning.cln.SignpsbtResponse> signPsbt(com.github.ElementsProject.lightning.cln.SignpsbtRequest request);

    Single<com.github.ElementsProject.lightning.cln.UtxopsbtResponse> utxoPsbt(com.github.ElementsProject.lightning.cln.UtxopsbtRequest request);

    Single<com.github.ElementsProject.lightning.cln.TxdiscardResponse> txDiscard(com.github.ElementsProject.lightning.cln.TxdiscardRequest request);

    Single<com.github.ElementsProject.lightning.cln.TxprepareResponse> txPrepare(com.github.ElementsProject.lightning.cln.TxprepareRequest request);

    Single<com.github.ElementsProject.lightning.cln.TxsendResponse> txSend(com.github.ElementsProject.lightning.cln.TxsendRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListpeerchannelsResponse> listPeerChannels(com.github.ElementsProject.lightning.cln.ListpeerchannelsRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListclosedchannelsResponse> listClosedChannels(com.github.ElementsProject.lightning.cln.ListclosedchannelsRequest request);

    Single<com.github.ElementsProject.lightning.cln.DecodepayResponse> decodePay(com.github.ElementsProject.lightning.cln.DecodepayRequest request);

    Single<com.github.ElementsProject.lightning.cln.DecodeResponse> decode(com.github.ElementsProject.lightning.cln.DecodeRequest request);

    Single<com.github.ElementsProject.lightning.cln.DisconnectResponse> disconnect(com.github.ElementsProject.lightning.cln.DisconnectRequest request);

    Single<com.github.ElementsProject.lightning.cln.FeeratesResponse> feerates(com.github.ElementsProject.lightning.cln.FeeratesRequest request);

    Single<com.github.ElementsProject.lightning.cln.FetchinvoiceResponse> fetchInvoice(com.github.ElementsProject.lightning.cln.FetchinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.FundchannelResponse> fundChannel(com.github.ElementsProject.lightning.cln.FundchannelRequest request);

    Single<com.github.ElementsProject.lightning.cln.GetrouteResponse> getRoute(com.github.ElementsProject.lightning.cln.GetrouteRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListforwardsResponse> listForwards(com.github.ElementsProject.lightning.cln.ListforwardsRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListpaysResponse> listPays(com.github.ElementsProject.lightning.cln.ListpaysRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListhtlcsResponse> listHtlcs(com.github.ElementsProject.lightning.cln.ListhtlcsRequest request);

    Single<com.github.ElementsProject.lightning.cln.PingResponse> ping(com.github.ElementsProject.lightning.cln.PingRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendcustommsgResponse> sendCustomMsg(com.github.ElementsProject.lightning.cln.SendcustommsgRequest request);

    Single<com.github.ElementsProject.lightning.cln.SetchannelResponse> setChannel(com.github.ElementsProject.lightning.cln.SetchannelRequest request);

    Single<com.github.ElementsProject.lightning.cln.SigninvoiceResponse> signInvoice(com.github.ElementsProject.lightning.cln.SigninvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.SignmessageResponse> signMessage(com.github.ElementsProject.lightning.cln.SignmessageRequest request);

    Single<com.github.ElementsProject.lightning.cln.WaitblockheightResponse> waitBlockHeight(com.github.ElementsProject.lightning.cln.WaitblockheightRequest request);

    Single<com.github.ElementsProject.lightning.cln.WaitResponse> wait(com.github.ElementsProject.lightning.cln.WaitRequest request);

    Single<com.github.ElementsProject.lightning.cln.StopResponse> stop(com.github.ElementsProject.lightning.cln.StopRequest request);

    Single<com.github.ElementsProject.lightning.cln.PreapprovekeysendResponse> preApproveKeysend(com.github.ElementsProject.lightning.cln.PreapprovekeysendRequest request);

    Single<com.github.ElementsProject.lightning.cln.PreapproveinvoiceResponse> preApproveInvoice(com.github.ElementsProject.lightning.cln.PreapproveinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.StaticbackupResponse> staticBackup(com.github.ElementsProject.lightning.cln.StaticbackupRequest request);
}