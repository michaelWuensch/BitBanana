package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.OnChainTransaction;

public class OnChainTransactionItem extends TransactionItem {
    private OnChainTransaction mOnChainTransaction;

    public OnChainTransactionItem(OnChainTransaction onChainTransaction) {
        mOnChainTransaction = onChainTransaction;
        mCreationDate = onChainTransaction.getTimeStamp();
    }

    @Override
    public int getType() {
        return TYPE_ON_CHAIN_TRANSACTION;
    }

    public OnChainTransaction getOnChainTransaction() {
        return mOnChainTransaction;
    }

    @Override
    public Serializable getSerializedTransaction() {
        return mOnChainTransaction;
    }
}
