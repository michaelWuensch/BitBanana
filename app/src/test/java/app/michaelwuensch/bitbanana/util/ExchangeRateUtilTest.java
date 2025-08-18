package app.michaelwuensch.bitbanana.util;

import static junit.framework.TestCase.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ExchangeRateUtilTest {

    // The exchange rate read from the endpoints is in relation to BTC.
    // Internally we use Milli Satoshis. That's why a received rate of 1.0 has to equal 1E-11 after parsing.

    @Test
    public void parseBlockchainInfoResponse() throws JSONException {
        JSONObject in = new JSONObject(readStringFromFile("exchange_rate_blockchain_info_response.json"));
        JSONObject out = ExchangeRateUtil.getInstance().parseBlockchainInfoResponse(in);

        assertEquals(2, out.length());
        assertEquals(1E-11, out.getJSONObject("USD").getDouble("rate"));
    }

    @Test
    public void parseCoinbaseResponse() throws JSONException {
        JSONObject in = new JSONObject(readStringFromFile("exchange_rate_coinbase_response.json"));
        JSONObject out = ExchangeRateUtil.getInstance().parseCoinbaseResponse(in, true);

        assertEquals(2, out.length());
        assertEquals(1E-11, out.getJSONObject("USD").getDouble("rate"));
    }

    @Test
    public void parseMempoolResponse() throws JSONException {
        JSONObject in = new JSONObject(readStringFromFile("exchange_rate_mempool_response.json"));
        JSONObject out = ExchangeRateUtil.getInstance().parseMempoolResponse(in);

        assertEquals(2, out.length());
        assertEquals(1E-11, out.getJSONObject("USD").getDouble("rate"));
    }

    private String readStringFromFile(String filename) {
        InputStream inputstream = this.getClass().getClassLoader().getResourceAsStream(filename);
        return new BufferedReader(new InputStreamReader(inputstream))
                .lines().collect(Collectors.joining("\n"));
    }

}