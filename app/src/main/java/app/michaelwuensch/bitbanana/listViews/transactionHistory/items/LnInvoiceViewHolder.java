package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import android.view.View;

import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Components;


public class LnInvoiceViewHolder extends TransactionViewHolder {

    private LnInvoiceItem mLnInvoiceItem;

    public LnInvoiceViewHolder(View v) {
        super(v);
    }

    public void bindLnInvoiceItem(LnInvoiceItem lnInvoiceItem) {
        mLnInvoiceItem = lnInvoiceItem;

        // Standard state. This prevents list entries to get mixed states because of recycling of the ViewHolder.
        setDisplayMode(true);

        setFeeSat(0, false);
        setTimeOfDay(lnInvoiceItem.mCreationDate);

        // Set description
        if (lnInvoiceItem.getInvoice().getMemo().equals("")) {
            // See if we have a message in custom records
            boolean customRecordMessage = false;
            try {
                Map<Long, ByteString> customRecords = lnInvoiceItem.getInvoice().getHtlcs(0).getCustomRecordsMap();
                for (Long key : customRecords.keySet()) {
                    if (key == PaymentUtil.KEYSEND_MESSAGE_RECORD) {
                        setSecondaryDescription(customRecords.get(key).toString(StandardCharsets.UTF_8), true);
                        customRecordMessage = true;
                        break;
                    }
                }
            } catch (Exception ignored) {

            }
            if (!customRecordMessage)
                setSecondaryDescription("", false);
        } else {
            setSecondaryDescription(lnInvoiceItem.getInvoice().getMemo(), true);
        }

        Long amt = lnInvoiceItem.getInvoice().getValue();
        Long amtPayed = lnInvoiceItem.getInvoice().getAmtPaidSat();

        if (amt.equals(0L)) {
            // if no specific value was requested
            if (!amtPayed.equals(0L)) {
                // The invoice has been payed
                setIcon(TransactionIcon.LIGHTNING);
                setPrimaryDescription(mContext.getString(R.string.received));
                setAmount(amtPayed, true);
            } else {
                // The invoice has not been payed yet
                setIcon(TransactionIcon.PENDING);
                setAmountPending(0L, false, true);

                if (Wallet_Components.getInstance().isInvoiceExpired(lnInvoiceItem.getInvoice())) {
                    // The invoice has expired
                    setPrimaryDescription(mContext.getString(R.string.request_expired));
                    setDisplayMode(false);
                } else {
                    // The invoice has not yet expired
                    setPrimaryDescription(mContext.getString(R.string.requested_payment));
                }
            }
        } else {
            // if a specific value was requested
            if (Wallet_Components.getInstance().isInvoicePayed(lnInvoiceItem.getInvoice())) {
                // The invoice has been payed
                setIcon(TransactionIcon.LIGHTNING);
                setPrimaryDescription(mContext.getString(R.string.received));
                setAmount(amtPayed, true);
            } else {
                // The invoice has not been payed yet
                setIcon(TransactionIcon.PENDING);
                setAmountPending(amt, true, true);

                if (Wallet_Components.getInstance().isInvoiceExpired(lnInvoiceItem.getInvoice())) {
                    // The invoice has expired
                    setPrimaryDescription(mContext.getString(R.string.request_expired));
                    setDisplayMode(false);
                } else {
                    // The invoice has not yet expired
                    setPrimaryDescription(mContext.getString(R.string.requested_payment));
                }
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
}
