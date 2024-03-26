package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.LnInvoice;

public class LnInvoiceItem extends TransactionItem {
    private LnInvoice mInvoice;

    public LnInvoiceItem(LnInvoice invoice) {
        mInvoice = invoice;
        if (invoice.isPaid())
            mCreationDate = invoice.getPaidAt();
        else
            mCreationDate = invoice.getCreatedAt();
    }

    @Override
    public int getType() {
        return TYPE_LN_INVOICE;
    }

    public LnInvoice getInvoice() {
        return mInvoice;
    }

    @Override
    public Serializable getSerializedTransaction() {
        return mInvoice;
    }
}
