package app.michaelwuensch.bitbanana.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


public class FeeEstimationUtil {

    private static final String LOG_TAG = FeeEstimationUtil.class.getSimpleName();

    private static final String INTERNAL = "Internal";
    private static final String BLOCKSTREAM = "Blockstream";
    private static final String BLOCKSTREAM_TOR = "Blockstream (v3 Tor)";
    private static final String BLOCKSTREAM_HOST = "https://blockstream.info";
    private static final String BLOCKSTREAM_TOR_HOST = "http://explorerzydxu5ecjrkwceayqybizmpjjznk5izmitf2modhcusuqlid.onion";
    private static final String MEMPOOL = "Mempool.space";
    private static final String MEMPOOL_TOR = "Mempool (v3 Tor)";
    private static final String MEMPOOL_HOST = "https://mempool.space";
    private static final String MEMPOOL_TOR_HOST = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion";

    private final Set<FeeEstimationListener> mFeeEstimationListeners = new HashSet<>();

    private static FeeEstimationUtil mInstance;


    private FeeEstimationUtil() {
    }

    public static FeeEstimationUtil getInstance() {
        if (mInstance == null) {
            mInstance = new FeeEstimationUtil();
        }

        return mInstance;
    }

    public void getFeeEstimates() {

        if (!MonetaryUtil.getInstance().getSecondCurrency().isBitcoin() ||
                !PrefsUtil.getPrefs().contains(PrefsUtil.AVAILABLE_FIAT_CURRENCIES)) {

            String provider = PrefsUtil.getFeeEstimationProvider();

            switch (provider) {
                case INTERNAL:
                    if (BackendManager.getCurrentBackend().supportsFeeEstimation())
                        sendInternalRequest();
                    else
                        sendMempoolRequest(MEMPOOL_HOST);
                    break;
                case BLOCKSTREAM:
                    sendBlockstreamRequest(BLOCKSTREAM_HOST);
                    break;
                case BLOCKSTREAM_TOR:
                    sendBlockstreamRequest(BLOCKSTREAM_TOR_HOST);
                    break;
                case MEMPOOL:
                    sendMempoolRequest(MEMPOOL_HOST);
                    break;
                case MEMPOOL_TOR:
                    sendMempoolRequest(MEMPOOL_TOR_HOST);
                    break;
                case "Custom":
                    sendMempoolRequest(PrefsUtil.getCustomFeeEstimationProviderHost());
                    break;
                default:
                    sendMempoolRequest(MEMPOOL_HOST);
            }

            BBLog.v(LOG_TAG, "Fee estimation request initiated");
        }
    }

    /**
     * Calls the nodes internal fee estimation implementation.
     * When executed this request saves the result in shared preferences
     */
    private void sendInternalRequest() {

        CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(BackendManager.api().getFeeEstimates()
                .subscribe(response -> {
                    FeeEstimates estimates = new FeeEstimates();
                    estimates.setNextBlockFee(response.getNextBlockFee());
                    estimates.setHourFee(response.getHourFee());
                    estimates.setDayFee(response.getDayFee());
                    estimates.setMinimumFee(response.getMinimumFee());
                    applyFeeEstimatesAndSaveInPreferences(estimates);
                    compositeDisposable.dispose();
                }, throwable -> {
                    BBLog.w(LOG_TAG, "Exception in fee estimation request task.");
                    BBLog.w(LOG_TAG, throwable.getMessage());
                }));
    }

    /**
     * Creates and sends a request that fetches fee estimate data from a mempool instance.
     * When executed this request saves the result in shared preferences
     */
    private void sendMempoolRequest(String host) {
        if (host == null || host.isEmpty()) {
            BBLog.w(LOG_TAG, "Custom fee estimation provider host is empty.");
            return;
        }

        if (host.endsWith("/"))
            host = host.substring(0, host.length() - 1);

        if (!(host.startsWith("https://") || host.startsWith("http://")))
            if (host.toLowerCase().contains(".onion"))
                host = "http://" + host;
            else
                host = "https://" + host;

        Request request = new Request.Builder()
                .url(host + "/api/v1/fees/recommended")
                .build();

        HttpClient.getInstance().getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                BBLog.w(LOG_TAG, "Fetching fee estimates from mempool instance failed");
                BBLog.w(LOG_TAG, e.getMessage());
                if (e.getMessage() != null)
                    if (e.getMessage().startsWith("Unable to resolve host") && e.getMessage().contains(".onion"))
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Context context = App.getAppContext();
                                if (context != null) {
                                    Toast.makeText(context, R.string.error_fee_estimation_requires_tor_toast, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                BBLog.v(LOG_TAG, "Received fee estimates from mempool instance");
                String responseData = response.body().string();
                BBLog.d(LOG_TAG, responseData);
                JSONObject responseJson = null;
                try {
                    responseJson = new JSONObject(responseData);
                } catch (JSONException e) {
                    BBLog.w(LOG_TAG, "mempool response could not be parsed as json");
                    e.printStackTrace();
                    if (responseData.toLowerCase().contains("cloudflare") && responseData.toLowerCase().contains("captcha-bypass")) {
                        broadcastFeeEstimationUpdateFailed(FeeEstimationListener.ERROR_CLOUDFLARE_BLOCKED_TOR, RefConstants.ERROR_DURATION_VERY_LONG);
                    }
                }
                if (responseJson != null) {
                    FeeEstimates estimates = parseMempoolResponse(responseJson);
                    applyFeeEstimatesAndSaveInPreferences(estimates);
                }
            }
        });
    }


    /**
     * Creates and sends a request that fetches fee estimate data from blockstream.
     * When executed this request saves the result in shared preferences.
     */
    private void sendBlockstreamRequest(String host) {

        Request request = new Request.Builder()
                .url(host + "/api/fee-estimates")
                .build();

        HttpClient.getInstance().getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                BBLog.w(LOG_TAG, "Fetching fee estimates from blockstream failed");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                BBLog.v(LOG_TAG, "Received fee estimates from blockstream");
                String responseData = response.body().string();
                JSONObject responseJson = null;
                try {
                    responseJson = new JSONObject(responseData);
                } catch (JSONException e) {
                    BBLog.w(LOG_TAG, "blockstream response could not be parsed as json");
                    e.printStackTrace();
                    if (responseData.toLowerCase().contains("cloudflare") && responseData.toLowerCase().contains("captcha-bypass")) {
                        broadcastFeeEstimationUpdateFailed(FeeEstimationListener.ERROR_CLOUDFLARE_BLOCKED_TOR, RefConstants.ERROR_DURATION_VERY_LONG);
                    }
                }
                if (responseJson != null) {
                    FeeEstimates estimates = parseBlockstreamResponse(responseJson);
                    applyFeeEstimatesAndSaveInPreferences(estimates);
                }
            }
        });
    }


    /**
     * This function parses a mempool recommended fee estimation response.
     */
    public FeeEstimates parseMempoolResponse(JSONObject response) {
        FeeEstimates estimates = new FeeEstimates();
        // loop through all returned keys
        Iterator<String> iter = response.keys();
        while (iter.hasNext()) {
            String type = iter.next();
            try {
                if (type.equals("fastestFee"))
                    estimates.setNextBlockFee(response.getInt(type));
                if (type.equals("hourFee"))
                    estimates.setHourFee(response.getInt(type));
                if (type.equals("economyFee"))
                    estimates.setDayFee(response.getInt(type));
                if (type.equals("minimumFee"))
                    estimates.setMinimumFee(response.getInt(type));
            } catch (JSONException e) {
                BBLog.e(LOG_TAG, "Unable to read fee estimate from mempool response.");
            }
        }
        return estimates;
    }

    /**
     * This function parses a blockstream recommended fee estimation response.
     */
    public FeeEstimates parseBlockstreamResponse(JSONObject response) {
        FeeEstimates estimates = new FeeEstimates();
        // loop through all returned keys
        Iterator<String> iter = response.keys();
        while (iter.hasNext()) {
            String type = iter.next();
            try {
                if (type.equals("1"))
                    estimates.setNextBlockFee((int) response.getDouble(type));
                if (type.equals("6"))
                    estimates.setHourFee((int) response.getDouble(type));
                if (type.equals("144"))
                    estimates.setDayFee((int) response.getDouble(type));
                if (type.equals("1008"))
                    estimates.setMinimumFee((int) response.getDouble(type));
            } catch (JSONException e) {
                BBLog.e(LOG_TAG, "Unable to read fee estimate from blockstream response.");
            }
        }
        return estimates;
    }

    private void applyFeeEstimatesAndSaveInPreferences(FeeEstimates feeEstimates) {
        final SharedPreferences.Editor editor = PrefsUtil.editPrefs();
        editor.putInt(PrefsUtil.FEE_ESTIMATE_NEXT_BLOCK, feeEstimates.NextBlockFee);
        editor.putInt(PrefsUtil.FEE_ESTIMATE_HOUR, feeEstimates.HourFee);
        editor.putInt(PrefsUtil.FEE_ESTIMATE_DAY, feeEstimates.DayFee);
        editor.putInt(PrefsUtil.FEE_ESTIMATE_MINIMUM, feeEstimates.MinimumFee);
        editor.putLong(PrefsUtil.FEE_ESTIMATE_TIMESTAMP, feeEstimates.Timestamp);
        editor.commit();
        BBLog.d(LOG_TAG, "New fee estimates are: " + feeEstimates.NextBlockFee + ", " + feeEstimates.HourFee + ", " + feeEstimates.getDayFee() + ", " + feeEstimates.getMinimumFee());
        broadcastFeeEstimationUpdate();
    }


    // Event handling to notify all registered listeners to an fee estimation change.

    private void broadcastFeeEstimationUpdate() {
        for (FeeEstimationListener listener : mFeeEstimationListeners) {
            listener.onFeeEstimationUpdated();
        }
    }

    private void broadcastFeeEstimationUpdateFailed(int error, int duration) {
        for (FeeEstimationListener listener : mFeeEstimationListeners) {
            listener.onFeeEstimationUpdateFailed(error, duration);
        }
    }

    public void registerFeeEstimationListener(FeeEstimationListener listener) {
        mFeeEstimationListeners.add(listener);
    }

    public void unregisterFeeEstimationListener(FeeEstimationListener listener) {
        mFeeEstimationListeners.remove(listener);
    }

    public interface FeeEstimationListener {

        int ERROR_CLOUDFLARE_BLOCKED_TOR = 0;

        void onFeeEstimationUpdated();

        void onFeeEstimationUpdateFailed(int error, int duration);
    }

    /**
     * FeeEstimates in sat/vB
     */
    public class FeeEstimates {
        private long Timestamp;
        private int NextBlockFee;
        private int HourFee;
        private int DayFee;
        private int MinimumFee;

        public int getNextBlockFee() {
            return NextBlockFee;
        }

        public void setNextBlockFee(int nextBlockFee) {
            NextBlockFee = nextBlockFee;
        }

        public int getHourFee() {
            return HourFee;
        }

        public void setHourFee(int hourFee) {
            HourFee = hourFee;
        }

        public int getDayFee() {
            return DayFee;
        }

        public void setDayFee(int dayFee) {
            DayFee = dayFee;
        }

        public int getMinimumFee() {
            return MinimumFee;
        }

        public void setMinimumFee(int minimumFee) {
            MinimumFee = minimumFee;
        }

        public long getTimestamp() {
            return Timestamp;
        }

        public void setTimestamp(long timestamp) {
            Timestamp = timestamp;
        }
    }
}
