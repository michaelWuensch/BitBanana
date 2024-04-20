package app.michaelwuensch.bitbanana.models;

import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;

/**
 * This class helps to organize the various types of balances.
 */
public class Balances {
    private static final String LOG_TAG = Balances.class.getSimpleName();
    private final long mOnChainBalanceConfirmed;
    private final long mOnChainBalanceUnconfirmed;
    private final long mChannelBalance;
    private final long mChannelBalancePendingOpen;
    private final long mChannelBalanceLimbo;

    public static Builder newBuilder() {
        return new Builder();
    }

    private Balances(Builder builder) {
        mOnChainBalanceConfirmed = builder.mOnChainBalanceConfirmed;
        mOnChainBalanceUnconfirmed = builder.mOnChainBalanceUnconfirmed;
        mChannelBalance = builder.mChannelBalance;
        mChannelBalancePendingOpen = builder.mChannelBalancePendingOpen;
        mChannelBalanceLimbo = builder.mChannelBalanceLimbo;
    }

    public long total() {
        return mOnChainBalanceConfirmed + mOnChainBalanceUnconfirmed + mChannelBalance + mChannelBalancePendingOpen + mChannelBalanceLimbo;
    }

    public long onChainTotal() {
        return mOnChainBalanceConfirmed + mOnChainBalanceUnconfirmed;
    }

    public long onChainConfirmed() {
        return mOnChainBalanceConfirmed;
    }

    public long onChainUnconfirmed() {
        return mOnChainBalanceUnconfirmed;
    }

    public long channelBalance() {
        return mChannelBalance;
    }

    public long channelBalancePending() {
        return mChannelBalancePendingOpen;
    }

    // ChannelsLimboBalance is the amount in pending closing channels.
    public long channelBalanceLimbo() {
        return mChannelBalanceLimbo;
    }


    public void debugPrint() {
        // In LND a cooperative close is in both, "On Chain Unconfirmed" & "Channels Pending Close", which will cause a to high total balance until the next block is mined
        // At the same time a pending force close initiated by the channel partner is ONLY in "Channels Pending Close".
        BBLog.d(LOG_TAG, "------ Balances in " + MonetaryUtil.getInstance().getPrimaryDisplayUnit() + " -------");
        BBLog.d(LOG_TAG, "Total: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromMSats(total(), false));
        BBLog.d(LOG_TAG, "On Chain total: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromMSats(onChainTotal(), false));
        BBLog.d(LOG_TAG, "On Chain confirmed: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromMSats(onChainConfirmed(), false));
        BBLog.d(LOG_TAG, "On Chain unconfirmed: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromMSats(onChainUnconfirmed(), false));
        BBLog.d(LOG_TAG, "Open Channels: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromMSats(channelBalance(), false));
        BBLog.d(LOG_TAG, "Channels Pending Open: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromMSats(channelBalancePending(), false));
        BBLog.d(LOG_TAG, "Channels Pending Close: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromMSats(channelBalanceLimbo(), false));
        BBLog.d(LOG_TAG, "----------------------------");
    }

    public static class Builder {
        private long mOnChainBalanceConfirmed;
        private long mOnChainBalanceUnconfirmed;
        private long mChannelBalance;
        private long mChannelBalancePendingOpen;
        private long mChannelBalanceLimbo;

        private Builder() {
        }

        public Balances build() {
            return new Balances(this);
        }

        public Builder setOnChainConfirmed(long onChainConfirmed) {
            mOnChainBalanceConfirmed = onChainConfirmed;
            return this;
        }

        public Builder setOnChainUnconfirmed(long onChainUnconfirmed) {
            mOnChainBalanceUnconfirmed = onChainUnconfirmed;
            return this;
        }

        public Builder setChannelBalance(long channelBalance) {
            mChannelBalance = channelBalance;
            return this;
        }

        public Builder setChannelBalancePendingOpen(long channelBalancePendingOpen) {
            mChannelBalancePendingOpen = channelBalancePendingOpen;
            return this;
        }

        public Builder setChannelBalanceLimbo(long channelBalanceLimbo) {
            mChannelBalanceLimbo = channelBalanceLimbo;
            return this;
        }
    }
}
