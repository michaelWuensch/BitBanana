package app.michaelwuensch.bitbanana;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.qrCodeGen.QRCodeGenerator;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UriUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;
import io.reactivex.rxjava3.disposables.CompositeDisposable;


public class GeneratedRequestActivity extends BaseAppCompatActivity implements Wallet_TransactionHistory.InvoiceSubscriptionListener {

    private static final String LOG_TAG = GeneratedRequestActivity.class.getSimpleName();

    private String mDataToEncodeInQRCode;
    private String mDataToCopyOrShare;
    private boolean mOnChain;
    private Bip21Invoice mBip21Invoice;
    private DecodedBolt11 mLnInvoice;
    private ConstraintLayout mClRequestView;
    private ConstraintLayout mClPaymentReceivedView;
    private TextView mFinishedAmount;
    private CompositeDisposable mCompositeDisposable;
    private Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mOnChain = extras.getBoolean("onChain");
            mBip21Invoice = (Bip21Invoice) extras.getSerializable("bip21Invoice");
            mLnInvoice = (DecodedBolt11) extras.getSerializable("lnInvoice");
        }

        mCompositeDisposable = new CompositeDisposable();
        mVibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

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
            mDataToEncodeInQRCode = mBip21Invoice.toString();
            if (mBip21Invoice.getAmount() == 0 && (mBip21Invoice.getMessage() == null || mBip21Invoice.getMessage().isEmpty())) {
                mDataToCopyOrShare = mBip21Invoice.getAddress();
            } else {
                mDataToCopyOrShare = mDataToEncodeInQRCode;
            }

        } else {
            // Generate lightning request data to encode
            mDataToEncodeInQRCode = UriUtil.generateLightningUri(mLnInvoice.getBolt11String());
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
                new UserGuardian(GeneratedRequestActivity.this, new UserGuardian.OnGuardianConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        ClipBoardUtil.copyToClipboard(getApplicationContext(), "Request", mDataToCopyOrShare);
                    }

                    @Override
                    public void onCancelled() {

                    }
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

        if (!BackendManager.getCurrentBackend().supportsEventSubscriptions())
            pollBackendToCheckIfPaid();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();

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
                // Check if the invoice was paid
                if (invoice.isPaid()) {
                    // The updated invoice is paid, now check if it is the invoice we currently have opened.
                    if (invoice.getBolt11().equals(mLnInvoice.getBolt11String())) {
                        showPaidScreen(invoice.getAmountPaid());
                    }
                }
            }
        });
    }

    private void pollBackendToCheckIfPaid() {
        BBLog.v(LOG_TAG, "Poll backend to check if requested payment is paid.");
        if (mOnChain) {
            // ToDo: Implement checking for on-chain payments
        } else {
            mCompositeDisposable.add(BackendManager.api().getInvoice(mLnInvoice.getPaymentHash())
                    .delaySubscription(500, TimeUnit.MILLISECONDS) // Delay is important to prevent endless loop if it fails.
                    .subscribe(response -> {
                        if (response == null) {
                            BBLog.i(LOG_TAG, "The requested invoice does not exist.");
                            pollBackendToCheckIfPaid();
                            return;
                        }
                        if (response.isPaid())
                            showPaidScreen(response.getAmountPaid());
                        else
                            pollBackendToCheckIfPaid();
                    }, throwable -> {
                        BBLog.e(LOG_TAG, "Error fetching invoice request: " + throwable.getMessage());
                        pollBackendToCheckIfPaid();
                    }));
        }
    }

    private void showPaidScreen(long amount) {
        // It was paid, show success screen
        mFinishedAmount.setText(MonetaryUtil.getInstance().getPrimaryDisplayStringFromMSats(amount, true));
        mClPaymentReceivedView.setVisibility(View.VISIBLE);
        mClRequestView.setVisibility(View.GONE);
        mVibrator.vibrate(RefConstants.VIBRATE_LONG);
        if (!BackendManager.getCurrentBackend().supportsEventSubscriptions()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // We delay it as the node might not return the correct results if we call this to early (CoreLightning)
                    Wallet_Balance.getInstance().fetchBalances();
                    Wallet_TransactionHistory.getInstance().fetchTransactionHistory();
                }
            }, 250);
        }
    }
}
