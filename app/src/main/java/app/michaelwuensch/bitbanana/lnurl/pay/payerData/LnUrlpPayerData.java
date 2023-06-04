package app.michaelwuensch.bitbanana.lnurl.pay.payerData;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Please refer to the following reference:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/18.md
 */
public class LnUrlpPayerData implements Serializable {

    private String name;
    private String pubkey;
    private String identifier;
    private String email;
    private LnUrlpAuthData auth;

    private LnUrlpPayerData(String name, String pubkey, String identifier, String email, LnUrlpAuthData auth) {
        this.name = name;
        this.pubkey = pubkey;
        this.identifier = identifier;
        this.email = email;
        this.auth = auth;
    }

    public String getUrlEncodedPayerData() {
        String payerDataJson = new Gson().toJson(this);
        return UrlEscapers.urlFormParameterEscaper().escape(payerDataJson);
    }

    public String getAsJsonString() {
        return new Gson().toJson(this);
    }

    public boolean isEmpty() {
        return new Gson().toJson(this).equals("{}");
    }

    public static class Builder {
        private String mName;
        private String mPubkey;
        private String mIdentifier;
        private String mEmail;
        private LnUrlpAuthData mAuthData;

        public LnUrlpPayerData.Builder setName(String name) {
            this.mName = name;
            return this;
        }

        public LnUrlpPayerData.Builder setPubkey(String pubkey) {
            this.mPubkey = pubkey;
            return this;
        }

        public LnUrlpPayerData.Builder setIdentifier(String identifier) {
            this.mIdentifier = identifier;
            return this;
        }

        public LnUrlpPayerData.Builder setEmail(String email) {
            this.mEmail = email;
            return this;
        }

        public LnUrlpPayerData.Builder setAuthData(LnUrlpAuthData authData) {
            this.mAuthData = authData;
            return this;
        }

        public LnUrlpPayerData build() {
            return new LnUrlpPayerData(mName, mPubkey, mIdentifier, mEmail, mAuthData);
        }
    }
}
