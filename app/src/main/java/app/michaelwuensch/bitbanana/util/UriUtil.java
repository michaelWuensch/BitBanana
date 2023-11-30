package app.michaelwuensch.bitbanana.util;

import androidx.annotation.NonNull;

/**
 * This class is used to handle typical URIs in the bitcoin space.
 */
public class UriUtil {
    public static final String URI_PREFIX_LIGHTNING = "lightning:";
    public static final String URI_PREFIX_BITCOIN = "bitcoin:";
    public static final String URI_PREFIX_LNDCONNECT = "lndconnect://";
    public static final String URI_PREFIX_LNURLC = "lnurlc://";
    public static final String URI_PREFIX_LNURLP = "lnurlp://";
    public static final String URI_PREFIX_LNURLW = "lnurlw://";
    public static final String URI_PREFIX_LNURLA = "keyauth://";

    public static String generateLightningUri(@NonNull String data) {
        if (isLightningUri(data)) {
            return data;
        }

        return URI_PREFIX_LIGHTNING + data;
    }

    public static String generateBitcoinUri(@NonNull String data) {
        if (isBitcoinUri(data)) {
            return data;
        }

        return URI_PREFIX_BITCOIN + data;
    }

    public static boolean isLightningUri(String data) {
        return hasPrefix(URI_PREFIX_LIGHTNING, data);
    }

    public static boolean isBitcoinUri(String data) {
        return hasPrefix(URI_PREFIX_BITCOIN, data);
    }

    public static boolean isLNDConnectUri(String data) {
        return hasPrefix(URI_PREFIX_LNDCONNECT, data);
    }

    public static boolean isLNURLUri(String data) {
        return hasPrefix(URI_PREFIX_LNURLC, data) || hasPrefix(URI_PREFIX_LNURLP, data) || hasPrefix(URI_PREFIX_LNURLW, data) || hasPrefix(URI_PREFIX_LNURLA, data);
    }

    public static boolean isLNURLCUri(String data) {
        return hasPrefix(URI_PREFIX_LNURLC, data);
    }

    public static boolean isLNURLPUri(String data) {
        return hasPrefix(URI_PREFIX_LNURLP, data);
    }

    public static boolean isLNURLWUri(String data) {
        return hasPrefix(URI_PREFIX_LNURLW, data);
    }

    public static boolean isLNURLAUri(String data) {
        return hasPrefix(URI_PREFIX_LNURLA, data);
    }

    public static String removeURI(@NonNull String data) {
        if (isLightningUri(data)) {
            return data.substring(URI_PREFIX_LIGHTNING.length());
        } else if (isBitcoinUri(data)) {
            return data.substring(URI_PREFIX_BITCOIN.length());
        } else if (isLNDConnectUri(data)) {
            return data.substring(URI_PREFIX_LNDCONNECT.length());
        } else if (isLNURLCUri(data)) {
            return data.substring((URI_PREFIX_LNURLC).length());
        } else if (isLNURLPUri(data)) {
            return data.substring((URI_PREFIX_LNURLP).length());
        } else if (isLNURLWUri(data)) {
            return data.substring((URI_PREFIX_LNURLW).length());
        } else if (isLNURLAUri(data)) {
            return data.substring((URI_PREFIX_LNURLA).length());
        } else {
            return data;
        }
    }

    private static boolean hasPrefix(String prefix, String data) {
        if (data == null)
            return false;
        if (prefix == null)
            return false;
        if (data.isEmpty() || data.length() < prefix.length()) {
            return false;
        }

        return data.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }
}
