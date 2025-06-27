package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import android.view.View;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.labels.LabelsUtil;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.util.FeatureManager;

public class LnPaymentViewHolder extends TransactionViewHolder {

    private static final String LOG_TAG = LnPaymentViewHolder.class.getSimpleName();

    private LnPaymentItem mLnPaymentItem;

    public LnPaymentViewHolder(View v) {
        super(v);
    }

    public void bindLnPaymentItem(LnPaymentItem lnPaymentItem) {
        mLnPaymentItem = lnPaymentItem;
        LnPayment payment = lnPaymentItem.getPayment();

        // Standard state. This prevents list entries to get mixed states because of recycling of the ViewHolder.
        setTranslucent(false);

        if (payment.hasDestinationPubKey()) {
            String payeeName = ContactsManager.getInstance().getNameByContactData(payment.getDestinationPubKey());
            if (payment.getDestinationPubKey().equals(payeeName)) {
                setPrimaryDescription(mContext.getResources().getString(R.string.sent));
            } else {
                setPrimaryDescription(payeeName);
            }
        } else
            setPrimaryDescription(mContext.getResources().getString(R.string.sent));

        setIcon(TransactionIcon.LIGHTNING);
        setTimeOfDay(lnPaymentItem.mCreationDate);
        setAmount(payment.getAmountPaid() * -1, true);
        setFee(payment.getFee(), true);

        // Set description
        String label = null;
        if (FeatureManager.isLabelsEnabled()) {
            label = LabelsUtil.getLabel(payment);
        }
        if (label != null) {
            setSecondaryDescription(label, true);
        } else if (payment.hasDescription()) {
            setSecondaryDescription(payment.getDescription(), true);
        } else if (payment.hasKeysendMessage()) {
            setSecondaryDescription(payment.getKeysendMessage(), true);
        } else if (payment.hasBolt12PayerNote())
            setSecondaryDescription(payment.getBolt12PayerNote(), true);
        else {
            setSecondaryDescription("", false);
        }

        // Set on click listener
        setOnRootViewClickListener(lnPaymentItem, HistoryListItem.TYPE_LN_PAYMENT);
    }

    @Override
    public void refreshViewHolder() {
        bindLnPaymentItem(mLnPaymentItem);
        super.refreshViewHolder();
    }

    public void rebind() {
        bindLnPaymentItem(mLnPaymentItem);
    }
}
