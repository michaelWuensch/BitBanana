package app.michaelwuensch.bitbanana.backends.lndHub.models;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class LndHubUserInvoice {
    @SerializedName("payment_hash")
    private String paymentHash;

    @SerializedName("payment_request")
    private String paymentRequest;

    @SerializedName("pay_req")
    private String payReq;

    private String description;
    private long timestamp;

    @SerializedName("type")
    private String invoiceType;

    @SerializedName("expire_time")
    private long expireTime;

    private int amt;
    private boolean ispaid;
    private boolean keysend;

    @SerializedName("custom_records")
    private Map<String, Object> customRecords;

    public String getPaymentHash() {
        return paymentHash;
    }

    public String getPaymentRequest() {
        return paymentRequest;
    }

    public String getPayReq() {
        return payReq;
    }

    public String getDescription() {
        return description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public int getAmt() {
        return amt;
    }

    public boolean isPaid() {
        return ispaid;
    }

    public boolean isKeysend() {
        return keysend;
    }

    public Map<String, Object> getCustomRecords() {
        return customRecords;
    }
}
