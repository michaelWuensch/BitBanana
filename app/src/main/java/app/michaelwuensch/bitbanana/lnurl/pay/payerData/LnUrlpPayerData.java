package app.michaelwuensch.bitbanana.lnurl.pay.payerData;

import androidx.annotation.NonNull;

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

    public static class Builder {
        private String mName;
        private String mPubkey;
        private String mIdentifier;
        private String mEmail;
        private LnUrlpAuthData mAuthData;

        public LnUrlpPayerData.Builder setName(@NonNull String name) {
            this.mName = name;
            return this;
        }

        public LnUrlpPayerData.Builder setPubkey(@NonNull String pubkey) {
            this.mPubkey = pubkey;
            return this;
        }

        public LnUrlpPayerData.Builder setIdentifier(@NonNull String identifier) {
            this.mIdentifier = identifier;
            return this;
        }

        public LnUrlpPayerData.Builder setEmail(@NonNull String email) {
            this.mEmail = email;
            return this;
        }

        public LnUrlpPayerData.Builder setAuthData(@NonNull LnUrlpAuthData authData) {
            this.mAuthData = authData;
            return this;
        }

        public LnUrlpPayerData build() {
            return new LnUrlpPayerData(mName, mPubkey, mIdentifier, mEmail, mAuthData);
        }
    }
}
