package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import com.google.common.net.UrlEscapers;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;

public class Bip21Invoice implements Serializable {

    private final String Address;
    private final long Amount;
    private final String Message;
    private final boolean hasMessage;
    /**
     * The lightning parameter is actually not a direct specification of BIP21 but rather a proposal found here: https://bitcoinqr.dev/
     */
    private final String Lightning;
    private final boolean hasLightning;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Bip21Invoice(Builder builder) {
        this.Address = builder.Address;
        this.Amount = builder.Amount;
        this.Message = builder.Message;
        this.hasMessage = builder.hasMessage;
        this.Lightning = builder.Lightning;
        this.hasLightning = builder.hasLightning;
    }

    public String getAddress() {
        return Address;
    }

    /**
     * The requested amount in msat
     */
    public long getAmount() {
        return Amount;
    }

    public boolean hasAmountSpecified() {
        return Amount != 0;
    }

    public String getMessage() {
        return Message;
    }

    public boolean hasDescription() {
        return hasMessage;
    }

    public String getLightning() {
        return Lightning;
    }

    public boolean hasLightning() {
        return hasLightning;
    }

    @NonNull
    @Override
    public String toString() {
        String bitcoinInvoice = UriUtil.generateBitcoinUri(Address);

        if (Amount != 0)
            bitcoinInvoice = UriUtil.appendParameter(bitcoinInvoice, "amount", MonetaryUtil.getInstance().msatsToBitcoinString(Amount));
        if (hasMessage) {
            String escapedMessage = UrlEscapers.urlPathSegmentEscaper().escape(Message);
            bitcoinInvoice = UriUtil.appendParameter(bitcoinInvoice, "message", escapedMessage);
        }
        if (hasLightning) {
            bitcoinInvoice = UriUtil.appendParameter(bitcoinInvoice, "lightning", Lightning);
        }

        return bitcoinInvoice;
    }

    //Builder Class
    public static class Builder {

        private String Address;
        private long Amount;
        private String Message;
        private boolean hasMessage;
        private String Lightning;
        private boolean hasLightning;

        private Builder() {
            // required parameters
        }

        public Bip21Invoice build() {
            return new Bip21Invoice(this);
        }

        public Builder setAddress(String address) {
            this.Address = address;
            return this;
        }

        /**
         * The requested amount in msat
         */
        public Builder setAmount(long amount) {
            this.Amount = amount;
            return this;
        }

        public Builder setMessage(String message) {
            Message = message;
            hasMessage = message != null && !message.isEmpty();
            return this;
        }

        public Builder setLightning(String lightning) {
            Lightning = lightning;
            hasLightning = lightning != null && !lightning.isEmpty();
            return this;
        }
    }
}