package app.michaelwuensch.bitbanana.util;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class BBCurrencyTest {

    // 1 sat = 0.02 MXN, so 0.01 MXN (the smallest fraction) is worth less than a satoshi.
    private static final double RATE_MXN_CHEAPER_THAN_SAT = 2e-5;

    private Locale mDefaultLocale;

    @Before
    public void setUp() {
        mDefaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void tearDown() {
        Locale.setDefault(mDefaultLocale);
    }

    private BBCurrency mxn() {
        return new BBCurrency("MXN", RATE_MXN_CHEAPER_THAN_SAT, 0);
    }

    @Test
    public void fiatTextInputIsTruncatedToSats() {
        // This is why the typed value must not be formatted from the converted msat amount.
        BBCurrency mxn = mxn();
        assertEquals(61000L, mxn.TextInputToValueInMsats("1.23"));
        assertEquals("1.22", mxn.formatValueAsTextInputString(mxn.TextInputToValueInMsats("1.23"), false, false, 2));
        assertEquals("0.00", mxn.formatValueAsTextInputString(mxn.TextInputToValueInMsats("0.01"), false, false, 2));
    }

    @Test
    public void formatTextInputKeepsFiatValueSmallerThanSat() {
        BBCurrency mxn = mxn();
        assertEquals("0.01", mxn.formatTextInputString("0.01", false, 2));
        assertEquals("0.05", mxn.formatTextInputString("0.05", false, 2));
        assertEquals("1.23", mxn.formatTextInputString("1.23", false, 2));
        assertEquals("1,234.57", mxn.formatTextInputString("1234.57", false, 2));
        // Lightning (msat precision) must behave the same for fiat
        assertEquals("1.23", mxn.formatTextInputString("1.23", true, 2));
    }

    @Test
    public void formatTextInputKeepsTypedFractionDigits() {
        BBCurrency mxn = mxn();
        assertEquals("1", mxn.formatTextInputString("1", false, -1));
        assertEquals("1", mxn.formatTextInputString("1.", false, -1));
        assertEquals("1.0", mxn.formatTextInputString("1.0", false, 1));
        assertEquals("1.20", mxn.formatTextInputString("1.20", false, 2));
        assertEquals("0.0", mxn.formatTextInputString("0.0", false, 1));
    }

    @Test
    public void formatTextInputAddsGroupingAndIsIdempotent() {
        BBCurrency mxn = mxn();
        assertEquals("1,234,567.8", mxn.formatTextInputString("1234567.8", false, 1));
        assertEquals("1,234,567.8", mxn.formatTextInputString("1,234,567.8", false, 1));
    }

    @Test
    public void formatTextInputWithGermanLocale() {
        Locale.setDefault(Locale.GERMANY);
        BBCurrency mxn = mxn();
        assertEquals("1.234.567,89", mxn.formatTextInputString("1234567,89", false, 2));
        assertEquals("1.234.567,89", mxn.formatTextInputString("1.234.567,89", false, 2));
        assertEquals("0,01", mxn.formatTextInputString("0,01", false, 2));
    }

    @Test
    public void formatTextInputNormalizesPersianAndArabicDigits() {
        BBCurrency mxn = mxn();
        assertEquals("123.45", mxn.formatTextInputString("۱۲۳.۴۵", false, 2));
        assertEquals("123.45", mxn.formatTextInputString("١٢٣٫٤٥", false, 2));
    }

    @Test
    public void formatTextInputEmptyOrSeparatorOnlyIsZero() {
        BBCurrency mxn = mxn();
        assertEquals("0", mxn.formatTextInputString("", false, -1));
        assertEquals("0", mxn.formatTextInputString(".", false, -1));
    }

    @Test
    public void formatTextInputMatchesMsatRoundTripForBitcoinUnits() {
        // For bitcoin units the round trip through msats is lossless, so both ways have to produce the same result.
        BBCurrency sat = new BBCurrency(BBCurrency.CURRENCY_CODE_SATOSHI);
        assertEquals("1,234", sat.formatTextInputString("1234", false, -1));
        assertEquals(sat.formatValueAsTextInputString(sat.TextInputToValueInMsats("1234"), false, false, -1), sat.formatTextInputString("1234", false, -1));
        assertEquals("1,234.567", sat.formatTextInputString("1234.567", true, 3));
        assertEquals(sat.formatValueAsTextInputString(sat.TextInputToValueInMsats("1234.567"), false, true, 3), sat.formatTextInputString("1234.567", true, 3));

        BBCurrency btc = new BBCurrency(BBCurrency.CURRENCY_CODE_BTC);
        assertEquals("0.00000001", btc.formatTextInputString("0.00000001", false, 8));
        assertEquals(btc.formatValueAsTextInputString(btc.TextInputToValueInMsats("0.00000001"), false, false, 8), btc.formatTextInputString("0.00000001", false, 8));
        assertEquals("1.23456789", btc.formatTextInputString("1.23456789", false, 8));
        assertEquals(btc.formatValueAsTextInputString(btc.TextInputToValueInMsats("1.23456789"), false, false, 8), btc.formatTextInputString("1.23456789", false, 8));
    }
}
