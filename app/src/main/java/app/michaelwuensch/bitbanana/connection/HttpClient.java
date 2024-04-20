package app.michaelwuensch.bitbanana.connection;

import java.net.InetSocketAddress;
import java.net.Proxy;

import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import okhttp3.OkHttpClient;

/**
 * Singleton to handle the okHttp client
 */
public class HttpClient {
    private static HttpClient mHttpClientInstance;
    private OkHttpClient mHttpClient;
    private static final String LOG_TAG = HttpClient.class.getSimpleName();


    private HttpClient() {
        mHttpClient = createHttpClient();
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

    public void restartHttpClient() {
        if (PrefsUtil.isTorEnabled()) {
            BBLog.d(LOG_TAG, "HttpClient restarted. Socks Proxy Port: " + TorManager.getInstance().getSocksProxyPort());
        } else {
            BBLog.d(LOG_TAG, "HttpClient restarted.");
        }
        mHttpClient.dispatcher().cancelAll();
        mHttpClient = createHttpClient();
    }

    public static synchronized HttpClient getInstance() {
        if (mHttpClientInstance == null) {
            mHttpClientInstance = new HttpClient();
        }
        return mHttpClientInstance;
    }

    public OkHttpClient getClient() {
        return mHttpClient;
    }
}
