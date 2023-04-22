package app.michaelwuensch.bitbanana.util;

import android.content.res.Resources;
import android.icu.util.Currency;

import java.util.Locale;

public class SystemUtil {
    /**
     * The locale of the Android system this app runs on.
     * This will be unaffected by whatever is chosen in BitBananas language settings.
     *
     * @return
     */
    public static Locale getSystemLocale() {
        return Resources.getSystem().getConfiguration().getLocales().get(0);
    }

    /**
     * This function will return a currency code (ISO 4217, 3 Letters) that corresponds to the locale of the
     * system.
     *
     * @return
     */
    public static String getSystemCurrencyCode() {
        return Currency.getInstance(getSystemLocale()).getCurrencyCode();
    }
}
