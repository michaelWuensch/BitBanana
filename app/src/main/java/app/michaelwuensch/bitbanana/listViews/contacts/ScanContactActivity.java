package app.michaelwuensch.bitbanana.listViews.contacts;

import android.content.Intent;
import android.os.Bundle;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseScannerActivity;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnAddress;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.RefConstants;

public class ScanContactActivity extends BaseScannerActivity {

    public static final String EXTRA_NODE_URI = "EXTRA_NODE_URI";
    public static final String EXTRA_LN_ADDRESS = "EXTRA_LN_ADDRESS";
    public static final String EXTRA_BOLT12_OFFER = "EXTRA_BOLT12_OFFER";
    private static final String LOG_TAG = ScanContactActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setPasteButtonVisibility(false);
    }

    @Override
    public void onButtonPasteClick() {
        super.onButtonPasteClick();

        String clipboardContent = ClipBoardUtil.getPrimaryContent(getApplicationContext(), false);
        if (clipboardContent != null)
            processUserData(clipboardContent);
        else
            showError(getResources().getString(R.string.error_emptyClipboardConnect), RefConstants.ERROR_DURATION_SHORT);
    }

    @Override
    public void onButtonInstructionsHelpClick() {
        HelpDialogUtil.showDialog(ScanContactActivity.this, R.string.help_dialog_scanContact);
    }

    @Override
    public void handleCameraResult(String result) {
        super.handleCameraResult(result);

        processUserData(result);
    }

    private boolean processUserData(String rawData) {
        LightningNodeUri nodeUri = LightningNodeUriParser.parseNodeUri(rawData);

        if (nodeUri == null) {
            LnAddress lnAddress = new LnAddress(rawData);
            if (lnAddress.isValidLnurlAddress() || lnAddress.isValidBip353DnsRecordAddress()) {
                return finishWithLNAddress(lnAddress);
            } else {
                try {
                    DecodedBolt12 offer = InvoiceUtil.decodeBolt12(rawData);
                    return finishWithBolt12Offer(offer);
                } catch (Exception e) {
                    showError(getResources().getString(R.string.error_invalid_contact_data), RefConstants.ERROR_DURATION_LONG);
                    return false;
                }
            }
        } else {
            return finishWithNode(nodeUri);
        }
    }

    private boolean finishWithNode(LightningNodeUri nodeUri) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NODE_URI, nodeUri);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    private boolean finishWithLNAddress(LnAddress lnAddress) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_LN_ADDRESS, lnAddress);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    private boolean finishWithBolt12Offer(DecodedBolt12 decodedBolt12) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BOLT12_OFFER, decodedBolt12);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }
}
