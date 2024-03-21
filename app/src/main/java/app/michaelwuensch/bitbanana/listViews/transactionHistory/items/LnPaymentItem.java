package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import java.io.Serializable;

import app.michaelwuensch.bitbanana.models.LnPayment;

public class LnPaymentItem extends TransactionItem {
    private LnPayment mPayment;

    public LnPaymentItem(LnPayment payment) {
        mPayment = payment;
        mCreationDate = payment.getCreatedAt();
    }

    @Override
    public int getType() {
        return TYPE_LN_PAYMENT;
    }

    public LnPayment getPayment() {
        return mPayment;
    }

    @Override
    public Serializable getSerializedTransaction() {
        return mPayment;
    }
}
