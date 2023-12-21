package app.michaelwuensch.bitbanana.listViews.contacts;

import android.content.Intent;
import android.os.Bundle;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseScannerActivity;
import app.michaelwuensch.bitbanana.models.LNAddress;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.LightningNodeUirParser;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;

public class ScanContactActivity extends BaseScannerActivity {

    public static final String EXTRA_NODE_URI = "EXTRA_NODE_URI";
    public static final String EXTRA_LN_ADDRESS = "EXTRA_LN_ADDRESS";
    private static final String LOG_TAG = ScanContactActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setPasteButtonVisibility(false);
    }

    @Override
    public void onButtonPasteClick() {
        super.onButtonPasteClick();

        try {
            String clipboardContent = ClipBoardUtil.getPrimaryContent(getApplicationContext(), false);
            processUserData(clipboardContent);
        } catch (NullPointerException e) {
            showError(getResources().getString(R.string.error_emptyClipboardConnect), RefConstants.ERROR_DURATION_SHORT);
        }
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
        LightningNodeUri nodeUri = LightningNodeUirParser.parseNodeUri(rawData);

        if (nodeUri == null) {
            LNAddress lnAddress = new LNAddress(rawData);
            if (lnAddress.isValid()) {
                return finishWithLNAddress(lnAddress);
            } else {
                showError(getResources().getString(R.string.error_lightning_uri_invalid), RefConstants.ERROR_DURATION_LONG);
                return false;
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

    private boolean finishWithLNAddress(LNAddress lnAddress) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_LN_ADDRESS, lnAddress);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }
}
