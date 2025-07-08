package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import android.view.View;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.labels.LabelsManager;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.util.FeatureManager;


public class LnInvoiceViewHolder extends TransactionViewHolder {

    private LnInvoiceItem mLnInvoiceItem;

    public LnInvoiceViewHolder(View v) {
        super(v);
    }

    public void bindLnInvoiceItem(LnInvoiceItem lnInvoiceItem) {
        mLnInvoiceItem = lnInvoiceItem;
        LnInvoice invoice = lnInvoiceItem.getInvoice();

        // Standard state. This prevents list entries to get mixed states because of recycling of the ViewHolder.
        setTranslucent(false);

        setFee(0, false);
        setTimeOfDay(lnInvoiceItem.mCreationDate);

        // Set description
        String label = null;
        if (FeatureManager.isLabelsEnabled()) {
            label = LabelsManager.getLabel(invoice);
        }
        if (label != null) {
            setSecondaryDescription(label, true);
        } else if (invoice.hasMemo()) {
            setSecondaryDescription(invoice.getMemo(), true);
        } else if (invoice.hasKeysendMessage()) {
            setSecondaryDescription(invoice.getKeysendMessage(), true);
        } else if (invoice.hasBolt12PayerNote()) {
            setSecondaryDescription(invoice.getBolt12PayerNote(), true);
        } else {
            setSecondaryDescription("", false);
        }

        if (invoice.isPaid()) {
            setIcon(TransactionIcon.LIGHTNING);
            setPrimaryDescription(mContext.getString(R.string.received));
            setAmount(invoice.getAmountPaid(), true);
        } else {
            setIcon(TransactionIcon.PENDING);
            setAmountPending(invoice.getAmountRequested(), invoice.hasRequestAmountSpecified(), true);
            if (invoice.isExpired()) {
                setPrimaryDescription(mContext.getString(R.string.request_expired));
                setTranslucent(true);
            } else {
                setPrimaryDescription(mContext.getString(R.string.requested_payment));
            }
        }

        // Set on click listener
        setOnRootViewClickListener(lnInvoiceItem, HistoryListItem.TYPE_LN_INVOICE);
    }

    @Override
    public void refreshViewHolder() {
        bindLnInvoiceItem(mLnInvoiceItem);
        super.refreshViewHolder();
    }

    public void rebind() {
        bindLnInvoiceItem(mLnInvoiceItem);
    }
}
