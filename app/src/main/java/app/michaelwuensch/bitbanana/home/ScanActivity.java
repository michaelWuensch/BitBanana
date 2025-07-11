package app.michaelwuensch.bitbanana.home;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import java.net.URL;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.baseClasses.BaseScannerActivity;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.BitcoinStringAnalyzer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.NfcUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.StaticInternetIdentifierReader;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ScanActivity extends BaseScannerActivity {

    public static final String EXTRA_GENERIC_SCAN_DATA = "genericScanData";

    private static final String LOG_TAG = ScanActivity.class.getSimpleName();

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        //NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }


    @Override
    public void onButtonPasteClick() {
        super.onButtonPasteClick();

        String clipboardString = ClipBoardUtil.getPrimaryContent(getApplicationContext(), true);

        if (clipboardString != null)
            readData(clipboardString);
        else
            showError(getResources().getString(R.string.error_emptyClipboardPayment), RefConstants.ERROR_DURATION_SHORT);
    }

    @Override
    public void onButtonInstructionsHelpClick() {
        HelpDialogUtil.showDialog(ScanActivity.this, R.string.help_dialog_scanGeneric);
    }

    @Override
    public void handleCameraResult(String result) {
        super.handleCameraResult(result);

        readData(result);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void readData(String data) {

        /* We are not allowed to access LNURL links twice.
        Therefore we first have to check if it is a LNURL and then hand over to the HomeActivity.
        Executing the rest twice doesn't harm anyone and makes sure the errors are displayed on the scan activity.
         */
        if (BitcoinStringAnalyzer.isLnUrl(data)) {
            readableDataFound(data);
            return;
        }
        if (StaticInternetIdentifierReader.isLnAddress(data)) {
            readableDataFound(data);
            return;
        }

        BitcoinStringAnalyzer.analyze(ScanActivity.this, compositeDisposable, data, new BitcoinStringAnalyzer.OnDataDecodedListener() {
            @Override
            public void onValidLightningInvoice(DecodedBolt11 decodedBolt11, Bip21Invoice fallbackOnChainInvoice) {
                readableDataFound(data);
            }

            @Override
            public void onValidBitcoinInvoice(Bip21Invoice onChainInvoice) {
                readableDataFound(data);
            }

            @Override
            public void onValidBolt12Offer(DecodedBolt12 decodedBolt12, Bip21Invoice fallbackOnChainInvoice) {
                readableDataFound(data);
            }

            @Override
            public void onLnAddressFound() {

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
                readableDataFound(data);
            }

            @Override
            public void onValidLnUrlAuth(URL url) {
                // never reached
            }

            @Override
            public void onValidConnectData(BackendConfig backendConfig) {
                readableDataFound(data);
            }

            @Override
            public void onValidNodeUri(LightningNodeUri nodeUri) {
                readableDataFound(data);
            }

            @Override
            public void onValidURL(String url) {
                readableDataFound(data);
            }

            @Override
            public void onError(String error, int duration) {
                showError(error, duration);
            }

            @Override
            public void onNoReadableData() {
                showError(getString(R.string.string_analyzer_unrecognized_data), RefConstants.ERROR_DURATION_SHORT);
            }
        });


    }

    private void readableDataFound(String data) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_GENERIC_SCAN_DATA, data);
        setResult(HomeActivity.RESULT_CODE_GENERIC_SCAN, intent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE);
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, NfcUtil.IntentFilters(), null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NfcUtil.readTag(this, intent, this::readData);
    }
}
