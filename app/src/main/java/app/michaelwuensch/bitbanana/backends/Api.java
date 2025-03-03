package app.michaelwuensch.bitbanana.backends;

import java.util.List;

import app.michaelwuensch.bitbanana.models.BBLogItem;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.models.Channels.CloseChannelRequest;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannelRequest;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.PublicChannelInfo;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.Channels.UpdateRoutingPolicyRequest;
import app.michaelwuensch.bitbanana.models.CreateBolt12OfferRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceResponse;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.FeeEstimateResponse;
import app.michaelwuensch.bitbanana.models.FetchInvoiceFromOfferRequest;
import app.michaelwuensch.bitbanana.models.Forward;
import app.michaelwuensch.bitbanana.models.LeaseUTXORequest;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.models.NodeInfo;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.Peer;
import app.michaelwuensch.bitbanana.models.ReleaseUTXORequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.models.SendOnChainPaymentRequest;
import app.michaelwuensch.bitbanana.models.SignMessageResponse;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
import app.michaelwuensch.bitbanana.models.Watchtower;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class Api {

    public Api() {

    }

    protected IllegalStateException unsupportedException() {
        return new IllegalStateException("The method is not implemented for the current backend. (" + BackendManager.getCurrentBackendType() + ")");
    }

    public Single<CurrentNodeInfo> getCurrentNodeInfo() {
        return Single.error(unsupportedException());
    }

    public Single<NodeInfo> getNodeInfo(String pubKey) {
        return Single.error(unsupportedException());
    }

    public Single<Balances> getBalances() {
        return Single.error(unsupportedException());
    }

    public Single<SignMessageResponse> signMessageWithNode(String message) {
        return Single.error(unsupportedException());
    }

    public Single<VerifyMessageResponse> verifyMessageWithNode(String message, String signature) {
        return Single.error(unsupportedException());
    }

    public Single<List<Utxo>> listUTXOs(long currentBlockHeight) {
        return Single.error(unsupportedException());
    }

    public Single<List<OpenChannel>> listOpenChannels() {
        return Single.error(unsupportedException());
    }

    public Single<List<PendingChannel>> listPendingChannels() {
        return Single.error(unsupportedException());
    }

    public Single<List<ClosedChannel>> listClosedChannels() {
        return Single.error(unsupportedException());
    }

    public Single<PublicChannelInfo> getPublicChannelInfo(ShortChannelId shortChannelId) {
        return Single.error(unsupportedException());
    }

    public Single<List<String>> updateRoutingPolicy(UpdateRoutingPolicyRequest updateRoutingPolicyRequest) {
        return Single.error(unsupportedException());
    }

    /**
     * This will fetch all invoice from the node in a paginated way.
     *
     * @param firstIndexOffset Use 0 as start of the recursion to fetch all invoices
     * @param pageSize         How many invoices are fetched per page (per Api call). Make this big enough so it is fast, but small enough to not hit any message length limits.
     * @return A list of all invoices
     */
    public Single<List<LnInvoice>> listInvoices(long firstIndexOffset, int pageSize) {
        return Single.error(unsupportedException());
    }

    public Observable<LnInvoice> subscribeToInvoices() {
        return Observable.error(unsupportedException());
    }

    public Single<LnInvoice> getInvoice(String paymentHash) {
        return Single.error(unsupportedException());
    }

    public Single<List<OnChainTransaction>> listOnChainTransactions() {
        return Single.error(unsupportedException());
    }

    public Observable<OnChainTransaction> subscribeToOnChainTransactions() {
        return Observable.error(unsupportedException());
    }

    /**
     * This will fetch all lightning payments from the node in a paginated way.
     *
     * @param firstIndexOffset Use 0 as start of the recursion to fetch all payments
     * @param pageSize         How many payments are fetched per page (per Api call). Make this big enough so it is fast, but small enough to not hit any message length limits.
     * @return A list of all payments
     */
    public Single<List<LnPayment>> listLnPayments(long firstIndexOffset, int pageSize) {
        return Single.error(unsupportedException());
    }

    /**
     * This will fetch all forwarding events that happened
     *
     * @param firstIndexOffset Use 0 as start of the recursion to fetch all forward events
     * @param pageSize         How many forward events are fetched per page (per Api call). Make this big enough so it is fast, but small enough to not hit any message length limits.
     * @param startTime        Starting time in seconds since UNIX epoch. All forward events that are more recent than this will be returned.
     * @return A list of all matching forwarding events
     */
    public Single<List<Forward>> listForwards(long firstIndexOffset, int pageSize, long startTime) {
        return Single.error(unsupportedException());
    }

    public Observable<LnPayment> subscribeToLnPayments() {
        return Observable.error(unsupportedException());
    }

    public Single<List<Peer>> listPeers() {
        return Single.error(unsupportedException());
    }

    public Single<List<Watchtower>> listWatchtowers() {
        return Single.error(unsupportedException());
    }

    public Single<List<Bolt12Offer>> listBolt12Offers() {
        return Single.error(unsupportedException());
    }

    public Single<List<BBLogItem>> listBackendLogs() {
        return Single.error(unsupportedException());
    }

    public Single<Watchtower> getWatchtower(String pubKey) {
        return Single.error(unsupportedException());
    }

    public Completable addWatchtower(String pubKey, String address) {
        return Completable.error(unsupportedException());
    }

    /**
     * Deactivates a watchtower so it is no longer used for backups until it is reactivated. (use AddWatchtower to reactivate)
     * After reactivation the remaining backups in the sessions can still be used.
     */
    public Single<String> deactivateWatchtower(String pubKey) {
        return Single.error(unsupportedException());
    }

    /**
     * Removes a watchtower and terminates all associated sessions.
     * This means the remaining backups in the sessions can no longer be used and if you paid for that service you basically spent that money for nothing.
     */
    public Completable removeWatchtower(String pubKey) {
        return Completable.error(unsupportedException());
    }

    public Single<LightningNodeUri> getOwnWatchtowerInfo() {
        return Single.error(unsupportedException());
    }

    public Single<CreateInvoiceResponse> createInvoice(CreateInvoiceRequest createInvoiceRequest) {
        return Single.error(unsupportedException());
    }

    public Single<String> getNewOnchainAddress(NewOnChainAddressRequest newOnChainAddressRequest) {
        return Single.error(unsupportedException());
    }

    public Single<SendLnPaymentResponse> sendLnPayment(SendLnPaymentRequest sendLnPaymentRequest) {
        return Single.error(unsupportedException());
    }

    public Completable sendOnChainPayment(SendOnChainPaymentRequest sendOnChainPaymentRequest) {
        return Completable.error(unsupportedException());
    }

    public Completable connectPeer(LightningNodeUri lightningNodeUri) {
        return Completable.error(unsupportedException());
    }

    public Completable disconnectPeer(String pubKey) {
        return Completable.error(unsupportedException());
    }

    public Single<FeeEstimateResponse> getFeeEstimates() {
        return Single.error(unsupportedException());
    }

    public Single<Double> getTransactionSizeVByte(String address, long amount) {
        return Single.error(unsupportedException());
    }

    public Completable openChannel(OpenChannelRequest openChannelRequest) {
        return Completable.error(unsupportedException());
    }

    public Completable closeChannel(CloseChannelRequest closeChannelRequest) {
        return Completable.error(unsupportedException());
    }

    public Single<Long> estimateRoutingFee(String PubKey, long amount) {
        return Single.error(unsupportedException());
    }

    public Single<Bolt12Offer> createBolt12Offer(CreateBolt12OfferRequest createBolt12OfferRequest) {
        return Single.error(unsupportedException());
    }

    public Completable disableBolt12Offer(String offerId) {
        return Completable.error(unsupportedException());
    }

    public Completable enableBolt12Offer(String offerId) {
        return Completable.error(unsupportedException());
    }

    public Single<String> fetchInvoiceFromBolt12Offer(FetchInvoiceFromOfferRequest fetchInvoiceFromOfferRequest) {
        return Single.error(unsupportedException());
    }

    public Completable leaseUTXO(LeaseUTXORequest leaseUTXORequest) {
        return Completable.error(unsupportedException());
    }

    public Completable releaseUTXO(ReleaseUTXORequest releaseUTXORequest) {
        return Completable.error(unsupportedException());
    }
}
