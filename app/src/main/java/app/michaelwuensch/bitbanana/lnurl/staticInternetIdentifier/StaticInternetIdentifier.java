package app.michaelwuensch.bitbanana.lnurl.staticInternetIdentifier;

import android.content.Context;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.models.LnAddress;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Please refer to the following specification
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/16.md
 */

public class StaticInternetIdentifier {

    public static final String ARGS_KEY = "staticInternetIdentifier";
    private static final String LOG_TAG = StaticInternetIdentifier.class.getSimpleName();

    public static void checkIfValidStaticInternetIdentifier(Context ctx, String address, OnStaticIdentifierChecked listener) {
        LnAddress lnAddress = new LnAddress(address);
        if (!lnAddress.isValid()) {
            listener.onNoStaticInternetIdentifierData();
            return;
        }

        String requestUrl = IdentifierToRequest(lnAddress);

        okhttp3.Request lightningAddressRequest = new okhttp3.Request.Builder()
                .url(requestUrl)
                .build();

        HttpClient.getInstance().getClient().newCall(lightningAddressRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                BBLog.e(LOG_TAG, e.getMessage());
                listener.onError(e.getMessage(), RefConstants.ERROR_DURATION_SHORT);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                boolean isBlockedByCloudflare = false;
                try {
                    String responseAsString = response.body().string();
                    isBlockedByCloudflare = responseAsString.toLowerCase().contains("cloudflare") && responseAsString.toLowerCase().contains("captcha-bypass");
                    BBLog.d(LOG_TAG, responseAsString);
                    LnUrlPayResponse lnUrlPayResponse = new Gson().fromJson(responseAsString, LnUrlPayResponse.class);
                    if (lnUrlPayResponse.hasError()) {
                        listener.onError(lnUrlPayResponse.getReason(), RefConstants.ERROR_DURATION_MEDIUM);
                        return;
                    }
                    listener.onValidInternetIdentifier(lnUrlPayResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (isBlockedByCloudflare) {
                        listener.onError(ctx.getResources().getString(R.string.error_tor_blocked_lnurl, address.split("@")[1]), RefConstants.ERROR_DURATION_VERY_LONG);
                    } else {
                        listener.onError(ctx.getResources().getString(R.string.error_static_identifier_response_unknown, address.split("@")[1]), RefConstants.ERROR_DURATION_MEDIUM);
                    }
                }
            }
        });
    }

    private static String IdentifierToRequest(LnAddress lnAddress) {
        if (lnAddress.isTor()) {
            return "http://" + lnAddress.getDomain() + "/.well-known/lnurlp/" + lnAddress.getUsername();
        } else {
            return "https://" + lnAddress.getDomain() + "/.well-known/lnurlp/" + lnAddress.getUsername();
        }
    }

    public interface OnStaticIdentifierChecked {

        void onValidInternetIdentifier(LnUrlPayResponse lnUrlPayResponse);

        void onError(String error, int duration);

        void onNoStaticInternetIdentifierData();
    }
}
