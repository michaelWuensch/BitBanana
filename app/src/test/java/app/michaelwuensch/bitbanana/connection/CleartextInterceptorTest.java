package app.michaelwuensch.bitbanana.connection;

import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CleartextInterceptorTest {

    private static final Proxy TOR_PROXY = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 9050));

    private static InetSocketAddress address(String ip) throws IOException {
        return new InetSocketAddress(InetAddress.getByName(ip), 80);
    }

    private static boolean allowed(String url, Proxy proxy, InetSocketAddress socketAddress) {
        return CleartextInterceptor.isCleartextAllowed(HttpUrl.get(url), proxy, socketAddress);
    }

    @Test
    public void https_isAlwaysAllowed() throws IOException {
        assertTrue(allowed("https://lndhub.example.com/", Proxy.NO_PROXY, address("93.184.216.34")));
        assertTrue(allowed("https://lndhub.example.com/", TOR_PROXY, InetSocketAddress.createUnresolved("lndhub.example.com", 443)));
    }

    @Test
    public void httpOnion_isAllowed() {
        assertTrue(allowed("http://abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion/", TOR_PROXY,
                InetSocketAddress.createUnresolved("abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion", 80)));
    }

    @Test
    public void httpPublicAddress_isRefused() throws IOException {
        assertFalse(allowed("http://lndhub.example.com/", Proxy.NO_PROXY, address("93.184.216.34")));
        assertFalse(allowed("http://lndhub.example.com/", Proxy.NO_PROXY, address("2606:2800:220:1:248:1893:25c8:1946")));
    }

    @Test
    public void httpClearnetOverTor_isRefused() {
        // The Tor exit node could read everything.
        assertFalse(allowed("http://lndhub.example.com/", TOR_PROXY, InetSocketAddress.createUnresolved("lndhub.example.com", 80)));
    }

    @Test
    public void httpPrivateAddressOverProxy_isRefused() throws IOException {
        assertFalse(allowed("http://192.168.1.10/", TOR_PROXY, address("192.168.1.10")));
    }

    @Test
    public void httpPrivateAddress_isAllowed() throws IOException {
        // Hostnames like umbrel.local are checked by the address they resolved to.
        assertTrue(allowed("http://umbrel.local:3008/", Proxy.NO_PROXY, address("192.168.1.10")));
        assertTrue(allowed("http://10.0.0.5/", Proxy.NO_PROXY, address("10.0.0.5")));
        assertTrue(allowed("http://172.16.0.5/", Proxy.NO_PROXY, address("172.16.0.5")));
        assertTrue(allowed("http://localhost/", Proxy.NO_PROXY, address("127.0.0.1")));
    }

    @Test
    public void httpMissingAddress_isRefused() {
        assertFalse(allowed("http://192.168.1.10/", Proxy.NO_PROXY, null));
        assertFalse(allowed("http://192.168.1.10/", null, null));
    }

    @Test
    public void isPrivateAddress() throws IOException {
        // Loopback
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("127.0.0.1")));
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("::1")));
        // LAN
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("10.255.255.255")));
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("172.31.0.1")));
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("192.168.0.1")));
        // Link-local
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("169.254.1.1")));
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("fe80::1")));
        // Carrier-grade NAT (Tailscale)
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("100.64.0.1")));
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("100.101.102.103")));
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("100.127.255.255")));
        // Unique local (Tailscale IPv6)
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("fd7a:115c:a1e0::1")));
        assertTrue(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("fc00::1")));

        // Public
        assertFalse(CleartextInterceptor.isPrivateAddress(null));
        assertFalse(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("8.8.8.8")));
        assertFalse(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("172.32.0.1")));
        assertFalse(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("100.63.255.255")));
        assertFalse(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("100.128.0.1")));
        assertFalse(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("2001:4860:4860::8888")));
        assertFalse(CleartextInterceptor.isPrivateAddress(InetAddress.getByName("fe00::1")));
    }

    @Test
    public void givenHttpsRequest_whenIntercept_thenRequestProceeds() throws IOException {
        FakeChain chain = new FakeChain("https://lndhub.example.com/auth?type=auth");

        Response response = new CleartextInterceptor().intercept(chain);

        assertEquals(200, response.code());
    }

    @Test
    public void givenHttpRequestWithoutConnection_whenIntercept_thenRequestIsRefused() {
        FakeChain chain = new FakeChain("http://lndhub.example.com/auth?type=auth");

        try {
            new CleartextInterceptor().intercept(chain);
            fail("IOException expected");
        } catch (IOException expected) {
            assertTrue(CleartextInterceptor.isCleartextRefused(expected));
            assertFalse(chain.proceeded);
        }
    }

    @Test
    public void isCleartextRefused_checksCauses() {
        assertTrue(CleartextInterceptor.isCleartextRefused(new RuntimeException(new CleartextInterceptor.CleartextRefusedException("host"))));
        assertFalse(CleartextInterceptor.isCleartextRefused(new IOException("other")));
        assertFalse(CleartextInterceptor.isCleartextRefused(null));
    }

    private static class FakeChain implements Interceptor.Chain {
        private final Request mRequest;
        boolean proceeded = false;

        FakeChain(String url) {
            mRequest = new Request.Builder().url(url).build();
        }

        @Override
        public Request request() {
            return mRequest;
        }

        @Override
        public Response proceed(Request request) {
            proceeded = true;
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .build();
        }

        @Override
        public Connection connection() {
            return null;
        }

        @Override
        public Call call() {
            return null;
        }

        @Override
        public int connectTimeoutMillis() {
            return 0;
        }

        @Override
        public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
            return this;
        }

        @Override
        public int readTimeoutMillis() {
            return 0;
        }

        @Override
        public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
            return this;
        }

        @Override
        public int writeTimeoutMillis() {
            return 0;
        }

        @Override
        public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
            return this;
        }
    }
}
