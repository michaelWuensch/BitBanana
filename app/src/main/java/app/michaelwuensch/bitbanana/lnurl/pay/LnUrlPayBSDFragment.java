package app.michaelwuensch.bitbanana.lnurl.pay;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.ExpandableTextView;
import app.michaelwuensch.bitbanana.customView.NumpadView;
import app.michaelwuensch.bitbanana.customView.PaymentCommentView;
import app.michaelwuensch.bitbanana.lnurl.pay.payerData.LnUrlpPayerData;
import app.michaelwuensch.bitbanana.lnurl.pay.payerData.PayerDataView;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.HexUtil;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UtilFunctions;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


public class LnUrlPayBSDFragment extends BaseBSDFragment {

    private static final String LOG_TAG = LnUrlPayBSDFragment.class.getSimpleName();

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private ConstraintLayout mContentTopLayout;
    private View mSendInputsView;
    private EditText mEtAmount;
    private PaymentCommentView mPcvComment;
    private TextView mTvUnit;
    private View mDescriptionView;
    private NumpadView mNumpad;
    private Button mBtnSend;
    private TextView mTvSuccessActionText;
    private TextView mTvPayee;
    private PayerDataView mPayerDataView;

    private long mFixedAmount;
    private boolean mAmountValid = true;
    private long mMinSendable;
    private long mMaxSendable;
    private String mServiceURLString;
    private long mFinalChosenAmount;

    private ExpandableTextView mDescription;

    private Handler mHandler;
    private LnUrlPayResponse mPaymentData;
    private LnUrlpPayerData mPayerData;
    private long mSendAmount;
    private boolean mBlockOnInputChanged;

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

        // Calculate correct min and max withdrawal value for LNURL.
        mMaxSendable = Math.min((mPaymentData.getMaxSendable()), WalletUtil.getMaxLightningSendAmount());
        mMinSendable = mPaymentData.getMinSendable();

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
        mSendInputsView = view.findViewById(R.id.sendInputsView);
        mEtAmount = view.findViewById(R.id.sendAmount);
        mPcvComment = view.findViewById(R.id.paymentComment);
        mTvUnit = view.findViewById(R.id.unit);
        mDescription = view.findViewById(R.id.expandableDescription);
        mDescriptionView = view.findViewById(R.id.sendDescriptionTopLayout);
        mTvPayee = view.findViewById(R.id.sendPayee);
        mNumpad = view.findViewById(R.id.numpadView);
        mBtnSend = view.findViewById(R.id.sendButton);
        mTvSuccessActionText = view.findViewById(R.id.successActionText);
        mPayerDataView = view.findViewById(R.id.payerDataView);

        mBSDScrollableMainView.setTitle(R.string.pay);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mResultView.setOnOkListener(this::dismiss);

        mNumpad.bindEditText(mEtAmount);

        // deactivate default keyboard for number input.
        mEtAmount.setShowSoftInputOnFocus(false);

        // set unit to current primary unit
        mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());

        // Handle comment field
        if (mPaymentData.isCommentAllowed()) {
            mPcvComment.setupCharLimit(mPaymentData.getCommentMaxLength());
        } else {
            mPcvComment.setVisibility(View.GONE);
        }

        // Scroll to comment when focused
        mPcvComment.setOnFocusChangedListener(new PaymentCommentView.onCommentFocusChangedListener() {
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
        }

        mPayerDataView.setOnFieldFocusedListener(new PayerDataView.onFieldFocusedListener() {
            @Override
            public void onFieldFocused(int offset) {
                mBSDScrollableMainView.focusOnView(mPayerDataView, offset);
            }
        });

        // Input validation for the amount field.
        mEtAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {

                // remove the last inputted character if not valid
                if (!mAmountValid) {
                    mNumpad.removeOneDigit();
                    return;
                }

                if (!mEtAmount.getText().toString().equals(".")) {

                    if (mFixedAmount != 0L) {
                        mEtAmount.setTextColor(getResources().getColor(R.color.white));
                        mBtnSend.setEnabled(true);
                        mBtnSend.setTextColor(getResources().getColor(R.color.banana_yellow));
                        return;
                    }

                    // make text red if input is too large or too small
                    if (mSendAmount > mMaxSendable) {
                        mEtAmount.setTextColor(getResources().getColor(R.color.red));
                        String maxAmount = getResources().getString(R.string.max_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mMaxSendable, true);
                        Toast.makeText(getActivity(), maxAmount, Toast.LENGTH_SHORT).show();
                        mBtnSend.setEnabled(false);
                        mBtnSend.setTextColor(getResources().getColor(R.color.gray));
                    } else if (mSendAmount < mMinSendable) {
                        mEtAmount.setTextColor(getResources().getColor(R.color.red));
                        String minAmount = getResources().getString(R.string.min_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mMinSendable, true);
                        Toast.makeText(getActivity(), minAmount, Toast.LENGTH_SHORT).show();
                        mBtnSend.setEnabled(false);
                        mBtnSend.setTextColor(getResources().getColor(R.color.gray));
                    } else {
                        mEtAmount.setTextColor(getResources().getColor(R.color.white));
                        mBtnSend.setEnabled(true);
                        mBtnSend.setTextColor(getResources().getColor(R.color.banana_yellow));
                    }
                    if (mSendAmount == 0) {
                        mBtnSend.setEnabled(false);
                        mBtnSend.setTextColor(getResources().getColor(R.color.gray));
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before,
                                      int count) {
                if (arg0.length() == 0) {
                    // No entered text so will show hint
                    mEtAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                } else {
                    mEtAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                }

                if (mBlockOnInputChanged)
                    return;

                // validate input
                mAmountValid = MonetaryUtil.getInstance().validateCurrentCurrencyInput(arg0.toString(), true);
                if (mAmountValid) {
                    mSendAmount = MonetaryUtil.getInstance().convertCurrentCurrencyTextInputToMsat(arg0.toString());
                }
            }
        });

        if (mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_IDENTIFIER) != null) {
            mTvPayee.setText(mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_IDENTIFIER));
        } else if (mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_EMAIL) != null) {
            mTvPayee.setText(mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_EMAIL));
        } else if (mServiceURLString != null) {
            mTvPayee.setText(mServiceURLString);
        } else {
            mTvPayee.setText(R.string.unknown);
        }

        // Description. If we have a long description we show only that.
        if (mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_LONG_DESCRIPTION) != null) {
            mDescription.setContent(R.string.description, mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_LONG_DESCRIPTION));

        } else if (mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_TEXT) != null) {
            mDescription.setContent(R.string.description, mPaymentData.getMetadataAsString(LnUrlPayResponse.METADATA_TEXT));
        } else {
            mDescriptionView.setVisibility(View.GONE);
        }

        if (mPaymentData.getMinSendable() == mPaymentData.getMaxSendable()) {
            // A specific amount was requested. We are not allowed to change the amount.
            mFixedAmount = mPaymentData.getMaxSendable();
            mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, true));
            mEtAmount.clearFocus();
            mEtAmount.setFocusable(false);
            mEtAmount.setEnabled(false);
        } else {
            // No specific amount was requested. Let User input an amount, but pre fill with maxWithdraw amount.
            mNumpad.setVisibility(View.VISIBLE);
            mSendAmount = mMinSendable;
            mBlockOnInputChanged = true;
            mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mMinSendable, true));
            mBlockOnInputChanged = false;

            mHandler.postDelayed(() -> {
                // We have to call this delayed, as otherwise it will still bring up the softKeyboard
                mEtAmount.requestFocus();
                mEtAmount.setSelection(mEtAmount.getText().length());
            }, 600);
        }


        // Action when clicked on "send"
        mBtnSend.setText(R.string.activity_send);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switchToWithdrawProgressScreen();

                BBLog.v(LOG_TAG, "Lnurl pay initiated...");

                if (mFixedAmount == 0L) {
                    mFinalChosenAmount = mSendAmount;
                } else {
                    mFinalChosenAmount = mFixedAmount;
                }

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
                        .setAmount(mFinalChosenAmount)
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


        // Action when clicked on unit
        if (MonetaryUtil.getInstance().hasMoreThanOneCurrency()) {
            LinearLayout llUnit = view.findViewById(R.id.unitLayout);
            llUnit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBlockOnInputChanged = true;
                    MonetaryUtil.getInstance().switchToNextCurrency();
                    if (mFixedAmount == 0L) {
                        mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mSendAmount, true));
                    } else {
                        mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, true));
                    }
                    mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
                    mEtAmount.setSelection(mEtAmount.getText().length());
                    mBlockOnInputChanged = false;
                }
            });
        } else {
            view.findViewById(R.id.switchUnitImage).setVisibility(View.GONE);
        }

        if (mMinSendable > mMaxSendable) {
            // There is no way the payment can be routed... show an error immediately
            switchToWithdrawProgressScreen();
            switchToFailedScreen(getResources().getString(R.string.lnurl_pay_insufficient_channel_balance));
        }

        return view;
    }

    private void validateSecondResponse(@NonNull String secondPayResponse) {

        BBLog.d(LOG_TAG, "Second pay response: " + secondPayResponse);

        LnUrlPaySecondResponse lnUrlPaySecondResponse = new Gson().fromJson(secondPayResponse, LnUrlPaySecondResponse.class);

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

                String metadataHash;
                if (mPayerData == null) {
                    metadataHash = mPaymentData.getMetadataHash();
                } else {
                    metadataHash = UtilFunctions.sha256Hash(mPaymentData.getMetadata() + mPayerData.getAsJsonString());
                }

                if (decodedBolt11.isExpired()) {
                    // Show error: payment request expired.
                    BBLog.e(LOG_TAG, "LNURL: Payment request expired.");
                    switchToFailedScreen(getString(R.string.lnurl_pay_received_invalid_payment_request, mServiceURLString));
                } else if (decodedBolt11.hasNoAmountSpecified()) {
                    // Disable 0 sat invoices
                    BBLog.e(LOG_TAG, "LNURL: 0 sat payments are not allowed.");
                    switchToFailedScreen(getString(R.string.lnurl_pay_received_invalid_payment_request, mServiceURLString));
                } else if (decodedBolt11.getAmountRequested() != mFinalChosenAmount) {
                    BBLog.e(LOG_TAG, "LNURL: The amount in the payment request is not equal to what you wanted to send.");
                    switchToFailedScreen(getString(R.string.lnurl_pay_received_invalid_payment_request, mServiceURLString));
                } else if (decodedBolt11.getDescriptionHash() == null || !decodedBolt11.getDescriptionHash().equals(metadataHash)) {
                    BBLog.e(LOG_TAG, "LNURL: The hash in the invoice does not match the hash of from the metadata send before.");
                    switchToFailedScreen(getString(R.string.lnurl_pay_received_invalid_payment_request, mServiceURLString));
                } else {
                    SendLnPaymentRequest sendPaymentRequest = PaymentUtil.prepareBolt11InvoicePayment(decodedBolt11, decodedBolt11.getAmountRequested());
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

        mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mFinalChosenAmount, true));

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
}
