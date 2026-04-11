package app.michaelwuensch.bitbanana;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.lnurl.LnUrlReader;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlFinalWithdrawRequest;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.qrCodeGen.QRCodeGenerator;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.BitcoinStringAnalyzer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.NfcUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UriUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


public class GeneratedRequestActivity extends BaseAppCompatActivity implements Wallet_TransactionHistory.InvoiceSubscriptionListener {

    private static final String LOG_TAG = GeneratedRequestActivity.class.getSimpleName();

    private String mDataToEncodeInQRCode;
    private String mDataToCopyOrShare;
    private boolean mOnChain;
    private Bip21Invoice mBip21Invoice;
    private DecodedBolt11 mLnInvoice;
    private ConstraintLayout mClRequestView;
    private ConstraintLayout mClPaymentReceivedView;
    private View mButtonsLayout;
    private View mWithdrawProgressLayout;
    private TextView mFinishedAmount;
    private CompositeDisposable mCompositeDisposable;
    private Vibrator mVibrator;
    private NfcAdapter mNfcAdapter;
    private String mServiceURLString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

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
        mButtonsLayout = findViewById(R.id.buttonsLayout);
        mWithdrawProgressLayout = findViewById(R.id.withdrawProgressLayout);
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

            // Update transaction history so it shows the new invoice when we close the activity.
            if (!BackendManager.getCurrentBackend().supportsEventSubscriptions()) {
                Wallet_TransactionHistory.getInstance().fetchTransactionHistory();
            }
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
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            PendingIntent pendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(
                        this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(
                        this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            }
            if (!mOnChain) {
                mNfcAdapter.enableForegroundDispatch(this, pendingIntent, NfcUtil.IntentFilters(), null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NfcUtil.readTag(this, intent, new NfcUtil.OnNfcResponseListener() {
            @Override
            public void onSuccess(String payload) {
                if (BitcoinStringAnalyzer.isLnUrl(payload)) {
                    setWithdrawalProgressVisibility(true);
                    withdrawFromLnURL(payload);
                } else {
                    setWithdrawalProgressVisibility(false);
                    showError(getResources().getString(R.string.error_nfc_no_withdrawal), RefConstants.ERROR_DURATION_SHORT);
                }
            }
        });
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
        // Check if the invoice was paid
        if (invoice.isPaid()) {
            // The updated invoice is paid, now check if it is the invoice we currently have opened.
            if (invoice.getBolt11().equals(mLnInvoice.getBolt11String())) {
                showPaidScreen(invoice.getAmountPaid());
            }
        }
    }

    private void pollBackendToCheckIfPaid() {
        BBLog.v(LOG_TAG, "Poll backend to check if requested payment is paid.");
        if (mOnChain) {
            // ToDo: Implement checking for on-chain payments
        } else {
            mCompositeDisposable.add(BackendManager.api().getInvoice(mLnInvoice.getPaymentHash())
                    .observeOn(AndroidSchedulers.mainThread())
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

    private void withdrawFromLnURL(String lnurlString) {
        LnUrlReader.readLnUrl(this, lnurlString, new LnUrlReader.OnLnUrlReadListener() {
            @Override
            public void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse) {

                // Extract the URL from the Withdraw service
                try {
                    URL url = new URL(withdrawResponse.getCallback());
                    mServiceURLString = url.getHost();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                if (mLnInvoice.getAmountRequested() > withdrawResponse.getMaxWithdrawable()) {
                    String maxAmount = getResources().getString(R.string.error_withdraw_max_amount, MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(withdrawResponse.getMaxWithdrawable(), true));
                    setWithdrawalProgressVisibility(false);
                    showError(maxAmount, RefConstants.ERROR_DURATION_SHORT);
                } else if (mLnInvoice.getAmountRequested() < withdrawResponse.getMinWithdrawable()) {
                    String minAmount = getResources().getString(R.string.error_withdraw_min_amount, MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(withdrawResponse.getMinWithdrawable(), true));
                    setWithdrawalProgressVisibility(false);
                    showError(minAmount, RefConstants.ERROR_DURATION_SHORT);
                } else if (mLnInvoice.getAmountRequested() > WalletUtil.getMaxLightningReceiveAmount()) {
                    String errorMsg = getString(R.string.error_insufficient_lightning_receive_liquidity, MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(WalletUtil.getMaxLightningReceiveAmount(), true));
                    setWithdrawalProgressVisibility(false);
                    showError(errorMsg, 7000);
                } else {
                    // Forward our invoice to the LNURL service to initiate withdraw.
                    LnUrlFinalWithdrawRequest lnUrlFinalWithdrawRequest = new LnUrlFinalWithdrawRequest.Builder()
                            .setCallback(withdrawResponse.getCallback())
                            .setK1(withdrawResponse.getK1())
                            .setInvoice(mLnInvoice.getBolt11String())
                            .build();

                    okhttp3.Request lnUrlRequest = new Request.Builder()
                            .url(lnUrlFinalWithdrawRequest.requestAsString())
                            .build();

                    HttpClient.getInstance().getClient().newCall(lnUrlRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            if (mServiceURLString != null) {
                                setWithdrawalProgressVisibility(false);
                                showError(getResources().getString(R.string.lnurl_service_not_responding, mServiceURLString), RefConstants.ERROR_DURATION_MEDIUM);
                            } else {
                                setWithdrawalProgressVisibility(false);
                                String host = getResources().getString(R.string.host);
                                showError(getResources().getString(R.string.lnurl_service_not_responding, host), RefConstants.ERROR_DURATION_MEDIUM);
                            }
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            try {
                                validateSecondResponse(response.body().string());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            @Override
            public void onValidLnUrlPay(LnUrlPayResponse payResponse) {
                setWithdrawalProgressVisibility(false);
                showError(getResources().getString(R.string.error_nfc_no_withdrawal), RefConstants.ERROR_DURATION_SHORT);
            }

            @Override
            public void onValidLnUrlChannel(LnUrlChannelResponse channelResponse) {
                setWithdrawalProgressVisibility(false);
                showError(getResources().getString(R.string.error_nfc_no_withdrawal), RefConstants.ERROR_DURATION_SHORT);
            }

            @Override
            public void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse) {
                setWithdrawalProgressVisibility(false);
                showError(getResources().getString(R.string.error_nfc_no_withdrawal), RefConstants.ERROR_DURATION_SHORT);
            }

            @Override
            public void onValidLnUrlAuth(URL url) {
                setWithdrawalProgressVisibility(false);
                showError(getResources().getString(R.string.error_nfc_no_withdrawal), RefConstants.ERROR_DURATION_SHORT);
            }

            @Override
            public void onError(String error, int duration) {
                setWithdrawalProgressVisibility(false);
                showError(error, RefConstants.ERROR_DURATION_MEDIUM);
            }

            @Override
            public void onNoLnUrlData() {
                // This should never be reached...
                setWithdrawalProgressVisibility(false);
                showError(getResources().getString(R.string.error_nfc_no_withdrawal), RefConstants.ERROR_DURATION_SHORT);
            }
        });
    }

    private void validateSecondResponse(@NonNull String withdrawResponse) {
        LnUrlWithdrawResponse lnUrlWithdrawResponse = new Gson().fromJson(withdrawResponse, LnUrlWithdrawResponse.class);

        if (lnUrlWithdrawResponse.getStatus() != null) {
            if (lnUrlWithdrawResponse.getStatus().equals("OK")) {
                showPaidScreen(mLnInvoice.getAmountRequested());
            } else {
                BBLog.d(LOG_TAG, "LNURL: Failed to withdraw. " + lnUrlWithdrawResponse.getReason());
                setWithdrawalProgressVisibility(false);
                showError(lnUrlWithdrawResponse.getReason(), RefConstants.ERROR_DURATION_MEDIUM);
            }
        } else {
            BBLog.d(LOG_TAG, "LNURL: Failed to withdraw. " + withdrawResponse);
            setWithdrawalProgressVisibility(false);
            showError(withdrawResponse, RefConstants.ERROR_DURATION_MEDIUM);
        }
    }

    private void showPaidScreen(long amount) {
        // This has to happen on the UI thread. Only this thread can change the UI.
        runOnUiThread(new Runnable() {
            public void run() {
                // It was paid, show success screen
                mFinishedAmount.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(amount, true));
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
        });
    }

    private void setWithdrawalProgressVisibility(boolean visible) {
        // This has to happen on the UI thread. Only this thread can change the UI.
        runOnUiThread(new Runnable() {
            public void run() {
                mWithdrawProgressLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
                mButtonsLayout.setVisibility(visible ? View.GONE : View.VISIBLE);
            }
        });
    }
}
