package app.michaelwuensch.bitbanana.peers;

import android.content.Intent;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseScannerActivity;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.NodeConfigsManager;
import app.michaelwuensch.bitbanana.lightning.LightningNodeUri;
import app.michaelwuensch.bitbanana.lightning.LightningParser;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;

public class ScanPeerActivity extends BaseScannerActivity {

    public static final String EXTRA_NODE_URI = "EXTRA_NODE_URI";
    private static final String LOG_TAG = ScanPeerActivity.class.getSimpleName();

    @Override
    public void onButtonPasteClick() {
        super.onButtonPasteClick();

        try {
            String clipboardContent = ClipBoardUtil.getPrimaryContent(getApplicationContext(), false);
            processPeerData(clipboardContent);
        } catch (NullPointerException e) {
            showError(getResources().getString(R.string.error_emptyClipboardConnect), RefConstants.ERROR_DURATION_SHORT);
        }
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

    private boolean processPeerData(String rawData) {
        LightningNodeUri nodeUri = LightningParser.parseNodeUri(rawData);
        return finishWithNode(nodeUri);
    }

    private boolean finishWithNode(LightningNodeUri nodeUri) {
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
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
