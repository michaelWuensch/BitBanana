package app.michaelwuensch.bitbanana.backends;

import java.util.List;

import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.PublicChannelInfo;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.Channels.UpdateRoutingPolicyRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceResponse;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.models.NodeInfo;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class Api {

    public Api() {

    }

    protected IllegalStateException unsupportedException() {
        return new IllegalStateException("Unknown or unsupported backend type");
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

    public Single<String> signMessageWithNode(String message) {
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

    public Single<List<String>> updateRoutingPolicy(UpdateRoutingPolicyRequest request) {
        return Single.error(unsupportedException());
    }

    /**
     * This will fetch all invoice from the node in a paginated way.
     *
     * @param page     Use 0 as start of the recursion to fetch all invoices
     * @param pageSize How many invoices are fetched per page (per Api call). Make this big enough so it is fast, but small enough to not hit any message length limits.
     * @return A list of all invoices
     */
    public Single<List<LnInvoice>> listInvoices(int page, int pageSize) {
        return Single.error(unsupportedException());
    }

    public Observable<LnInvoice> subscribeToInvoices() {
        return Observable.error(unsupportedException());
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
     * @param page     Use 0 as start of the recursion to fetch all payments
     * @param pageSize How many payments are fetched per page (per Api call). Make this big enough so it is fast, but small enough to not hit any message length limits.
     * @return A list of all payments
     */
    public Single<List<LnPayment>> listLnPayments(int page, int pageSize) {
        return Single.error(unsupportedException());
    }

    public Observable<LnPayment> subscribeToLnPayments() {
        return Observable.error(unsupportedException());
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
}
