package app.michaelwuensch.bitbanana.util;

import static junit.framework.TestCase.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class FeeEstimationUtilTest {

    @Test
    public void parseBlockstreamResponse() throws JSONException {
        JSONObject in = new JSONObject(readStringFromFile("fee_estimates_blockstream_response.json"));
        FeeEstimationUtil.FeeEstimates out = FeeEstimationUtil.getInstance().parseBlockstreamResponse(in);

        assertEquals(in.getInt("1"), out.getNextBlockFee());
        assertEquals(in.getInt("6"), out.getHourFee());
        assertEquals(in.getInt("144"), out.getDayFee());
        assertEquals(in.getInt("1008"), out.getMinimumFee());
    }

    @Test
    public void parseMempoolResponse() throws JSONException {
        JSONObject in = new JSONObject(readStringFromFile("fee_estimates_mempool_response.json"));
        FeeEstimationUtil.FeeEstimates out = FeeEstimationUtil.getInstance().parseMempoolResponse(in);

        assertEquals(in.getInt("fastestFee"), out.getNextBlockFee());
        assertEquals(in.getInt("hourFee"), out.getHourFee());
        assertEquals(in.getInt("economyFee"), out.getDayFee());
        assertEquals(in.getInt("minimumFee"), out.getMinimumFee());
    }

    private String readStringFromFile(String filename) {
        InputStream inputstream = this.getClass().getClassLoader().getResourceAsStream(filename);
        return new BufferedReader(new InputStreamReader(inputstream))
                .lines().collect(Collectors.joining("\n"));
    }
}