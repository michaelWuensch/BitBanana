package app.michaelwuensch.bitbanana.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;

import app.michaelwuensch.bitbanana.R;


/**
 * The UserGuardian is designed to help inexperienced people keeping their bitcoin safe.
 * Use this class to show security warnings whenever the user does something that harms
 * his security or privacy.
 * To avoid too many popups, these messages have a "do not show again" option.
 * <p>
 * Please note that a dialog which will not be shown (do not show again checked) executes
 * the callback like it does when "ok" is pressed.
 */
public class UserGuardian {

    private static final String DIALOG_COPY_TO_CLIPBOARD = "guardianCopyToClipboard";
    private static final String DIALOG_PASTE_FROM_CLIPBOARD = "guardianPasteFromClipboard";
    private static final String DIALOG_DISABLE_SCRAMBLED_PIN = "guardianDisableScrambledPin";
    private static final String DIALOG_DISABLE_SCREEN_PROTECTION = "guardianDisableScreenProtection";
    private static final String DIALOG_LOW_ON_CHAIN_FEE = "guardianLowOnChainFee";
    private static final String DIALOG_HIGH_ONCHAIN_FEE = "guardianHighOnCainFees";
    private static final String DIALOG_OLD_EXCHANGE_RATE = "guardianOldExchangeRate";
    private static final String DIALOG_REMOTE_CONNECT = "guardianRemoteConnect";
    private static final String DIALOG_OLD_NODE_SOFTWARE_VERSION = "guardianOldNodeSoftwareVersion";
    private static final String DIALOG_EXTERNAL_LINK = "guardianExternalLink";
    private static final String DIALOG_ZERO_AMOUNT_INVOICE = "guardianZeroAmountInvoice";
    private static final String DIALOG_CERTIFICATE_VERIFICATION = "guardianCertificateVerification";
    private static final String DIALOG_BACKUP_OVERRIDES_EXISTING_DATA = "guardianBackupOverridesExistingData";
    private static final String DIALOG_ALLOW_UNSPECIFIED_AMOUNT_INVOICES = "guardianAllowUnspecifiedAmountInvoice";
    private static final String DIALOG_CUSTODIAL_LNDHUB = "guardianCustodialLndHub";
    private static final String DIALOG_CUSTODIAL_LND_ACCOUNT_RESTRICTED = "guardianCustodialLndAccountRestricted";
    private static final String DIALOG_REMOVE_WATCHTOWER = "guardianRemoveWatchtower";
    private static final String DIALOG_RELEASE_UTXO_FROM_3RD_PARTY = "releaseUtxoFrom3rdParty";
    private static final String DIALOG_COPY_LOG = "guardianCopyLog";
    private static final String DIALOG_CONSOLIDATE = "guardianConsolidate";
    private static final String DIALOG_BIOMETRICS_AND_EMERGENCY_UNLOCK_1 = "guardianBiometricsAndEmergencyUnlock1";
    private static final String DIALOG_BIOMETRICS_AND_EMERGENCY_UNLOCK_2 = "guardianBiometricsAndEmergencyUnlock2";

    public static final int CLIPBOARD_DATA_TYPE_ONCHAIN = 0;
    public static final int CLIPBOARD_DATA_TYPE_LIGHTNING = 1;
    public static final int CLIPBOARD_DATA_TYPE_NODE_URI = 2;

    private final Context mContext;
    private OnGuardianConfirmedListener mListener;
    private String mCurrentDialogName;
    private CheckBox mDontShowAgain;

    public UserGuardian(Context ctx) {
        mContext = ctx;
    }

    public UserGuardian(Context ctx, OnGuardianConfirmedListener listener) {
        mContext = ctx;
        mListener = listener;
    }

    /**
     * Reset all "do not show again" selections.
     */
    public static void reenableAllSecurityWarnings() {
        PrefsUtil.editPrefs()
                .putBoolean(DIALOG_COPY_TO_CLIPBOARD, true)
                .putBoolean(DIALOG_PASTE_FROM_CLIPBOARD, true)
                .putBoolean(DIALOG_DISABLE_SCRAMBLED_PIN, true)
                .putBoolean(DIALOG_DISABLE_SCREEN_PROTECTION, true)
                .putBoolean(DIALOG_HIGH_ONCHAIN_FEE, true)
                .putBoolean(DIALOG_HIGH_ONCHAIN_FEE, true)
                .putBoolean(DIALOG_OLD_EXCHANGE_RATE, true)
                .putBoolean(DIALOG_REMOTE_CONNECT, true)
                .putBoolean(DIALOG_OLD_NODE_SOFTWARE_VERSION, true)
                .putBoolean(DIALOG_EXTERNAL_LINK, true)
                .putBoolean(DIALOG_ZERO_AMOUNT_INVOICE, true)
                .putBoolean(DIALOG_CERTIFICATE_VERIFICATION, true)
                .putBoolean(DIALOG_ALLOW_UNSPECIFIED_AMOUNT_INVOICES, true)
                .putBoolean(DIALOG_CUSTODIAL_LNDHUB, true)
                .putBoolean(DIALOG_CUSTODIAL_LND_ACCOUNT_RESTRICTED, true)
                .putBoolean(DIALOG_REMOVE_WATCHTOWER, true)
                .putBoolean(DIALOG_RELEASE_UTXO_FROM_3RD_PARTY, true)
                .putBoolean(DIALOG_COPY_LOG, true)
                .putBoolean(DIALOG_CONSOLIDATE, true)
                .putBoolean(DIALOG_BIOMETRICS_AND_EMERGENCY_UNLOCK_1, true)
                .putBoolean(DIALOG_BIOMETRICS_AND_EMERGENCY_UNLOCK_2, true)
                .putBoolean(PrefsUtil.NOTIFICATIONS_DECLINED, false)
                .apply();
    }

    /**
     * Warn the user about security issues when copying stuff to clipboard.
     * Also provide the user with a check string to secure himself
     *
     * @param data the data that is copied to clipboard
     */
    public void securityCopyToClipboard(String data, int type) {
        mCurrentDialogName = DIALOG_COPY_TO_CLIPBOARD;

        String compareString;
        String message = "";
        switch (type) {
            case CLIPBOARD_DATA_TYPE_ONCHAIN:
                if (data.length() > 15) {
                    compareString = data.substring(0, 15) + " ...";
                    message = mContext.getResources().getString(R.string.guardian_copyToClipboard_onChain, compareString);
                }
                break;
            case CLIPBOARD_DATA_TYPE_LIGHTNING:
                if (data.length() > 15) {
                    compareString = "... " + data.substring(data.length() - 8);
                    message = mContext.getResources().getString(R.string.guardian_copyToClipboard_lightning, compareString);
                }
                break;
            case CLIPBOARD_DATA_TYPE_NODE_URI:
                if (data.length() > 15) {
                    compareString = data.substring(0, 8) + " ...";
                    message = mContext.getResources().getString(R.string.guardian_copyToClipboard_onChain, compareString);
                }
                break;
        }

        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about pasting a payment request from clipboard.
     */
    public void securityPasteFromClipboard() {
        mCurrentDialogName = DIALOG_PASTE_FROM_CLIPBOARD;
        AlertDialog.Builder adb = createDontShowAgainDialog(false);
        adb.setMessage(R.string.guardian_pasteFromClipboard);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user to not disable scrambled pin input.
     */
    public void securityScrambledPin() {
        mCurrentDialogName = DIALOG_DISABLE_SCRAMBLED_PIN;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        adb.setMessage(R.string.guardian_disableScrambledPin);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user to not disable screen protection.
     */
    public void securityScreenProtection() {
        mCurrentDialogName = DIALOG_DISABLE_SCREEN_PROTECTION;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        adb.setMessage(R.string.guardian_disableScreenProtection);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about low On-Chain fees.
     * Hopefully this prevents users from creating transactions that get stuck.
     */
    public void securityLowOnChainFee(int satPerVByte) {
        mCurrentDialogName = DIALOG_LOW_ON_CHAIN_FEE;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        adb.setMessage(mContext.getResources().getString(R.string.guardian_low_onchain_fee, satPerVByte));
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about high On-Chain fees.
     * The user will be displayed a message which shows the amount of fee compared to
     * the transactions value.
     *
     * @param feeRate 0 = 0% ; 1 = 100% (equal transaction amount) ; >1 you pay more fees than you transact
     */
    public void securityHighOnChainFee(float feeRate) {
        mCurrentDialogName = DIALOG_HIGH_ONCHAIN_FEE;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        String feeRateString = String.format("%.1f", feeRate * 100);
        adb.setMessage(mContext.getResources().getString(R.string.guardian_highOnChainFee, feeRateString));
        showGuardianDialog(adb);
    }

    /**
     * Warn the user if he tries to request some Bitcoin while his primary currency is a
     * fiat currency and the exchange rate data has come of age.
     *
     * @param age in seconds
     */
    public void securityOldExchangeRate(double age) {
        mCurrentDialogName = DIALOG_OLD_EXCHANGE_RATE;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        String ageString = String.format("%.1f", age / 3600);
        adb.setMessage(mContext.getResources().getString(R.string.guardian_oldExchangeRate, ageString));
        showGuardianDialog(adb);
    }

    /**
     * Warn the user if he is trying to connect to a remote server.
     */
    public void securityConnectToRemoteServer(String host) {
        mCurrentDialogName = DIALOG_REMOTE_CONNECT;
        AlertDialog.Builder adb = createDialog(true);
        String message = mContext.getResources().getString(R.string.guardian_remoteConnect, host);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user if he is trying to connect to a nostr wallet connect remote server.
     */
    public void securityConnectToNostrWalletConnect(String pubkey) {
        mCurrentDialogName = DIALOG_REMOTE_CONNECT;
        AlertDialog.Builder adb = createDialog(true);
        String message = mContext.getResources().getString(R.string.guardian_remoteConnect_nwc, pubkey);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about using a node the runs old unsupported software.
     */
    public void securityOldNodeSoftwareVersion(String nodeSoftwareName, String versionName) {
        mCurrentDialogName = DIALOG_OLD_NODE_SOFTWARE_VERSION;
        AlertDialog.Builder adb = createDialog(false);
        String message = mContext.getResources().getString(R.string.guardian_oldNodeSoftwareVersion, nodeSoftwareName, versionName);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about possible zero amount invoice exploit when trying to pay such an invoice
     */
    public void securityZeroAmountInvoice() {
        mCurrentDialogName = DIALOG_ZERO_AMOUNT_INVOICE;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        String message = mContext.getResources().getString(R.string.guardian_zero_amount_invoice);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about possible unspecified amount invoice exploit when trying to enable creating such invoices.
     */
    public void securityAllowUnspecifiedAmountInvoices() {
        mCurrentDialogName = DIALOG_ALLOW_UNSPECIFIED_AMOUNT_INVOICES;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        String message = mContext.getResources().getString(R.string.guardian_invoices_without_specified_amount);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about possible zero amount invoice exploit.
     */
    public void securityCertificateVerification() {
        mCurrentDialogName = DIALOG_CERTIFICATE_VERIFICATION;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        String message = mContext.getResources().getString(R.string.guardian_certificate_verification);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user before overriding the connection or contact data that already exists by restoring a backup.
     * Although extremely unlikely in the worst case this could lead to a loss of funds if the app was the only way the user could still access his node.
     */
    public void securityBackupOverridesExistingData() {
        mCurrentDialogName = DIALOG_BACKUP_OVERRIDES_EXISTING_DATA;
        AlertDialog.Builder adb = createDialog(true);
        String message = mContext.getResources().getString(R.string.guardian_backup_overrides_existing_data);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user before connecting to a LndHub account as this might be custodial and the user might risk funds.
     */
    public void securityCustodialLndHub() {
        mCurrentDialogName = DIALOG_CUSTODIAL_LNDHUB;
        AlertDialog.Builder adb = createDontShowAgainDialog(false);
        String message = mContext.getResources().getString(R.string.guardian_custodial_lndhub);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user before connecting to a LndHub account as this might be custodial and the user might risk funds.
     */
    public void securityCustodialLndHubInfoButton() {
        mCurrentDialogName = DIALOG_CUSTODIAL_LNDHUB;
        AlertDialog.Builder adb = createDialog(false);
        String message = mContext.getResources().getString(R.string.guardian_custodial_lndhub);
        adb.setMessage(message);
        showGuardianDialog(adb, true);
    }

    /**
     * Warn the user before connecting to a nostr wallet connect wallet as this might be custodial and the user might risk funds.
     */
    public void securityCustodialNwcInfoButton() {
        mCurrentDialogName = DIALOG_CUSTODIAL_LNDHUB;
        AlertDialog.Builder adb = createDialog(false);
        String message = mContext.getResources().getString(R.string.guardian_custodial_nwc);
        adb.setMessage(message);
        showGuardianDialog(adb, true);
    }

    /**
     * Warn the user before connecting to an account restricted LND instance as this might be custodial and the user might risk funds.
     */
    public void securityCustodialLndAccountRestricted() {
        mCurrentDialogName = DIALOG_CUSTODIAL_LND_ACCOUNT_RESTRICTED;
        AlertDialog.Builder adb = createDontShowAgainDialog(false);
        String message = mContext.getResources().getString(R.string.guardian_custodial_lnd_accounts);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user before connecting to an account restricted LND instance as this might be custodial and the user might risk funds.
     */
    public void securityCustodialLndAccountRestrictedInfoButton() {
        mCurrentDialogName = DIALOG_CUSTODIAL_LND_ACCOUNT_RESTRICTED;
        AlertDialog.Builder adb = createDialog(false);
        String message = mContext.getResources().getString(R.string.guardian_custodial_lnd_accounts);
        adb.setMessage(message);
        showGuardianDialog(adb, true);
    }

    /**
     * Warn the user if biometrics are enabled and he wants to set an emergency unlock method.
     */
    public void securityEmergencyUnlockIneffectiveDisableBiometrics() {
        mCurrentDialogName = DIALOG_BIOMETRICS_AND_EMERGENCY_UNLOCK_1;
        AlertDialog.Builder adb = createDialog(true);
        String message = mContext.getResources().getString(R.string.guardian_emergency_unlock_ineffective);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user if he as an emergency unlock method set and wants to enable biometrics.
     */
    public void securityEmergencyUnlockIneffectiveDoNotEnableBiometrics() {
        mCurrentDialogName = DIALOG_BIOMETRICS_AND_EMERGENCY_UNLOCK_2;
        AlertDialog.Builder adb = createDialog(true);
        String message = mContext.getResources().getString(R.string.guardian_emergency_unlock_ineffective_2);
        adb.setMessage(message);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about accessing a transaction or address with a non tor block explorer.
     */
    public void privacyExternalLink() {
        mCurrentDialogName = DIALOG_EXTERNAL_LINK;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        adb.setMessage(R.string.guardian_externalLink);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user about potential loss of privacy when consolidating UTXOs.
     */
    public void privacyConsolidateUTXOs() {
        mCurrentDialogName = DIALOG_CONSOLIDATE;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        adb.setMessage(R.string.guardian_consolidate_utxos);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user before removing a watchtower which could potentially cause a paid service to be lost.
     */
    public void dumbRemoveWatchtower() {
        mCurrentDialogName = DIALOG_OLD_NODE_SOFTWARE_VERSION;
        AlertDialog.Builder adb = createDialog(true);
        adb.setMessage(R.string.guardian_remove_watchtower);
        showGuardianDialog(adb);
    }

    /**
     * Warn the user before releasing an UTXO that was NOT leased manually using the BitBanana UI.
     * Hopefully this prevents users from messing with internal lnd stuff like PBSTs.
     */
    public void securityReleaseUTXO() {
        mCurrentDialogName = DIALOG_RELEASE_UTXO_FROM_3RD_PARTY;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        adb.setMessage(mContext.getResources().getString(R.string.guardian_release_utxo));
        showGuardianDialog(adb);
    }

    /**
     * Warn the user before releasing an UTXO that was NOT leased manually using the BitBanana UI.
     * Hopefully this prevents users from messing with internal lnd stuff like PBSTs.
     */
    public void securityCopyLog() {
        mCurrentDialogName = DIALOG_COPY_LOG;
        AlertDialog.Builder adb = createDontShowAgainDialog(true);
        adb.setMessage(mContext.getResources().getString(R.string.guardian_copy_logs));
        showGuardianDialog(adb);
    }

    /**
     * Create a dialog with a "do not show again" option that is already set up
     * except the message.
     * This helps keeping the dialog functions organized and simple.
     *
     * @param hasCancelOption whether it has a cancel option or not
     * @return returns a preconfigured AlertDialog.Builder which can be further configured later
     */
    private AlertDialog.Builder createDontShowAgainDialog(Boolean hasCancelOption) {
        AlertDialog.Builder adb = new AlertDialog.Builder(mContext);
        LayoutInflater adbInflater = LayoutInflater.from(mContext);
        View DialogLayout = adbInflater.inflate(R.layout.dialog_checkbox, null);
        mDontShowAgain = DialogLayout.findViewById(R.id.skip);
        View titleView = adbInflater.inflate(R.layout.guardian_title, null);
        adb.setView(DialogLayout);
        adb.setCustomTitle(titleView);
        adb.setCancelable(false); // prevents cancelling when tapping outside of the dialog
        adb.setOnCancelListener(dialogInterface -> {
            if (mListener != null)
                mListener.onCancelled();
        });
        adb.setPositiveButton(R.string.ok, (dialog, which) -> {

            if (mDontShowAgain.isChecked()) {
                PrefsUtil.editPrefs().putBoolean(mCurrentDialogName, false).apply();
            }

            if (mListener != null) {
                // Execute interface callback on "OK"
                mListener.onConfirmed();
            }
        });
        if (hasCancelOption) {
            adb.setNegativeButton(R.string.cancel, (dialog, which) -> {
                if (mListener != null)
                    mListener.onCancelled();
            });
        }
        return adb;
    }

    /**
     * Create a dialog that is already set up
     * except the message.
     * This helps keeping the dialog functions organized and simple.
     *
     * @param hasCancelOption whether it has a cancel option or not
     * @return returns a preconfigured AlertDialog.Builder which can be further configured later
     */
    private AlertDialog.Builder createDialog(Boolean hasCancelOption) {
        AlertDialog.Builder adb = new AlertDialog.Builder(mContext);
        LayoutInflater adbInflater = LayoutInflater.from(mContext);
        View titleView = adbInflater.inflate(R.layout.guardian_title, null);
        adb.setCustomTitle(titleView);
        adb.setCancelable(false); // prevents cancelling when tapping outside of the dialog
        adb.setPositiveButton(R.string.ok, (dialog, which) -> {
            if (mListener != null) {
                // Execute interface callback on "OK"
                mListener.onConfirmed();
            }
        });
        if (hasCancelOption) {
            adb.setNegativeButton(R.string.cancel, (dialog, which) -> {
                if (mListener != null)
                    mListener.onCancelled();
            });
        }
        return adb;
    }


    /**
     * Show the dialog or execute callback if it should not be shown.
     *
     * @param adb The AlertDialog.Builder which should be shown.
     */
    private void showGuardianDialog(AlertDialog.Builder adb) {
        showGuardianDialog(adb, false);
    }

    private void showGuardianDialog(AlertDialog.Builder adb, boolean alwaysShow) {
        if (PrefsUtil.getPrefs().getBoolean(mCurrentDialogName, true) || alwaysShow) {
            Dialog dlg = adb.create();
            // Apply FLAG_SECURE to dialog to prevent screen recording
            if (PrefsUtil.isScreenRecordingPrevented()) {
                dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
            dlg.show();
        } else {
            if (mListener != null) {
                mListener.onConfirmed();
            }
        }
    }

    public interface OnGuardianConfirmedListener {
        void onConfirmed();

        void onCancelled();
    }
}
