package app.michaelwuensch.bitbanana.util;

/**
 * This class helps to organize the various types of balances.
 */
public class Balances {
    private static final String LOG_TAG = Balances.class.getSimpleName();
    private final long mOnChainBalanceTotal;
    private final long mOnChainBalanceConfirmed;
    private final long mOnChainBalanceUnconfirmed;
    private final long mChannelBalance;
    private final long mChannelBalancePendingOpen;
    private final long mChannelBalanceLimbo;

    public Balances(long onChainTotal, long onChainConfirmed,
                    long onChainUnconfirmed, long channelBalance,
                    long channelBalancePendingOpen, long channelBalanceLimbo) {
        mOnChainBalanceTotal = onChainTotal;
        mOnChainBalanceConfirmed = onChainConfirmed;
        mOnChainBalanceUnconfirmed = onChainUnconfirmed;
        mChannelBalance = channelBalance;
        mChannelBalancePendingOpen = channelBalancePendingOpen;
        mChannelBalanceLimbo = channelBalanceLimbo;
    }

    public long total() {
        return mOnChainBalanceTotal + mChannelBalance + mChannelBalancePendingOpen + mChannelBalanceLimbo;
    }

    public long onChainTotal() {
        return mOnChainBalanceTotal;
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
        // On the same time a pending force close initiated by the channel partner is ONLY in "Channels Pending Close".
        BBLog.d(LOG_TAG, "------ Balances in " + MonetaryUtil.getInstance().getPrimaryDisplayUnit() + " -------");
        BBLog.d(LOG_TAG, "Total: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromSats(total()));
        BBLog.d(LOG_TAG, "On Chain total: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromSats(onChainTotal()));
        BBLog.d(LOG_TAG, "On Chain confirmed: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromSats(onChainConfirmed()));
        BBLog.d(LOG_TAG, "On Chain unconfirmed: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromSats(onChainUnconfirmed()));
        BBLog.d(LOG_TAG, "Open Channels: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromSats(channelBalance()));
        BBLog.d(LOG_TAG, "Channels Pending Open: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromSats(channelBalancePending()));
        BBLog.d(LOG_TAG, "Channels Pending Close: " + MonetaryUtil.getInstance().getPrimaryDisplayAmountStringFromSats(channelBalanceLimbo()));
        BBLog.d(LOG_TAG, "----------------------------");
    }
}
