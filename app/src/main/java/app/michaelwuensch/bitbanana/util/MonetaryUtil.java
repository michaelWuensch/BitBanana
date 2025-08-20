package app.michaelwuensch.bitbanana.util;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;


/**
 * This Singleton helps to display any value in the desired format.
 */
public class MonetaryUtil {

    private static final String LOG_TAG = MonetaryUtil.class.getSimpleName();

    private static MonetaryUtil mInstance;
    private static int mCurrencyIndex;
    private List<BBCurrency> mCurrencies;

    public static MonetaryUtil getInstance() {
        if (mInstance == null) {
            mInstance = new MonetaryUtil();
        }

        return mInstance;
    }

    private MonetaryUtil() {
        mCurrencies = new ArrayList<>();
        reloadAllCurrencies();
    }

    private int getCurrencyIndex() {
        return PrefsUtil.getPrefs().getInt(PrefsUtil.CURRENT_CURRENCY_INDEX, 0);
    }

    private int nextCurrencyIndex() {
        int currentIndex = getCurrencyIndex();
        if (currentIndex + 1 >= mCurrencies.size())
            return 0;
        return currentIndex + 1;
    }

    private BBCurrency getCurrentCurrency() {
        try {
            return mCurrencies.get(getCurrencyIndex());
        } catch (IndexOutOfBoundsException e) {
            setCurrentCurrencyIndex(0);
            return mCurrencies.get(0);
        }
    }

    private BBCurrency getNextCurrency() {
        return mCurrencies.get(nextCurrencyIndex());
    }

    private void setCurrentCurrencyIndex(int i) {
        PrefsUtil.editPrefs().putInt(PrefsUtil.CURRENT_CURRENCY_INDEX, 0).commit();
    }

    public void reloadAllCurrencies() {
        // Clear old
        mCurrencies = new ArrayList<>();

        addCurrency(PrefsUtil.getFirstCurrencyCode(), false, 0);
        addCurrency(PrefsUtil.getSecondCurrencyCode(), false, 0);
        addCurrency(PrefsUtil.getThirdCurrencyCode(), false, 0);
        addCurrency(PrefsUtil.getForthCurrencyCode(), false, 0);
        addCurrency(PrefsUtil.getFifthCurrencyCode(), false, 0);

        if (getCurrencyIndex() >= mCurrencies.size())
            setCurrentCurrencyIndex(0);
    }

    public void updateCurrencyByCurrencyCode(String code) {
        for (int i = 0; i < mCurrencies.size(); i++) {
            if (mCurrencies.get(i).getCode().equals(code)) {
                addCurrency(code, true, i);
                break;
            }
        }
    }

    public void updateCurrencyUIs() {
        // Calling switch currency twice will update all currency labels across the app
        // while keeping the same currency as current currency
        int temp = getCurrencyIndex();
        setCurrentCurrencyIndex(nextCurrencyIndex());
        setCurrentCurrencyIndex(temp);
    }

    /**
     * Creates a Currency object depending on the provided currencyCode (USD, EUR, ccBTC, ccMBTC, ...) and adds it to our list of available currency of the MonetaryUtil.
     * Fiat and Bitcoin currency codes are allowed here.
     * By loading it, we have access to it without parsing the JSON string over and over.
     *
     * @param currencyCode (USD, EUR, etc.)
     */
    private void addCurrency(String currencyCode, boolean update, int updatePos) {
        // Abort if it is no currency
        if (currencyCode.equals("none"))
            return;
        // Abort if we already have that currency
        for (BBCurrency currency : mCurrencies)
            if (currency.getCode().equals(currencyCode))
                return;

        switch (currencyCode) {
            case BBCurrency.CURRENCY_CODE_BTC:
            case BBCurrency.CURRENCY_CODE_MBTC:
            case BBCurrency.CURRENCY_CODE_BIT:
            case BBCurrency.CURRENCY_CODE_SATOSHI:
                mCurrencies.add(new BBCurrency(currencyCode));
                break;
            default:
                try {
                    JSONObject selectedCurrency = new JSONObject(PrefsUtil.getPrefs().getString("fiat_" + currencyCode, "{}"));
                    BBCurrency bbCurrency;
                    if (selectedCurrency.has("symbol")) {
                        bbCurrency = new BBCurrency(currencyCode,
                                selectedCurrency.getDouble("rate"),
                                selectedCurrency.getLong("timestamp"),
                                selectedCurrency.getString("symbol"));
                    } else {
                        bbCurrency = new BBCurrency(currencyCode,
                                selectedCurrency.getDouble("rate"),
                                selectedCurrency.getLong("timestamp"));
                    }

                    if (update)
                        mCurrencies.set(updatePos, bbCurrency);
                    else
                        mCurrencies.add(bbCurrency);
                } catch (JSONException e) {
                    // App was probably never started before. If we can't find the fiat in the prefs,
                    // create a placeholder currency.
                    if (!update)
                        mCurrencies.add(new BBCurrency("USD", 0, 0));
                }
        }
    }

    public boolean isCurrentCurrencyBitcoin() {
        return getCurrentCurrency().isBitcoin();
    }

    public boolean isCurrentCurrencyFiat() {
        return !getCurrentCurrency().isBitcoin();
    }

    public boolean hasMoreThanOneCurrency() {
        return mCurrencies.size() > 1;
    }

    /**
     * Returns if the user has selected to display at least one fiat currency in his currency settings.
     */
    public boolean displaysAtLeastOneFiatCurrency() {
        boolean fiat = false;
        for (BBCurrency currency : mCurrencies)
            if (!currency.isBitcoin()) {
                fiat = true;
                break;
            }
        return fiat;
    }

    /**
     * Get the amount and display unit of the current currency as properly formatted string.
     *
     * @param msats in milli Satoshis
     * @return formatted string
     */
    public CharSequence getCurrentCurrencyDisplayStringFromMSats(long msats, boolean msatPrecision) {
        return TextUtils.concat(getCurrentCurrencyDisplayAmountStringFromMSats(msats, msatPrecision), " ", getCurrentCurrencyDisplayUnit());
    }

    /**
     * Get the msat amount displayed as a properly formatted string in the current currency.
     *
     * @param msats milli Satoshis
     * @return formatted string
     */
    public CharSequence getCurrentCurrencyDisplayAmountStringFromMSats(long msats, boolean msatPrecision) {
        return getCurrentCurrency().formatValueAsDisplayString(msats, msatPrecision);
    }


    /**
     * Get the display unit of the current currency as properly formatted string.
     *
     * @return formatted string
     */
    public String getCurrentCurrencyDisplayUnit() {
        return getCurrentCurrency().getSymbol();
    }

    /**
     * Get the amount of the next currency as properly formatted string.
     *
     * @param msats
     * @return formatted string
     */
    public CharSequence getNextCurrencyDisplayAmountStringFromMSats(long msats, boolean msatPrecision) {
        return getNextCurrency().formatValueAsDisplayString(msats, msatPrecision);
    }


    /**
     * Get the display unit of the next currency as properly formatted string.
     *
     * @return formatted string
     */
    public String getNextCurrencyDisplayUnit() {
        return getNextCurrency().getSymbol();
    }


    /**
     * This function returns how old our fiat exchange rate data is.
     *
     * @return Age in seconds.
     */
    public long getCurrentCurrencyExchangeRateAge() {
        if (getCurrentCurrency().isBitcoin())
            return 0;
        return (System.currentTimeMillis() / 1000) - getCurrentCurrency().getTimestamp();
    }

    /**
     * Use the next currency as the current currency.
     */
    public void switchToNextCurrency() {
        PrefsUtil.editPrefs().putInt(PrefsUtil.CURRENT_CURRENCY_INDEX, nextCurrencyIndex()).apply();
    }


    /**
     * This function ensures that if we have an msat value that is not a multiple of 1000 it will display as msat,
     * while it will display in the chosen currency if it is a multiple of 1000
     */
    public CharSequence getDisplayStringFromMsats(long msats) {
        if (isCurrentCurrencyBitcoin()) {
            if (msats % 1000 == 0)
                return getCurrentCurrencyDisplayStringFromMSats(msats, true);
            else
                return msats + " msat";
        } else {
            return getCurrentCurrencyDisplayStringFromMSats(msats, true);
        }
    }

    /**
     * This function will return the currency name from the given ISO4217 code (3 Letters).
     *
     * @return The name of the currency. Returns null if the currency code was not found.
     */
    public String getCurrencyNameFromCurrencyCode(String currencyCode) {
        try {
            return android.icu.util.Currency.getInstance(currencyCode).getDisplayName(Locale.getDefault());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * This function will return the narrow symbol from the given ISO4217 code (3 Letters).
     *
     * @return The narrow symbol of the currency. Returns null if the currency code was not found.
     */
    public String getCurrencyNarrowSymbolFromCurrencyCode(String currencyCode) {
        try {
            return android.icu.util.Currency.getInstance(currencyCode).getName(Locale.getDefault(), android.icu.util.Currency.NARROW_SYMBOL_NAME, null);
        } catch (Exception e) {
            return null;
        }
    }

    public String msatsToCurrentCurrencyTextInputString(long msats, boolean allowMsat) {
        return getCurrentCurrency().formatValueAsTextInputString(msats, true, allowMsat);
    }

    /**
     * Converts the supplied value to msat. The exchange rate of the current currency is used.
     *
     * @param textInputValue
     * @return String without grouping or fractions
     */
    public long convertCurrentCurrencyTextInputToMsat(String textInputValue) {
        return getCurrentCurrency().TextInputToValueInMsats(textInputValue);
    }

    /**
     * Converts the given msat to a bitcoin string without grouping and maximum fractions of 8 digits.
     * This string is for example used in bitcoin on-chain invoices.
     */
    public String msatsToBitcoinString(long msats) {
        BBCurrency btcCurrency = new BBCurrency(BBCurrency.CURRENCY_CODE_BTC);
        return btcCurrency.formatValueAsTextInputString(msats, false, false);
    }

    /**
     * Checks if a numerical currency input is valid.
     * This function always checks against the rules of the primary currency.
     */
    public boolean validateCurrentCurrencyInput(String input, boolean allowMsat) {
        return getCurrentCurrency().validateInput(input, allowMsat);
    }

    /**
     * Localized fiat display.
     * This takes into account if currency symbol is before or after amount.
     * All in all it didn't look good if no currency symbol was available. Therefore
     * it is never used and we always show the symbol / iso4217 code after the amount.
     */
    private String localizedFiatAmount(long value) {
        double fiatValue = (getCurrentCurrency().getRate()) * value;
        NumberFormat nf = NumberFormat.getCurrencyInstance(SystemUtil.getSystemLocale());
        nf.setCurrency(Currency.getInstance(getCurrentCurrency().getCode()));
        return nf.format(fiatValue);
    }

    /**
     * Makes the last three digits 0 in the msat amount truncating it to full satoshis
     */
    public long mSatsTruncatedToSats(long mSats) {
        return (mSats / 1000L) * 1000L;
    }
}