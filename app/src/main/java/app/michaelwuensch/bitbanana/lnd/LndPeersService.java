package app.michaelwuensch.bitbanana.lnd;

import io.reactivex.rxjava3.core.Single;

public interface LndPeersService {

    Single<com.github.lightningnetwork.lnd.peersrpc.NodeAnnouncementUpdateResponse> updateNodeAnnouncement(com.github.lightningnetwork.lnd.peersrpc.NodeAnnouncementUpdateRequest request);
}