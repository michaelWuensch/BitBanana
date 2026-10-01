package app.michaelwuensch.bitbanana.lnurl.pay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LnUrlPaySuccessActionTest {

    private static final String CALLBACK = "https://pay.shop.example/lnurlp/callback";

    private static LnUrlPaySuccessAction urlAction(String url) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", "url");
        json.addProperty("description", "Thank you");
        json.addProperty("url", url);
        return new Gson().fromJson(json, LnUrlPaySuccessAction.class);
    }

    private static LnUrlPaySuccessAction messageAction(String message, String description) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", "message");
        json.addProperty("message", message);
        json.addProperty("description", description);
        return new Gson().fromJson(json, LnUrlPaySuccessAction.class);
    }

    @Test
    public void givenSameHost_whenIsUrlAllowed_thenTrue() {
        assertTrue(urlAction("https://pay.shop.example/order/123").isUrlAllowed(CALLBACK));
    }

    @Test
    public void givenOtherSubdomainOfSameSite_whenIsUrlAllowed_thenTrue() {
        assertTrue(urlAction("https://shop.example/order/123").isUrlAllowed(CALLBACK));
        assertTrue(urlAction("https://www.shop.example/order/123").isUrlAllowed(CALLBACK));
    }

    @Test
    public void givenDifferentSite_whenIsUrlAllowed_thenFalse() {
        assertFalse(urlAction("https://evil.example/order/123").isUrlAllowed(CALLBACK));
        // Looks similar, but is a different site.
        assertFalse(urlAction("https://shop.example.evil.example/order/123").isUrlAllowed(CALLBACK));
        assertFalse(urlAction("https://pay.shop.example@evil.example/order/123").isUrlAllowed(CALLBACK));
    }

    @Test
    public void givenDifferentSitesUnderSamePublicSuffix_whenIsUrlAllowed_thenFalse() {
        // co.uk is a public suffix, so a.co.uk and b.co.uk are different sites.
        assertFalse(urlAction("https://b.co.uk/order").isUrlAllowed("https://a.co.uk/callback"));
    }

    @Test
    public void givenOtherSchemes_whenIsUrlAllowed_thenFalse() {
        assertFalse(urlAction("lightning:lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypq").isUrlAllowed(CALLBACK));
        assertFalse(urlAction("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4?amount=1").isUrlAllowed(CALLBACK));
        assertFalse(urlAction("http://pay.shop.example/order/123").isUrlAllowed(CALLBACK));
        assertFalse(urlAction("tel:+123456789").isUrlAllowed(CALLBACK));
    }

    @Test
    public void givenOnionService_whenIsUrlAllowed_thenHttpIsAllowed() {
        String onion = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion";

        assertTrue(urlAction("http://" + onion + "/order/123").isUrlAllowed("http://" + onion + "/callback"));
    }

    @Test
    public void givenInvalidOrMissingValues_whenIsUrlAllowed_thenFalse() {
        assertFalse(urlAction(null).isUrlAllowed(CALLBACK));
        assertFalse(urlAction("https://inva lid.example/").isUrlAllowed(CALLBACK));
        assertFalse(urlAction("https://pay.shop.example/order/123").isUrlAllowed(null));
    }

    @Test
    public void givenLongTexts_whenGet_thenLimitedTo144Characters() {
        String longText = "x".repeat(500);

        LnUrlPaySuccessAction action = messageAction(longText, longText);

        assertEquals(LnUrlPaySuccessAction.MAX_TEXT_LENGTH, action.getMessage().length());
        assertEquals(LnUrlPaySuccessAction.MAX_TEXT_LENGTH, action.getDescription().length());
        assertTrue(action.getMessage().endsWith("…"));
    }

    @Test
    public void givenShortOrMissingTexts_whenGet_thenUnchangedOrEmpty() {
        LnUrlPaySuccessAction action = messageAction("Thanks!", null);

        assertEquals("Thanks!", action.getMessage());
        assertEquals("", action.getDescription());
    }
}
