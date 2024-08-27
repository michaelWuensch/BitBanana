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
    public static final String URI_PREFIX_LND_HUB = "lndhub://";
    public static final String URI_PREFIX_CORE_LIGHTNING_GRPC = "cln-grpc://";
    public static final String URI_PREFIX_C_LIGHTNING_REST = "c-lightning-rest://";

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

    /**
     * Returns if it is a URI that BitBanana can uses to establish a node connection.
     */
    public static boolean isConnectUri(String data) {
        return isLNDConnectUri(data) || isCoreLightningGRPCUri(data);
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

    public static boolean isLNDHUBUri(String data) {
        return hasPrefix(URI_PREFIX_LND_HUB, data);
    }

    public static boolean isCoreLightningGRPCUri(String data) {
        return hasPrefix(URI_PREFIX_CORE_LIGHTNING_GRPC, data);
    }

    public static boolean isCLightningRestUri(String data) {
        return hasPrefix(URI_PREFIX_C_LIGHTNING_REST, data);
    }

    public static String removeURI(@NonNull String data) {
        if (isLightningUri(data)) {
            return data.substring(URI_PREFIX_LIGHTNING.length());
        } else if (isBitcoinUri(data)) {
            return data.substring(URI_PREFIX_BITCOIN.length());
        } else if (isLNDConnectUri(data)) {
            return data.substring(URI_PREFIX_LNDCONNECT.length());
        } else if (isLNURLCUri(data)) {
            return data.substring(URI_PREFIX_LNURLC.length());
        } else if (isLNURLPUri(data)) {
            return data.substring(URI_PREFIX_LNURLP.length());
        } else if (isLNURLWUri(data)) {
            return data.substring(URI_PREFIX_LNURLW.length());
        } else if (isLNURLAUri(data)) {
            return data.substring(URI_PREFIX_LNURLA.length());
        } else if (isLNDHUBUri(data)) {
            return data.substring(URI_PREFIX_LND_HUB.length());
        } else if (isCoreLightningGRPCUri(data)) {
            return data.substring(URI_PREFIX_CORE_LIGHTNING_GRPC.length());
        } else if (isCLightningRestUri(data)) {
            return data.substring(URI_PREFIX_C_LIGHTNING_REST.length());
        } else {
            return data;
        }
    }

    public static String appendParameter(String base, String name, String value) {
        if (!base.contains("?"))
            return base + "?" + name + "=" + value;
        else
            return base + "&" + name + "=" + value;
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
