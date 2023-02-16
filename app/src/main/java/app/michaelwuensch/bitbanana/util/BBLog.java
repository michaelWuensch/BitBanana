package app.michaelwuensch.bitbanana.util;

import android.util.Log;

import app.michaelwuensch.bitbanana.BuildConfig;

/**
 * Use this class instead of the default log to prevent log messages in release builds.
 * As an additional "(BBLog)"-TAG is always included, it is easy to just show logs
 * created from this class by using "BBLog" as a filter.
 */
public class BBLog {
    private final static String additionalLogTag = "(BBLog) ";

    public static void v(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.v(additionalLogTag + tag, message);
        }
    }

    public static void v(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.v(additionalLogTag + tag, message, tr);
        }
    }

    public static void d(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.d(additionalLogTag + tag, message);
        }
    }

    public static void d(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.d(additionalLogTag + tag, message, tr);
        }
    }

    public static void i(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.i(additionalLogTag + tag, message);
        }
    }

    public static void i(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.i(additionalLogTag + tag, message, tr);
        }
    }

    public static void w(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.w(additionalLogTag + tag, message);
        }
    }

    public static void w(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.w(additionalLogTag + tag, message, tr);
        }
    }

    public static void e(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.e(additionalLogTag + tag, message);
        }
    }

    public static void e(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.e(additionalLogTag + tag, message, tr);
        }
    }
}
