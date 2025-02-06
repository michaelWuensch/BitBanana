package app.michaelwuensch.bitbanana.home;

import android.content.Intent;
import android.os.Bundle;

import app.michaelwuensch.bitbanana.baseClasses.BaseScannerActivity;
import app.michaelwuensch.bitbanana.listViews.peers.ScanPeerActivity;

public class ManualSendScanActivity extends BaseScannerActivity {

    public static final int RESULT_CODE_MANUAL_SCAN_INPUT = 101;
    public static final String SCAN_RESULT = "SCAN_RESULT";
    private static final String LOG_TAG = ScanPeerActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setPasteButtonVisibility(false);
        hideScannerInstructions();
    }

    @Override
    public void handleCameraResult(String result) {
        super.handleCameraResult(result);

        Intent intent = new Intent();
        intent.putExtra(SCAN_RESULT, result);
        setResult(RESULT_CODE_MANUAL_SCAN_INPUT, intent);
        finish();
    }
}
