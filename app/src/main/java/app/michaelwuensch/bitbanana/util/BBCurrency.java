package app.michaelwuensch.bitbanana.util;

import android.icu.util.Currency;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to create currency objects,
 * which hold all information relevant for BitBanana about that currency.
 * Basically, it is a wrapper that allows us to unify both, fiat and bitcoin currencies.
 */
public class BBCurrency {

    // BTC unit constants
    public static final String CURRENCY_CODE_BTC = "ccBTC";
    public static final String SYMBOL_BTC = "BTC";
    public static final double RATE_BTC = 1e-11;
    public static final int MAX_INTEGER_DIGITS_BTC = 8;
    public static final int MAX_FRACTION_DIGITS_BTC = 8;


    // mBTC unit constants
    public static final String CURRENCY_CODE_MBTC = "ccMBTC";
    public static final String SYMBOL_MBTC = "mBTC";
    public static final double RATE_MBTC = 1e-8;
    public static final int MAX_INTEGER_DIGITS_MBTC = 11;
    public static final int MAX_FRACTION_DIGITS_MBTC = 5;

    // bit unit constants
    public static final String CURRENCY_CODE_BIT = "ccBIT";
    public static final String SYMBOL_BIT = "bit";
    public static final double RATE_BIT = 1e-5;
    public static final int MAX_INTEGER_DIGITS_BIT = 14;
    public static final int MAX_FRACTION_DIGITS_BIT = 2;

    // sat unit constants
    public static final String CURRENCY_CODE_SATOSHI = "ccSAT";
    public static final String SYMBOL_SATOSHI = "sat";
    public static final double RATE_SATOSHI = 1e-3;
    public static final int MAX_INTEGER_DIGITS_SATOSHI = 16;
    public static final int MAX_FRACTION_DIGITS_SATOSHI = 0;

    /**
     * The currency Code. Used as display symbol if Symbol is empty.
     * Example: "USD", "EUR", "ccBTC", ...
     */
    private String mCode;

    /**
     * The exchange rate to milli Satoshis, as milli Satoshis are always our base unit.
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

    /**
     * Just supplying a code is only supported for bitcoin units.
     * Creating a currency with this constructor will initialize it as a bitcoin unit.
     *
     * @param code
     */
    public BBCurrency(String code) {
        mIsBitcoin = true;
        mCode = code;
    }

    public String getCode() {
        return mCode;
    }

    public double getRate() {
        if (mIsBitcoin) {
            switch (getCode()) {
                case CURRENCY_CODE_BTC:
                    return RATE_BTC;
                case CURRENCY_CODE_MBTC:
                    return RATE_MBTC;
                case CURRENCY_CODE_BIT:
                    return RATE_BIT;
                default:
                    return RATE_SATOSHI;
            }
        }
        return mRate;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public String getSymbol() {
        if (mIsBitcoin) {
            switch (getCode()) {
                case CURRENCY_CODE_BTC:
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        return "\u20BF";
                    else
                        return SYMBOL_BTC;
                case CURRENCY_CODE_MBTC:
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        return "m\u20BF";
                    else
                        return SYMBOL_MBTC;
                case CURRENCY_CODE_BIT:
                    return SYMBOL_BIT;
                default:
                    return SYMBOL_SATOSHI;

            }
        } else {
            // return iso4217 Symbol if available.
            try {
                String iso4217Symbol = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    iso4217Symbol = Currency.getInstance(mCode).getName(Locale.getDefault(), Currency.NARROW_SYMBOL_NAME, null);
                } else {
                    iso4217Symbol = Currency.getInstance(mCode).getSymbol(Locale.getDefault());
                }
                if (!iso4217Symbol.equals(mCode))
                    return iso4217Symbol;
            } catch (Exception ignored) {
            }
        }
        // in all other cases return whatever is saved in mSymbol
        if (mSymbol != null)
            return mSymbol;
        else
            return getCode();
    }

    public int getMaxIntegerDigits() {
        if (mIsBitcoin) {
            switch (getCode()) {
                case CURRENCY_CODE_BTC:
                    return MAX_INTEGER_DIGITS_BTC;
                case CURRENCY_CODE_MBTC:
                    return MAX_INTEGER_DIGITS_MBTC;
                case CURRENCY_CODE_BIT:
                    return MAX_INTEGER_DIGITS_BIT;
                default:
                    return MAX_INTEGER_DIGITS_SATOSHI;
            }
        } else {
            // Fiat
            return 22;
        }
    }

    public int getMaxFractionsDigits() {
        if (mIsBitcoin) {
            switch (getCode()) {
                case CURRENCY_CODE_BTC:
                    return MAX_FRACTION_DIGITS_BTC;
                case CURRENCY_CODE_MBTC:
                    return MAX_FRACTION_DIGITS_MBTC;
                case CURRENCY_CODE_BIT:
                    return MAX_FRACTION_DIGITS_BIT;
                default:
                    return MAX_FRACTION_DIGITS_SATOSHI;
            }
        } else {
            // Fiat
            // return iso4217 Symbol if available.
            try {
                Currency curr = Currency.getInstance(getCode());
                if (curr != null) {
                    return curr.getDefaultFractionDigits();
                }
            } catch (Exception ignored) {

            }
            // in all other cases return 2
            return 2;
        }
    }

    public CharSequence formatValueAsDisplayString(long msats, boolean MsatPrecision) {
        NumberFormat nf = NumberFormat.getNumberInstance(SystemUtil.getSystemLocale());
        DecimalFormat df = (DecimalFormat) nf;
        df.setMinimumIntegerDigits(1);
        df.setMaximumIntegerDigits(getMaxIntegerDigits());

        if (mIsBitcoin) {
            int maxFractionDigits = 0;
            if (MsatPrecision && getCode().equals(CURRENCY_CODE_SATOSHI))
                maxFractionDigits = getMaxFractionsDigits() + 3;
            else
                maxFractionDigits = getMaxFractionsDigits();

            df.setMaximumFractionDigits(maxFractionDigits);
            String result = df.format(msats * getRate());

            // If we have a fraction, then always show all fraction digits for bits and sats
            if (result.contains(String.valueOf(df.getDecimalFormatSymbols().getDecimalSeparator()))) {
                if (getCode().equals(CURRENCY_CODE_BIT) || getCode().equals(CURRENCY_CODE_SATOSHI)) {
                    df.setMinimumFractionDigits(maxFractionDigits);
                }
            }
        } else {
            df.setMinimumFractionDigits(2);
            df.setMaximumFractionDigits(getMaxFractionsDigits());
        }

        double value = msats * getRate();

        // Format with attributes so we can span only the fraction
        AttributedCharacterIterator it = df.formatToCharacterIterator(value);

        // Reconstruct text
        StringBuilder txt = new StringBuilder();
        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            txt.append(c);
        }

        SpannableStringBuilder sp = new SpannableStringBuilder(txt);
        if (getCode().equals(CURRENCY_CODE_SATOSHI)) {

            // Apply spans: shrink fraction (and optionally the separator)
            for (char c = it.first(); c != CharacterIterator.DONE; ) {
                int start = it.getIndex();
                int end = it.getRunLimit();
                Map<AttributedCharacterIterator.Attribute, Object> attrs = it.getAttributes();

                if (attrs.containsKey(NumberFormat.Field.FRACTION)) {
                    sp.setSpan(new RelativeSizeSpan(0.8f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    //sp.setSpan(new AlphaSpan(0.8f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                // Optional: also scale the decimal separator
                if (attrs.containsKey(NumberFormat.Field.DECIMAL_SEPARATOR)) {
                    // sp.setSpan(new RelativeSizeSpan(0.8f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                it.setIndex(end);
                c = it.current();
            }
        }

        return sp;
    }

    public String formatValueAsTextInputString(long msats, boolean returnEmptyForZero, boolean MsatPrecision) {
        if (msats == 0)
            if (returnEmptyForZero)
                return "";
            else
                return "0";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        DecimalFormat df = (DecimalFormat) nf;
        df.setGroupingUsed(true);

        if (MsatPrecision && getCode().equals(CURRENCY_CODE_SATOSHI))
            df.setMaximumFractionDigits(getMaxFractionsDigits() + 3);
        else
            df.setMaximumFractionDigits(getMaxFractionsDigits());

        return df.format(msats * getRate());
    }

    public long TextInputToValueInMsats(String textInput) {
        String textInputMSatString = TextInputToValueInMsatsString(textInput);
        try {
            if (getCode().equals(CURRENCY_CODE_SATOSHI))
                return Long.parseLong(textInputMSatString);
            else
                return MonetaryUtil.getInstance().mSatsTruncatedToSats((Long.parseLong(textInputMSatString))); // We want it to be truncated to full satoshis when entering feat amounts.
        } catch (NumberFormatException e) {
            // This ensures it returns 0 instead of crashing for huge numbers.
            return 0L;
        }
    }

    private String TextInputToValueInMsatsString(String textInput) {
        String textInputMSatString;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        DecimalFormat df = (DecimalFormat) nf;
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(0);
        textInput = stripGrouping(textInput);
        textInput = textInput.replace(",", "."); // This is needed as parseDouble needs a "." as decimal separator.
        if (textInput == null || textInput.equals("") || textInput.equals(".")) {
            textInputMSatString = "0";
        } else {
            double value = Double.parseDouble(textInput);
            double result = (value / getRate());
            textInputMSatString = df.format(result);
        }
        return textInputMSatString;
    }

    public boolean validateInput(String input, boolean MsatPrecision) {

        input = stripGrouping(input);

        int numberOfDecimals = getMaxFractionsDigits();

        // Override decimals for sats
        if (getCode().equals(CURRENCY_CODE_SATOSHI))
            numberOfDecimals = 0;

        if (MsatPrecision && getCode().equals(CURRENCY_CODE_SATOSHI))
            numberOfDecimals = numberOfDecimals + 3;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        String decimalSeparator = String.valueOf(symbols.getDecimalSeparator());
        if (input.equals(decimalSeparator) && numberOfDecimals != 0) {
            return true;
        }

        if (numberOfDecimals == 0 && input.contains(decimalSeparator))
            return false;

        // Regex selecting any or no number of digits optionally followed by "." or "," that is followed by up to numberOfDecimals digits
        String regexPattern = "[0-9]*([" + decimalSeparator + "]{0,1}[0-9]{0," + numberOfDecimals + "})";

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(input);

        boolean matchedPattern = matcher.matches();

        // Check if the input is too large. We limit the input to the value equivalent to 1000 BTC.
        // This will prevent any overflow errors when calculating in mSats.
        boolean notTooBig;
        try {
            notTooBig = (Long.parseLong(TextInputToValueInMsatsString(input)) <= 1e14);
        } catch (NumberFormatException e) {
            return false;
        }

        boolean validZeros;
        if (input.startsWith("0")) {
            if (input.length() > 1) {
                if (input.startsWith("0" + decimalSeparator)) {
                    validZeros = true;
                } else {
                    validZeros = false;
                }
            } else {
                validZeros = true;
            }
        } else {
            validZeros = true;
        }
        return matchedPattern && notTooBig && validZeros;
    }

    public boolean isBitcoin() {
        return mIsBitcoin;
    }

    public static boolean isBitcoinCurrencyCode(String currencyCode) {
        return (currencyCode.equals(CURRENCY_CODE_BTC) || currencyCode.equals(CURRENCY_CODE_MBTC) || currencyCode.equals(CURRENCY_CODE_BIT) || currencyCode.equals(CURRENCY_CODE_SATOSHI));
    }

    public static String stripGrouping(String input) {
        if (input == null) return null;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        char grouping = symbols.getGroupingSeparator();

        // Remove the locale's grouping separator when it's between digits.
        String pattern;
        if (isSpaceLike(grouping)) {
            // In space-grouping locales, remove any of these anywhere.
            pattern = "[\\u0020\\u00A0\\u202F\\u2009\\u2007\\u2060]+";
        } else {
            // Quote the literal grouping char and remove all occurrences.
            pattern = Pattern.quote(String.valueOf(grouping)) + "+";
        }

        // Only remove grouping characters; keep decimal separator and sign.
        String withoutGroups = input.replaceAll(pattern, "");

        // Optional: trim outer whitespace; inner spaces between digits were handled above.
        return withoutGroups.trim();
    }

    private static boolean isSpaceLike(char c) {
        return c == '\u0020'   // space
                || c == '\u00A0'   // no-break space
                || c == '\u202F'   // narrow no-break space
                || c == '\u2009'   // thin space
                || c == '\u2007'   // figure space
                || c == '\u2060';  // word joiner
    }
}
