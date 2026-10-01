package app.michaelwuensch.bitbanana.lnurl.pay;

import androidx.annotation.Nullable;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.lnurl.LnUrlReader;
import okhttp3.HttpUrl;

/**
 * Please refer to the following references:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/09.md
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/10.md
 */
public class LnUrlPaySuccessAction implements Serializable {

    public static final String TAG_URL = "url";
    public static final String TAG_MESSAGE = "message";
    public static final String TAG_AES = "aes";

    // LUD-09: description and message are limited to 144 characters.
    // We enforce it, so a service cannot fill the dialog with long, misleading texts.
    static final int MAX_TEXT_LENGTH = 144;

    private String tag;
    private String ciphertext;
    private String iv;
    private String message;
    private String description;
    private String url;

    public String getTag() {
        return tag;
    }


    public boolean isMessage() {
        return tag != null && tag.equals(LnUrlPaySuccessAction.TAG_MESSAGE);
    }

    public boolean isUrl() {
        return tag != null && tag.equals(LnUrlPaySuccessAction.TAG_URL);
    }

    public boolean isAes() {
        return tag != null && tag.equals(LnUrlPaySuccessAction.TAG_AES);
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getIv() {
        return iv;
    }

    public String getMessage() {
        return limitLength(message);
    }

    public String getDescription() {
        return limitLength(description);
    }

    public String getUrl() {
        return url;
    }

    /**
     * LUD-09: The url has to be opened in a browser and its domain has to be the same as the domain of the callback.
     * We only allow https (or http for onion services) and accept subdomains of the same site (e.g. pay.shop.example and shop.example).
     * Without this check, a service could make the wallet open any app with any data (e.g. a lightning: or bitcoin: URI for a second payment)
     * or send the user to an unrelated website right after the payment.
     *
     * @param callback the callback URL of the pay request the success action belongs to
     */
    public boolean isUrlAllowed(@Nullable String callback) {
        if (!LnUrlReader.isSecureLnUrlUrl(url) || !LnUrlReader.isSecureLnUrlUrl(callback))
            return false;
        return getSite(HttpUrl.get(url)).equals(getSite(HttpUrl.get(callback)));
    }

    /**
     * Returns the registrable domain (e.g. shop.example for pay.shop.example), based on the public suffix list.
     * Falls back to the host if there is none (e.g. for IP addresses).
     */
    private static String getSite(HttpUrl url) {
        String site = url.topPrivateDomain();
        return site != null ? site : url.host();
    }

    private static String limitLength(@Nullable String text) {
        if (text == null)
            return "";
        if (text.length() <= MAX_TEXT_LENGTH)
            return text;
        return text.substring(0, MAX_TEXT_LENGTH - 1) + "…";
    }
}
