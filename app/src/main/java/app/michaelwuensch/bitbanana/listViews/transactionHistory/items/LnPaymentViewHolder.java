package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import android.view.View;

import com.github.lightningnetwork.lnd.lnrpc.Hop;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LnPaymentViewHolder extends TransactionViewHolder {

    private static final String LOG_TAG = LnPaymentViewHolder.class.getSimpleName();

    private CompositeDisposable mCompositeDisposable;
    private LnPaymentItem mLnPaymentItem;

    public LnPaymentViewHolder(View v) {
        super(v);
    }

    public void setCompositeDisposable(CompositeDisposable compositeDisposable) {
        mCompositeDisposable = compositeDisposable;
    }

    public void bindLnPaymentItem(LnPaymentItem lnPaymentItem) {
        mLnPaymentItem = lnPaymentItem;

        // Standard state. This prevents list entries to get mixed states because of recycling of the ViewHolder.
        setDisplayMode(true);

        Hop lastHop = lnPaymentItem.getPayment().getHtlcs(0).getRoute().getHops(lnPaymentItem.getPayment().getHtlcs(0).getRoute().getHopsCount() - 1);
        String payee = lastHop.getPubKey();

        String payeeName = ContactsManager.getInstance().getNameByContactData(payee);
        if (payee.equals(payeeName)) {
            setPrimaryDescription(mContext.getResources().getString(R.string.sent));
        } else {
            setPrimaryDescription(payeeName);
        }

        setIcon(TransactionIcon.LIGHTNING);
        setTimeOfDay(lnPaymentItem.mCreationDate);
        setAmount(lnPaymentItem.getPayment().getValueSat() * -1, true);
        setFeeSat(lnPaymentItem.getPayment().getFeeSat(), true);

        if (lnPaymentItem.getMemo() == null || lnPaymentItem.getMemo().isEmpty()) {
            // See if we have a message in custom records
            boolean customRecordMessage = false;
            try {
                Map<Long, ByteString> customRecords = lastHop.getCustomRecordsMap();
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
            setSecondaryDescription(lnPaymentItem.getMemo(), true);
        }

        // Set on click listener
        setOnRootViewClickListener(lnPaymentItem, HistoryListItem.TYPE_LN_PAYMENT);
    }

    @Override
    public void refreshViewHolder() {
        bindLnPaymentItem(mLnPaymentItem);
        super.refreshViewHolder();
    }
}
