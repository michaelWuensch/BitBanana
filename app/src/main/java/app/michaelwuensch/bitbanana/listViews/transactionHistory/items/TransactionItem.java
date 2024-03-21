package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import java.io.Serializable;

public abstract class TransactionItem extends HistoryListItem {

    abstract public Serializable getSerializedTransaction();
}
