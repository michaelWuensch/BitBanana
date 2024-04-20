package app.michaelwuensch.bitbanana.listViews.transactionHistory;

import java.io.Serializable;

public interface TransactionSelectListener {

    void onTransactionSelect(Serializable transaction, int type);
}
