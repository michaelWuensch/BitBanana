package app.michaelwuensch.bitbanana.lnurl.pay.payerData;

import java.io.Serializable;

/**
 * Please refer to the following reference:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/18.md
 */
public class LnUrlpRequestedPayerData implements Serializable {

    private LnUrlpRequestedPayerDataEntry name;
    private LnUrlpRequestedPayerDataEntry pubkey;
    private LnUrlpRequestedPayerDataEntry identifier;
    private LnUrlpRequestedPayerDataEntry email;
    private LnUrlpRequestedPayerDataAuthEntry auth;

    public boolean isNameSupported() {
        return name != null;
    }

    public boolean isPubkeySupported() {
        return pubkey != null;
    }

    public boolean isIdentifierSupported() {
        return identifier != null;
    }

    public boolean isEmailSupported() {
        return email != null;
    }

    public boolean isAuthSupported() {
        return auth != null;
    }

    public boolean isNameMandatory() {
        if (isNameSupported()) {
            return name.isMandatory();
        }
        return false;
    }

    public boolean isPubkeyMandatory() {
        if (isPubkeySupported()) {
            return pubkey.isMandatory();
        }
        return false;
    }

    public boolean isIdentifierMandatory() {
        if (isIdentifierSupported()) {
            return identifier.isMandatory();
        }
        return false;
    }

    public boolean isEmailMandatory() {
        if (isEmailSupported()) {
            return email.isMandatory();
        }
        return false;
    }

    public boolean isAuthMandatory() {
        if (isAuthSupported()) {
            return auth.isMandatory();
        }
        return false;
    }

    public String getAuthK1() {
        if (isAuthSupported()) {
            return auth.getK1();
        }
        return null;
    }
}
