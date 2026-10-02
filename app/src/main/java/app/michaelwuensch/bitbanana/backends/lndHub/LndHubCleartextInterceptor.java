package app.michaelwuensch.bitbanana.backends.lndHub;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;

import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import okhttp3.Connection;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Network interceptor for all LndHub requests.
 * LndHub transmits the user credentials and access tokens with every request. Over unencrypted http everyone on the network path
 * could read them and take over the (custodial) account. Therefore unencrypted http is only allowed for:
 * - Tor hidden services, as Tor encrypts and authenticates the connection itself.
 * - Private network addresses (loopback, LAN, link-local, unique local and the carrier-grade NAT range used by Tailscale).
 * <p>
 * As a network interceptor it is called after the connection was established, but before anything is sent.
 * This allows us to check the address we are actually connected to, which also covers hostnames like "umbrel.local" and redirects.
 */
public class LndHubCleartextInterceptor implements Interceptor {
    private static final String LOG_TAG = LndHubCleartextInterceptor.class.getSimpleName();

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        if (!request.isHttps()) {
            Connection connection = chain.connection();
            if (connection == null || !isCleartextAllowed(request.url(), connection.route().proxy(), connection.route().socketAddress())) {
                BBLog.w(LOG_TAG, "Refused unencrypted LndHub connection to " + request.url().host());
                throw new CleartextRefusedException(request.url().host());
            }
        }
        return chain.proceed(request);
    }

    /**
     * @param url           The url of the request.
     * @param proxy         The proxy used for the connection.
     * @param socketAddress The address the connection is established to.
     */
    public static boolean isCleartextAllowed(@NonNull HttpUrl url, @Nullable Proxy proxy, @Nullable InetSocketAddress socketAddress) {
        if (url.isHttps())
            return true;

        if (RemoteConnectUtil.isTorHostAddress(url.host()))
            return true;

        // Through a proxy (e.g. Tor to a clearnet host) we do not have a direct connection to a private network.
        // Over Tor the exit node could read everything.
        if (proxy == null || proxy.type() != Proxy.Type.DIRECT || socketAddress == null)
            return false;

        return isPrivateAddress(socketAddress.getAddress());
    }

    public static boolean isPrivateAddress(@Nullable InetAddress address) {
        if (address == null)
            return false;

        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress())
            return true;

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            // 100.64.0.0/10 (carrier-grade NAT, used by Tailscale)
            return (bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 64;
        }
        if (address instanceof Inet6Address) {
            // fc00::/7 (unique local addresses, also used by Tailscale)
            return (bytes[0] & 0xFE) == 0xFC;
        }
        return false;
    }

    public static class CleartextRefusedException extends IOException {
        public CleartextRefusedException(String host) {
            super("Refused unencrypted LndHub connection to " + host);
        }
    }

    /**
     * Returns true if the throwable or one of its causes is a CleartextRefusedException.
     */
    public static boolean isCleartextRefused(@Nullable Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof CleartextRefusedException)
                return true;
            throwable = throwable.getCause();
        }
        return false;
    }
}
