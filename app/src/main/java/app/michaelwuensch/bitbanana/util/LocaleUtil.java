package app.michaelwuensch.bitbanana.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.preference.PreferenceManager;

import java.util.Locale;


public class LocaleUtil {


    public static Context setLocale(Context ctx) {
        return updateResources(ctx, getLanguageCode(ctx), getLanguageCountry(ctx));
    }

    // Get selected language code
    private static String getLanguageCode(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (prefs.getString(PrefsUtil.LANGUAGE, PrefsUtil.LANGUAGE_SYSTEM_DEFAULT).equals(PrefsUtil.LANGUAGE_SYSTEM_DEFAULT)) {
            return Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();
        } else {
            return prefs.getString(PrefsUtil.LANGUAGE_CODE, PrefsUtil.LANGUAGE_SYSTEM_DEFAULT);
        }
    }

    // Get selected language country
    private static String getLanguageCountry(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (prefs.getString(PrefsUtil.LANGUAGE, PrefsUtil.LANGUAGE_SYSTEM_DEFAULT).equals(PrefsUtil.LANGUAGE_SYSTEM_DEFAULT)) {
            return Resources.getSystem().getConfiguration().getLocales().get(0).getCountry();
        } else {
            return prefs.getString(PrefsUtil.LANGUAGE_COUNTRY_CODE, "");
        }
    }

    // Create and return a new context, based on the current context updated with the desired locale
    private static Context updateResources(Context context, String languageCode, String languageCountry) {
        Locale locale;
        if (languageCountry.isEmpty()) {
            locale = new Locale(languageCode);
        } else {
            locale = new Locale(languageCode, languageCountry);
        }
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
        return context;
    }

}
