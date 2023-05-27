package app.michaelwuensch.bitbanana.lnurl.pay;

import androidx.annotation.NonNull;

import java.util.Random;

import app.michaelwuensch.bitbanana.util.UtilFunctions;

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
    private String[] mFromNodes;
    private String mComment;

    private LnUrlSecondPayRequest(String callback, long amount, String[] fromNodes, String comment) {
        mCallback = callback;
        mAmount = amount;
        mFromNodes = fromNodes;
        mComment = comment;
    }

    public String getCallback() {
        return mCallback;
    }

    public long getAmount() {
        return mAmount;
    }

    public String[] getFromNodes() {
        return mFromNodes;
    }

    public String getComment() {
        return mComment;
    }

    public String requestAsString() {
        String paramStart = mCallback.contains("?") ? "&" : "?";
        if (mFromNodes == null) {
            return mCallback + paramStart + "amount=" + mAmount + appendComment() + "&nonce=" + generateNonce();
        } else {
            String fromNodesString = "";
            for (int i = 0; i < mFromNodes.length; i++) {
                if (i == mFromNodes.length - 1) {
                    fromNodesString = fromNodesString + mFromNodes[i];
                } else {
                    fromNodesString = fromNodesString + mFromNodes[i] + ",";
                }
            }
            return mCallback + paramStart + "amount=" + mAmount + appendComment() + "&nonce=" + generateNonce() + "&fromnodes=" + fromNodesString;
        }
    }


    public static class Builder {
        private String mCallback;
        private Long mAmount;
        private String[] mFromNodes;
        private String mComment;

        public Builder setCallback(@NonNull String callback) {
            this.mCallback = callback;

            return this;
        }

        public Builder setAmount(@NonNull Long amount) {
            this.mAmount = amount;

            return this;
        }

        public Builder setFromNodes(String[] fromNodes) {
            this.mFromNodes = fromNodes;

            return this;
        }

        public Builder setComment(String comment) {
            this.mComment = comment;

            return this;
        }

        public LnUrlSecondPayRequest build() {
            return new LnUrlSecondPayRequest(mCallback, mAmount, mFromNodes, mComment);
        }
    }

    private String generateNonce() {
        byte[] b = new byte[8];
        new Random().nextBytes(b);
        return UtilFunctions.bytesToHex(b);
    }

    private String appendComment() {
        if (mComment == null || mComment.isEmpty())
            return "";
        return "&comment=" + getComment();
    }
}
