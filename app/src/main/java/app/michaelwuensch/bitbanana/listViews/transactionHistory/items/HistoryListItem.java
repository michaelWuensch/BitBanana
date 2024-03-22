package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

public abstract class HistoryListItem implements Comparable<HistoryListItem> {

    public static final int TYPE_DATE = 0;
    public static final int TYPE_ON_CHAIN_TRANSACTION = 1;
    public static final int TYPE_LN_INVOICE = 2;
    public static final int TYPE_LN_PAYMENT = 3;

    public long mCreationDate = 0;

    abstract public int getType();

    public boolean equalsWithSameContent(Object o) {
        if (!equals(o)) {
            return false;
        }

        switch (this.getType()) {
            case TYPE_ON_CHAIN_TRANSACTION:
                int thisConfs = ((OnChainTransactionItem) this).getOnChainTransaction().getConfirmations();
                int oConfs = ((OnChainTransactionItem) o).getOnChainTransaction().getConfirmations();
                return thisConfs == oConfs;
            case TYPE_LN_INVOICE:
                if (((LnInvoiceItem) this).getInvoice().getAmountPaid() != ((LnInvoiceItem) o).getInvoice().getAmountPaid())
                    return false;
                else
                    return ((LnInvoiceItem) this).getInvoice().isExpired() == ((LnInvoiceItem) o).getInvoice().isExpired();
            case TYPE_LN_PAYMENT:
                return ((LnPaymentItem) this).getPayment().getStatus() == ((LnPaymentItem) o).getPayment().getStatus();
            default:
                return true;
        }
    }

    @Override
    public int compareTo(HistoryListItem o) {
        HistoryListItem other = (HistoryListItem) o;
        return Long.compare(other.mCreationDate, this.mCreationDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryListItem that = (HistoryListItem) o;

        if (this.getType() != that.getType()) {
            return false;
        }

        switch (this.getType()) {
            case TYPE_ON_CHAIN_TRANSACTION:
                return ((OnChainTransactionItem) this).getOnChainTransaction().getTransactionId().equals(((OnChainTransactionItem) that).getOnChainTransaction().getTransactionId());
            case TYPE_LN_INVOICE:
                return ((LnInvoiceItem) this).getInvoice().getAddIndex() == ((LnInvoiceItem) that).getInvoice().getAddIndex();
            case TYPE_LN_PAYMENT:
                return ((LnPaymentItem) this).getPayment().getPaymentHash().equals(((LnPaymentItem) that).getPayment().getPaymentHash());
            default:
                return mCreationDate == that.mCreationDate;
        }
    }

    @Override
    public int hashCode() {
        switch (this.getType()) {
            case TYPE_ON_CHAIN_TRANSACTION:
                return ((OnChainTransactionItem) this).getOnChainTransaction().getTransactionId().hashCode();
            case TYPE_LN_INVOICE:
                return Long.valueOf(((LnInvoiceItem) this).getInvoice().getAddIndex()).hashCode();
            case TYPE_LN_PAYMENT:
                return Long.valueOf(((LnPaymentItem) this).getPayment().getPaymentHash()).hashCode();
            default:
                return Long.valueOf(mCreationDate).hashCode();
        }
    }
}
