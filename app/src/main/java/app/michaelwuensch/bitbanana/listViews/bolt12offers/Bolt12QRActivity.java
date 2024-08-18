package app.michaelwuensch.bitbanana.listViews.bolt12offers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.constraintlayout.utils.widget.ImageFilterView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.qrCodeGen.QRCodeGenerator;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Bolt12QRActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = Bolt12QRActivity.class.getSimpleName();

    private String mDataToEncodeInQRCode;
    private String mDataToCopyOrShare;
    private Bolt12Offer mBolt12Offer;
    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mBolt12Offer = (Bolt12Offer) extras.getSerializable("bolt12offer");
        }

        mCompositeDisposable = new CompositeDisposable();

        setContentView(R.layout.activity_bolt12_qr);

        // Generate lightning request data to encode
        mDataToEncodeInQRCode = UriUtil.generateLightningUri(mBolt12Offer.getDecodedBolt12().getBolt12String());
        mDataToCopyOrShare = mDataToEncodeInQRCode;

        // Generate "QR-Code"
        Bitmap bmpQRCode = QRCodeGenerator.bitmapFromText(mDataToEncodeInQRCode, 750);
        ImageFilterView ivQRCode = findViewById(R.id.QRCode);
        ivQRCode.setImageBitmap(bmpQRCode);

        // Action when long clicked on "QR-Code"
        ivQRCode.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDetails();
                return false;
            }
        });


        // Action when clicked on "share"
        View btnShare = findViewById(R.id.shareBtn);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, mDataToCopyOrShare);
                shareIntent.setType("text/plain");
                String title = getResources().getString(R.string.shareDialogTitle);
                startActivity(Intent.createChooser(shareIntent, title));
            }
        });

        // Action when clicked on "details"
        Button btnDetails = findViewById(R.id.detailsButton);
        btnDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetails();
            }
        });

        // Action when clicked on "copy"
        View btnCopyLink = findViewById(R.id.copyBtn);
        btnCopyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ask user to confirm risks about clipboard manipulation
                new UserGuardian(Bolt12QRActivity.this, new UserGuardian.OnGuardianConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        ClipBoardUtil.copyToClipboard(getApplicationContext(), "Payment Code", mDataToCopyOrShare);
                    }

                    @Override
                    public void onCancelled() {

                    }
                }).securityCopyToClipboard(mDataToCopyOrShare, UserGuardian.CLIPBOARD_DATA_TYPE_LIGHTNING);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    private void showDetails() {
        AlertDialog.Builder adb = new AlertDialog.Builder(Bolt12QRActivity.this)
                .setTitle(R.string.details)
                .setMessage(mDataToEncodeInQRCode)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        Dialog dlg = adb.create();
        // Apply FLAG_SECURE to dialog to prevent screen recording
        if (PrefsUtil.isScreenRecordingPrevented()) {
            if (dlg.getWindow() != null)
                dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        dlg.show();
    }
}
