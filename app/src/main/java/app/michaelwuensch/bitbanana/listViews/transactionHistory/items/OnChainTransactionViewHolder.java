package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import android.view.View;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.WalletUtil;


public class OnChainTransactionViewHolder extends TransactionViewHolder {

    private OnChainTransactionItem mOnChainTransactionItem;

    public OnChainTransactionViewHolder(View v) {
        super(v);
    }

    public void bindOnChainTransactionItem(OnChainTransactionItem onChainTransactionItem) {
        mOnChainTransactionItem = onChainTransactionItem;

        // Standard state. This prevents list entries to get mixed states because of recycling of the ViewHolder.
        setDisplayMode(true);

        if (onChainTransactionItem.getOnChainTransaction().getNumConfirmations() == 0) {
            setDisplayMode(false);
        }

        // Get amounts
        Long amount = onChainTransactionItem.getOnChainTransaction().getAmount();
        long fee = onChainTransactionItem.getOnChainTransaction().getTotalFees();

        setTimeOfDay(onChainTransactionItem.mCreationDate);

        // is internal?
        if (WalletUtil.isChannelTransaction(onChainTransactionItem.getOnChainTransaction())) {

            setIcon(TransactionIcon.INTERNAL);
            setFeeSat(fee, false);

            // Internal transactions are a mess in LND. Some transaction values are not populated, sometimes value and fee is switched.
            // There are transactions for force closes that never get confirmations and get deleted on restarting LND ...

            switch (amount.compareTo(0L)) {
                case 0:
                    // amount = 0
                    setAmount(amount, false);
                    setPrimaryDescription(mContext.getString(R.string.force_closed_channel));
                    String pubkeyForceClose = WalletUtil.getNodePubKeyFromChannelTransaction(onChainTransactionItem.getOnChainTransaction());
                    String aliasForceClose = AliasManager.getInstance().getAlias(pubkeyForceClose);
                    setSecondaryDescription(aliasForceClose, true);
                    break;
                case 1:
                    // amount > 0 (Channel closed)
                    setAmount(amount, false);
                    setPrimaryDescription(mContext.getString(R.string.closed_channel));
                    String pubkeyClosed = WalletUtil.getNodePubKeyFromChannelTransaction(onChainTransactionItem.getOnChainTransaction());
                    String aliasClosed = AliasManager.getInstance().getAlias(pubkeyClosed);
                    setSecondaryDescription(aliasClosed, true);
                    break;
                case -1:
                    if (onChainTransactionItem.getOnChainTransaction().getLabel().toLowerCase().contains("sweep")) {
                        // in some rare cases for sweep transactions the value is actually the fee payed for the sweep.
                        setAmount(amount, true);
                        setPrimaryDescription(mContext.getString(R.string.closed_channel));
                        String aliasClose = AliasManager.getInstance().getAlias(WalletUtil.getNodePubKeyFromChannelTransaction(onChainTransactionItem.getOnChainTransaction()));
                        setSecondaryDescription(aliasClose, true);
                    } else {
                        // amount < 0 (Channel opened)
                        // Here we use the fee for the amount, as this is what we actually have to pay.
                        // Doing it this way looks nicer than having 0 for amount and the fee in small.
                        setAmount(fee * -1, true);
                        setPrimaryDescription(mContext.getString(R.string.opened_channel));
                        String aliasOpened = AliasManager.getInstance().getAlias(WalletUtil.getNodePubKeyFromChannelTransaction(onChainTransactionItem.getOnChainTransaction()));
                        setSecondaryDescription(aliasOpened, true);
                    }
                    break;
            }
        } else {
            // It is a normal transaction
            setIcon(TransactionIcon.ONCHAIN);
            setSecondaryDescription("", false);

            switch (amount.compareTo(0L)) {
                case 0:
                    // amount = 0 (should actually not happen)
                    setFeeSat(fee, false);
                    setPrimaryDescription(mContext.getString(R.string.internal));
                    break;
                case 1:
                    // amount > 0 (received on-chain)
                    setAmount(amount, true);
                    setFeeSat(fee, false);
                    setPrimaryDescription(mContext.getString(R.string.received));
                    break;
                case -1:
                    // amount < 0 (sent on-chain)
                    setAmount(amount + fee, true);
                    setFeeSat(fee, true);
                    setPrimaryDescription(mContext.getString(R.string.sent));
                    break;
            }
        }

        // Set on click listener
        setOnRootViewClickListener(onChainTransactionItem, HistoryListItem.TYPE_ON_CHAIN_TRANSACTION);
    }

    @Override
    public void refreshViewHolder() {
        bindOnChainTransactionItem(mOnChainTransactionItem);
        super.refreshViewHolder();
    }
}
