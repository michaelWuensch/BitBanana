package app.michaelwuensch.bitbanana.lnurl.withdraw;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import app.michaelwuensch.bitbanana.customView.BBAmountInput;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBExpandableTextInfoBox;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.ClearFocusListener;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


public class LnUrlWithdrawBSDFragment extends BaseBSDFragment implements ClearFocusListener {

    private static final String LOG_TAG = LnUrlWithdrawBSDFragment.class.getSimpleName();

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private BBAmountInput mAmountInput;
    private BBExpandableTextInfoBox mServiceView;
    private BBExpandableTextInfoBox mDescriptionView;
    private ConstraintLayout mContentTopLayout;
    private View mWithdrawInputs;
    private BBButton mBtnWithdraw;

    private String mServiceURLString;
    private Handler mHandler;
    private LnUrlWithdrawResponse mWithdrawData;

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

        mAmountInput = view.findViewById(R.id.amountInput);
        mWithdrawInputs = view.findViewById(R.id.withdrawInputsView);
        mServiceView = view.findViewById(R.id.withdrawSourceView);
        mDescriptionView = view.findViewById(R.id.descriptionView);

        mBtnWithdraw = view.findViewById(R.id.withdrawButton);

        mBSDScrollableMainView.setTitle(R.string.withdraw);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mResultView.setOnOkListener(this::dismiss);

        mDescriptionView.setClearFocusListener(this);
        mServiceView.setClearFocusListener(this);

        mAmountInput.setupView();
        mAmountInput.setOnChain(false);
        mAmountInput.setSendAllEnabled(false);
        mAmountInput.setOnAmountInputActionListener(new BBAmountInput.OnAmountInputActionListener() {
            @Override
            public boolean onAfterTextChanged(String newText, long amount, boolean isFixedAmount, boolean isOnChain) {
                if (newText.equals(".") || amount == 0)
                    return false;

                if (isFixedAmount) {
                    return true;
                }

                if (amount > mWithdrawData.getMaxWithdrawable()) {
                    String maxAmount = getResources().getString(R.string.max_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mWithdrawData.getMaxWithdrawable(), true);
                    Toast.makeText(getActivity(), maxAmount, Toast.LENGTH_SHORT).show();
                    return false;
                } else if (amount < mWithdrawData.getMinWithdrawable()) {
                    String minAmount = getResources().getString(R.string.min_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mWithdrawData.getMinWithdrawable(), true);
                    Toast.makeText(getActivity(), minAmount, Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    long maxWithdrawable = WalletUtil.getMaxLightningReceiveAmount();
                    if (amount > maxWithdrawable) {
                        String errorMsg = getString(R.string.error_insufficient_lightning_receive_liquidity, MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(maxWithdrawable, true));
                        showError(errorMsg, 7000);
                        return false;
                    } else {
                        return true;
                    }
                }
            }

            @Override
            public void onInputValidityChanged(boolean valid) {
                mBtnWithdraw.setButtonEnabled(valid);
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

        if (mServiceURLString != null) {
            mServiceView.setContent(mServiceURLString);
        } else {
            mServiceView.setContent(R.string.unknown);
        }

        if (mWithdrawData.getDefaultDescription() == null || mWithdrawData.getDefaultDescription().isEmpty()) {
            mDescriptionView.setVisibility(View.GONE);
        } else {
            mDescriptionView.setVisibility(View.VISIBLE);
            mDescriptionView.setContent(mWithdrawData.getDefaultDescription());
        }

        if (mWithdrawData.getMinWithdrawable() == mWithdrawData.getMaxWithdrawable()) {
            // A specific amount was requested. We are not allowed to change the amount.
            mAmountInput.setFixedAmount(mWithdrawData.getMaxWithdrawable());
        } else {
            // No specific amount was requested. Let User input an amount, but pre fill with maxWithdraw amount.
            mAmountInput.setAmount(Math.min((mWithdrawData.getMaxWithdrawable()), WalletUtil.getMaxLightningReceiveAmount()));
            mAmountInput.selectText();
        }


        // Action when clicked on "withdraw"
        mBtnWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switchToWithdrawProgressScreen();

                BBLog.d(LOG_TAG, "Trying to withdraw...");

                // Create ln-invoice
                CreateInvoiceRequest request = CreateInvoiceRequest.newBuilder()
                        .setAmount(mAmountInput.getAmount())
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


        if (mWithdrawData.getMinWithdrawable() > WalletUtil.getMaxLightningReceiveAmount()) {
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
                mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(mAmountInput.getAmount(), true));
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

    @Override
    public void onClearFocus() {
        mAmountInput.clearFocus();
    }
}
