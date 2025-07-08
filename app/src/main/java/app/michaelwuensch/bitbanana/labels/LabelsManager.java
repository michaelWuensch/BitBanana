package app.michaelwuensch.bitbanana.labels;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;

/**
 * This SINGLETON class is used to load and save labels.
 * <p>
 * The labels are stored encrypted in the default shared preferences.
 */
public class LabelsManager {

    private static final String LOG_TAG = LabelsManager.class.getSimpleName();
    private static LabelsManager mInstance;
    private WalletLabelsCollection mWalletLabelsCollection;
    private final Set<LabelChangedListener> mLabelChangedListeners = new HashSet<>();

    private LabelsManager() {

        String decrypted = null;
        try {
            decrypted = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.LABELS, "");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        if (isValidJson(decrypted)) {
            mWalletLabelsCollection = new Gson().fromJson(decrypted, WalletLabelsCollection.class);
            if (hasAnyLabels()) {
                if (mWalletLabelsCollection.version < RefConstants.LABELS_JSON_VERSION) {
                    // Here we can convert labels if we change the structure later
                }
            } else {
                mWalletLabelsCollection = createEmptyWalletLabelsCollection();
            }
        } else {
            mWalletLabelsCollection = createEmptyWalletLabelsCollection();
        }
    }

    // used for unit tests
    public LabelsManager(String walletLabelsCollectionString) {
        try {
            mWalletLabelsCollection = new Gson().fromJson(walletLabelsCollectionString, WalletLabelsCollection.class);
        } catch (JsonSyntaxException e) {
            mWalletLabelsCollection = createEmptyWalletLabelsCollection();
        }
        if (mWalletLabelsCollection == null) {
            mWalletLabelsCollection = createEmptyWalletLabelsCollection();
        }
    }

    public static LabelsManager getInstance() {
        if (mInstance == null) {
            mInstance = new LabelsManager();
        }
        return mInstance;
    }

    /**
     * Used to determine if the provided String is a valid Labels JSON.
     *
     * @param labelsString parses as JSON
     * @return if the JSON syntax is valid
     */
    private static boolean isValidJson(String labelsString) {
        try {
            WalletLabelsCollection walletLabelsCollection = new Gson().fromJson(labelsString, WalletLabelsCollection.class);
            return walletLabelsCollection != null;
        } catch (JsonSyntaxException ex) {
            return false;
        }
    }

    public WalletLabelsCollection getWalletLabelsCollection() {
        return mWalletLabelsCollection;
    }

    private WalletLabelsCollection createEmptyWalletLabelsCollection() {
        WalletLabelsCollection walletLabelsCollection = new WalletLabelsCollection();
        walletLabelsCollection.setVersion(RefConstants.LABELS_JSON_VERSION);
        return walletLabelsCollection;
    }

    /**
     * Get all labels associated with the current wallet.
     *
     * @return
     */
    public Labels getWalletLabels() {
        if (BackendManager.getCurrentBackendConfig() == null || mWalletLabelsCollection == null)
            return new Labels();

        for (WalletLabelsEntry walletLabelsEntry : mWalletLabelsCollection.getWalletLabelsList()) {
            if (walletLabelsEntry.getWalletId().equals(BackendManager.getCurrentBackendConfig().getWalletID()))
                return walletLabelsEntry.getLabels();
        }

        mWalletLabelsCollection.addWalletLabelsEntry(new WalletLabelsEntry(BackendManager.getCurrentBackendConfig().getWalletID(), new Labels()));
        return getWalletLabels();
    }

    public void saveLabel(Label label, Labels.LabelType labelType) {
        Labels walletLabels = getWalletLabels();
        if (walletLabels == null)
            mWalletLabelsCollection.addWalletLabelsEntry(new WalletLabelsEntry(BackendManager.getCurrentBackendConfig().getWalletID(), new Labels()));
        switch (labelType) {
            case UTXO:
                updateLabel(walletLabels.getUtxoLabels(), label);
                break;
            case ON_CHAIN_TRANSACTION:
                updateLabel(walletLabels.getTransactionLabels(), label);
                break;
            case LN_PAYMENT:
                updateLabel(walletLabels.getPaymentLabels(), label);
                break;
            case LN_INVOICE:
                updateLabel(walletLabels.getInvoiceLabels(), label);
                break;
        }
        try {
            apply();
        } catch (GeneralSecurityException | IOException e) {
            BBLog.e(LOG_TAG, "Error saving label.");
        }
    }


    public void deleteLabel(Label label, Labels.LabelType labelType) {
        Labels walletLabels = getWalletLabels();
        if (walletLabels == null)
            mWalletLabelsCollection.addWalletLabelsEntry(new WalletLabelsEntry(BackendManager.getCurrentBackendConfig().getWalletID(), new Labels()));
        switch (labelType) {
            case UTXO:
                deleteLabel(walletLabels.getUtxoLabels(), label);
                break;
            case ON_CHAIN_TRANSACTION:
                deleteLabel(walletLabels.getTransactionLabels(), label);
                break;
            case LN_PAYMENT:
                deleteLabel(walletLabels.getPaymentLabels(), label);
                break;
            case LN_INVOICE:
                deleteLabel(walletLabels.getInvoiceLabels(), label);
                break;
        }
        try {
            apply();
        } catch (GeneralSecurityException | IOException e) {
            BBLog.e(LOG_TAG, "Error deleting label.");
        }
    }

    public boolean hasAnyLabels() {
        return !mWalletLabelsCollection.getWalletLabelsList().isEmpty();
    }

    /**
     * Removes all labels (for all wallets!).
     * Do not forget to call apply() afterwards to make this change permanent.
     */
    public void removeAllLabels() {
        mWalletLabelsCollection = createEmptyWalletLabelsCollection();
    }


    /**
     * Saves the current state of all labels encrypted to default shared preferences.
     * Always use this after you have changed anything on the configurations.
     *
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws UnrecoverableEntryException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws NoSuchProviderException
     * @throws BadPaddingException
     * @throws KeyStoreException
     * @throws IllegalBlockSizeException
     */
    public void apply() throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, UnrecoverableEntryException, InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchProviderException, BadPaddingException, KeyStoreException, IllegalBlockSizeException {
        // Convert JSON object to string
        String jsonString = getWalletLabelsCollectionJson();

        // Save the new labels in encrypted prefs
        try {
            PrefsUtil.editEncryptedPrefs().putString(PrefsUtil.LABELS, jsonString).commit();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public String getWalletLabelsCollectionJson() {
        return new Gson().toJson(mWalletLabelsCollection);
    }

    public void restoreWalletLabelsCollectionJson(String walletLabelsCollectionJson) {
        mWalletLabelsCollection = new Gson().fromJson(walletLabelsCollectionJson, WalletLabelsCollection.class);
        try {
            apply();
        } catch (GeneralSecurityException | IOException e) {
            BBLog.e(LOG_TAG, "Error restoring labels.");
        }
    }

    // This adds a label to the set if it is not present and updates it if it was present.
    private ArrayList<Label> updateLabel(ArrayList<Label> labels, Label label) {
        // remove old label if one was available
        for (Label l : labels) {
            if (l.getId().equals(label.getId())) {
                labels.remove(l);
                break;
            }
        }
        labels.add(label);
        return labels;
    }

    private ArrayList<Label> deleteLabel(ArrayList<Label> labels, Label label) {
        for (Label l : labels) {
            if (l.getId().equals(label.getId())) {
                labels.remove(l);
                break;
            }
        }
        return labels;
    }

    public static String getLabel(Utxo utxo) {
        if (BackendManager.getCurrentBackendConfig() == null || utxo == null)
            return null;

        ArrayList<Label> labels = getInstance().getWalletLabels().getUtxoLabels();
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

        ArrayList<Label> labels = getInstance().getWalletLabels().getTransactionLabels();
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

        ArrayList<Label> labels = getInstance().getWalletLabels().getPaymentLabels();
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

        ArrayList<Label> labels = getInstance().getWalletLabels().getInvoiceLabels();
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