package app.michaelwuensch.bitbanana.connection;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsqueryresult.DnsQueryResult;
import org.minidns.dnsqueryresult.StandardDnsQueryResult;
import org.minidns.source.AbstractDnsDataSource;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * DNS data source for minidns that sends all queries as DNS-over-HTTPS requests (RFC 8484) instead of classic, unencrypted DNS.
 * <p>
 * The requests are made with our default HttpClient, which means they automatically go through Tor if Tor is enabled.
 * This only replaces the transport. DNSSEC is still validated locally by minidns, so a resolver cannot forge any data.
 * It could only refuse to answer, in which case the next resolver of the list is used.
 * <p>
 * Without this, minidns falls back to unencrypted queries to Google DNS on Android, as it is not able to detect the DNS servers of the device.
 */
public class DohDnsDataSource extends AbstractDnsDataSource {
    private static final String LOG_TAG = DohDnsDataSource.class.getSimpleName();

    private static final MediaType DNS_MESSAGE = MediaType.get("application/dns-message");

    // Cloudflare's DoH resolver as Tor onion service. Requests to it never leave the Tor network.
    // https://developers.cloudflare.com/1.1.1.1/other-ways-to-use-1.1.1.1/dns-over-tor/
    static final String CLOUDFLARE_ONION = "https://dns4torpnlfs2ifuz2s2yf3fc7rdmsbhm6rw75euj35pac6ap25zgqad.onion/dns-query";
    static final String QUAD9 = "https://dns.quad9.net/dns-query";
    static final String CLOUDFLARE = "https://cloudflare-dns.com/dns-query";
    static final String MULLVAD = "https://dns.mullvad.net/dns-query";

    // The resolvers are tried in this order. If one fails, the next one is used.
    private static final List<String> RESOLVERS = Arrays.asList(QUAD9, CLOUDFLARE, MULLVAD);
    private static final List<String> RESOLVERS_TOR = Arrays.asList(CLOUDFLARE_ONION, QUAD9, CLOUDFLARE);

    private final Supplier<OkHttpClient> mClientSupplier;
    private final Supplier<List<String>> mResolversSupplier;
    private final LongSupplier mNanoTime;
    // If the preferred resolver fails, the resolver we fell back to is tried first for a while, so a resolver that is down does not slow down every single query.
    // Afterwards the preferred resolver is tried again (e.g. the onion resolver, which might just have needed longer to connect).
    @VisibleForTesting
    static final long FALLBACK_RESOLVER_VALIDITY_NANOS = 10L * 60 * 1_000_000_000;
    private volatile String mFallbackResolver;
    private volatile long mFallbackResolverTimestamp;

    public DohDnsDataSource() {
        this(() -> HttpClient.getInstance().getClient(), () -> PrefsUtil.isTorEnabled() ? RESOLVERS_TOR : RESOLVERS, System::nanoTime);
    }

    @VisibleForTesting
    DohDnsDataSource(Supplier<OkHttpClient> clientSupplier, Supplier<List<String>> resolversSupplier, LongSupplier nanoTime) {
        mClientSupplier = clientSupplier;
        mResolversSupplier = resolversSupplier;
        mNanoTime = nanoTime;
    }

    /**
     * @param address Ignored. This is the DNS server minidns would contact with classic DNS. All queries go to the DoH resolvers instead.
     * @param port    Ignored, see address.
     */
    @Override
    public DnsQueryResult query(DnsMessage message, InetAddress address, int port) throws IOException {
        byte[] query = message.toArray();
        List<String> resolvers = mResolversSupplier.get();
        IOException lastException = null;

        for (String resolver : getResolversInOrder(resolvers)) {
            try {
                DnsMessage response = queryResolver(resolver, query);
                if (response.id != message.id)
                    throw new IOException("Response ID does not match the query ID");
                rememberWorkingResolver(resolver, resolvers);
                BBLog.v(LOG_TAG, "DoH query to " + HttpUrl.get(resolver).host() + " succeeded.");
                return new StandardDnsQueryResult(address, port, DnsQueryResult.QueryMethod.tcp, message, response);
            } catch (IOException e) {
                BBLog.w(LOG_TAG, "DoH query to " + HttpUrl.get(resolver).host() + " failed: " + e.getMessage());
                lastException = e;
            }
        }
        // Never fall back to classic DNS here. That would leak the query unencrypted and around Tor.
        throw lastException != null ? lastException : new IOException("No DoH resolver available");
    }

    private List<String> getResolversInOrder(List<String> resolvers) {
        int index = isFallbackResolverValid() ? resolvers.indexOf(mFallbackResolver) : -1;
        if (index <= 0)
            return resolvers;
        // Start with the fallback resolver, keep the order of the others.
        List<String> ordered = new ArrayList<>(resolvers.subList(index, resolvers.size()));
        ordered.addAll(resolvers.subList(0, index));
        return ordered;
    }

    private void rememberWorkingResolver(String resolver, List<String> resolvers) {
        if (resolver.equals(resolvers.get(0))) {
            // The preferred resolver works (again), use the normal order.
            mFallbackResolver = null;
        } else if (!resolver.equals(mFallbackResolver) || !isFallbackResolverValid()) {
            // We had to fall back. Remember it for a while. The period starts with the fallback, not with every successful query.
            mFallbackResolver = resolver;
            mFallbackResolverTimestamp = mNanoTime.getAsLong();
        }
    }

    private boolean isFallbackResolverValid() {
        return mFallbackResolver != null && mNanoTime.getAsLong() - mFallbackResolverTimestamp < FALLBACK_RESOLVER_VALIDITY_NANOS;
    }

    @NonNull
    private DnsMessage queryResolver(String resolver, byte[] query) throws IOException {
        Request request = new Request.Builder()
                .url(resolver)
                .header("Accept", DNS_MESSAGE.toString())
                .post(RequestBody.create(query, DNS_MESSAGE))
                .build();

        try (Response response = mClientSupplier.get().newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null)
                throw new IOException("HTTP " + response.code());
            return new DnsMessage(body.bytes());
        }
    }
}
