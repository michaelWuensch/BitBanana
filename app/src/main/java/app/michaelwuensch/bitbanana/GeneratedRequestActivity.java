package app.michaelwuensch.bitbanana;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.constraintlayout.widget.ConstraintLayout;

import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.qrCodeGen.QRCodeGenerator;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;


public class GeneratedRequestActivity extends BaseAppCompatActivity implements Wallet_TransactionHistory.InvoiceSubscriptionListener {

    private static final String LOG_TAG = GeneratedRequestActivity.class.getSimpleName();

    private String mDataToEncodeInQRCode;
    private String mDataToCopyOrShare;
    private boolean mOnChain;
    private String mAddress;
    private String mMemo;
    private String mAmount;
    private String mLnInvoice;
    private ConstraintLayout mClRequestView;
    private ConstraintLayout mClPaymentReceivedView;
    private TextView mFinishedAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mOnChain = extras.getBoolean("onChain");
            mAddress = extras.getString("address");
            mAmount = extras.getString("amount");
            mMemo = extras.getString("memo");
            mLnInvoice = extras.getString("lnInvoice");
        }

        setContentView(R.layout.activity_generate_request);


        // Register listeners
        Wallet_TransactionHistory.getInstance().registerInvoiceSubscriptionListener(this);

        mClRequestView = findViewById(R.id.requestView);
        mClPaymentReceivedView = findViewById(R.id.paymentReceivedView);
        mFinishedAmount = findViewById(R.id.finishedText2);
        mClPaymentReceivedView.setVisibility(View.GONE);


        if (mOnChain) {
            // Show "On Chain" at top
            ImageView ivTypeIcon = findViewById(R.id.requestTypeIcon);
            ivTypeIcon.setImageResource(R.drawable.ic_onchain_black_24dp);
            TextView tvTypeText = findViewById(R.id.requestTypeText);
            tvTypeText.setText(R.string.onChain);

            // Set the icon for the request payed screen
            ImageView ivTypeIcon2 = findViewById(R.id.finishedPaymentTypeIcon);
            ivTypeIcon2.setImageResource(R.drawable.ic_onchain_black_24dp);


            // Generate on-chain request data to encode
            mDataToEncodeInQRCode = InvoiceUtil.generateBitcoinInvoice(mAddress, mAmount, mMemo, null);
            if ((mAmount.isEmpty() || mAmount.equals("") || mAmount.equals("0")) && (mMemo.isEmpty() || mMemo.equals(""))) {
                mDataToCopyOrShare = mAddress;
            } else {
                mDataToCopyOrShare = mDataToEncodeInQRCode;
            }

        } else {
            // Generate lightning request data to encode
            mDataToEncodeInQRCode = UriUtil.generateLightningUri(mLnInvoice);
            mDataToCopyOrShare = mDataToEncodeInQRCode;
        }


        // Generate "QR-Code"
        Bitmap bmpQRCode = QRCodeGenerator.bitmapFromText(mDataToEncodeInQRCode, 750);
        ImageFilterView ivQRCode = findViewById(R.id.requestQRCode);
        ivQRCode.setImageBitmap(bmpQRCode);

        // Action when long clicked on "QR-Code"
        ivQRCode.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder adb = new AlertDialog.Builder(GeneratedRequestActivity.this)
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
                    dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
                dlg.show();
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
        Button btnDetails = findViewById(R.id.requestDetailsButton);
        btnDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder adb = new AlertDialog.Builder(GeneratedRequestActivity.this)
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
                    dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
                dlg.show();
            }
        });

        // Action when clicked on "copy"
        View btnCopyLink = findViewById(R.id.copyBtn);
        btnCopyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ask user to confirm risks about clipboard manipulation
                new UserGuardian(GeneratedRequestActivity.this, () -> {
                    // Copy data to clipboard
                    ClipBoardUtil.copyToClipboard(getApplicationContext(), "Request", mDataToCopyOrShare);
                }).securityCopyToClipboard(mDataToCopyOrShare, mOnChain ? UserGuardian.CLIPBOARD_DATA_TYPE_ONCHAIN : UserGuardian.CLIPBOARD_DATA_TYPE_LIGHTNING);
            }
        });

        // Action when clicked on "ok" Button
        Button btnOk = findViewById(R.id.okButton);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister listeners
        Wallet_TransactionHistory.getInstance().unregisterInvoiceSubscriptionListener(this);
    }

    @Override
    public void onNewInvoiceAdded(LnInvoice invoice) {

    }

    @Override
    public void onExistingInvoiceUpdated(LnInvoice invoice) {
        // This has to happen on the UI thread. Only this thread can change the UI.
        runOnUiThread(new Runnable() {
            public void run() {
                // Check if the invoice was payed
                if (invoice.isPaid()) {
                    // The updated invoice is payed, now check if it is the invoice we currently have opened.
                    if (invoice.getBolt11().equals(mLnInvoice)) {

                        // It was payed, show success screen
                        mFinishedAmount.setText(MonetaryUtil.getInstance().getPrimaryDisplayStringFromMSats(invoice.getAmountPaid()));
                        mClPaymentReceivedView.setVisibility(View.VISIBLE);
                        mClRequestView.setVisibility(View.GONE);
                    }
                }
            }
        });
    }
}
