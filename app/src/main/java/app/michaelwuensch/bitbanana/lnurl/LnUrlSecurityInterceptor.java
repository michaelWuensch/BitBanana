package app.michaelwuensch.bitbanana.lnurl;

import androidx.annotation.NonNull;

import java.io.IOException;

import app.michaelwuensch.bitbanana.util.BBLog;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Network interceptor for all LNURL requests.
 * As a network interceptor it is called for every single request that goes over the network, including all requests that result from redirects.
 * This makes sure a service cannot circumvent the https requirement of LUD-01 by redirecting to an unencrypted http URL.
 */
public class LnUrlSecurityInterceptor implements Interceptor {
    private static final String LOG_TAG = LnUrlSecurityInterceptor.class.getSimpleName();

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        if (!LnUrlReader.isSecureLnUrlUrl(request.url().toString())) {
            BBLog.w(LOG_TAG, "Refused unencrypted LNURL connection to " + request.url().host());
            throw new IOException("Refused unencrypted LNURL connection to " + request.url().host());
        }
        return chain.proceed(request);
    }
}
