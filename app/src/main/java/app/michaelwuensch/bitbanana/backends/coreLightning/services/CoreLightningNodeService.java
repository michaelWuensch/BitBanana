package app.michaelwuensch.bitbanana.backends.coreLightning.services;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface CoreLightningNodeService {

    Single<com.github.ElementsProject.lightning.cln.GetinfoResponse> getinfo(com.github.ElementsProject.lightning.cln.GetinfoRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListpeersResponse> listPeers(com.github.ElementsProject.lightning.cln.ListpeersRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListfundsResponse> listFunds(com.github.ElementsProject.lightning.cln.ListfundsRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendpayResponse> sendPay(com.github.ElementsProject.lightning.cln.SendpayRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListchannelsResponse> listChannels(com.github.ElementsProject.lightning.cln.ListchannelsRequest request);

    Single<com.github.ElementsProject.lightning.cln.AddgossipResponse> addGossip(com.github.ElementsProject.lightning.cln.AddgossipRequest request);

    Single<com.github.ElementsProject.lightning.cln.AddpsbtoutputResponse> addPsbtOutput(com.github.ElementsProject.lightning.cln.AddpsbtoutputRequest request);

    Single<com.github.ElementsProject.lightning.cln.AutocleaninvoiceResponse> autoCleanInvoice(com.github.ElementsProject.lightning.cln.AutocleaninvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.AutocleanonceResponse> autoCleanOnce(com.github.ElementsProject.lightning.cln.AutocleanonceRequest request);

    Single<com.github.ElementsProject.lightning.cln.AutocleanstatusResponse> autoCleanStatus(com.github.ElementsProject.lightning.cln.AutocleanstatusRequest request);

    Single<com.github.ElementsProject.lightning.cln.CheckmessageResponse> checkMessage(com.github.ElementsProject.lightning.cln.CheckmessageRequest request);

    Single<com.github.ElementsProject.lightning.cln.CloseResponse> close(com.github.ElementsProject.lightning.cln.CloseRequest request);

    Single<com.github.ElementsProject.lightning.cln.ConnectResponse> connectPeer(com.github.ElementsProject.lightning.cln.ConnectRequest request);

    Single<com.github.ElementsProject.lightning.cln.CreateinvoiceResponse> createInvoice(com.github.ElementsProject.lightning.cln.CreateinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.DatastoreResponse> datastore(com.github.ElementsProject.lightning.cln.DatastoreRequest request);

    Single<com.github.ElementsProject.lightning.cln.DatastoreusageResponse> datastoreUsage(com.github.ElementsProject.lightning.cln.DatastoreusageRequest request);

    Single<com.github.ElementsProject.lightning.cln.CreateonionResponse> createOnion(com.github.ElementsProject.lightning.cln.CreateonionRequest request);

    Single<com.github.ElementsProject.lightning.cln.DeldatastoreResponse> delDatastore(com.github.ElementsProject.lightning.cln.DeldatastoreRequest request);

    Single<com.github.ElementsProject.lightning.cln.DelinvoiceResponse> delInvoice(com.github.ElementsProject.lightning.cln.DelinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.DevforgetchannelResponse> devForgetChannel(com.github.ElementsProject.lightning.cln.DevforgetchannelRequest request);

    Single<com.github.ElementsProject.lightning.cln.EmergencyrecoverResponse> emergencyRecover(com.github.ElementsProject.lightning.cln.EmergencyrecoverRequest request);

    Single<com.github.ElementsProject.lightning.cln.RecoverResponse> recover(com.github.ElementsProject.lightning.cln.RecoverRequest request);

    Single<com.github.ElementsProject.lightning.cln.RecoverchannelResponse> recoverChannel(com.github.ElementsProject.lightning.cln.RecoverchannelRequest request);

    Single<com.github.ElementsProject.lightning.cln.InvoiceResponse> invoice(com.github.ElementsProject.lightning.cln.InvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.InvoicerequestResponse> createInvoiceRequest(com.github.ElementsProject.lightning.cln.InvoicerequestRequest request);

    Single<com.github.ElementsProject.lightning.cln.DisableinvoicerequestResponse> disableInvoiceRequest(com.github.ElementsProject.lightning.cln.DisableinvoicerequestRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListinvoicerequestsResponse> listInvoiceRequests(com.github.ElementsProject.lightning.cln.ListinvoicerequestsRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListdatastoreResponse> listDatastore(com.github.ElementsProject.lightning.cln.ListdatastoreRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListinvoicesResponse> listInvoices(com.github.ElementsProject.lightning.cln.ListinvoicesRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendonionResponse> sendOnion(com.github.ElementsProject.lightning.cln.SendonionRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListsendpaysResponse> listSendPays(com.github.ElementsProject.lightning.cln.ListsendpaysRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListtransactionsResponse> listTransactions(com.github.ElementsProject.lightning.cln.ListtransactionsRequest request);

    Single<com.github.ElementsProject.lightning.cln.MakesecretResponse> makeSecret(com.github.ElementsProject.lightning.cln.MakesecretRequest request);

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

    Single<com.github.ElementsProject.lightning.cln.DelpayResponse> delPay(com.github.ElementsProject.lightning.cln.DelpayRequest request);

    Single<com.github.ElementsProject.lightning.cln.DelforwardResponse> delForward(com.github.ElementsProject.lightning.cln.DelforwardRequest request);

    Single<com.github.ElementsProject.lightning.cln.DisableofferResponse> disableOffer(com.github.ElementsProject.lightning.cln.DisableofferRequest request);

    Single<com.github.ElementsProject.lightning.cln.DisconnectResponse> disconnect(com.github.ElementsProject.lightning.cln.DisconnectRequest request);

    Single<com.github.ElementsProject.lightning.cln.FeeratesResponse> feerates(com.github.ElementsProject.lightning.cln.FeeratesRequest request);

    Single<com.github.ElementsProject.lightning.cln.FetchinvoiceResponse> fetchInvoice(com.github.ElementsProject.lightning.cln.FetchinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.Fundchannel_cancelResponse> fundChannel_Cancel(com.github.ElementsProject.lightning.cln.Fundchannel_cancelRequest request);

    Single<com.github.ElementsProject.lightning.cln.Fundchannel_completeResponse> fundChannel_Complete(com.github.ElementsProject.lightning.cln.Fundchannel_completeRequest request);

    Single<com.github.ElementsProject.lightning.cln.FundchannelResponse> fundChannel(com.github.ElementsProject.lightning.cln.FundchannelRequest request);

    Single<com.github.ElementsProject.lightning.cln.Fundchannel_startResponse> fundChannel_Start(com.github.ElementsProject.lightning.cln.Fundchannel_startRequest request);

    Single<com.github.ElementsProject.lightning.cln.GetlogResponse> getLog(com.github.ElementsProject.lightning.cln.GetlogRequest request);

    Single<com.github.ElementsProject.lightning.cln.FunderupdateResponse> funderUpdate(com.github.ElementsProject.lightning.cln.FunderupdateRequest request);

    Single<com.github.ElementsProject.lightning.cln.GetrouteResponse> getRoute(com.github.ElementsProject.lightning.cln.GetrouteRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListforwardsResponse> listForwards(com.github.ElementsProject.lightning.cln.ListforwardsRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListoffersResponse> listOffers(com.github.ElementsProject.lightning.cln.ListoffersRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListpaysResponse> listPays(com.github.ElementsProject.lightning.cln.ListpaysRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListhtlcsResponse> listHtlcs(com.github.ElementsProject.lightning.cln.ListhtlcsRequest request);

    Single<com.github.ElementsProject.lightning.cln.MultifundchannelResponse> multiFundChannel(com.github.ElementsProject.lightning.cln.MultifundchannelRequest request);

    Single<com.github.ElementsProject.lightning.cln.MultiwithdrawResponse> multiWithdraw(com.github.ElementsProject.lightning.cln.MultiwithdrawRequest request);

    Single<com.github.ElementsProject.lightning.cln.OfferResponse> offer(com.github.ElementsProject.lightning.cln.OfferRequest request);

    Single<com.github.ElementsProject.lightning.cln.Openchannel_abortResponse> openChannel_Abort(com.github.ElementsProject.lightning.cln.Openchannel_abortRequest request);

    Single<com.github.ElementsProject.lightning.cln.Openchannel_bumpResponse> openChannel_Bump(com.github.ElementsProject.lightning.cln.Openchannel_bumpRequest request);

    Single<com.github.ElementsProject.lightning.cln.Openchannel_initResponse> openChannel_Init(com.github.ElementsProject.lightning.cln.Openchannel_initRequest request);

    Single<com.github.ElementsProject.lightning.cln.Openchannel_signedResponse> openChannel_Signed(com.github.ElementsProject.lightning.cln.Openchannel_signedRequest request);

    Single<com.github.ElementsProject.lightning.cln.Openchannel_updateResponse> openChannel_Update(com.github.ElementsProject.lightning.cln.Openchannel_updateRequest request);

    Single<com.github.ElementsProject.lightning.cln.PingResponse> ping(com.github.ElementsProject.lightning.cln.PingRequest request);

    Single<com.github.ElementsProject.lightning.cln.PluginResponse> plugin(com.github.ElementsProject.lightning.cln.PluginRequest request);

    Single<com.github.ElementsProject.lightning.cln.RenepaystatusResponse> renePayStatus(com.github.ElementsProject.lightning.cln.RenepaystatusRequest request);

    Single<com.github.ElementsProject.lightning.cln.RenepayResponse> renePay(com.github.ElementsProject.lightning.cln.RenepayRequest request);

    Single<com.github.ElementsProject.lightning.cln.ReserveinputsResponse> reserveInputs(com.github.ElementsProject.lightning.cln.ReserveinputsRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendcustommsgResponse> sendCustomMsg(com.github.ElementsProject.lightning.cln.SendcustommsgRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendinvoiceResponse> sendInvoice(com.github.ElementsProject.lightning.cln.SendinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.SendonionmessageResponse> sendOnionMessage(com.github.ElementsProject.lightning.cln.SendonionmessageRequest request);

    Single<com.github.ElementsProject.lightning.cln.SetchannelResponse> setChannel(com.github.ElementsProject.lightning.cln.SetchannelRequest request);

    Single<com.github.ElementsProject.lightning.cln.SetconfigResponse> setConfig(com.github.ElementsProject.lightning.cln.SetconfigRequest request);

    Single<com.github.ElementsProject.lightning.cln.SetpsbtversionResponse> setPsbtVersion(com.github.ElementsProject.lightning.cln.SetpsbtversionRequest request);

    Single<com.github.ElementsProject.lightning.cln.SigninvoiceResponse> signInvoice(com.github.ElementsProject.lightning.cln.SigninvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.SignmessageResponse> signMessage(com.github.ElementsProject.lightning.cln.SignmessageRequest request);

    Single<com.github.ElementsProject.lightning.cln.Splice_initResponse> splice_Init(com.github.ElementsProject.lightning.cln.Splice_initRequest request);

    Single<com.github.ElementsProject.lightning.cln.Splice_signedResponse> splice_Signed(com.github.ElementsProject.lightning.cln.Splice_signedRequest request);

    Single<com.github.ElementsProject.lightning.cln.Splice_updateResponse> splice_Update(com.github.ElementsProject.lightning.cln.Splice_updateRequest request);

    Single<com.github.ElementsProject.lightning.cln.UnreserveinputsResponse> unreserveInputs(com.github.ElementsProject.lightning.cln.UnreserveinputsRequest request);

    Single<com.github.ElementsProject.lightning.cln.UpgradewalletResponse> upgradeWallet(com.github.ElementsProject.lightning.cln.UpgradewalletRequest request);

    Single<com.github.ElementsProject.lightning.cln.WaitblockheightResponse> waitBlockHeight(com.github.ElementsProject.lightning.cln.WaitblockheightRequest request);

    Single<com.github.ElementsProject.lightning.cln.WaitResponse> wait(com.github.ElementsProject.lightning.cln.WaitRequest request);

    Single<com.github.ElementsProject.lightning.cln.ListconfigsResponse> listConfigs(com.github.ElementsProject.lightning.cln.ListconfigsRequest request);

    Single<com.github.ElementsProject.lightning.cln.StopResponse> stop(com.github.ElementsProject.lightning.cln.StopRequest request);

    Single<com.github.ElementsProject.lightning.cln.HelpResponse> help(com.github.ElementsProject.lightning.cln.HelpRequest request);

    Single<com.github.ElementsProject.lightning.cln.PreapprovekeysendResponse> preApproveKeysend(com.github.ElementsProject.lightning.cln.PreapprovekeysendRequest request);

    Single<com.github.ElementsProject.lightning.cln.PreapproveinvoiceResponse> preApproveInvoice(com.github.ElementsProject.lightning.cln.PreapproveinvoiceRequest request);

    Single<com.github.ElementsProject.lightning.cln.StaticbackupResponse> staticBackup(com.github.ElementsProject.lightning.cln.StaticbackupRequest request);

    Single<com.github.ElementsProject.lightning.cln.BkprchannelsapyResponse> bkprChannelsApy(com.github.ElementsProject.lightning.cln.BkprchannelsapyRequest request);

    Single<com.github.ElementsProject.lightning.cln.BkprdumpincomecsvResponse> bkprDumpIncomeCsv(com.github.ElementsProject.lightning.cln.BkprdumpincomecsvRequest request);

    Single<com.github.ElementsProject.lightning.cln.BkprinspectResponse> bkprInspect(com.github.ElementsProject.lightning.cln.BkprinspectRequest request);

    Single<com.github.ElementsProject.lightning.cln.BkprlistaccounteventsResponse> bkprListAccountEvents(com.github.ElementsProject.lightning.cln.BkprlistaccounteventsRequest request);

    Single<com.github.ElementsProject.lightning.cln.BkprlistbalancesResponse> bkprListBalances(com.github.ElementsProject.lightning.cln.BkprlistbalancesRequest request);

    Single<com.github.ElementsProject.lightning.cln.BkprlistincomeResponse> bkprListIncome(com.github.ElementsProject.lightning.cln.BkprlistincomeRequest request);

    Single<com.github.ElementsProject.lightning.cln.BlacklistruneResponse> blacklistRune(com.github.ElementsProject.lightning.cln.BlacklistruneRequest request);

    Single<com.github.ElementsProject.lightning.cln.CheckruneResponse> checkRune(com.github.ElementsProject.lightning.cln.CheckruneRequest request);

    Single<com.github.ElementsProject.lightning.cln.CreateruneResponse> createRune(com.github.ElementsProject.lightning.cln.CreateruneRequest request);

    Single<com.github.ElementsProject.lightning.cln.ShowrunesResponse> showRunes(com.github.ElementsProject.lightning.cln.ShowrunesRequest request);

    Observable<com.github.ElementsProject.lightning.cln.BlockAddedNotification> subscribeBlockAdded(com.github.ElementsProject.lightning.cln.StreamBlockAddedRequest request);

    Observable<com.github.ElementsProject.lightning.cln.ChannelOpenFailedNotification> subscribeChannelOpenFailed(com.github.ElementsProject.lightning.cln.StreamChannelOpenFailedRequest request);

    Observable<com.github.ElementsProject.lightning.cln.ChannelOpenedNotification> subscribeChannelOpened(com.github.ElementsProject.lightning.cln.StreamChannelOpenedRequest request);

    Observable<com.github.ElementsProject.lightning.cln.PeerConnectNotification> subscribeConnect(com.github.ElementsProject.lightning.cln.StreamConnectRequest request);

    Observable<com.github.ElementsProject.lightning.cln.CustomMsgNotification> subscribeCustomMsg(com.github.ElementsProject.lightning.cln.StreamCustomMsgRequest request);
}