package app.michaelwuensch.bitbanana.lnurl;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class LnUrlSecurityInterceptorTest {

    @Test
    public void givenHttpsRequest_whenIntercept_thenRequestProceeds() throws IOException {
        FakeChain chain = new FakeChain("https://service.example/lnurlp/alice");

        Response response = new LnUrlSecurityInterceptor().intercept(chain);

        assertEquals(200, response.code());
    }

    @Test
    public void givenHttpOnionRequest_whenIntercept_thenRequestProceeds() throws IOException {
        FakeChain chain = new FakeChain("http://abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion/lnurlp/alice");

        Response response = new LnUrlSecurityInterceptor().intercept(chain);

        assertEquals(200, response.code());
    }

    @Test
    public void givenHttpClearnetRequest_whenIntercept_thenRequestIsRefused() {
        // This is what a redirect from https to http looks like for a network interceptor: a separate request to the http URL.
        FakeChain chain = new FakeChain("http://service.example/lnurlp/alice");

        try {
            new LnUrlSecurityInterceptor().intercept(chain);
            fail("IOException expected");
        } catch (IOException expected) {
            assertFalse(chain.proceeded);
        }
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
