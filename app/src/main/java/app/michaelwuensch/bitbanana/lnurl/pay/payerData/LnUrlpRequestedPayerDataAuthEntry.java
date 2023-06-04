package app.michaelwuensch.bitbanana.lnurl.pay.payerData;

import java.io.Serializable;

/**
 * Please refer to the following reference:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/18.md
 */
public class LnUrlpRequestedPayerDataAuthEntry extends LnUrlpRequestedPayerDataEntry implements Serializable {

    private String k1;

    public String getK1() {
        return k1;
    }
}
