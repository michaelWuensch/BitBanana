package app.michaelwuensch.bitbanana.labels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.Utxo;

public class LabelsUtil {

    private static final String LOG_TAG = LabelsUtil.class.getSimpleName();

    private static LabelsUtil mInstance = null;
    private final Set<LabelChangedListener> mLabelChangedListeners = new HashSet<>();

    private LabelsUtil() {
    }

    public static LabelsUtil getInstance() {

        if (mInstance == null) {
            mInstance = new LabelsUtil();
        }

        return mInstance;
    }

    public static String getLabel(Utxo utxo) {
        if (BackendManager.getCurrentBackendConfig() == null || utxo == null)
            return null;

        ArrayList<Label> labels = BackendManager.getCurrentBackendConfig().getLabels().getUtxoLabels();
        if (labels.isEmpty())
            return null;

        for (Label label : labels) {
            if (label.getId().equals(utxo.getOutpoint().toString()))
                return label.getLabel();
        }
        return null;
    }

    public static String getLabel(OnChainTransaction transaction) {
        if (BackendManager.getCurrentBackendConfig() == null || transaction == null)
            return null;

        ArrayList<Label> labels = BackendManager.getCurrentBackendConfig().getLabels().getTransactionLabels();
        if (labels.isEmpty())
            return null;

        for (Label label : labels) {
            if (label.getId().equals(transaction.getTransactionId()))
                return label.getLabel();
        }
        return null;
    }

    public static String getLabel(LnPayment payment) {
        if (BackendManager.getCurrentBackendConfig() == null || payment == null)
            return null;

        ArrayList<Label> labels = BackendManager.getCurrentBackendConfig().getLabels().getPaymentLabels();
        if (labels.isEmpty())
            return null;

        for (Label label : labels) {
            if (label.getId().equals(payment.getPaymentHash()))
                return label.getLabel();
        }
        return null;
    }

    public static String getLabel(LnInvoice invoice) {
        if (BackendManager.getCurrentBackendConfig() == null || invoice == null)
            return null;

        ArrayList<Label> labels = BackendManager.getCurrentBackendConfig().getLabels().getInvoiceLabels();
        if (labels.isEmpty())
            return null;

        for (Label label : labels) {
            if (label.getId().equals(invoice.getPaymentHash()))
                return label.getLabel();
        }
        return null;
    }


    /**
     * Notify all listeners to label changed events
     */
    public void broadcastLabelChanged() {
        for (LabelChangedListener listener : mLabelChangedListeners) {
            listener.onLabelChanged();
        }
    }

    public void registerLabelChangedListener(LabelChangedListener listener) {
        mLabelChangedListeners.add(listener);
    }

    public void unregisterLabelChangedListener(LabelChangedListener listener) {
        mLabelChangedListeners.remove(listener);
    }

    public interface LabelChangedListener {
        void onLabelChanged();
    }
}
