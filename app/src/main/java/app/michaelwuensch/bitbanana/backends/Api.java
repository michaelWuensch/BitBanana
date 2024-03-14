package app.michaelwuensch.bitbanana.backends;

import java.util.List;

import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.PublicChannelInfo;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.Channels.UpdateRoutingPolicyRequest;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.NodeInfo;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
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

    public Single<List<Utxo>> getUTXOs(long currentBlockHeight) {
        return Single.error(unsupportedException());
    }

    public Single<List<OpenChannel>> getOpenChannels() {
        return Single.error(unsupportedException());
    }

    public Single<List<PendingChannel>> getPendingChannels() {
        return Single.error(unsupportedException());
    }

    public Single<List<ClosedChannel>> getClosedChannels() {
        return Single.error(unsupportedException());
    }

    public Single<PublicChannelInfo> getPublicChannelInfo(ShortChannelId shortChannelId) {
        return Single.error(unsupportedException());
    }

    public Single<List<String>> updateRoutingPolicy(UpdateRoutingPolicyRequest request) {
        return Single.error(unsupportedException());
    }
}
