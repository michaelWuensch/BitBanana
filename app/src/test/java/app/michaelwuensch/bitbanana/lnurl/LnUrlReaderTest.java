package app.michaelwuensch.bitbanana.lnurl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LnUrlReaderTest {

    @Test
    public void givenHttpsUrl_whenIsSecureLnUrlUrl_thenTrue() {
        assertTrue(LnUrlReader.isSecureLnUrlUrl("https://service.example/lnurlp/alice"));
    }

    @Test
    public void givenHttpClearnetUrl_whenIsSecureLnUrlUrl_thenFalse() {
        assertFalse(LnUrlReader.isSecureLnUrlUrl("http://service.example/lnurlp/alice"));
    }

    @Test
    public void givenHttpOnionUrl_whenIsSecureLnUrlUrl_thenTrue() {
        assertTrue(LnUrlReader.isSecureLnUrlUrl("http://abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion/lnurlp/alice"));
    }

    @Test
    public void givenHttpUrlWithOnionOnlyInPath_whenIsSecureLnUrlUrl_thenFalse() {
        assertFalse(LnUrlReader.isSecureLnUrlUrl("http://service.example/x.onion/lnurlp/alice"));
    }

    @Test
    public void givenHttpUrlWithOnionAsSubdomain_whenIsSecureLnUrlUrl_thenFalse() {
        assertFalse(LnUrlReader.isSecureLnUrlUrl("http://abc.onion.service.example/lnurlp/alice"));
    }

    @Test
    public void givenOtherScheme_whenIsSecureLnUrlUrl_thenFalse() {
        assertFalse(LnUrlReader.isSecureLnUrlUrl("ftp://service.example/lnurlp/alice"));
        assertFalse(LnUrlReader.isSecureLnUrlUrl("file:///data/data/app/secret"));
    }

    @Test
    public void givenInvalidOrMissingUrl_whenIsSecureLnUrlUrl_thenFalse() {
        assertFalse(LnUrlReader.isSecureLnUrlUrl("https://inva lid.example/"));
        assertFalse(LnUrlReader.isSecureLnUrlUrl(""));
        assertFalse(LnUrlReader.isSecureLnUrlUrl(null));
    }

    @Test
    public void givenLud17Uri_whenLud17ToUrl_thenHttps() {
        assertEquals("https://service.example/lnurlp/alice", LnUrlReader.lud17ToUrl("lnurlp://service.example/lnurlp/alice"));
    }

    @Test
    public void givenLud17OnionUri_whenLud17ToUrl_thenHttp() {
        String onion = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion";

        assertEquals("http://" + onion + "/withdraw?k1=abc", LnUrlReader.lud17ToUrl("lnurlw://" + onion + "/withdraw?k1=abc"));
    }

    @Test
    public void givenLud17UriWithOnionOnlyInPath_whenLud17ToUrl_thenStillHttps() {
        assertEquals("https://service.example/x.onion/pay", LnUrlReader.lud17ToUrl("lnurlp://service.example/x.onion/pay"));
    }
}
