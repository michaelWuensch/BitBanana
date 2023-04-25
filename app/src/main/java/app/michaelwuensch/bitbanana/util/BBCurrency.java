package app.michaelwuensch.bitbanana.util;

import android.icu.util.Currency;

/**
 * This class is used to create currency objects,
 * which hold all information relevant for BitBanana about that currency.
 * Basically, it is a wrapper that allows us to unify both, fiat and bitcoin currencies.
 */
public class BBCurrency {

    /**
     * The currency Code. Used as display symbol if Symbol is empty.
     * Example: "USD", "EUR", "BTC", ...
     */
    private String mCode;

    /**
     * The exchange rate to Satoshis, as Satoshis are always our base unit.
     */
    private double mRate;

    /**
     * The symbol commonly used.
     * Example for USD: $
     */
    private String mSymbol;

    /**
     * Time of the exchange rate data (in seconds since 00:00:00 UTC on January 1, 1970)
     * This is used to protect the User from initiate an "invoice" with old exchange rate data.
     */
    private long mTimestamp;

    /**
     * States if this currency is a bitcoin unit (e.g. BTC) or another currency
     * with a changing exchange rate like fiat currencies or other cryptos.
     */
    private boolean mIsBitcoin;

    public BBCurrency(String code, double rate, long timestamp) {
        mIsBitcoin = false;
        mCode = code;
        mRate = rate;
        mTimestamp = timestamp;
    }

    public BBCurrency(String code, double rate, long timestamp, String symbol) {
        mIsBitcoin = false;
        mCode = code;
        mRate = rate;
        mTimestamp = timestamp;
        mSymbol = symbol;
    }

    public BBCurrency(String code, double rate) {
        mIsBitcoin = true;
        mCode = code;
        mRate = rate;
    }

    public BBCurrency(String code, double rate, String symbol) {
        mIsBitcoin = true;
        mCode = code;
        mRate = rate;
        mSymbol = symbol;
    }


    public String getCode() {
        return mCode;
    }

    public double getRate() {
        return mRate;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public String getSymbol() {
        // return iso4217 Symbol if available.
        if (!isBitcoin()) {
            try {
                String iso4217Symbol = Currency.getInstance(mCode).getSymbol();
                if (!iso4217Symbol.equals(mCode))
                    return iso4217Symbol;
            } catch (Exception ignored) {
            }
        }
        // in all other cases return whatever is saved in mSymbol
        return mSymbol;
    }

    public boolean isBitcoin() {
        return mIsBitcoin;
    }
}
