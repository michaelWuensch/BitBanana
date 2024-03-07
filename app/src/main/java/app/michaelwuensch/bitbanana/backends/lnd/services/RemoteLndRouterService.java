package app.michaelwuensch.bitbanana.backends.lnd.services;

import com.github.lightningnetwork.lnd.routerrpc.RouterGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultObservable;
import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import app.michaelwuensch.bitbanana.backends.RemoteStreamObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndRouterService implements LndRouterService {

    private final RouterGrpc.RouterStub asyncStub;

    public RemoteLndRouterService(Channel channel, CallCredentials callCredentials) {
        asyncStub = RouterGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.Payment> sendPaymentV2(com.github.lightningnetwork.lnd.routerrpc.SendPaymentRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.sendPaymentV2(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.Payment> trackPaymentV2(com.github.lightningnetwork.lnd.routerrpc.TrackPaymentRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.trackPaymentV2(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.lnrpc.Payment> trackPayments(com.github.lightningnetwork.lnd.routerrpc.TrackPaymentsRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.trackPayments(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.RouteFeeResponse> estimateRouteFee(com.github.lightningnetwork.lnd.routerrpc.RouteFeeRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.estimateRouteFee(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.SendToRouteResponse> sendToRoute(com.github.lightningnetwork.lnd.routerrpc.SendToRouteRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendToRoute(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.lnrpc.HTLCAttempt> sendToRouteV2(com.github.lightningnetwork.lnd.routerrpc.SendToRouteRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.sendToRouteV2(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.ResetMissionControlResponse> resetMissionControl(com.github.lightningnetwork.lnd.routerrpc.ResetMissionControlRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.resetMissionControl(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.QueryMissionControlResponse> queryMissionControl(com.github.lightningnetwork.lnd.routerrpc.QueryMissionControlRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.queryMissionControl(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.XImportMissionControlResponse> xImportMissionControl(com.github.lightningnetwork.lnd.routerrpc.XImportMissionControlRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.xImportMissionControl(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.GetMissionControlConfigResponse> getMissionControlConfig(com.github.lightningnetwork.lnd.routerrpc.GetMissionControlConfigRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.getMissionControlConfig(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.SetMissionControlConfigResponse> setMissionControlConfig(com.github.lightningnetwork.lnd.routerrpc.SetMissionControlConfigRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.setMissionControlConfig(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.QueryProbabilityResponse> queryProbability(com.github.lightningnetwork.lnd.routerrpc.QueryProbabilityRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.queryProbability(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.BuildRouteResponse> buildRoute(com.github.lightningnetwork.lnd.routerrpc.BuildRouteRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.buildRoute(request, new RemoteSingleObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.routerrpc.HtlcEvent> subscribeHtlcEvents(com.github.lightningnetwork.lnd.routerrpc.SubscribeHtlcEventsRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.subscribeHtlcEvents(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.routerrpc.PaymentStatus> sendPayment(com.github.lightningnetwork.lnd.routerrpc.SendPaymentRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.sendPayment(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Observable<com.github.lightningnetwork.lnd.routerrpc.PaymentStatus> trackPayment(com.github.lightningnetwork.lnd.routerrpc.TrackPaymentRequest request) {
        return DefaultObservable.createDefault(emitter -> asyncStub.trackPayment(request, new RemoteStreamObserver<>(emitter)));
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.routerrpc.UpdateChanStatusResponse> updateChanStatus(com.github.lightningnetwork.lnd.routerrpc.UpdateChanStatusRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.updateChanStatus(request, new RemoteSingleObserver<>(emitter)));
    }

}