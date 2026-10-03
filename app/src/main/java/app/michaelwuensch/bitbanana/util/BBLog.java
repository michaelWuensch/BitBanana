package app.michaelwuensch.bitbanana.util;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern URI_SCHEME_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*):(.*)$", Pattern.DOTALL);
    // Schemes whose opaque part is payment data (invoice, address, ...). For these the beginning is shown to be able to identify the type of data.
    private static final Set<String> PAYMENT_SCHEMES = new HashSet<>(Arrays.asList("lightning", "bitcoin"));
    // Query parameters that never contain secrets and are helpful for debugging.
    private static final Set<String> NON_SENSITIVE_QUERY_PARAMS = new HashSet<>(Arrays.asList("relay", "tag", "action", "amount", "label", "message"));
    private static final int VISIBLE_PREFIX_LENGTH = 6;

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

    /**
     * Use this instead of Throwable.printStackTrace().
     * The message of an exception might contain sensitive data (e.g. an URL with a token), therefore the stack trace is only printed in debug builds.
     * If logging is enabled, the exception is also added to the in app log. In release builds its message is redacted.
     */
    public static void printStackTrace(@Nullable Throwable throwable) {
        if (throwable == null)
            return;
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            throwable.printStackTrace();
        }
        if (PrefsUtil.isLoggingEnabled()) {
            String message = BuildConfig.BUILD_TYPE.equals("debug") ? throwable.toString() : redactException(throwable);
            String tag = "BBLog";
            // Use the location where the exception was caught, as the location where it was thrown is often inside a library.
            StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            if (stackTrace.length > 1) {
                StackTraceElement caller = stackTrace[1];
                String className = caller.getClassName();
                tag = className.substring(className.lastIndexOf('.') + 1);
                if (tag.contains("$"))
                    tag = tag.substring(0, tag.indexOf('$'));
                message = message + " (" + caller.getFileName() + ":" + caller.getLineNumber() + ")";
            }
            addLogItem(message, tag, BBLogItem.Verbosity.ERROR);
        }
    }

    /**
     * Use this for data that might contain secrets, like scanned QR codes, URIs or connection strings.
     * In debug builds the data is returned unchanged. In release builds everything that could be a secret is replaced,
     * while the structure (scheme, host, port, names of query parameters, lengths) stays visible for debugging.
     * <p>
     * Example: lndconnect://mynode.example:10009?cert=&lt;redacted, 1180 chars&gt;&amp;macaroon=&lt;redacted, 516 chars&gt;
     */
    public static String redactSensitive(@Nullable String data) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return String.valueOf(data);
        }
        return redact(data);
    }

    /**
     * Use this for exceptions that occurred while processing data that might contain secrets.
     * Their messages often include the processed input (e.g. URISyntaxException contains the complete URI).
     * In debug builds the message is returned unchanged. In release builds only information that cannot contain the input is returned.
     */
    public static String redactSensitiveException(@Nullable Throwable throwable) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            return throwable == null ? "null" : String.valueOf(throwable.getMessage());
        }
        return redactException(throwable);
    }

    @VisibleForTesting
    static String redactException(@Nullable Throwable throwable) {
        if (throwable == null)
            return "null";
        if (throwable instanceof URISyntaxException) {
            URISyntaxException uriSyntaxException = (URISyntaxException) throwable;
            return "URISyntaxException: " + uriSyntaxException.getReason() + " at index " + uriSyntaxException.getIndex() + " (message redacted)";
        }
        return throwable.getClass().getSimpleName() + " (message redacted)";
    }

    @VisibleForTesting
    static String redact(@Nullable String data) {
        if (data == null)
            return "null";

        Matcher schemeMatcher = URI_SCHEME_PATTERN.matcher(data);
        if (!schemeMatcher.matches()) {
            // Not a URI, e.g. a raw invoice or a hex encoded macaroon.
            return redactWithPrefix(data);
        }

        String scheme = schemeMatcher.group(1);
        String rest = schemeMatcher.group(2);

        if (!rest.startsWith("//")) {
            // Opaque URI, e.g. lightning:lnbc... or bitcoin:bc1...?amount=...
            if (PAYMENT_SCHEMES.contains(scheme.toLowerCase()))
                return scheme + ":" + redactWithPrefix(rest);
            return scheme + ":" + placeholder(rest.length());
        }
        rest = rest.substring(2);

        String fragment = null;
        int fragmentIndex = rest.indexOf('#');
        if (fragmentIndex >= 0) {
            fragment = rest.substring(fragmentIndex + 1);
            rest = rest.substring(0, fragmentIndex);
        }
        String query = null;
        int queryIndex = rest.indexOf('?');
        if (queryIndex >= 0) {
            query = rest.substring(queryIndex + 1);
            rest = rest.substring(0, queryIndex);
        }

        StringBuilder redacted = new StringBuilder(scheme).append("://");

        // User info (e.g. user:password@host)
        int atIndex = rest.indexOf('@');
        int slashIndex = rest.indexOf('/');
        if (atIndex >= 0 && (slashIndex < 0 || atIndex < slashIndex)) {
            redacted.append(placeholder(atIndex)).append('@');
            rest = rest.substring(atIndex + 1);
            if (URI_SCHEME_PATTERN.matcher(rest).matches() && rest.contains("://")) {
                // Nested URI as used by lndhub (lndhub://user:password@https://host)
                StringBuilder nested = new StringBuilder(rest);
                if (query != null)
                    nested.append('?').append(query);
                if (fragment != null)
                    nested.append('#').append(fragment);
                return redacted.append(redact(nested.toString())).toString();
            }
            slashIndex = rest.indexOf('/');
        }

        // Host and port stay visible, the path might contain secrets (e.g. LNURL-withdraw links)
        if (slashIndex >= 0) {
            redacted.append(rest, 0, slashIndex).append('/');
            String path = rest.substring(slashIndex + 1);
            if (!path.isEmpty())
                redacted.append(placeholder(path.length()));
        } else {
            redacted.append(rest);
        }

        if (query != null) {
            redacted.append('?');
            String[] params = query.split("&", -1);
            for (int i = 0; i < params.length; i++) {
                if (i > 0)
                    redacted.append('&');
                int equalsIndex = params[i].indexOf('=');
                if (equalsIndex < 0) {
                    redacted.append(placeholder(params[i].length()));
                    continue;
                }
                String key = params[i].substring(0, equalsIndex);
                String value = params[i].substring(equalsIndex + 1);
                redacted.append(key).append('=');
                if (NON_SENSITIVE_QUERY_PARAMS.contains(key.toLowerCase()))
                    redacted.append(value);
                else
                    redacted.append(placeholder(value.length()));
            }
        }

        if (fragment != null)
            redacted.append('#').append(placeholder(fragment.length()));

        return redacted.toString();
    }

    private static String redactWithPrefix(String data) {
        if (data.length() <= VISIBLE_PREFIX_LENGTH)
            return placeholder(data.length());
        return data.substring(0, VISIBLE_PREFIX_LENGTH) + "..." + placeholder(data.length() - VISIBLE_PREFIX_LENGTH);
    }

    private static String placeholder(int length) {
        if (length == 0)
            return "";
        return "<redacted, " + length + " chars>";
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
