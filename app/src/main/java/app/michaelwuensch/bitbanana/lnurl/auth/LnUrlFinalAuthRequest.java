package app.michaelwuensch.bitbanana.lnurl.auth;

import androidx.annotation.NonNull;

import java.net.URL;

/**
 * This class helps to construct the final auth request to authenticate at a LNService.
 * <p>
 * Please refer to step 3 in the following reference:
 * https://github.com/lnurl/luds/blob/luds/04.md
 */
public class LnUrlFinalAuthRequest {

    private URL mDecodedLnUrl;
    private String mSig;
    private String mLinkingKey;

    private LnUrlFinalAuthRequest(URL decodedLnUrl, String sig, String linkingKey) {
        mDecodedLnUrl = decodedLnUrl;
        mSig = sig;
        mLinkingKey = linkingKey;
    }

    public String getSig() {
        return mSig;
    }

    public URL getDecodedLnUrl() {
        return mDecodedLnUrl;
    }

    public String getLinkingKey() {
        return mLinkingKey;
    }

    public String requestAsString() {
        return mDecodedLnUrl.toString() + "&sig=" + mSig + "&key=" + mLinkingKey;
    }


    public static class Builder {
        private URL mDecodedLnUrl;
        private String mSig;
        private String mLinkingKey;

        public Builder setDecodedLnUrl(@NonNull URL decodedLnUrl) {
            this.mDecodedLnUrl = decodedLnUrl;

            return this;
        }

        public Builder setSig(@NonNull String sig) {
            this.mSig = sig;

            return this;
        }

        public Builder setLinkingKey(@NonNull String linkingKey) {
            this.mLinkingKey = linkingKey;

            return this;
        }

        public LnUrlFinalAuthRequest build() {
            return new LnUrlFinalAuthRequest(mDecodedLnUrl, mSig, mLinkingKey);
        }
    }
}
