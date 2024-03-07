package app.michaelwuensch.bitbanana.backends.lnd.services;

import com.github.lightningnetwork.lnd.invoicesrpc.InvoicesGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultObservable;
import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import app.michaelwuensch.bitbanana.backends.RemoteStreamObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndInvoicesService implements LndInvoicesService {

    private final InvoicesGrpc.InvoicesStub asyncStub;

    public RemoteLndInvoicesService(Channel channel, CallCredentials callCredentials) {
        asyncStub = InvoicesGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.Invoice> subscribeSingleInvoice(com.github.lightningnetwork.lnd.invoicesrpc.SubscribeSingleInvoiceRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeSingleInvoice(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.invoicesrpc.CancelInvoiceResp> cancelInvoice(com.github.lightningnetwork.lnd.invoicesrpc.CancelInvoiceMsg request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.cancelInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.invoicesrpc.AddHoldInvoiceResp> addHoldInvoice(com.github.lightningnetwork.lnd.invoicesrpc.AddHoldInvoiceRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.addHoldInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.invoicesrpc.SettleInvoiceResp> settleInvoice(com.github.lightningnetwork.lnd.invoicesrpc.SettleInvoiceMsg request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.settleInvoice(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.Invoice> lookupInvoiceV2(com.github.lightningnetwork.lnd.invoicesrpc.LookupInvoiceMsg request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.lookupInvoiceV2(request, new RemoteSingleObserver<>(emitter)));
    }

}