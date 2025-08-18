package app.michaelwuensch.bitbanana.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.Currency;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


public class ExchangeRateUtil {

    private static final String LOG_TAG = ExchangeRateUtil.class.getSimpleName();

    private static final String BLOCKCHAIN_INFO = "Blockchain.info";
    private static final String COINBASE = "Coinbase";
    private static final String MEMPOOL = "Mempool.space";
    private static final String MEMPOOL_TOR = "Mempool (v3 Tor)";
    private static final String MEMPOOL_HOST = "https://mempool.space";
    private static final String MEMPOOL_TOR_HOST = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion";
    private static final String RATE = "rate";
    private static final String SYMBOL = "symbol";
    private static final String TIMESTAMP = "timestamp";

    private final Set<ExchangeRateUtil.ExchangeRateListener> mExchangeRateListeners = new HashSet<>();

    private static ExchangeRateUtil mInstance;


    private ExchangeRateUtil() {
    }

    public static ExchangeRateUtil getInstance() {
        if (mInstance == null) {
            mInstance = new ExchangeRateUtil();
        }

        return mInstance;
    }

    public void getExchangeRates(boolean force) {

        if (force || MonetaryUtil.getInstance().displaysAtLeastOneFiatCurrency() ||
                !PrefsUtil.getPrefs().contains(PrefsUtil.AVAILABLE_FIAT_CURRENCIES)) {

            String provider = PrefsUtil.getExchangeRateProvider();

            switch (provider) {
                case BLOCKCHAIN_INFO:
                    sendBlockchainInfoRequest();
                    break;
                case COINBASE:
                    sendCoinbaseRequest();
                    break;
                case MEMPOOL:
                    sendMempoolRequest(MEMPOOL_HOST);
                    break;
                case MEMPOOL_TOR:
                    sendMempoolRequest(MEMPOOL_TOR_HOST);
                    break;
                case "Custom":
                    sendMempoolRequest(PrefsUtil.getCustomExchangeRateProviderHost());
                    break;
                default:
                    sendBlockchainInfoRequest();
            }

            BBLog.d(LOG_TAG, "Exchange rate request initiated");
        }
    }

    /**
     * Creates and sends a request that fetches fiat exchange rate data from a mempool instance.
     * When executed this request saves the result in shared preferences and
     * updates the currentCurrency of the MonetaryUtil Singleton.
     */
    private void sendMempoolRequest(String host) {
        if (host == null || host.isEmpty()) {
            BBLog.w(LOG_TAG, "Custom exchange rate provider host is empty.");
            return;
        }

        if (host.endsWith("/"))
            host = host.substring(0, host.length() - 1);

        if (!(host.startsWith("https://") || host.startsWith("http://")))
            if (host.toLowerCase().contains(".onion"))
                host = "http://" + host;
            else
                host = "https://" + host;

        Request rateRequest = new Request.Builder()
                .url(host + "/api/v1/prices")
                .build();

        HttpClient.getInstance().getClient().newCall(rateRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                BBLog.w(LOG_TAG, "Fetching exchange rates from mempool instance failed");
                BBLog.w(LOG_TAG, e.getMessage());
                if (e.getMessage() != null)
                    if (e.getMessage().startsWith("Unable to resolve host") && e.getMessage().contains(".onion"))
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Context context = App.getAppContext();
                                if (context != null) {
                                    Toast.makeText(context, R.string.error_exchange_rate_requires_tor_toast, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                BBLog.d(LOG_TAG, "Received exchange rates from mempool instance");
                String responseData = response.body().string();
                JSONObject responseJson = null;
                try {
                    responseJson = new JSONObject(responseData);
                } catch (JSONException e) {
                    BBLog.w(LOG_TAG, "mempool response could not be parsed as json");
                    e.printStackTrace();
                    if (responseData.toLowerCase().contains("cloudflare") && responseData.toLowerCase().contains("captcha-bypass")) {
                        broadcastExchangeRateUpdateFailed(ExchangeRateListener.ERROR_CLOUDFLARE_BLOCKED_TOR, RefConstants.ERROR_DURATION_VERY_LONG);
                    }
                }
                if (responseJson != null) {
                    JSONObject responseRates = parseMempoolResponse(responseJson);
                    applyExchangeRatesAndSaveInPreferences(responseRates);
                }
            }
        });
    }


    /**
     * Creates and sends a request that fetches fiat exchange rate data from "blockchain.info".
     * When executed this request saves the result in shared preferences and
     * updates the currentCurrency of the MonetaryUtil Singleton.
     */
    private void sendBlockchainInfoRequest() {

        Request rateRequest = new Request.Builder()
                .url("https://blockchain.info/ticker")
                .build();

        HttpClient.getInstance().getClient().newCall(rateRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                BBLog.w(LOG_TAG, "Fetching exchange rates from blockchain.info failed");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                BBLog.d(LOG_TAG, "Received exchange rates from blockchain.info");
                String responseData = response.body().string();
                JSONObject responseJson = null;
                try {
                    responseJson = new JSONObject(responseData);
                } catch (JSONException e) {
                    BBLog.w(LOG_TAG, "blockchain.info response could not be parsed as json");
                    e.printStackTrace();
                    if (responseData.toLowerCase().contains("cloudflare") && responseData.toLowerCase().contains("captcha-bypass")) {
                        broadcastExchangeRateUpdateFailed(ExchangeRateListener.ERROR_CLOUDFLARE_BLOCKED_TOR, RefConstants.ERROR_DURATION_VERY_LONG);
                    }
                }
                if (responseJson != null) {
                    JSONObject responseRates = parseBlockchainInfoResponse(responseJson);
                    applyExchangeRatesAndSaveInPreferences(responseRates);
                }
            }
        });
    }

    /**
     * Creates and sends a request that fetches fiat exchange rate data from Coinbase.
     * When executed this request saves the result in shared preferences and
     * updates the currentCurrency of the MonetaryUtil Singleton.
     */
    private void sendCoinbaseRequest() {

        Request rateRequest = new Request.Builder()
                .url("https://api.coinbase.com/v2/exchange-rates?currency=BTC")
                .build();

        HttpClient.getInstance().getClient().newCall(rateRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                BBLog.w(LOG_TAG, "Fetching exchange rates from coinbase failed");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                BBLog.d(LOG_TAG, "Received exchange rates from coinbase");
                String responseData = response.body().string();
                JSONObject responseJson = null;
                try {
                    responseJson = new JSONObject(responseData);
                } catch (JSONException e) {
                    BBLog.w(LOG_TAG, "Coinbase response could not be parsed as json");
                    e.printStackTrace();
                    if (responseData.toLowerCase().contains("cloudflare") && responseData.toLowerCase().contains("captcha-bypass") || responseData.toLowerCase().contains("challenge")) {
                        broadcastExchangeRateUpdateFailed(ExchangeRateListener.ERROR_CLOUDFLARE_BLOCKED_TOR, RefConstants.ERROR_DURATION_VERY_LONG);
                    }
                }
                if (responseJson != null) {
                    JSONObject responseRates = parseCoinbaseResponse(responseJson, false);
                    applyExchangeRatesAndSaveInPreferences(responseRates);
                }
            }
        });
    }

    /**
     * This function parses a mempool exchange rate response.
     * All the response parser functions return a similar formatted JSON Object
     * {"USD":{"rate":0.1231, "timestamp":...},"EUR":{...}}
     *
     * @param response a JSON response that comes from a mempool instance
     * @return
     */
    public JSONObject parseMempoolResponse(JSONObject response) {

        JSONObject formattedRates = new JSONObject();
        TreeMap<String, JSONObject> sortedRates = new TreeMap<>(); // TreeMap to sort the keys

        // loop through all returned currencies
        Iterator<String> iter = response.keys();
        while (iter.hasNext()) {
            String fiatCode = iter.next();
            if (fiatCode.equals("time"))
                continue;
            try {
                JSONObject currentCurrency = new JSONObject();
                currentCurrency.put(RATE, response.getInt(fiatCode) * BBCurrency.RATE_BTC);
                currentCurrency.put(TIMESTAMP, System.currentTimeMillis() / 1000);
                sortedRates.put(fiatCode, currentCurrency);
            } catch (JSONException e) {
                BBLog.e(LOG_TAG, "Unable to read exchange rate from mempool response.");
            }
        }

        // Convert the sorted TreeMap back to JSONObject if necessary
        for (Map.Entry<String, JSONObject> entry : sortedRates.entrySet()) {
            try {
                formattedRates.put(entry.getKey(), entry.getValue());
            } catch (JSONException ignored) {

            }
        }

        return formattedRates;
    }

    /**
     * This function parses a blockchain.info exchange rate response.
     * All the response parser functions return a similar formatted JSON Object
     * {"USD":{"rate":0.1231, "timestamp":...},"EUR":{...}}
     *
     * @param response a JSON response that comes from Blockchain.info
     * @return
     */
    public JSONObject parseBlockchainInfoResponse(JSONObject response) {

        JSONObject formattedRates = new JSONObject();
        // loop through all returned currencies
        Iterator<String> iter = response.keys();
        while (iter.hasNext()) {
            String fiatCode = iter.next();
            try {
                JSONObject ReceivedCurrency = response.getJSONObject(fiatCode);
                JSONObject currentCurrency = new JSONObject();
                currentCurrency.put(RATE, ReceivedCurrency.getDouble("15m") * BBCurrency.RATE_BTC);
                currentCurrency.put(SYMBOL, ReceivedCurrency.getString("symbol"));
                currentCurrency.put(TIMESTAMP, System.currentTimeMillis() / 1000);
                formattedRates.put(fiatCode, currentCurrency);
            } catch (JSONException e) {
                BBLog.e(LOG_TAG, "Unable to read exchange rate from blockchain.info response.");
            }
        }

        // Switch order as blockchain.info has USD first. We want to have it alphabetically.
        try {
            JSONObject tempCurrency = formattedRates.getJSONObject("USD");
            formattedRates.remove("USD");
            formattedRates.put("USD", tempCurrency);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return formattedRates;
    }

    /**
     * This function parses a coinbase exchange rate response.
     * All the response parser functions return a similar formatted JSON Object
     * {"USD":{"rate":0.1231, "timestamp":...},"EUR":{...}}
     *
     * @param response a JSON response that comes from Coinbases API
     * @return
     */
    public JSONObject parseCoinbaseResponse(JSONObject response, boolean isUnitTest) {

        JSONObject responseRates = null;
        try {
            responseRates = response.getJSONObject("data").getJSONObject("rates");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject formattedRates = new JSONObject();
        if (responseRates != null) {
            Iterator<String> iter = responseRates.keys();
            while (iter.hasNext()) {
                String rateCode = iter.next();

                try {
                    if (isUnitTest) {
                        // We cannot call android.icu stuff in unit tests. Anyway, that is just for filtering out shitcoins which we don't need to cover in the test. Therefore we just skip that part during tests.
                    } else {
                        Currency curr = Currency.getInstance(rateCode);
                        if (curr.getName(Locale.US, Currency.LONG_NAME, null).equals(rateCode)
                                && curr.getName(Locale.US, Currency.NARROW_SYMBOL_NAME, null).equals(rateCode))
                            continue;
                    }
                } catch (Exception e) {
                    continue;
                }

                try {
                    JSONObject currentCurrency = new JSONObject();
                    currentCurrency.put(RATE, responseRates.getDouble(rateCode) * BBCurrency.RATE_BTC);
                    currentCurrency.put(TIMESTAMP, System.currentTimeMillis() / 1000);
                    formattedRates.put(rateCode, currentCurrency);
                } catch (JSONException e) {
                    BBLog.e(LOG_TAG, "Unable to read exchange rate from coinbase response.");
                }
            }
        } else {
            return null;
        }

        return formattedRates;
    }

    private void applyExchangeRatesAndSaveInPreferences(JSONObject exchangeRates) {

        final SharedPreferences.Editor editor = PrefsUtil.editPrefs();
        editor.remove(PrefsUtil.AVAILABLE_FIAT_CURRENCIES);
        editor.commit();

        JSONArray availableCurrenciesArray = new JSONArray();


        // loop through all returned currencies and save them in the preferences.
        Iterator<String> iter = exchangeRates.keys();
        while (iter.hasNext()) {
            String rateCode = iter.next();
            try {
                JSONObject tempRate = exchangeRates.getJSONObject(rateCode);
                availableCurrenciesArray.put(rateCode);
                editor.putString("fiat_" + rateCode, tempRate.toString());

                // Update fiat currencies of the Monetary util
                MonetaryUtil.getInstance().updateCurrencyByCurrencyCode(rateCode);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // JSON Object that will hold all available currencies to later populate selection list in the settings.
        JSONObject availableCurrencies = new JSONObject();

        try {
            // Save the codes of all found currencies in a JSON object, which will then be stored on shared preferences
            availableCurrencies.put("currencies", availableCurrenciesArray);
            editor.putString(PrefsUtil.AVAILABLE_FIAT_CURRENCIES, availableCurrencies.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        editor.commit();
        setDefaultCurrency();
        broadcastExchangeRateUpdate();
    }

    private void setDefaultCurrency() {
        // If this was the first time executed since installation, automatically set the
        // currency to correct currency according to the systems locale. Only do this,
        // if this currency is included in the fetched data.
        // The user might also have changed the provider and his currency is no longer available. Also switch to default in this case.
        if (!PrefsUtil.getPrefs().getBoolean(PrefsUtil.IS_DEFAULT_CURRENCY_SET, false) || !isCurrencyAvailable(PrefsUtil.getSecondCurrencyCode())) {
            String currencyCode = SystemUtil.getSystemCurrencyCode();
            if (currencyCode != null) {
                if (!PrefsUtil.getPrefs().getString("fiat_" + currencyCode, "").isEmpty()) {
                    final SharedPreferences.Editor editor = PrefsUtil.editPrefs();
                    editor.putBoolean(PrefsUtil.IS_DEFAULT_CURRENCY_SET, true);
                    editor.putString(PrefsUtil.SECOND_CURRENCY, currencyCode);
                    editor.commit();
                    MonetaryUtil.getInstance().reloadAllCurrencies();
                }
            }
        }

        // Handle currencies that are set in settings but are no longer available by the current exchange rate provider
        if (!isCurrencyAvailable(PrefsUtil.getThirdCurrencyCode())
                || !isCurrencyAvailable(PrefsUtil.getForthCurrencyCode())
                || !isCurrencyAvailable(PrefsUtil.getFifthCurrencyCode())) {
            final SharedPreferences.Editor editor = PrefsUtil.editPrefs();
            if (!isCurrencyAvailable(PrefsUtil.getThirdCurrencyCode()))
                editor.putString(PrefsUtil.THIRD_CURRENCY, "none");
            if (!isCurrencyAvailable(PrefsUtil.getForthCurrencyCode()))
                editor.putString(PrefsUtil.FORTH_CURRENCY, "none");
            if (!isCurrencyAvailable(PrefsUtil.getFifthCurrencyCode()))
                editor.putString(PrefsUtil.FIFTH_CURRENCY, "none");
            editor.commit();
            MonetaryUtil.getInstance().reloadAllCurrencies();
        }
    }

    private boolean isCurrencyAvailable(String currency) {
        if (BBCurrency.isBitcoinCurrencyCode(currency))
            return true;
        try {
            JSONObject jsonAvailableCurrencies = new JSONObject(PrefsUtil.getPrefs().getString(PrefsUtil.AVAILABLE_FIAT_CURRENCIES, PrefsUtil.DEFAULT_FIAT_CURRENCIES));
            JSONArray currencies = jsonAvailableCurrencies.getJSONArray("currencies");

            for (int i = 0, count = currencies.length(); i < count; i++) {
                if (currencies.getString(i).equals(currency)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            BBLog.e(LOG_TAG, "Error reading JSON from Preferences: " + e.getMessage());
        }
        return false;
    }

    // Event handling to notify all registered listeners to an exchange rate change.

    private void broadcastExchangeRateUpdate() {
        for (ExchangeRateUtil.ExchangeRateListener listener : mExchangeRateListeners) {
            listener.onExchangeRatesUpdated();
        }
    }

    private void broadcastExchangeRateUpdateFailed(int error, int duration) {
        for (ExchangeRateUtil.ExchangeRateListener listener : mExchangeRateListeners) {
            listener.onExchangeRateUpdateFailed(error, duration);
        }
    }

    public void registerExchangeRateListener(ExchangeRateUtil.ExchangeRateListener listener) {
        mExchangeRateListeners.add(listener);
    }

    public void unregisterExchangeRateListener(ExchangeRateUtil.ExchangeRateListener listener) {
        mExchangeRateListeners.remove(listener);
    }

    public interface ExchangeRateListener {

        int ERROR_CLOUDFLARE_BLOCKED_TOR = 0;

        void onExchangeRatesUpdated();

        void onExchangeRateUpdateFailed(int error, int duration);
    }

}
