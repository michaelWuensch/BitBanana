package app.michaelwuensch.bitbanana.lnurl.pay;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.customView.BBAmountInput;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBExpandableTextInfoBox;
import app.michaelwuensch.bitbanana.customView.BBTextInputBox;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.ClearFocusListener;
import app.michaelwuensch.bitbanana.customView.PickChannelsView;
import app.michaelwuensch.bitbanana.lnurl.pay.payerData.LnUrlpPayerData;
import app.michaelwuensch.bitbanana.lnurl.pay.payerData.PayerDataView;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HexUtil;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


public class LnUrlPayBSDFragment extends BaseBSDFragment implements ClearFocusListener, PickChannelsView.OnPickChannelViewButtonListener {

    private static final String LOG_TAG = LnUrlPayBSDFragment.class.getSimpleName();

    private ActivityResultLauncher<Intent> mActivityResultLauncherSelectChannel;

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private BBExpandableTextInfoBox mPayeeView;
    private BBExpandableTextInfoBox mDescriptionView;
    private BBAmountInput mAmountInput;
    private ConstraintLayout mContentTopLayout;
    private View mSendInputsView;
    private BBTextInputBox mPcvComment;
    private BBButton mBtnSend;
    private TextView mTvSuccessActionText;
    private PayerDataView mPayerDataView;
    private PickChannelsView mPickChannelsView;

    private String mServiceURLString;

    private Handler mHandler;
    private LnUrlPayResponse mPaymentData;
    private LnUrlpPayerData mPayerData;

    public static LnUrlPayBSDFragment createLnUrlPayDialog(LnUrlPayResponse response) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(LnUrlPayResponse.ARGS_KEY, response);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        LnUrlPayBSDFragment lnUrlPayBSDFragment = new LnUrlPayBSDFragment();
        lnUrlPayBSDFragment.setArguments(intent.getExtras());
        return lnUrlPayBSDFragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle args = getArguments();
        mPaymentData = (LnUrlPayResponse) args.getSerializable(LnUrlPayResponse.ARGS_KEY);

        // Extract the URL from the service
        try {
            URL url = new URL(mPaymentData.getCallback());
            mServiceURLString = url.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        mHandler = new Handler();

        View view = inflater.inflate(R.layout.bsd_lnurl_pay, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mProgressView = view.findViewById(R.id.paymentProgressLayout);
        mPayeeView = view.findViewById(R.id.payeeView);
        mDescriptionView = view.findViewById(R.id.descriptionView);
        mAmountInput = view.findViewById(R.id.amountInput);
        mSendInputsView = view.findViewById(R.id.sendInputsView);
        mPcvComment = view.findViewById(R.id.paymentComment);
        mBtnSend = view.findViewById(R.id.sendButton);
        mTvSuccessActionText = view.findViewById(R.id.successActionText);
        mPayerDataView = view.findViewById(R.id.payerDataView);
        mPickChannelsView = view.findViewById(R.id.pickChannels);

        mBSDScrollableMainView.setTitle(R.string.pay);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mResultView.setOnOkListener(this::dismiss);

        // Initialize the ActivityResultLauncher for Channel selection
        mActivityResultLauncherSelectChannel = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        // Pass result to the pick channels view
                        mPickChannelsView.handleActivityResult(data);
                    }
                }
        );

        mPickChannelsView.setActivityResultLauncher(mActivityResultLauncherSelectChannel);
        mPickChannelsView.setClearFocusListener(this);
        mPickChannelsView.setVisibility(FeatureManager.isChannelPickingOnSendEnabled() ? View.VISIBLE : View.GONE);
        mPickChannelsView.setPickChannelsViewButtonListener(this);
        mPickChannelsView.setLastHopEnabled(false);

        // Handle comment field
        if (mPaymentData.isCommentAllowed()) {
            mPcvComment.setupCharLimit(mPaymentData.getCommentMaxLength());
        } else {
            mPcvComment.setVisibility(View.GONE);
        }

        // Scroll to comment when focused
        mPcvComment.setOnFocusChangedListener(new BBTextInputBox.onCommentFocusChangedListener() {
            @Override
            public void onFocusChanged(View view, boolean b) {
                if (b)
                    mBSDScrollableMainView.focusOnView(mPcvComment, 0);
            }
        });

        // Handle payer data view
        if (mPaymentData.requestsPayerData()) {
            mPayerDataView.setupView(mPaymentData.getRequestedPayerData(), getActivity());
            mPayerDataView.setVisibility(View.VISIBLE);
            if (mPaymentData.getRequestedPayerData().isAuthMandatory()) {
                // We do not support that yet
                switchToFailedScreen(getContext().getString(R.string.lnurl_payer_data_not_supported));
            }
        } else {
            mPayerDataView.setVisibility(View.GONE);
        }

        mPayerDataView.setOnFieldFocusedListener(new PayerDataView.onFieldFocusedListener() {
            @Override
            public void onFieldFocused(int offset) {
                mBSDScrollableMainView.focusOnView(mPayerDataView, offset);
            }
        });

        mPayeeView.setClearFocusListener(this);
        mDescriptionView.setClearFocusListener(this);

        mAmountInput.setupView();
        mAmountInput.setSendAllEnabled(false);
        mAmountInput.setOnChain(false);
        mAmountInput.setOnAmountInputActionListener(new BBAmountInput.OnAmountInputActionListener() {
            @Override
            public boolean onAfterTextChanged(String newText, long amount, boolean isFixedAmount, boolean isOnChain) {
                if (newText.equals(".") || amount == 0)
                    return false;

                if (isFixedAmount) {
                    return true;
                }

                if (amount > mPaymentData.getMaxSendable()) {
                    String maxAmount = getResources().getString(R.string.max_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mPaymentData.getMaxSendable(), true);
                    showError(maxAmount, 2000);
                    return false;
                } else if (amount < mPaymentData.getMinSendable()) {
                    String minAmount = getResources().getString(R.string.min_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mPaymentData.getMinSendable(), true);
                    showError(minAmount, 2000);
                    return false;
                } else {
                    long maxSendable = WalletUtil.getMaxLightningSendAmount();
                    if (amount > maxSendable) {
                        String message = getResources().getString(R.string.error_insufficient_lightning_sending_liquidity) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(maxSendable, true);
                        showError(message, 6000);
                        return false;
                    } else {
                        return true;
                    }
                }
            }

            @Override
            public void onInputValidityChanged(boolean valid) {
                mBtnSend.setButtonEnabled(valid);
            }

            @Override
            public void onInputChanged(boolean valid) {

            }

            @Override
            public void onSendAllCheckboxChanged(boolean checked) {

            }

            @Override
            public void onError(String message, int duration) {
                showError(message, duration);
            }
        });


        if (mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_IDENTIFIER) != null) {
            mPayeeView.setContent(mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_IDENTIFIER));
        } else if (mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_EMAIL) != null) {
            mPayeeView.setContent(mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_EMAIL));
        } else if (mServiceURLString != null) {
            mPayeeView.setContent(mServiceURLString);
        } else {
            mPayeeView.setContent(R.string.unknown);
        }

        // Description. If we have a long description we show only that.
        if (mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_LONG_DESCRIPTION) != null) {
            mDescriptionView.setContent(mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_LONG_DESCRIPTION));

        } else if (mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_TEXT) != null) {
            mDescriptionView.setContent(mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_TEXT));
        } else {
            mDescriptionView.setVisibility(View.GONE);
        }

        if (mPaymentData.getMinSendable() == mPaymentData.getMaxSendable()) {
            // A specific amount was requested. We are not allowed to change the amount.
            mAmountInput.setFixedAmount(mPaymentData.getMaxSendable());
        } else {
            // No specific amount was requested. Let User input an amount, but pre fill with maxWithdraw amount.
            mAmountInput.setAmount(mPaymentData.getMinSendable());
            mAmountInput.selectText();
        }


        // Action when clicked on "send"
        mBtnSend.setText(getResources().getString(R.string.activity_send));
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switchToWithdrawProgressScreen();

                BBLog.v(LOG_TAG, "Lnurl pay initiated...");

                // Gather Payer Data
                if (mPaymentData.requestsPayerData()) {
                    mPayerData = mPayerDataView.getData();
                    if (mPayerData.isEmpty()) {
                        BBLog.v(LOG_TAG, "Payer identification data was empty.");
                        mPayerData = null;
                    }
                } else {
                    mPayerData = null;
                }

                // Create send request
                LnUrlSecondPayRequest lnUrlSecondPayRequest = new LnUrlSecondPayRequest.Builder()
                        .setCallback(mPaymentData.getCallback())
                        .setAmount(mAmountInput.getAmount())
                        .setComment(mPcvComment.getData())
                        .setPayerData(mPayerData)
                        .build();

                BBLog.d(LOG_TAG, "Sent following request to service: " + lnUrlSecondPayRequest.requestAsString());


                okhttp3.Request lnUrlRequest = new Request.Builder()
                        .url(lnUrlSecondPayRequest.requestAsString())
                        .build();

                HttpClient.getInstance().getClient().newCall(lnUrlRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        if (mServiceURLString != null) {
                            switchToFailedScreen(getResources().getString(R.string.lnurl_service_not_responding, mServiceURLString));
                        } else {
                            String host = getResources().getString(R.string.host);
                            switchToFailedScreen(getResources().getString(R.string.lnurl_service_not_responding, host));
                        }
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try {
                            String responseContent = response.body().string();
                            validateSecondResponse(responseContent);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        if (mPaymentData.getMinSendable() > WalletUtil.getMaxLightningSendAmount()) {
            // There is no way the payment can be routed... show an error immediately
            switchToWithdrawProgressScreen();
            switchToFailedScreen(getResources().getString(R.string.lnurl_pay_insufficient_channel_balance));
        }

        return view;
    }

    private void validateSecondResponse(@NonNull String secondPayResponse) {

        BBLog.d(LOG_TAG, "Second pay response: " + secondPayResponse);

        LnUrlPaySecondResponse lnUrlPaySecondResponse = null;
        try {
            lnUrlPaySecondResponse = new Gson().fromJson(secondPayResponse, LnUrlPaySecondResponse.class);
        } catch (Exception e) {
            showError(secondPayResponse, 7000);
        }

        if (lnUrlPaySecondResponse == null) {
            switchToFailedScreen(getResources().getString(R.string.lnurl_invalid_response));
            return;
        }

        if (lnUrlPaySecondResponse.hasError()) {
            BBLog.d(LOG_TAG, "LNURL: Failed to pay. " + lnUrlPaySecondResponse.getReason());
            switchToFailedScreen(lnUrlPaySecondResponse.getReason());
        } else {
            try {
                DecodedBolt11 decodedBolt11 = InvoiceUtil.decodeBolt11(lnUrlPaySecondResponse.getPaymentRequest());
                BBLog.v(LOG_TAG, decodedBolt11.toString());

                if (decodedBolt11.isExpired()) {
                    // Show error: payment request expired.
                    BBLog.e(LOG_TAG, "LNURL: Payment request expired.");
                    switchToFailedScreen(getString(R.string.lnurl_pay_received_invalid_payment_request, mServiceURLString));
                } else if (decodedBolt11.hasNoAmountSpecified()) {
                    // Disable 0 sat invoices
                    BBLog.e(LOG_TAG, "LNURL: 0 sat payments are not allowed.");
                    switchToFailedScreen(getString(R.string.lnurl_pay_received_invalid_payment_request, mServiceURLString));
                } else if (decodedBolt11.getAmountRequested() != mAmountInput.getAmount()) {
                    BBLog.e(LOG_TAG, "LNURL: The amount in the payment request is not equal to what you wanted to send.");
                    switchToFailedScreen(getString(R.string.lnurl_pay_received_invalid_payment_request, mServiceURLString));
                } else {
                    SendLnPaymentRequest sendPaymentRequest = PaymentUtil.prepareBolt11InvoicePayment(decodedBolt11, decodedBolt11.getAmountRequested(), mPickChannelsView.getFirstHop(), mPickChannelsView.getLastHopPubkey(), -1);
                    BBLog.d(LOG_TAG, "The received invoice was validated successfully.");
                    sendPayment(lnUrlPaySecondResponse.getSuccessAction(), sendPaymentRequest);
                }
            } catch (Exception e) {
                // If we can't decode the bolt11 invoice, show the error the library throws (always english)
                switchToFailedScreen(e.getMessage());
                BBLog.e(LOG_TAG, e.getMessage());
            }
        }
    }


    private void sendPayment(LnUrlPaySuccessAction successAction, SendLnPaymentRequest sendPaymentRequest) {

        BBLog.d(LOG_TAG, "Trying to send lightning payment...");

        PaymentUtil.sendLnPayment(sendPaymentRequest, getCompositeDisposable(), new PaymentUtil.OnPaymentResult() {
            @Override
            public void onSuccess(SendLnPaymentResponse sendLnPaymentResponse) {
                mHandler.postDelayed(() -> executeSuccessAction(successAction, sendLnPaymentResponse), 300);
                if (!BackendManager.getCurrentBackend().supportsEventSubscriptions()) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // We delay it as the node might not return the correct results if we call this to early (CoreLightning)
                            Wallet_Balance.getInstance().fetchBalances();
                            Wallet_TransactionHistory.getInstance().fetchTransactionHistory();
                        }
                    }, 500);
                }
            }

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onError(String error, SendLnPaymentResponse sendLnPaymentResponse, int duration) {
                String errorPrefix = getResources().getString(R.string.error).toUpperCase() + ":";
                String errorMessage;
                if (sendLnPaymentResponse != null) {
                    switch (sendLnPaymentResponse.getFailureReason()) {
                        case TIMEOUT:
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_timeout);
                            break;
                        case NO_ROUTE:
                            if (sendLnPaymentResponse.getAmount() > 0 && sendLnPaymentResponse.getAmount() < 1000L)
                                errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_no_route_small_amount);
                            else
                                errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_no_route, PaymentUtil.getRelativeSettingsFeeLimit() * 100);
                            break;
                        case INSUFFICIENT_FUNDS:
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_insufficient_balance);
                            break;
                        case INCORRECT_PAYMENT_DETAILS:
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_invalid_details);
                            break;
                        case CANCELED:
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_canceled);
                            break;
                        default:
                            errorMessage = errorPrefix + "\n\n" + error;
                            break;
                    }
                } else {
                    errorMessage = errorPrefix + "\n\n" + error;
                }
                mHandler.postDelayed(() -> switchToFailedScreen(errorMessage), 300);
            }
        });
    }


    private void executeSuccessAction(LnUrlPaySuccessAction successAction, SendLnPaymentResponse sendLnPaymentResponse) {

        mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mAmountInput.getAmount(), true));

        if (successAction == null) {
            BBLog.d(LOG_TAG, "No Success action.");
            mTvSuccessActionText.setVisibility(View.GONE);
        } else if (successAction.isMessage()) {
            BBLog.d(LOG_TAG, "SuccessAction: Message: " + successAction.getMessage());
            mTvSuccessActionText.setText(successAction.getMessage());
        } else if (successAction.isUrl()) {
            BBLog.d(LOG_TAG, "SuccessAction: Url: " + successAction.getUrl());
            mTvSuccessActionText.setVisibility(View.GONE);

            ClipBoardUtil.copyToClipboard(getActivity(), "URL", successAction.getUrl());
            String message = successAction.getDescription() + "\n\n" + successAction.getUrl() + "\n";
            LayoutInflater adbInflater = LayoutInflater.from(getActivity());
            View titleView = adbInflater.inflate(R.layout.dialog_warning_header, null);
            ((TextView) titleView.findViewById(R.id.warningMessage)).setText(R.string.lnurl_pay_save_url);
            AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                    .setMessage(message)
                    .setCustomTitle(titleView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Call the url
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(successAction.getUrl()));
                            getActivity().startActivity(browserIntent);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            Dialog dlg = adb.create();
            // Apply FLAG_SECURE to dialog to prevent screen recording
            if (PrefsUtil.isScreenRecordingPrevented()) {
                dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
            dlg.show();
        } else if (successAction.isAes()) {
            // Decrypt ciphertext with payment preimage
            BBLog.d(LOG_TAG, "SuccessAction: Aes.");
            try {
                String decrypted = decrypt(successAction.getCiphertext(), HexUtil.hexToBytes(sendLnPaymentResponse.getPaymentPreimage()), successAction.getIv());
                BBLog.d(LOG_TAG, "Decrypted secret is: " + decrypted);
                mTvSuccessActionText.setVisibility(View.GONE);

                ClipBoardUtil.copyToClipboard(getActivity(), "Code", decrypted);
                String message = successAction.getDescription() + "\n\n" + decrypted + "\n";
                LayoutInflater adbInflater = LayoutInflater.from(getActivity());
                View titleView = adbInflater.inflate(R.layout.dialog_warning_header, null);
                ((TextView) titleView.findViewById(R.id.warningMessage)).setText(R.string.lnurl_pay_save_secret);
                AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                        .setMessage(message)
                        .setCustomTitle(titleView)
                        .setCancelable(false)
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

            } catch (Exception e) {
                e.printStackTrace();
                BBLog.e(LOG_TAG, "Decryption error!");
                mTvSuccessActionText.setText(R.string.lnurl_pay_success_secret_decrypt_error);
            }
        } else {
            BBLog.d(LOG_TAG, "Success action not supported.");
            mTvSuccessActionText.setVisibility(View.GONE);
        }
        switchToSuccessScreen();
    }


    public static String decrypt(String textToDecrypt, byte[] key, String iv) throws Exception {
        final String initializationVector = "8119745113154120";
        byte[] encrypted_bytes = Base64.decode(textToDecrypt, Base64.DEFAULT);
        byte[] iv_bytes = Base64.decode(iv, Base64.DEFAULT);
        SecretKeySpec sKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, sKeySpec, new IvParameterSpec(iv_bytes));
        byte[] decrypted = cipher.doFinal(encrypted_bytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }


    @Override
    public void onDestroyView() {
        mHandler.removeCallbacksAndMessages(null);

        super.onDestroyView();
    }

    private void switchToWithdrawProgressScreen() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Code to be executed on the main thread
                mProgressView.setVisibility(View.VISIBLE);
                mSendInputsView.setVisibility(View.INVISIBLE);
                mProgressView.startSpinning();
                mBSDScrollableMainView.animateTitleOut();
            }
        });
    }

    private void switchToSuccessScreen() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Code to be executed on the main thread
                mProgressView.spinningFinished(true);
                TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
                mSendInputsView.setVisibility(View.GONE);
                mResultView.setVisibility(View.VISIBLE);
                mResultView.setHeading(R.string.send_success, true);
            }
        });
    }

    private void switchToFailedScreen(String error) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Code to be executed on the main thread
                mProgressView.spinningFinished(false);
                TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
                mSendInputsView.setVisibility(View.GONE);
                mResultView.setVisibility(View.VISIBLE);

                // Set failed states
                mResultView.setHeading(R.string.send_fail, false);
                mResultView.setDetailsText(error);
            }
        });
    }

    @Override
    public void onClearFocus() {
        mAmountInput.clearFocus();
    }

    @Override
    public long onSelectChannelClicked() {
        return mAmountInput.getAmount();
    }

    @Override
    public void onResetPickedChannelClicked() {

    }
}
