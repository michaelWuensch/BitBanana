package app.michaelwuensch.bitbanana.baseClasses;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Size;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.PermissionsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import zxingcpp.BarcodeReader;

public abstract class BaseScannerActivity extends BaseAppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG = BaseScannerActivity.class.getSimpleName();
    protected int mHighlightColor;
    protected int mWhiteColor;
    protected ImageView mScannerInstructionsHelp;
    private ImageButton mBtnFlashlight;
    private ImageButton mBtnGalery;
    private TextView mTvPermissionRequired;
    private BBButton mButtonPaste;
    private BBButton mButtonHelp;

    private Vibrator mVibrator;
    private String mLastScannedText;
    private long mLastScanTimestamp;
    private CameraControl mCameraControl;
    private boolean mIsFlashlightActive;


    private ActivityResultLauncher<Intent> openGalleryRequest =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    Uri uri = result.getData().getData();
                    decodeQRCode(uri);
                }
            });

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        openGalleryRequest.launch(Intent.createChooser(intent, "Scan Gallery"));
    }

    private BarcodeReader.Options getBarcodeReaderOptions() {
        BarcodeReader.Options options = new BarcodeReader.Options();
        Set<BarcodeReader.Format> formats = new HashSet<>();
        formats.add(BarcodeReader.Format.QR_CODE);
        options.setFormats(formats);
        options.setTryInvert(true);
        options.setTryRotate(true);
        options.setMaxNumberOfSymbols(1);
        return options;
    }

    private void decodeQRCode(Uri imageUri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));

            BarcodeReader barcodeReader = new BarcodeReader();
            barcodeReader.setOptions(getBarcodeReaderOptions());
            List<BarcodeReader.Result> resultsList = barcodeReader.read(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), 0);
            if (!resultsList.isEmpty()) {
                handleCameraResult(resultsList.get(0).getText());
                return;
            }
            resultsList = barcodeReader.read(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), 90);
            if (!resultsList.isEmpty()) {
                handleCameraResult(resultsList.get(0).getText());
                return;
            }
            resultsList = barcodeReader.read(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), 180);
            if (!resultsList.isEmpty()) {
                handleCameraResult(resultsList.get(0).getText());
                return;
            }
            resultsList = barcodeReader.read(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), 270);
            if (!resultsList.isEmpty()) {
                handleCameraResult(resultsList.get(0).getText());
                return;
            }
            showError(getString(R.string.error_reading_qrCode_in_image), RefConstants.ERROR_DURATION_SHORT);
        } catch (Exception e) {
            // Handle exceptions (e.g., QR code not found)
            showError(getString(R.string.error_reading_qrCode_in_image), RefConstants.ERROR_DURATION_SHORT);
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_qr_code_scanner);
        setupToolbar();

        View fallbackOverlay = findViewById(R.id.fallbackOverlay);

        mVibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview use case (for the live camera view)
                Preview preview = new Preview.Builder().build();
                PreviewView previewView = findViewById(R.id.barcode_scanner);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis use case (for frame processing)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(
                                new ResolutionSelector.Builder()
                                        .setResolutionStrategy(
                                                new ResolutionStrategy(new Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
                                        )
                                        .build()
                        )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), image -> {
                    BarcodeReader barcodeReader = new BarcodeReader();
                    barcodeReader.setOptions(getBarcodeReaderOptions());
                    List<BarcodeReader.Result> resultsList = barcodeReader.read(image);
                    if (!resultsList.isEmpty()) {
                        runOnUiThread(() -> handleCameraResult(resultsList.get(0).getText()));
                    }

                    image.close(); // MUST close to avoid memory leaks
                });

                // Select back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Bind use cases
                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis
                );

                // Get a cameraControl instance
                mCameraControl = camera.getCameraControl();

                // Toggle background overlay based on streaming state
                previewView.getPreviewStreamState().observe(this, state1 -> {
                    if (state1 == PreviewView.StreamState.STREAMING) {
                        fallbackOverlay.setVisibility(View.GONE);
                    } else {
                        fallbackOverlay.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                BBLog.e(LOG_TAG, "Camera error:: " + e.getMessage());
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

        mTvPermissionRequired = findViewById(R.id.scannerPermissionRequired);

        // Prepare colors
        mHighlightColor = ContextCompat.getColor(this, R.color.banana_yellow);
        mWhiteColor = ContextCompat.getColor(this, R.color.white);

        mButtonPaste = findViewById(R.id.scannerPaste);
        mButtonPaste.setOnClickListener(this);
        mButtonHelp = findViewById(R.id.scannerHelp);
        mButtonHelp.setOnClickListener(this);
        mBtnFlashlight = findViewById(R.id.scannerFlashButton);
        mBtnFlashlight.setOnClickListener(this);
        mBtnGalery = findViewById(R.id.scannerGalleryButton);
        mBtnGalery.setOnClickListener(this);
        mScannerInstructionsHelp = findViewById(R.id.scannerInstructionsHelp);

        if (FeatureManager.isHelpButtonsEnabled()) {
            mScannerInstructionsHelp.setVisibility(View.VISIBLE);
            mScannerInstructionsHelp.setOnClickListener(this);
        } else {
            mScannerInstructionsHelp.setVisibility(View.GONE);
        }

        // if the device does not have flashlight in its camera,
        // then remove the switch flashlight button...
        if (!hasFlash()) {
            mBtnFlashlight.setVisibility(View.GONE);
        }

        checkForCameraPermission();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scannerPaste:
                onButtonPasteClick();
                break;
            case R.id.scannerHelp:
                onButtonHelpClick();
                break;
            case R.id.scannerGalleryButton:
                openGallery();
                break;
            case R.id.scannerFlashButton:
                onButtonFlashClick();
                break;
            case R.id.scannerInstructionsHelp:
                onButtonInstructionsHelpClick();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionsUtil.CAMERA_PERMISSION_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, show the camera view.
                    mTvPermissionRequired.setVisibility(View.GONE);
                    mBtnFlashlight.setEnabled(true);
                } else {
                    // Permission denied, show required permission message.
                    mTvPermissionRequired.setVisibility(View.VISIBLE);
                    mBtnFlashlight.setEnabled(false);
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void checkForCameraPermission() {
        // Check for camera permission
        if (!PermissionsUtil.hasCameraPermission(this)) {
            PermissionsUtil.requestCameraPermission(this, true);
        }
    }

    public void handleCameraResult(String result) {
        if (result == null || result.isEmpty())
            return;
        if (result.equals(mLastScannedText)) {
            if (System.currentTimeMillis() - mLastScanTimestamp < 3000) {
                // prevent scanning the same over and over
                return;
            }
        }
        mVibrator.vibrate(RefConstants.VIBRATE_SHORT);
        mLastScanTimestamp = System.currentTimeMillis();
        mLastScannedText = result;
        BBLog.v(LOG_TAG, "Scanned content: " + result);
    }

    public void onButtonPasteClick() {
        // handled in subclass
    }

    public void onButtonHelpClick() {
        // handled in subclass
    }

    public void onButtonInstructionsHelpClick() {
        // handled in subclass
    }

    public void onButtonFlashClick() {
        if (mIsFlashlightActive) {
            try {
                mCameraControl.enableTorch(false);
                mIsFlashlightActive = false;
                mBtnFlashlight.setImageTintList(ColorStateList.valueOf(mWhiteColor));
            } catch (Exception e) {
                // do nothing
            }
        } else {
            try {
                mCameraControl.enableTorch(true);
                mIsFlashlightActive = true;
                mBtnFlashlight.setImageTintList(ColorStateList.valueOf(mHighlightColor));
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    protected void showButtonHelp() {
        mButtonHelp.setVisibility(View.VISIBLE);
    }

    protected void setPasteButtonVisibility(boolean visible) {
        mButtonPaste.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Check if the device's camera has a Flashlight.
     *
     * @return true if there is Flashlight, otherwise false.
     */
    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void hideScannerInstructions() {
        mScannerInstructionsHelp.setVisibility(View.GONE);
    }
}
