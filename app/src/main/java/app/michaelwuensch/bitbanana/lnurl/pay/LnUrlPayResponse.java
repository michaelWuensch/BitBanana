package app.michaelwuensch.bitbanana.lnurl.pay;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;

import app.michaelwuensch.bitbanana.lnurl.LnUrlResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.payerData.LnUrlpRequestedPayerData;
import app.michaelwuensch.bitbanana.util.UtilFunctions;

/**
 * This class helps to work with the received response from a LNURL-pay request.
 * <p>
 * Please refer to step 3 in the following reference:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/06.md
 * <p>
 * For the comment implementation refer to:
 * https://github.com/fiatjaf/lnurl-rfc/blob/luds/12.md
 */
public class LnUrlPayResponse extends LnUrlResponse implements Serializable {

    public static final String ARGS_KEY = "lnurlPayResponse";
    public static final String METADATA_TEXT = "text/plain";
    public static final String METADATA_IMAGE_PNG = "image/png;base64";
    public static final String METADATA_IMAGE_JPEG = "image/jpeg;base64";
    public static final String METADATA_IDENTIFIER = "text/identifier";
    public static final String METADATA_EMAIL = "text/email";

    private String metadata;
    private int commentAllowed = 0;
    private LnUrlpRequestedPayerData payerData;

    /**
     * In milliSatoshis
     */
    private long maxSendable;
    /**
     * In milliSatoshis
     */
    private long minSendable;

    public long getMaxSendable() {
        return maxSendable;
    }

    public long getMinSendable() {
        return minSendable;
    }

    public String getMetadataAsString(String metadataName) {
        JsonArray[] list = getMetadataAsList();
        for (JsonArray jsonArray : list) {
            if (jsonArray.get(0).getAsString().equals(metadataName)) {
                return jsonArray.get(1).getAsString();
            }
        }
        return null;
    }

    public JsonArray getMetadataAsJsonArray(String metadataName) {
        JsonArray[] list = getMetadataAsList();
        for (JsonArray jsonArray : list) {
            if (jsonArray.get(0).getAsString().equals(metadataName)) {
                return jsonArray;
            }
        }
        return null;
    }

    private JsonArray[] getMetadataAsList() {
        Gson gson = new Gson();
        Type listType = new TypeToken<JsonArray[]>() {
        }.getType();
        return gson.fromJson(metadata, listType);
    }

    public String getMetadataHash() {
        return UtilFunctions.sha256Hash(metadata);
    }

    public String getMetadata() {
        return metadata;
    }

    public boolean isCommentAllowed() {
        return commentAllowed != 0;
    }

    public int getCommentMaxLength() {
        return commentAllowed;
    }

    public boolean requestsPayerData() {
        return payerData != null;
    }

    public LnUrlpRequestedPayerData getRequestedPayerData() {
        return payerData;
    }
}
