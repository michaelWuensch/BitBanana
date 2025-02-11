package app.michaelwuensch.bitbanana.listViews.peers;

import android.content.Intent;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseScannerActivity;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.RefConstants;

public class ScanPeerActivity extends BaseScannerActivity {

    public static final String EXTRA_NODE_URI = "EXTRA_NODE_URI";
    private static final String LOG_TAG = ScanPeerActivity.class.getSimpleName();

    @Override
    public void onButtonPasteClick() {
        super.onButtonPasteClick();

        String clipboardContent = ClipBoardUtil.getPrimaryContent(getApplicationContext(), false);
        if (clipboardContent != null)
            processPeerData(clipboardContent);
        else
            showError(getResources().getString(R.string.error_emptyClipboardConnect), RefConstants.ERROR_DURATION_SHORT);
    }

    @Override
    public void onButtonInstructionsHelpClick() {
        HelpDialogUtil.showDialog(ScanPeerActivity.this, R.string.help_dialog_scanPeer);
    }

    @Override
    public void handleCameraResult(String result) {
        super.handleCameraResult(result);

        processPeerData(result);
    }

    private void processPeerData(String rawData) {
        LightningNodeUri nodeUri = LightningNodeUriParser.parseNodeUri(rawData);
        if (nodeUri == null)
            showError(getString(R.string.error_provided_data_invalid) + "\n\n" + getString(R.string.error_invalid_peer_connection_data), 7000);
        else
            finishWithNode(nodeUri);
    }

    private boolean finishWithNode(LightningNodeUri nodeUri) {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_NODE_URI, nodeUri);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        } else {
            showError(getResources().getString(R.string.demo_setupNodeFirst), 5000);
            return false;
        }
    }
}
