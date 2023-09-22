package app.michaelwuensch.bitbanana.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;


/**
 * This Singleton helps to display any value in the desired format.
 */
public class MonetaryUtil {

    private static final String LOG_TAG = MonetaryUtil.class.getSimpleName();

    private static MonetaryUtil mInstance;
    private BBCurrency mFirstCurrency;
    private BBCurrency mSecondCurrency;

    public static MonetaryUtil getInstance() {
        if (mInstance == null) {
            mInstance = new MonetaryUtil();
        }

        return mInstance;
    }

    private MonetaryUtil() {

        loadFirstCurrency(PrefsUtil.getFirstCurrencyCode());

        String SecondCurrencyCode = PrefsUtil.getSecondCurrencyCode();
        switch (SecondCurrencyCode) {
            case BBCurrency.CURRENCY_CODE_BTC:
            case BBCurrency.CURRENCY_CODE_MBTC:
            case BBCurrency.CURRENCY_CODE_BIT:
            case BBCurrency.CURRENCY_CODE_SATOSHI:
                setSecondCurrency(SecondCurrencyCode);
                break;
            default:
                // Here we go if the user has selected a fiat currency as second currency.
                if (PrefsUtil.getPrefs().getString("fiat_" + PrefsUtil.getSecondCurrencyCode(), "").isEmpty()) {
                    mSecondCurrency = new BBCurrency(PrefsUtil.getSecondCurrencyCode(), 0, 0);
                } else {
                    loadSecondCurrencyFromPrefs(PrefsUtil.getSecondCurrencyCode());
                }
        }
    }

    /**
     * Creates a Currency object depending on the provided currencyCode (ccBTC, ccMBTC, ...) and sets it as the first currency of the MonetaryUtil.
     * This is only to be used with bitcoin currency codes. If an invalid value is supplied it will fallback to Satoshi
     *
     * @param currencyCode
     */
    public void loadFirstCurrency(String currencyCode) {
        if (currencyCode.equals(BBCurrency.CURRENCY_CODE_BTC) || currencyCode.equals(BBCurrency.CURRENCY_CODE_MBTC) || currencyCode.equals(BBCurrency.CURRENCY_CODE_BIT))
            setFirstCurrency(currencyCode);
        else
            setFirstCurrency(BBCurrency.CURRENCY_CODE_SATOSHI);
    }


    /**
     * Creates a Currency object depending on the provided currencyCode (USD, EUR, ccBTC, ccMBTC, ...) and sets it as the second currency of the MonetaryUtil.
     * Fiat and Bitcoin currency codes are allowed here.
     * By loading it, we have access to it without parsing the JSON string over and over.
     *
     * @param currencyCode (USD, EUR, etc.)
     */
    public void loadSecondCurrencyFromPrefs(String currencyCode) {
        switch (currencyCode) {
            case BBCurrency.CURRENCY_CODE_BTC:
            case BBCurrency.CURRENCY_CODE_MBTC:
            case BBCurrency.CURRENCY_CODE_BIT:
            case BBCurrency.CURRENCY_CODE_SATOSHI:
                setSecondCurrency(currencyCode);
                break;
            default:
                try {
                    JSONObject selectedCurrency = new JSONObject(PrefsUtil.getPrefs().getString("fiat_" + currencyCode, "{}"));
                    BBCurrency BBCurrency;
                    if (selectedCurrency.has("symbol")) {
                        BBCurrency = new BBCurrency(currencyCode,
                                selectedCurrency.getDouble("rate"),
                                selectedCurrency.getLong("timestamp"),
                                selectedCurrency.getString("symbol"));
                    } else {
                        BBCurrency = new BBCurrency(currencyCode,
                                selectedCurrency.getDouble("rate"),
                                selectedCurrency.getLong("timestamp"));
                    }

                    mSecondCurrency = BBCurrency;
                } catch (JSONException e) {
                    // App was probably never started before. If we can't find the fiat in the prefs,
                    // create a placeholder currency.
                    mSecondCurrency = new BBCurrency("USD", 0, 0);
                }
        }
    }

    public BBCurrency getFirstCurrency() {
        return mFirstCurrency;
    }

    public BBCurrency getSecondCurrency() {
        return mSecondCurrency;
    }

    /**
     * Get the amount and display unit of the primary currency as properly formatted string.
     *
     * @param sats in Satoshis
     * @return formatted string
     */
    public String getPrimaryDisplayStringFromSats(long sats) {
        return getPrimaryDisplayStringFromMSats(sats * 1000L);
    }

    /**
     * Get the amount and display unit of the primary currency as properly formatted string.
     *
     * @param msats in milli Satoshis
     * @return formatted string
     */
    public String getPrimaryDisplayStringFromMSats(long msats) {
        return getPrimaryDisplayAmountStringFromMSats(msats) + " " + getPrimaryDisplayUnit();
    }

    /**
     * Get the sat amount displayed as a properly formatted string in the primary currency.
     *
     * @param sats Satoshis
     * @return formatted string
     */
    public String getPrimaryDisplayAmountStringFromSats(long sats) {
        return getPrimaryDisplayAmountStringFromMSats(sats * 1000L);
    }

    /**
     * Get the msat amount displayed as a properly formatted string in the primary currency.
     *
     * @param msats milli Satoshis
     * @return formatted string
     */
    public String getPrimaryDisplayAmountStringFromMSats(long msats) {
        return getPrimaryCurrency().formatValueAsDisplayString(msats);
    }


    /**
     * Get the display unit of the primary currency as properly formatted string.
     *
     * @return formatted string
     */
    public String getPrimaryDisplayUnit() {
        return getPrimaryCurrency().getSymbol();
    }

    /**
     * Get the amount of the secondary currency as properly formatted string.
     *
     * @param sats in Satoshis
     * @return formatted string
     */
    public String getSecondaryDisplayAmountStringFromSats(long sats) {
        return getSecondaryCurrency().formatValueAsDisplayString(sats * 1000L);
    }


    /**
     * Get the display unit of the secondary currency as properly formatted string.
     *
     * @return formatted string
     */
    public String getSecondaryDisplayUnit() {
        return getSecondaryCurrency().getSymbol();
    }


    /**
     * This function returns how old our fiat exchange rate data is.
     *
     * @return Age in seconds.
     */
    public long getExchangeRateAge() {
        return (System.currentTimeMillis() / 1000) - mSecondCurrency.getTimestamp();
    }

    /**
     * Switch which of the currencies (first or second one) is used as primary currency.
     */
    public void switchCurrencies() {
        if (PrefsUtil.isFirstCurrencyPrimary()) {
            PrefsUtil.editPrefs().putBoolean(PrefsUtil.FIRST_CURRENCY_IS_PRIMARY, false).apply();
        } else {
            PrefsUtil.editPrefs().putBoolean(PrefsUtil.FIRST_CURRENCY_IS_PRIMARY, true).apply();
        }
    }

    /**
     * Get primary currency object
     *
     * @return
     */
    public BBCurrency getPrimaryCurrency() {
        if (PrefsUtil.isFirstCurrencyPrimary()) {
            return mFirstCurrency;
        } else {
            return mSecondCurrency;
        }
    }


    /**
     * Get secondary currency object
     *
     * @return
     */
    public BBCurrency getSecondaryCurrency() {
        if (PrefsUtil.isFirstCurrencyPrimary()) {
            return mSecondCurrency;
        } else {
            return mFirstCurrency;
        }
    }

    /**
     * This function ensures that if we have an msat value that is not a multiple of 1000 it will display as msat,
     * while it will display in the chosen currency if it is a multiple of 1000
     */
    public String getDisplayStringFromMsats(long msats) {
        if (getPrimaryCurrency().isBitcoin()) {
            if (msats % 1000 == 0)
                return getPrimaryDisplayStringFromSats(msats / 1000);
            else
                return msats + " msat";
        } else {
            long sats = msats / 1000;
            return getPrimaryDisplayStringFromSats(sats);
        }
    }

    /**
     * This function will return the currency name from the given ISO4217 code (3 Letters).
     *
     * @return The name of the currency. Returns null if the currency code was not found.
     */
    public String getCurrencyNameFromCurrencyCode(String currencyCode) {
        try {
            return android.icu.util.Currency.getInstance(currencyCode).getDisplayName(Locale.US);
        } catch (Exception e) {
            return null;
        }
    }

    public String satsToPrimaryTextInputString(long sats) {
        return getPrimaryCurrency().formatValueAsTextInputString(sats * 1000L, true);
    }

    /**
     * Converts the supplied value to satoshis. The exchange rate of the primary currency is used.
     *
     * @param primaryValue
     * @return String without grouping or fractions
     */
    public long convertPrimaryTextInputToSatoshi(String primaryValue) {
        long valueMSats = getPrimaryCurrency().TextInputToValueInMsats(primaryValue);
        return valueMSats / 1000L;
    }

    /**
     * Converts the given satoshis to bitcoin currency.
     *
     * @param satoshiValue
     * @return String without grouping and maximum fractions of 8 digits
     */
    public String convertSatoshiToBitcoin(String satoshiValue) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(8);

        if (satoshiValue == null || satoshiValue.equals("")) {
            return "0";
        } else {
            double value = Double.parseDouble(satoshiValue);
            double result = (value / 1e8);
            return df.format(result);
        }
    }

    /**
     * Converts the given satoshis to a bitcoin string without grouping and maximum fractions of 8 digits.
     * This string is for example used in bitcoin on-chain invoices.
     */
    public String satsToBitcoinString(long sats) {
        BBCurrency btcCurrency = new BBCurrency(BBCurrency.CURRENCY_CODE_BTC);
        return btcCurrency.formatValueAsTextInputString(sats * 1000L, false);
    }

    /**
     * Checks if a numerical currency input is valid.
     * This function always checks against the rules of the primary currency.
     */
    public boolean validateCurrencyInput(String input) {
        return getPrimaryCurrency().validateInput(input);
    }

    private void setFirstCurrency(String currencyCode) {
        mFirstCurrency = new BBCurrency(currencyCode);
    }

    private void setSecondCurrency(String currencyCode) {
        mSecondCurrency = new BBCurrency(currencyCode);
    }

    public void setSecondCurrency(String currencyCode, Double rate, Long timestamp) {
        mSecondCurrency = new BBCurrency(currencyCode, rate, timestamp);
    }

    void setSecondCurrency(String currencyCode, Double rate, Long timestamp, String symbol) {
        mSecondCurrency = new BBCurrency(currencyCode, rate, timestamp, symbol);
    }

    /**
     * Localized fiat display.
     * This takes into account if currency symbol is before or after amount.
     * All in all it didn't look good if no currency symbol was available. Therefore
     * it is never used and we always show the symbol / iso4217 code after the amount.
     */
    private String localizedFiatAmount(long value) {
        double fiatValue = (mSecondCurrency.getRate()) * value;
        NumberFormat nf = NumberFormat.getCurrencyInstance(SystemUtil.getSystemLocale());
        nf.setCurrency(Currency.getInstance(mSecondCurrency.getCode()));
        return nf.format(fiatValue);
    }
}