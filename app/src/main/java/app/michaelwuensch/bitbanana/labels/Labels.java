package app.michaelwuensch.bitbanana.labels;

import java.util.ArrayList;

/**
 * Class to store labels for a wallet.
 */
public class Labels {

    private ArrayList<Label> utxoLabels;
    private ArrayList<Label> transactionLabels;
    private ArrayList<Label> paymentLabels;
    private ArrayList<Label> invoiceLabels;

    public void setUtxoLabels(ArrayList<Label> utxoLabels) {
        this.utxoLabels = utxoLabels;
    }

    public void setTransactionLabels(ArrayList<Label> transactionLabels) {
        this.transactionLabels = transactionLabels;
    }

    public void setPaymentLabels(ArrayList<Label> paymentLabels) {
        this.paymentLabels = paymentLabels;
    }

    public void setInvoiceLabels(ArrayList<Label> invoiceLabels) {
        this.invoiceLabels = invoiceLabels;
    }

    public ArrayList<Label> getUtxoLabels() {
        if (utxoLabels == null)
            utxoLabels = new ArrayList<>();
        return utxoLabels;
    }

    public ArrayList<Label> getTransactionLabels() {
        if (transactionLabels == null)
            transactionLabels = new ArrayList<>();
        return transactionLabels;
    }

    public ArrayList<Label> getPaymentLabels() {
        if (paymentLabels == null)
            paymentLabels = new ArrayList<>();
        return paymentLabels;
    }

    public ArrayList<Label> getInvoiceLabels() {
        if (invoiceLabels == null)
            invoiceLabels = new ArrayList<>();
        return invoiceLabels;
    }

    /**
     * The label type. This allows us to not loop through unnecessary labels of types we are not interested in.
     */
    public enum LabelType {
        UNKNOWN,
        UTXO,
        ON_CHAIN_TRANSACTION,
        LN_PAYMENT,
        LN_INVOICE;

        public static LabelType parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return UNKNOWN;
            }
        }
    }
}
