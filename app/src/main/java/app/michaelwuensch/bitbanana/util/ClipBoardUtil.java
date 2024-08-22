package app.michaelwuensch.bitbanana.util;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.view.WindowManager;
import android.widget.Toast;

import java.net.URL;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ClipBoardUtil {
    private static final String LOG_TAG = ClipBoardUtil.class.getSimpleName();

    public static void copyToClipboard(Context context, String label, CharSequence data) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, data);
            clipboard.setPrimaryClip(clip);

            // On Android 13+ clipboard toasts are handled automatically. For older ones we provide it.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }

            // Make sure the data just copied to clipboard does not trigger a clipboard scan popup
            String clipboardContentHash = UtilFunctions.sha256Hash(data.toString());
            PrefsUtil.editPrefs().putString(PrefsUtil.LAST_CLIPBOARD_SCAN, clipboardContentHash).apply();
        }
    }

    public static String getPrimaryContent(Context context, boolean addToScanHistory) throws NullPointerException {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        String data = clipboard.getPrimaryClip().getItemAt(0).getText().toString();

        if (addToScanHistory) {
            // Make sure the data just pasted from the clipboard does not trigger a clipboard scan popup.
            // This prevents a popup directly after a new wallet was setup using paste.
            String clipboardContentHash = UtilFunctions.sha256Hash(data);
            PrefsUtil.editPrefs().putString(PrefsUtil.LAST_CLIPBOARD_SCAN, clipboardContentHash).apply();
        }
        return data;
    }


    public static void performClipboardScan(Context context, CompositeDisposable compositeDisposable, OnClipboardScanProceedListener listener) {

        if (!PrefsUtil.getPrefs().getBoolean(PrefsUtil.SCAN_CLIPBOARD, true)) {
            return;
        }

        try {
            getPrimaryContent(context, false);
        } catch (NullPointerException e) {
            return;
        }

        String clipboardContent = getPrimaryContent(context, false);
        String clipboardContentHash = UtilFunctions.sha256Hash(clipboardContent);

        if (PrefsUtil.getPrefs().getString(PrefsUtil.LAST_CLIPBOARD_SCAN, "").equals(clipboardContentHash)) {
            BBLog.v(LOG_TAG, "Clipboard with same content was checked before");
            return;
        } else {
            PrefsUtil.editPrefs().putString(PrefsUtil.LAST_CLIPBOARD_SCAN, clipboardContentHash).apply();
        }

        BBLog.v(LOG_TAG, "New Clipboard content found!");

        /* We are not allowed to access LNURL links twice.
        Therefore we first have to check if it is a LNURL and then hand over to the listener to perform the action.
        Executing the rest twice doesn't harm anyone.
         */
        if (BitcoinStringAnalyzer.isLnUrl(clipboardContent)) {
            showProceedQuestion(R.string.clipboard_scan_lnurl, context, listener);
            return;
        }

        BitcoinStringAnalyzer.analyze(context, compositeDisposable, clipboardContent, new BitcoinStringAnalyzer.OnDataDecodedListener() {
            @Override
            public void onValidLightningInvoice(DecodedBolt11 decodedBolt11) {
                showProceedQuestion(R.string.clipboard_scan_payment, context, listener);
            }

            @Override
            public void onValidBitcoinInvoice(String address, long amount, String message, String lightningInvoice) {
                showProceedQuestion(R.string.clipboard_scan_payment, context, listener);
            }

            @Override
            public void onValidBolt12Offer(DecodedBolt12 decodedBolt12) {
                showProceedQuestion(R.string.clipboard_scan_payment, context, listener);
            }

            @Override
            public void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse) {
                // never reached
            }

            @Override
            public void onValidLnUrlChannel(LnUrlChannelResponse channelResponse) {
                // never reached
            }

            @Override
            public void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse) {
                // never reached
            }

            @Override
            public void onValidLnUrlPay(LnUrlPayResponse payResponse) {
                // never reached
            }

            @Override
            public void onValidLnUrlAuth(URL url) {
                // never reached
            }

            @Override
            public void onValidInternetIdentifier(LnUrlPayResponse payResponse) {
                showProceedQuestion(R.string.clipboard_scan_payment, context, listener);
            }

            @Override
            public void onValidConnectData(BackendConfig backendConfig) {
                showProceedQuestion(R.string.clipboard_scan_connect, context, listener);
            }

            @Override
            public void onValidNodeUri(LightningNodeUri nodeUri) {
                showProceedQuestion(R.string.clipboard_scan_node_pubkey, context, listener);
            }

            @Override
            public void onValidURL(String url) {
                // ignore this. We don't want to annoy the user.
            }

            @Override
            public void onError(String error, int duration) {
                String errorMessage = context.getString(R.string.clipboard_error_heading) + "\n\n" + error;
                listener.onError(errorMessage, duration + 1000);
            }

            @Override
            public void onNoReadableData() {
                // in case we have unrecognized data, we just want to ignore it
            }
        });
    }

    private static void showProceedQuestion(int question, Context context, OnClipboardScanProceedListener listener) {
        AlertDialog.Builder adb = new AlertDialog.Builder(context)
                .setMessage(question)
                .setCancelable(true)
                .setPositiveButton(R.string.continue_string, (dialog, whichButton) -> listener.onProceed(getPrimaryContent(context, false)))
                .setNegativeButton(R.string.no, (dialog, which) -> {
                });

        Dialog dlg = adb.create();
        // Apply FLAG_SECURE to dialog to prevent screen recording
        if (PrefsUtil.isScreenRecordingPrevented()) {
            dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        dlg.show();
    }

    public interface OnClipboardScanProceedListener {
        void onProceed(String content);

        void onError(String error, int duration);
    }
}
