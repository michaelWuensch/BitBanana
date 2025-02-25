package app.michaelwuensch.bitbanana.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.BuildConfig;
import app.michaelwuensch.bitbanana.models.BBLogItem;

/**
 * Use this class instead of the default log to prevent log messages in release builds.
 * As an additional "(BBLog)"-TAG is always included, it is easy to just show logs
 * created from this class by using "BBLog" as a filter.
 */
public class BBLog {
    private final static String additionalLogTag = "(BBLog) ";

    private static ArrayList<BBLogItem> inAppLogItems = new ArrayList<>();
    private static final Set<LogAddedListener> mLogAddedListeners = new HashSet<>();

    public static void v(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.v(additionalLogTag + tag, message);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.VERBOSE);
        }
    }

    public static void v(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.v(additionalLogTag + tag, message, tr);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.VERBOSE);
        }
    }

    public static void d(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.d(additionalLogTag + tag, message);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.DEBUG);
        }
    }

    public static void d(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.d(additionalLogTag + tag, message, tr);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.DEBUG);
        }
    }

    public static void i(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.i(additionalLogTag + tag, message);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.INFO);
        }
    }

    public static void i(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.i(additionalLogTag + tag, message, tr);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.INFO);
        }
    }

    public static void w(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.w(additionalLogTag + tag, message);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.WARNING);
        }
    }

    public static void w(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.w(additionalLogTag + tag, message, tr);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.WARNING);
        }
    }

    public static void e(final String tag, String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.e(additionalLogTag + tag, message);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.ERROR);
        }
    }

    public static void e(final String tag, String message, Throwable tr) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Log.e(additionalLogTag + tag, message, tr);
        }
        if (PrefsUtil.isLoggingEnabled()) {
            addLogItem(message, tag, BBLogItem.Verbosity.ERROR);
        }
    }

    public static void addLogItem(String message, String tag, BBLogItem.Verbosity verbosity) {
        BBLogItem item = BBLogItem.newBuilder()
                .setMessage(message)
                .setTag(tag)
                .setVerbosity(verbosity)
                .setTimestamp(System.nanoTime())
                .build();
        inAppLogItems.add(item);
        broadcastLogAdded(item);
    }

    public static void clearInAppLog() {
        inAppLogItems.clear();
    }

    public static ArrayList<BBLogItem> getInAppLogItems() {
        return inAppLogItems;
    }

    public static void registerLogAddedListener(LogAddedListener listener) {
        mLogAddedListeners.add(listener);
    }

    public static void unregisterLogAddedListener(LogAddedListener listener) {
        mLogAddedListeners.remove(listener);
    }

    private static void broadcastLogAdded(BBLogItem logItem) {
        for (LogAddedListener listener : mLogAddedListeners) {
            listener.onLogAdded(logItem);
        }
    }

    public interface LogAddedListener {
        void onLogAdded(BBLogItem logItem);
    }
}
