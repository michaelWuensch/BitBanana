package app.michaelwuensch.bitbanana.util;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BBLogTest {

    @Test
    public void givenLndConnectUri_whenRedact_thenHostAndParamNamesStayVisible() {
        String uri = "lndconnect://mynode.example:10009?cert=MIICJjCCAc&macaroon=AgEDbG5kAvgB";

        String redacted = BBLog.redact(uri);

        assertEquals("lndconnect://mynode.example:10009?cert=<redacted, 10 chars>&macaroon=<redacted, 12 chars>", redacted);
    }

    @Test
    public void givenLndHubUri_whenRedact_thenCredentialsAreRedacted() {
        String uri = "lndhub://myuser:mypassword@https://lndhub.example/";

        String redacted = BBLog.redact(uri);

        assertEquals("lndhub://<redacted, 17 chars>@https://lndhub.example/", redacted);
    }

    @Test
    public void givenNostrWalletConnectUri_whenRedact_thenSecretIsRedactedButRelayIsVisible() {
        String uri = "nostr+walletconnect://b889ff5b1513b641e2a139f661a661364979c5beee91842f8f0ef42ab558e9d4?relay=wss%3A%2F%2Frelay.example&secret=71a8c14c1407c113601079c4302dab36460f0ccd0ad506f1f2dc73b5100e4f3c";

        String redacted = BBLog.redact(uri);

        assertEquals("nostr+walletconnect://b889ff5b1513b641e2a139f661a661364979c5beee91842f8f0ef42ab558e9d4?relay=wss%3A%2F%2Frelay.example&secret=<redacted, 64 chars>", redacted);
    }

    @Test
    public void givenLnurlWithdrawUrl_whenRedact_thenPathAndK1AreRedacted() {
        String url = "https://service.example/withdraw/api/v1/lnurl/abc123?tag=withdrawRequest&k1=deadbeef";

        String redacted = BBLog.redact(url);

        assertEquals("https://service.example/<redacted, 28 chars>?tag=withdrawRequest&k1=<redacted, 8 chars>", redacted);
    }

    @Test
    public void givenLightningUri_whenRedact_thenOnlyBeginningOfInvoiceIsVisible() {
        String uri = "lightning:lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypq";

        String redacted = BBLog.redact(uri);

        assertEquals("lightning:lnbc1p...<redacted, 61 chars>", redacted);
    }

    @Test
    public void givenUnknownOpaqueUri_whenRedact_thenNothingButSchemeIsVisible() {
        String data = "myuser:mypassword@host";

        String redacted = BBLog.redact(data);

        assertEquals("myuser:<redacted, 15 chars>", redacted);
    }

    @Test
    public void givenRawData_whenRedact_thenOnlyBeginningIsVisible() {
        String macaroon = "0201036c6e6402f801030a10";

        String redacted = BBLog.redact(macaroon);

        assertEquals("020103...<redacted, 18 chars>", redacted);
    }

    @Test
    public void givenShortRawData_whenRedact_thenEverythingIsRedacted() {
        assertEquals("<redacted, 4 chars>", BBLog.redact("abcd"));
    }

    @Test
    public void givenFragment_whenRedact_thenFragmentIsRedacted() {
        String redacted = BBLog.redact("https://host.example/#secretfragment");

        assertEquals("https://host.example/#<redacted, 14 chars>", redacted);
    }

    @Test
    public void givenParamWithoutValue_whenRedact_thenParamIsRedacted() {
        String redacted = BBLog.redact("clngrpc://host.example:9736?secrettoken&pubkey=02abc");

        assertFalse(redacted.contains("secrettoken"));
        assertEquals("clngrpc://host.example:9736?<redacted, 11 chars>&pubkey=<redacted, 5 chars>", redacted);
    }

    @Test
    public void givenUriSyntaxException_whenRedactException_thenInputIsNotContained() {
        try {
            new URI("lndconnect://host.example:10009?cert=AB CD&macaroon=SECRETMACAROON");
            fail("URISyntaxException expected");
        } catch (URISyntaxException e) {
            // Make sure the test is meaningful: the original message contains the secret.
            assertTrue(e.getMessage().contains("SECRETMACAROON"));

            String redacted = BBLog.redactException(e);

            assertFalse(redacted.contains("SECRETMACAROON"));
            assertEquals("URISyntaxException: Illegal character in query at index 39 (message redacted)", redacted);
        }
    }

    @Test
    public void givenOtherException_whenRedactException_thenOnlyClassNameIsReturned() {
        String redacted = BBLog.redactException(new IllegalArgumentException("secret=abc"));

        assertEquals("IllegalArgumentException (message redacted)", redacted);
    }

    @Test
    public void givenNull_whenRedact_thenNullString() {
        assertEquals("null", BBLog.redact(null));
    }
}
