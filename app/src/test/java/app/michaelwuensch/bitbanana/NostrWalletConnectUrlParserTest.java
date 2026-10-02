package app.michaelwuensch.bitbanana;

import org.junit.Test;

import app.michaelwuensch.bitbanana.backendConfigs.nostrWalletConnect.NostrWalletConnectUrlParser;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class NostrWalletConnectUrlParserTest {

    private static final String PUBKEY = "b889ff5b1513b641e2a139f661a661364979c5beee91842f8f0ef42ab558e9d4";
    private static final String SECRET = "71a8c14c1407c113601079c4302dab36460f0ccd0ad506f1f2dc73b5100e4f3c";
    private static final String CLEARNET_RELAY = "wss%3A%2F%2Frelay.example.com";
    private static final String TOR_RELAY = "ws%3A%2F%2Fexamplexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.onion";

    private static String nwcUri(String... relays) {
        StringBuilder sb = new StringBuilder("nostr+walletconnect://" + PUBKEY + "?");
        for (String relay : relays) {
            sb.append("relay=").append(relay).append("&");
        }
        sb.append("secret=").append(SECRET);
        return sb.toString();
    }

    @Test
    public void parse_clearnetRelay_noTor() {
        NostrWalletConnectUrlParser parser = new NostrWalletConnectUrlParser(nwcUri(CLEARNET_RELAY)).parse();

        assertFalse(parser.hasError());
        assertEquals("wss://relay.example.com", parser.getRelay());
        assertFalse(parser.hasTorRelay());
        assertFalse(parser.getBackendConfig().getUseTor());
    }

    @Test
    public void parse_torRelay_enablesTor() {
        NostrWalletConnectUrlParser parser = new NostrWalletConnectUrlParser(nwcUri(TOR_RELAY)).parse();

        assertFalse(parser.hasError());
        assertTrue(parser.hasTorRelay());
        assertTrue(parser.getBackendConfig().getUseTor());
    }

    @Test
    public void parse_multipleRelaysOneTor_enablesTor() {
        NostrWalletConnectUrlParser parser = new NostrWalletConnectUrlParser(nwcUri(TOR_RELAY, CLEARNET_RELAY)).parse();

        assertFalse(parser.hasError());
        assertTrue(parser.hasTorRelay());
        assertTrue(parser.getBackendConfig().getUseTor());
    }

    @Test
    public void parse_onionInPathOnly_noTor() {
        NostrWalletConnectUrlParser parser = new NostrWalletConnectUrlParser(nwcUri("wss%3A%2F%2Frelay.example.com%2Fx.onion")).parse();

        assertFalse(parser.hasError());
        assertFalse(parser.hasTorRelay());
        assertFalse(parser.getBackendConfig().getUseTor());
    }

    @Test
    public void containsTorRelay() {
        assertTrue(NostrWalletConnectUrlParser.containsTorRelay(nwcUri(TOR_RELAY)));
        assertTrue(NostrWalletConnectUrlParser.containsTorRelay(nwcUri(CLEARNET_RELAY, TOR_RELAY)));
        assertFalse(NostrWalletConnectUrlParser.containsTorRelay(nwcUri(CLEARNET_RELAY)));
        // Incomplete input while typing
        assertTrue(NostrWalletConnectUrlParser.containsTorRelay("nostr+walletconnect://" + PUBKEY + "?relay=" + TOR_RELAY));
        assertFalse(NostrWalletConnectUrlParser.containsTorRelay("nostr+walletconnect://" + PUBKEY + "?relay=ws%3A%2F%2Fexample.oni"));
        assertFalse(NostrWalletConnectUrlParser.containsTorRelay("nostr+walletconnect://"));
        assertFalse(NostrWalletConnectUrlParser.containsTorRelay(""));
        assertFalse(NostrWalletConnectUrlParser.containsTorRelay(null));
        // Not a NWC string
        assertFalse(NostrWalletConnectUrlParser.containsTorRelay("https://example.com?relay=" + TOR_RELAY));
    }
}
