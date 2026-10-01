package app.michaelwuensch.bitbanana.connection;

import java.net.InetSocketAddress;
import java.net.Proxy;

import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.lnurl.LnUrlSecurityInterceptor;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.StaticInternetIdentifierReader;
import okhttp3.OkHttpClient;

/**
 * Singleton to handle the okHttp client
 */
public class HttpClient {
    private static HttpClient mHttpClientInstance;
    private OkHttpClient mHttpClient;
    private OkHttpClient mLnUrlHttpClient;
    private static final String LOG_TAG = HttpClient.class.getSimpleName();


    private HttpClient() {
        mHttpClient = createHttpClient();
        mLnUrlHttpClient = createLnUrlHttpClient();
    }

    private OkHttpClient createHttpClient() {
        if (PrefsUtil.isTorEnabled()) {
            Proxy torProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", TorManager.getInstance().getSocksProxyPort()));

            return new OkHttpClient.Builder()
                    .proxy(torProxy)
                    .build();
        } else {
            return new OkHttpClient();
        }
    }

    /**
     * Derived from the default client. It shares its connection pool, dispatcher and Tor proxy,
     * but additionally refuses all unencrypted connections that are not allowed for LNURL (LUD-01), including redirects to them.
     */
    private OkHttpClient createLnUrlHttpClient() {
        return mHttpClient.newBuilder()
                .addNetworkInterceptor(new LnUrlSecurityInterceptor())
                .build();
    }

    public void restartHttpClient() {
        if (PrefsUtil.isTorEnabled()) {
            BBLog.d(LOG_TAG, "HttpClient restarted. Socks Proxy Port: " + TorManager.getInstance().getSocksProxyPort());
        } else {
            BBLog.d(LOG_TAG, "HttpClient restarted.");
        }
        mHttpClient.dispatcher().cancelAll();
        mHttpClient = createHttpClient();
        mLnUrlHttpClient = createLnUrlHttpClient();
        // The network path changed (e.g. Tor got connected). Prefetch TrustChain again.
        StaticInternetIdentifierReader.prefetchDnssecTrustChain();
    }

    public static synchronized HttpClient getInstance() {
        if (mHttpClientInstance == null) {
            mHttpClientInstance = new HttpClient();
            // Runs asynchronously, as it uses this client itself.
            StaticInternetIdentifierReader.prefetchDnssecTrustChain();
        }
        return mHttpClientInstance;
    }

    public OkHttpClient getClient() {
        return mHttpClient;
    }

    /**
     * Use this client for all requests to LNURL services (including lightning addresses).
     */
    public OkHttpClient getLnUrlClient() {
        return mLnUrlHttpClient;
    }
}
