package app.michaelwuensch.bitbanana.backends.lndHub.models;

import com.google.gson.annotations.SerializedName;

public class LndHubTx {
    @SerializedName("r_hash")
    private String rHash;

    @SerializedName("payment_hash")
    private String paymentHash;

    @SerializedName("payment_preimage")
    private String paymentPreimage;

    @SerializedName("payment_request")
    private String paymentRequest;

    private int value;

    private String type;

    private int fee;

    private long timestamp;

    private String memo;

    private boolean keysend;

    @SerializedName("custom_records")
    private Object customRecords;

    // Getters
    public String getRHash() {
        return rHash;
    }

    public String getPaymentHash() {
        return paymentHash;
    }

    public String getPaymentPreimage() {
        return paymentPreimage;
    }

    public String getPaymentRequest() {
        return paymentRequest;
    }

    public int getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public int getFee() {
        return fee;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMemo() {
        return memo;
    }

    public boolean isKeysend() {
        return keysend;
    }

    public Object getCustomRecords() {
        return customRecords;
    }
}
