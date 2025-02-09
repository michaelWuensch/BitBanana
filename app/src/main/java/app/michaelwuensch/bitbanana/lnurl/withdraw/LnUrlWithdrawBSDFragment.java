package app.michaelwuensch.bitbanana.lnurl.withdraw;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.ExpandableTextView;
import app.michaelwuensch.bitbanana.customView.NumpadView;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


public class LnUrlWithdrawBSDFragment extends BaseBSDFragment {

    private static final String LOG_TAG = LnUrlWithdrawBSDFragment.class.getSimpleName();

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private ConstraintLayout mContentTopLayout;
    private View mWithdrawInputs;
    private EditText mEtAmount;
    private ExpandableTextView mExtvDescription;
    private TextView mTvUnit;
    private NumpadView mNumpad;
    private BBButton mBtnWithdraw;
    private TextView mTvWithdrawSource;

    private long mFixedAmount;
    private boolean mAmountValid = true;
    private long mMinWithdrawable;
    private long mMaxWithdrawable;
    private String mServiceURLString;
    private Handler mHandler;
    private LnUrlWithdrawResponse mWithdrawData;
    private long mWithdrawAmount;
    private boolean mBlockOnInputChanged;

    public static LnUrlWithdrawBSDFragment createWithdrawDialog(LnUrlWithdrawResponse response) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(LnUrlWithdrawResponse.ARGS_KEY, response);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        LnUrlWithdrawBSDFragment lnUrlWithdrawBSDFragment = new LnUrlWithdrawBSDFragment();
        lnUrlWithdrawBSDFragment.setArguments(intent.getExtras());
        return lnUrlWithdrawBSDFragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle args = getArguments();
        mWithdrawData = (LnUrlWithdrawResponse) args.getSerializable(LnUrlWithdrawResponse.ARGS_KEY);

        // Calculate correct min and max withdrawal value for LNURL.
        mMaxWithdrawable = Math.min((mWithdrawData.getMaxWithdrawable()), WalletUtil.getMaxLightningReceiveAmount());
        mMinWithdrawable = mWithdrawData.getMinWithdrawable();

        // Extract the URL from the Withdraw service
        try {
            URL url = new URL(mWithdrawData.getCallback());
            mServiceURLString = url.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        mHandler = new Handler();

        View view = inflater.inflate(R.layout.bsd_lnurl_withdraw, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mProgressView = view.findViewById(R.id.paymentProgressLayout);

        mWithdrawInputs = view.findViewById(R.id.withdrawInputsView);
        mEtAmount = view.findViewById(R.id.withdrawAmount);
        mTvUnit = view.findViewById(R.id.unit);
        mExtvDescription = view.findViewById(R.id.withdrawDescription);
        mTvWithdrawSource = view.findViewById(R.id.withdrawSource);

        mNumpad = view.findViewById(R.id.numpadView);
        mBtnWithdraw = view.findViewById(R.id.withdrawButton);


        mBSDScrollableMainView.setTitle(R.string.withdraw);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mResultView.setOnOkListener(this::dismiss);

        mNumpad.bindEditText(mEtAmount);

        // deactivate default keyboard for number input.
        mEtAmount.setShowSoftInputOnFocus(false);

        // set unit to current primary unit
        mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());

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
                        mBtnWithdraw.setButtonEnabled(true);
                        return;
                    }

                    // make text red if input is too large or too small
                    if (mWithdrawAmount > mMaxWithdrawable) {
                        mEtAmount.setTextColor(getResources().getColor(R.color.red));
                        String maxAmount = getResources().getString(R.string.max_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mMaxWithdrawable, true);
                        Toast.makeText(getActivity(), maxAmount, Toast.LENGTH_SHORT).show();
                        mBtnWithdraw.setButtonEnabled(false);
                    } else if (mWithdrawAmount < mMinWithdrawable) {
                        mEtAmount.setTextColor(getResources().getColor(R.color.red));
                        String minAmount = getResources().getString(R.string.min_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mMinWithdrawable, true);
                        Toast.makeText(getActivity(), minAmount, Toast.LENGTH_SHORT).show();
                        mBtnWithdraw.setButtonEnabled(false);
                    } else {
                        mEtAmount.setTextColor(getResources().getColor(R.color.white));
                        mBtnWithdraw.setButtonEnabled(true);
                    }
                    if (mWithdrawAmount == 0) {
                        mBtnWithdraw.setButtonEnabled(false);
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
                if (mBlockOnInputChanged)
                    return;

                // validate input
                mAmountValid = MonetaryUtil.getInstance().validateCurrentCurrencyInput(arg0.toString(), false);
                if (mAmountValid) {
                    mWithdrawAmount = MonetaryUtil.getInstance().convertCurrentCurrencyTextInputToMsat(arg0.toString());
                }
            }
        });

        if (mServiceURLString != null) {
            mTvWithdrawSource.setText(mServiceURLString);
        } else {
            mTvWithdrawSource.setText(R.string.unknown);
        }

        if (mWithdrawData.getDefaultDescription() == null || mWithdrawData.getDefaultDescription().isEmpty()) {
            mExtvDescription.setVisibility(View.GONE);
        } else {
            mExtvDescription.setVisibility(View.VISIBLE);
            mExtvDescription.setContent(R.string.description, mWithdrawData.getDefaultDescription());
        }

        if (mWithdrawData.getMinWithdrawable() == mWithdrawData.getMaxWithdrawable()) {
            // A specific amount was requested. We are not allowed to change the amount.
            mFixedAmount = mWithdrawData.getMaxWithdrawable();
            mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, true));
            mEtAmount.clearFocus();
            mEtAmount.setFocusable(false);
            mEtAmount.setEnabled(false);
        } else {
            // No specific amount was requested. Let User input an amount, but pre fill with maxWithdraw amount.
            mNumpad.setVisibility(View.VISIBLE);
            mWithdrawAmount = mMaxWithdrawable;
            mBlockOnInputChanged = true;
            mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mMaxWithdrawable, true));
            mBlockOnInputChanged = false;

            mHandler.postDelayed(() -> {
                // We have to call this delayed, as otherwise it will still bring up the softKeyboard
                mEtAmount.requestFocus();
                mEtAmount.setSelection(mEtAmount.getText().length());
            }, 200);
        }


        // Action when clicked on "withdraw"
        mBtnWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switchToWithdrawProgressScreen();

                BBLog.d(LOG_TAG, "Trying to withdraw...");

                // Create ln-invoice
                long value;
                if (mFixedAmount == 0L) {
                    value = mWithdrawAmount;
                } else {
                    value = mFixedAmount;
                }

                CreateInvoiceRequest request = CreateInvoiceRequest.newBuilder()
                        .setAmount(value)
                        .setDescription(mWithdrawData.getDefaultDescription())
                        .setExpiry(300L)
                        .setIncludeRouteHints(PrefsUtil.getPrefs().getBoolean("includePrivateChannelHints", true))
                        .build();

                getCompositeDisposable().add(BackendManager.api().createInvoice(request)
                        .subscribe(response -> {

                            // Invoice was created. Now forward it to the LNURL service to initiate withdraw.
                            LnUrlFinalWithdrawRequest lnUrlFinalWithdrawRequest = new LnUrlFinalWithdrawRequest.Builder()
                                    .setCallback(mWithdrawData.getCallback())
                                    .setK1(mWithdrawData.getK1())
                                    .setInvoice(response.getBolt11())
                                    .build();

                            okhttp3.Request lnUrlRequest = new Request.Builder()
                                    .url(lnUrlFinalWithdrawRequest.requestAsString())
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
                                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                    try {
                                        validateSecondResponse(response.body().string());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }, throwable -> {
                            Toast.makeText(getActivity(), R.string.receive_generateRequest_failed, Toast.LENGTH_SHORT).show();
                            BBLog.d(LOG_TAG, "Add invoice request failed: " + throwable.getMessage());
                        }));
            }
        });


        // Action when clicked on receive unit
        if (MonetaryUtil.getInstance().hasMoreThanOneCurrency()) {
            LinearLayout llUnit = view.findViewById(R.id.unitLayout);
            llUnit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBlockOnInputChanged = true;
                    MonetaryUtil.getInstance().switchToNextCurrency();
                    if (mFixedAmount == 0L) {
                        mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mWithdrawAmount, false));
                    } else {
                        mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, false));
                    }
                    mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
                    mBlockOnInputChanged = false;
                }
            });
        } else {
            view.findViewById(R.id.switchUnitImage).setVisibility(View.GONE);
        }

        if (mMinWithdrawable > mMaxWithdrawable) {
            // There is no way the withdraw can be routed... show an error immediately
            switchToWithdrawProgressScreen();
            switchToFailedScreen(getResources().getString(R.string.lnurl_withdraw_insufficient_channel_balance));
        }

        return view;
    }

    private void validateSecondResponse(@NonNull String withdrawResponse) {
        LnUrlWithdrawResponse lnUrlWithdrawResponse = new Gson().fromJson(withdrawResponse, LnUrlWithdrawResponse.class);

        if (lnUrlWithdrawResponse.getStatus() != null) {
            if (lnUrlWithdrawResponse.getStatus().equals("OK")) {
                switchToSuccessScreen();
            } else {
                BBLog.d(LOG_TAG, "LNURL: Failed to withdraw. " + lnUrlWithdrawResponse.getReason());
                switchToFailedScreen(lnUrlWithdrawResponse.getReason());
            }
        } else {
            BBLog.d(LOG_TAG, "LNURL: Failed to withdraw. " + withdrawResponse);
            switchToFailedScreen(withdrawResponse);
        }
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
                mWithdrawInputs.setVisibility(View.INVISIBLE);
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
                mWithdrawInputs.setVisibility(View.GONE);
                mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mWithdrawAmount, true));
                mResultView.setVisibility(View.VISIBLE);
                mResultView.setHeading(R.string.lnurl_withdraw_success, true);
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
                mWithdrawInputs.setVisibility(View.GONE);
                mResultView.setVisibility(View.VISIBLE);

                // Set failed states
                mResultView.setHeading(R.string.lnurl_withdraw_fail, false);
                mResultView.setDetailsText(error);
            }
        });
    }
}
