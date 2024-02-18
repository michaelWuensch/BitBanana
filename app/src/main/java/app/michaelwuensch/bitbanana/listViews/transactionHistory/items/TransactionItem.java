package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import com.google.protobuf.ByteString;

public abstract class TransactionItem extends HistoryListItem {

    abstract public ByteString getTransactionByteString();
}
