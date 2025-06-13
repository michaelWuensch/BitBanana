package app.michaelwuensch.bitbanana.wallet;

import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;

/**
 * Class to store quick receive information for a BackendConfig.
 */
public class QuickReceiveConfig {

    private QuickReceiveType quickReceiveType = QuickReceiveType.OFF;
    private String customString;
    private String lnAddress;
    private String Bolt12ID;


    public QuickReceiveType getQuickReceiveType() {
        return this.quickReceiveType;
    }

    public void setQuickReceiveType(QuickReceiveType quickReceiveType) {
        this.quickReceiveType = quickReceiveType;
    }

    public String getCustomString() {
        return this.customString;
    }

    public void setCustomString(String customString) {
        this.customString = customString;
    }

    public String getLnAddress() {
        return this.lnAddress;
    }

    public void setLnAddress(String lnAddress) {
        this.lnAddress = lnAddress;
    }

    public String getBolt12ID() {
        return this.Bolt12ID;
    }

    public void setBolt12ID(String Bolt12ID) {
        this.Bolt12ID = Bolt12ID;
    }

    public boolean isSameQuickReceiveConfig(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        QuickReceiveConfig quickReceiveConfig = (QuickReceiveConfig) obj;
        return (quickReceiveConfig.getQuickReceiveType() == this.getQuickReceiveType()
                && quickReceiveConfig.getCustomString().equals(this.getCustomString())
                && quickReceiveConfig.getLnAddress().equals(this.getLnAddress())
                && quickReceiveConfig.getBolt12ID().equals(this.getBolt12ID()));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return isSameQuickReceiveConfig(obj);
    }

    /**
     * The quick receive type. Do we just show the quickReceiveString or should it be combined with a bip21 invoice, etc.?
     */
    public enum QuickReceiveType {
        OFF,
        LN_ADDRESS,
        BOLT12,
        ON_CHAIN_ADDRESS,
        ON_CHAIN_AND_LN_ADDRESS,
        ON_CHAIN_AND_BOLT12;

        public static QuickReceiveType parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return OFF;
            }
        }

        public String getDisplayName() {
            switch (this) {
                case OFF:
                    return App.getAppContext().getString(R.string.off);
                case LN_ADDRESS:
                    return App.getAppContext().getString(R.string.ln_address);
                case BOLT12:
                    return App.getAppContext().getString(R.string.bolt12_offer);
                case ON_CHAIN_ADDRESS:
                    return App.getAppContext().getString(R.string.quick_receive_type_on_chain_address);
                case ON_CHAIN_AND_LN_ADDRESS:
                    return App.getAppContext().getString(R.string.quick_receive_type_on_chain_and_ln_address);
                case ON_CHAIN_AND_BOLT12:
                    return App.getAppContext().getString(R.string.quick_receive_type_on_chain_and_bolt12);
                default:
                    return this.name();
            }
        }
    }
}
