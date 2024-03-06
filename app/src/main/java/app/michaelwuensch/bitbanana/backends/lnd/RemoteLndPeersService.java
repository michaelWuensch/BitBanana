package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.lightningnetwork.lnd.peersrpc.PeersGrpc;

import app.michaelwuensch.bitbanana.backends.DefaultSingle;
import app.michaelwuensch.bitbanana.backends.RemoteSingleObserver;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Single;

public class RemoteLndPeersService implements LndPeersService {

    private final PeersGrpc.PeersStub asyncStub;

    public RemoteLndPeersService(Channel channel, CallCredentials callCredentials) {
        asyncStub = PeersGrpc.newStub(channel).withCallCredentials(callCredentials);
    }

    @Override
    public Single<com.github.lightningnetwork.lnd.peersrpc.NodeAnnouncementUpdateResponse> updateNodeAnnouncement(com.github.lightningnetwork.lnd.peersrpc.NodeAnnouncementUpdateRequest request) {
        return DefaultSingle.createDefault(emitter -> asyncStub.updateNodeAnnouncement(request, new RemoteSingleObserver<>(emitter)));
    }

}