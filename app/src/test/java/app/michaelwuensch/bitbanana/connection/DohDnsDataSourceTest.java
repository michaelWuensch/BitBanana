package app.michaelwuensch.bitbanana.connection;

import org.junit.Test;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsmessage.Question;
import org.minidns.dnsqueryresult.DnsQueryResult;
import org.minidns.record.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DohDnsDataSourceTest {

    private final List<String> mRequestedPaths = Collections.synchronizedList(new ArrayList<>());
    private String mLastMethod;
    private String mLastContentType;
    private byte[] mLastBody;
    private final AtomicLong mNanoTime = new AtomicLong(0);

    // Simulates the DoH resolvers. No network connection is made.
    // Paths starting with "/fail" answer with HTTP 500, "/wrongid" answers with a mismatching ID, everything else answers correctly.
    private final OkHttpClient mClient = new OkHttpClient.Builder()
            .addInterceptor(chain -> fakeResolver(chain.request()))
            .build();

    private Response fakeResolver(Request request) throws IOException {
        String path = request.url().encodedPath();
        mRequestedPaths.add(path);
        mLastMethod = request.method();
        mLastContentType = request.body() == null || request.body().contentType() == null ? null : request.body().contentType().toString();
        Buffer buffer = new Buffer();
        if (request.body() != null)
            request.body().writeTo(buffer);
        mLastBody = buffer.readByteArray();

        Response.Builder response = new Response.Builder().request(request).protocol(Protocol.HTTP_1_1);
        if (path.startsWith("/fail"))
            return response.code(500).message("Error").body(ResponseBody.create(new byte[0], null)).build();

        DnsMessage query = new DnsMessage(mLastBody);
        DnsMessage.Builder dnsResponse = query.getResponseBuilder(DnsMessage.RESPONSE_CODE.NO_ERROR);
        if (path.startsWith("/wrongid"))
            dnsResponse.setId(query.id + 1);
        return response.code(200).message("OK")
                .body(ResponseBody.create(dnsResponse.build().toArray(), MediaType.get("application/dns-message")))
                .build();
    }

    private static String url(String path) {
        return "https://resolver.example" + path;
    }

    private DohDnsDataSource dataSource(String... paths) {
        List<String> resolvers = new ArrayList<>();
        for (String path : paths)
            resolvers.add(url(path));
        return new DohDnsDataSource(() -> mClient, () -> resolvers, mNanoTime::get);
    }

    private static DnsMessage query(int id) {
        return DnsMessage.builder()
                .setId(id)
                .setRecursionDesired(true)
                .setQuestion(new Question("alice.user._bitcoin-payment.example.com", Record.TYPE.TXT))
                .build();
    }

    @Test
    public void givenWorkingResolver_whenQuery_thenDnsMessageIsPostedAsDohRequest() throws IOException {
        DnsMessage query = query(42);

        DnsQueryResult result = dataSource("/ok").query(query, null, 53);

        assertEquals(42, result.response.id);
        assertEquals("POST", mLastMethod);
        assertEquals("application/dns-message", mLastContentType);
        assertArrayEquals(query.toArray(), mLastBody);
    }

    @Test
    public void givenFirstResolverFails_whenQuery_thenNextResolverAnswers() throws IOException {
        DnsQueryResult result = dataSource("/fail", "/ok").query(query(1), null, 53);

        assertEquals(1, result.response.id);
        assertEquals(Arrays.asList("/fail", "/ok"), mRequestedPaths);
    }

    @Test
    public void givenResponseWithWrongId_whenQuery_thenNextResolverAnswers() throws IOException {
        DnsQueryResult result = dataSource("/wrongid", "/ok").query(query(7), null, 53);

        assertEquals(7, result.response.id);
        assertEquals(Arrays.asList("/wrongid", "/ok"), mRequestedPaths);
    }

    @Test
    public void givenAllResolversFail_whenQuery_thenIOExceptionAndNoOtherRequest() {
        try {
            dataSource("/fail1", "/fail2").query(query(1), null, 53);
            fail("IOException expected");
        } catch (IOException expected) {
            assertEquals(Arrays.asList("/fail1", "/fail2"), mRequestedPaths);
        }
    }

    @Test
    public void givenFallbackHappened_whenNextQuery_thenFallbackResolverIsTriedFirst() throws IOException {
        DohDnsDataSource dataSource = dataSource("/fail", "/ok");
        dataSource.query(query(1), null, 53);
        mRequestedPaths.clear();

        dataSource.query(query(2), null, 53);

        assertEquals(Collections.singletonList("/ok"), mRequestedPaths);
    }

    @Test
    public void givenFallbackExpired_whenNextQuery_thenPreferredResolverIsTriedAgain() throws IOException {
        DohDnsDataSource dataSource = dataSource("/fail", "/ok");
        dataSource.query(query(1), null, 53);
        mRequestedPaths.clear();

        mNanoTime.addAndGet(DohDnsDataSource.FALLBACK_RESOLVER_VALIDITY_NANOS + 1);
        dataSource.query(query(2), null, 53);

        assertEquals(Arrays.asList("/fail", "/ok"), mRequestedPaths);
    }

    @Test
    public void givenFallbackInUse_whenQueriedRepeatedly_thenValidityIsNotExtended() throws IOException {
        DohDnsDataSource dataSource = dataSource("/fail", "/ok");
        dataSource.query(query(1), null, 53);

        // Keep using the fallback shortly before it expires. This must not extend its validity.
        mNanoTime.addAndGet(DohDnsDataSource.FALLBACK_RESOLVER_VALIDITY_NANOS - 1);
        dataSource.query(query(2), null, 53);
        mRequestedPaths.clear();

        mNanoTime.addAndGet(2);
        dataSource.query(query(3), null, 53);

        assertEquals(Arrays.asList("/fail", "/ok"), mRequestedPaths);
    }
}
