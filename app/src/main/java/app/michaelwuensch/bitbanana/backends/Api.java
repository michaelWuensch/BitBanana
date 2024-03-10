package app.michaelwuensch.bitbanana.backends;

import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
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

    public Single<Balances> getBalances() {
        return Single.error(unsupportedException());
    }

    public Single<String> signMessageWithNode(String message) {
        return Single.error(unsupportedException());
    }

    public Single<VerifyMessageResponse> verifyMessageWithNode(String message, String signature) {
        return Single.error(unsupportedException());
    }
}
