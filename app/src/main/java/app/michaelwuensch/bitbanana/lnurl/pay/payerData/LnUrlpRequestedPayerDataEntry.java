package app.michaelwuensch.bitbanana.lnurl.pay.payerData;

import java.io.Serializable;

/**
 * Please refer to the following reference:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/18.md
 */
public class LnUrlpRequestedPayerDataEntry implements Serializable {
    private boolean mandatory = false;

    public boolean isMandatory() {
        return mandatory;
    }
}
