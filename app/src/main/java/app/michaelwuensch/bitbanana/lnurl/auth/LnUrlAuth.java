package app.michaelwuensch.bitbanana.lnurl.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.lightningnetwork.lnd.lnrpc.SignMessageRequest;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.connection.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.lnurl.LnUrlResponse;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UtilFunctions;
import fr.acinq.secp256k1.Secp256k1;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Implementation of the LNURL Auth flow.
 * Refer to:
 * https://github.com/lnurl/luds/blob/luds/04.md
 * https://github.com/lnurl/luds/blob/luds/13.md
 */
public class LnUrlAuth {
    private static final String LOG_TAG = LnUrlAuth.class.getSimpleName();
    private static final String CANONICAL_PHRASE = "DO NOT EVER SIGN THIS TEXT WITH YOUR PRIVATE KEYS! IT IS ONLY USED FOR DERIVATION OF LNURL-AUTH HASHING-KEY, DISCLOSING ITS SIGNATURE WILL COMPROMISE YOUR LNURL-AUTH IDENTITY AND MAY LEAD TO LOSS OF FUNDS!";
    public static final int ACTION_REGISTER = 0;
    public static final int ACTION_LOGIN = 1;
    public static final int ACTION_LINK = 2;
    public static final int ACTION_AUTH = 3;

    URL mUrl;
    Context mContext;
    AuthListener mListener;

    public LnUrlAuth(Context context, URL url) {
        mContext = context;
        mUrl = url;
    }

    public String getHost() {
        if (mUrl == null)
            return null;
        return mUrl.getHost();
    }

    public int getAction() {
        if (mUrl == null)
            return -1;
        String action = UtilFunctions.getQueryParam(mUrl, "action");
        if (action == null)
            return -1;
        switch (action) {
            case "register":
                return ACTION_REGISTER;
            case "login":
                return ACTION_LOGIN;
            case "link":
                return ACTION_LINK;
            case "auth":
                return ACTION_AUTH;
            default:
                return -1;
        }
    }

    public void authenticate(CompositeDisposable disposable, AuthListener authListener) {
        mListener = authListener;

        String k1 = UtilFunctions.getQueryParam(mUrl, "k1");
        if (k1 == null || k1.length() != 64 || !UtilFunctions.isHex(k1)) {
            BBLog.e(LOG_TAG, "LNURL: Service did not provide a valid k1");
            mListener.onError("Service did not provide a valid k1");
            return;
        }

        SignMessageRequest request = SignMessageRequest.newBuilder()
                .setMsg(ByteString.copyFrom(CANONICAL_PHRASE.getBytes(StandardCharsets.UTF_8)))
                .build();

        disposable.add(LndConnection.getInstance().getLightningService().signMessage(request)
                .subscribe(signResponse -> {

                    // LUD-13: 3. LN WALLET defines hashingKey sha256(obtained signature).
                    String hashingKey = UtilFunctions.sha256Hash(signResponse.getSignature());

                    // LUD-13: 4. SERVICE domain name is extracted from auth LNURL and then service-specific linkingPrivKey is defined as hmacSha256(hashingKey, service domain name).
                    String linkingPrivKey = UtilFunctions.hmacSHA256(mUrl.getHost().getBytes(StandardCharsets.UTF_8), UtilFunctions.hexToBytes(hashingKey));

                    // LUD-04: 3. LN WALLET signs k1 on secp256k1 using linkingPrivKey and DER-encodes the signature. LN WALLET Then issues a GET to LN SERVICE using <LNURL_hostname_and_path>?<LNURL_existing_query_parameters>&sig=<hex(sign(hexToBytes(k1), linkingPrivKey))>&key=<hex(linkingKey)>
                    byte[] publicLinkingKeyUncompressed = Secp256k1.get().pubkeyCreate(UtilFunctions.hexToBytes(linkingPrivKey));
                    byte[] linkingKey = Secp256k1.get().pubKeyCompress(publicLinkingKeyUncompressed);
                    byte[] signed_K1 = Secp256k1.get().sign(UtilFunctions.hexToBytes(k1), UtilFunctions.hexToBytes(linkingPrivKey));
                    byte[] signed_K1_DER_encoded = Secp256k1.get().compact2der(signed_K1);

                    LnUrlFinalAuthRequest lnUrlFinalAuthRequest = new LnUrlFinalAuthRequest.Builder()
                            .setDecodedLnUrl(mUrl)
                            .setSig(UtilFunctions.bytesToHex(signed_K1_DER_encoded))
                            .setLinkingKey(UtilFunctions.bytesToHex(linkingKey))
                            .build();

                    BBLog.d(LOG_TAG, "Final auth request: " + lnUrlFinalAuthRequest.requestAsString());

                    okhttp3.Request lnUrlRequest = new Request.Builder()
                            .url(lnUrlFinalAuthRequest.requestAsString())
                            .build();

                    HttpClient.getInstance().getClient().newCall(lnUrlRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            BBLog.e(LOG_TAG, "LNURL: Final Auth request failed: " + e.getMessage());
                            mListener.onError(mContext.getString(R.string.lnurl_service_not_responding, getHost()));
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            // LUD-04: 3. LN SERVICE responds...
                            String responseContent = response.body().string();
                            if (responseContent != null) {
                                validateFinalResponse(responseContent);
                            } else {
                                BBLog.e(LOG_TAG, "LNURL: Response content was null.");
                                mListener.onError(mContext.getString(R.string.lnurl_invalid_response));
                            }
                        }
                    });

                }, throwable -> {
                    BBLog.e(LOG_TAG, "LNURL: Signing canonical phrase failed." + throwable.getMessage());
                    mListener.onError("Signing canonical phrase failed.");
                }));
    }

    private void validateFinalResponse(@NonNull String finalAuthResponse) {
        try {
            LnUrlResponse lnUrlResponse = new Gson().fromJson(finalAuthResponse, LnUrlResponse.class);
            if (lnUrlResponse.getStatus().equals("OK")) {
                BBLog.d(LOG_TAG, "LNURL: Authentication successful.");
                mListener.onSuccess();
            } else {
                BBLog.e(LOG_TAG, "LNURL: Authentication failed. Service answered with error:  " + lnUrlResponse.getReason());
                mListener.onError(lnUrlResponse.getReason());
            }
        } catch (Exception e) {
            BBLog.e(LOG_TAG, "LNURL: Invalid response, error parsing JSON: " + e.getMessage());
            if (PrefsUtil.isTorEnabled())
                mListener.onError(mContext.getString(R.string.lnurl_invalid_response) + " " + mContext.getString(R.string.error_tor_might_cause_problem));
            else
                mListener.onError(mContext.getString(R.string.lnurl_invalid_response));
        }
    }

    public interface AuthListener {
        void onError(String message);

        void onSuccess();
    }
}
