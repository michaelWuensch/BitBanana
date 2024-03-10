package app.michaelwuensch.bitbanana.lnurl.pay;

import androidx.annotation.NonNull;

import java.util.Random;

import app.michaelwuensch.bitbanana.lnurl.pay.payerData.LnUrlpPayerData;
import app.michaelwuensch.bitbanana.util.HexUtil;

/**
 * This class helps to construct the second pay request.
 * <p>
 * Please refer to step 5 in the following reference:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/06.md
 * <p>
 * For the comment implementation refer to:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/12.md
 */
public class LnUrlSecondPayRequest {

    private String mCallback;
    private long mAmount;
    private String mComment;
    private LnUrlpPayerData mPayerData;

    private LnUrlSecondPayRequest(String callback, long amount, String comment, LnUrlpPayerData payerData) {
        mCallback = callback;
        mAmount = amount;
        mComment = comment;
        mPayerData = payerData;
    }

    public String getCallback() {
        return mCallback;
    }

    public long getAmount() {
        return mAmount;
    }

    public String getComment() {
        return mComment;
    }

    public String requestAsString() {
        String paramStart = mCallback.contains("?") ? "&" : "?";
        return mCallback + paramStart + "amount=" + mAmount + appendComment() + appendPayerData() + "&nonce=" + generateNonce();
    }


    public static class Builder {
        private String mCallback;
        private Long mAmount;
        private String mComment;
        private LnUrlpPayerData mPayerData;

        public Builder setCallback(@NonNull String callback) {
            this.mCallback = callback;
            return this;
        }

        public Builder setAmount(@NonNull Long amount) {
            this.mAmount = amount;
            return this;
        }

        public Builder setComment(String comment) {
            this.mComment = comment;
            return this;
        }

        public Builder setPayerData(LnUrlpPayerData payerData) {
            this.mPayerData = payerData;
            return this;
        }

        public LnUrlSecondPayRequest build() {
            return new LnUrlSecondPayRequest(mCallback, mAmount, mComment, mPayerData);
        }
    }

    // The nonce prevents request caching on the server. It is actually no longer part of the spec but we keep it for now.
    private String generateNonce() {
        byte[] b = new byte[8];
        new Random().nextBytes(b);
        return HexUtil.bytesToHex(b);
    }

    private String appendComment() {
        if (mComment == null || mComment.isEmpty())
            return "";
        return "&comment=" + getComment();
    }

    private String appendPayerData() {
        if (mPayerData == null) {
            return "";
        }
        return "&payerdata=" + mPayerData.getUrlEncodedPayerData();
    }
}
