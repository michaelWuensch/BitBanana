package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import com.google.common.net.UrlEscapers;

import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;

import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;

public class Bip21Invoice implements Serializable {

    private static final String LOG_TAG = Bip21Invoice.class.getSimpleName();

    private final String Address;
    private final long Amount;
    private final String Label;
    private final boolean hasLabel;
    private final String Message;
    private final boolean hasMessage;
    /**
     * The lightning parameter is actually not a direct specification of BIP21 but rather a proposal found here: https://bitcoinqr.dev/
     */
    private final String Lightning;
    private final boolean hasLightning;
    private final String Offer;
    private final boolean hasOffer;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Bip21Invoice(Builder builder) {
        this.Address = builder.Address;
        this.Amount = builder.Amount;
        this.Label = builder.Label;
        this.hasLabel = builder.hasLabel;
        this.Message = builder.Message;
        this.hasMessage = builder.hasMessage;
        this.Lightning = builder.Lightning;
        this.hasLightning = builder.hasLightning;
        this.Offer = builder.Offer;
        this.hasOffer = builder.hasOffer;
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

    public String getLabel() {
        return Label;
    }

    public String getLabelURLDecoded() {
        if (!hasLabel)
            return Label;
        try {
            return URLDecoder.decode(Label, "UTF-8");
        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Error while decoding label: " + e.getMessage());
            return Label;
        }
    }

    public boolean hasLabel() {
        return hasLabel;
    }

    public String getMessage() {
        return Message;
    }

    public String getMessageURLDecoded() {
        if (!hasMessage)
            return Message;
        try {
            return URLDecoder.decode(Message, "UTF-8");
        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Error while decoding message: " + e.getMessage());
            return Message;
        }
    }

    public boolean hasMessage() {
        return hasMessage;
    }

    public String getLightning() {
        return Lightning;
    }

    public boolean hasLightning() {
        return hasLightning;
    }

    public String getOffer() {
        return Offer;
    }

    public boolean hasOffer() {
        return hasOffer;
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
        if (hasOffer)
            bitcoinInvoice = UriUtil.appendParameter(bitcoinInvoice, "lno", Offer);
        if (hasLightning)
            bitcoinInvoice = UriUtil.appendParameter(bitcoinInvoice, "lightning", Lightning);


        return bitcoinInvoice;
    }

    //Builder Class
    public static class Builder {

        private String Address;
        private long Amount;
        private String Label;
        private boolean hasLabel;
        private String Message;
        private boolean hasMessage;
        private String Lightning;
        private boolean hasLightning;
        private String Offer;
        private boolean hasOffer;

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

        public Builder setMessageURLEncode(String message) {
            try {
                Message = URLEncoder.encode(message, "UTF-8");
                hasMessage = message != null && !message.isEmpty();
            } catch (Exception e) {
                BBLog.w(LOG_TAG, "Error while encoding message: " + e.getMessage());
            }
            return this;
        }

        public Builder setLabel(String label) {
            Label = label;
            hasLabel = label != null && !label.isEmpty();
            return this;
        }

        public Builder setLabelURLEncode(String label) {
            try {
                Label = URLEncoder.encode(label, "UTF-8");
                hasLabel = label != null && !label.isEmpty();
            } catch (Exception e) {
                BBLog.w(LOG_TAG, "Error while encoding label: " + e.getMessage());
            }
            return this;
        }

        public Builder setLightning(String lightning) {
            Lightning = lightning;
            hasLightning = lightning != null && !lightning.isEmpty();
            return this;
        }

        public Builder setOffer(String offer) {
            Offer = offer;
            hasOffer = offer != null && !offer.isEmpty();
            return this;
        }
    }
}