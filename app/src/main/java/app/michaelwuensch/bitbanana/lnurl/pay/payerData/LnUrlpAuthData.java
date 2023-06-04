package app.michaelwuensch.bitbanana.lnurl.pay.payerData;

import java.io.Serializable;

/**
 * Please refer to the following reference:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/18.md
 */
public class LnUrlpAuthData implements Serializable {
    private String key;
    private String k1;
    private String sig;

    public LnUrlpAuthData(String key, String k1, String sig) {
        this.key = key;
        this.k1 = k1;
        this.sig = sig;
    }
}
