package app.michaelwuensch.bitbanana.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.GeneratedRequestActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.BBAmountInput;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBTextInputBox;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.listViews.channels.ManageChannelsActivity;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;


public class ReceiveBSDFragment extends BaseBSDFragment {

    private static final String LOG_TAG = ReceiveBSDFragment.class.getSimpleName();

    private BSDScrollableMainView mBSDScrollableMainView;
    private BBAmountInput mAmountInput;
    private BBButton mBtnLn;
    private BBButton mBtnOnChain;
    private View mChooseTypeView;
    private View mCustomizeRequestLayout;
    private BBTextInputBox mDescriptionView;
    private BBButton mBtnGenerateRequest;
    private boolean mOnChain;
    private TextView mTvNoIncomingBalance;
    private BBButton mBtnManageChannels;
    private View mViewNoIncomingBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_receive, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mAmountInput = view.findViewById(R.id.amountInput);
        mDescriptionView = view.findViewById(R.id.descriptionView);
        mBtnLn = view.findViewById(R.id.lnBtn);
        mCustomizeRequestLayout = view.findViewById(R.id.customizeRequestLayout);
        mBtnOnChain = view.findViewById(R.id.onChainBtn);
        mChooseTypeView = view.findViewById(R.id.chooseTypeLayout);
        mBtnGenerateRequest = view.findViewById(R.id.generateRequestButton);
        mTvNoIncomingBalance = view.findViewById(R.id.noIncomingChannelBalanceText);
        mViewNoIncomingBalance = view.findViewById(R.id.noIncomingChannelBalanceView);
        mBtnManageChannels = view.findViewById(R.id.manageChannels);

        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setHelpButtonVisibility(true);
        mBSDScrollableMainView.setHelpMessage(R.string.help_dialog_LightningVsOnChain);
        mBSDScrollableMainView.setTitle(R.string.receive);

        // Action when clicked on "Lightning" Button
        mBtnLn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToLightning();
            }
        });

        // Action when clicked on "On-Chain" Button
        mBtnOnChain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToOnChain();
            }
        });

        // Action when clicked on "Generate Request" button
        mBtnGenerateRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGenerateRequestClicked();
            }
        });

        // Action when clicked on "manage Channels" button
        mBtnManageChannels.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(getActivity(), ManageChannelsActivity.class);
                startActivity(intent);
                dismiss();
            }
        });

        mAmountInput.setupView();
        mAmountInput.setSendAllEnabled(false);
        mAmountInput.setOnAmountInputActionListener(new BBAmountInput.OnAmountInputActionListener() {
            @Override
            public boolean onAfterTextChanged(String newText, long amount, boolean isFixedAmount, boolean isOnChain) {
                // make text red if input is too large
                if (mOnChain) {
                    // always mark it valid, we have no limit for on-chain
                    return true;
                } else {
                    long maxReceivable;
                    if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                        maxReceivable = WalletUtil.getMaxLightningReceiveAmount();
                    } else {
                        maxReceivable = 500000000000000L;
                    }
                    if (!newText.equals(".")) {
                        if (amount > maxReceivable) {
                            String errorMsg = getString(R.string.error_insufficient_lightning_receive_liquidity, MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(maxReceivable, true));
                            showError(errorMsg, 7000);
                            return false;
                        } else if (amount == 0 && !FeatureManager.isBolt11WithoutAmountEnabled()) {
                            // Disable 0 sat ln invoices
                            return false;
                        } else {
                            return true;
                        }
                    } else {
                        return false;
                    }
                }
            }

            @Override
            public void onInputValidityChanged(boolean valid) {
                mBtnGenerateRequest.setButtonEnabled(valid);
            }

            @Override
            public void onInputChanged(boolean valid) {

            }

            @Override
            public void onSendAllCheckboxChanged(boolean checked) {

            }

            @Override
            public void onError(String message, int duration) {

            }
        });


        if (!(BackendManager.getCurrentBackend().supportsOnChainReceive() && BackendManager.getCurrentBackend().supportsBolt11Receive())) {
            if (BackendManager.getCurrentBackend().supportsOnChainReceive())
                switchToOnChain();
            if (BackendManager.getCurrentBackend().supportsBolt11Receive())
                switchToLightning();
        }

        return view;
    }

    private void switchToOnChain() {
        mOnChain = true;
        mAmountInput.setOnChain(true);
        if (BackendManager.getCurrentBackendType() == BackendConfig.BackendType.LND_HUB) {
            onGenerateRequestClicked();
            return;
        }

        // Manage visibilities and animation

        // Transitions do not look good when also animation the keyboard in at the same time, therefore we do it without animation
        //AutoTransition autoTransition = new AutoTransition();
        //autoTransition.setDuration(200);
        //TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView(), autoTransition);

        mBSDScrollableMainView.setHelpButtonVisibility(false);
        mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_on_chain);
        mBSDScrollableMainView.setTitle(R.string.receive_on_chain_request);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mCustomizeRequestLayout.setVisibility(View.VISIBLE);
        mChooseTypeView.setVisibility(View.GONE);

        // Request focus on amount input
        mAmountInput.requestFocusDelayed();
        showKeyboard();
    }

    private void switchToLightning() {
        mOnChain = false;
        mAmountInput.setOnChain(false);
        mDescriptionView.setupCharLimit(640);
        boolean canReceiveLightningPayment = hasLightningIncomeBalance() || !BackendConfigsManager.getInstance().hasAnyBackendConfigs();

        // Manage visibilities and animation

        // Transitions do not look good when also animation the keyboard in at the same time, therefore we do it without animation
        //AutoTransition autoTransition = new AutoTransition();
        //autoTransition.setDuration(200);
        //TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView(), autoTransition);

        mBSDScrollableMainView.setHelpButtonVisibility(false);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_lightning);
        mBSDScrollableMainView.setTitle(R.string.receive_lightning_request);

        mChooseTypeView.setVisibility(View.GONE);

        if (FeatureManager.isBolt11WithoutAmountEnabled()) {
            mBtnGenerateRequest.setButtonEnabled(true);
        } else {
            mBtnGenerateRequest.setButtonEnabled(false);
        }

        if (canReceiveLightningPayment) {
            mTvNoIncomingBalance.setVisibility(View.GONE);
            mCustomizeRequestLayout.setVisibility(View.VISIBLE);
            // Request focus on amount input, delayed to prevent system keyboard from popping up
            mAmountInput.requestFocusDelayed();
            showKeyboard();
        } else {
            mViewNoIncomingBalance.setVisibility(View.VISIBLE);
        }
    }

    private void onGenerateRequestClicked() {
        if (MonetaryUtil.getInstance().isCurrentCurrencyFiat() && MonetaryUtil.getInstance().getCurrentCurrencyExchangeRateAge() > 3600) {
            // Warn the user if his primary currency is not of type bitcoin and his exchange rate is older than 1 hour.
            new UserGuardian(getActivity(), new UserGuardian.OnGuardianConfirmedListener() {
                @Override
                public void onConfirmed() {
                    generateRequest();
                }

                @Override
                public void onCancelled() {

                }
            }).securityOldExchangeRate(MonetaryUtil.getInstance().getCurrentCurrencyExchangeRateAge());
        } else {
            generateRequest();
        }
    }

    private void generateRequest() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            // The wallet is setup. Communicate with LND and generate the request.
            if (mOnChain) {
                // generate onChain request
                getCompositeDisposable().add(BackendManager.api().getNewOnchainAddress(WalletUtil.getNewOnChainAddressRequest())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            Bip21Invoice bip21Invoice = Bip21Invoice.newBuilder()
                                    .setAddress(response)
                                    .setAmount(mAmountInput.getAmount())
                                    .setMessageURLEncode(mDescriptionView.getData())
                                    .build();
                            Intent intent = new Intent(getActivity(), GeneratedRequestActivity.class);
                            intent.putExtra("onChain", mOnChain);
                            intent.putExtra("bip21Invoice", bip21Invoice);
                            startActivity(intent);
                            dismiss();
                        }, throwable -> {
                            showError(getString(R.string.receive_generateRequest_failed), 3000);
                            BBLog.e(LOG_TAG, "New address request failed: " + throwable.fillInStackTrace());
                        }));

            } else {
                // generate lightning request
                CreateInvoiceRequest invoiceRequest = CreateInvoiceRequest.newBuilder()
                        .setAmount(mAmountInput.getAmount())
                        .setDescription(mDescriptionView.getData())
                        .setExpiry(Long.parseLong(PrefsUtil.getPrefs().getString("lightning_expiry", "86400"))) // in seconds
                        .setIncludeRouteHints(PrefsUtil.getPrefs().getBoolean("includePrivateChannelHints", true))
                        .build();

                getCompositeDisposable().add(BackendManager.api().createInvoice(invoiceRequest)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            Intent intent = new Intent(getActivity(), GeneratedRequestActivity.class);
                            intent.putExtra("onChain", mOnChain);
                            intent.putExtra("lnInvoice", InvoiceUtil.decodeBolt11(response.getBolt11()));
                            startActivity(intent);
                            dismiss();
                        }, throwable -> {
                            showError(getString(R.string.receive_generateRequest_failed), 3000);
                            BBLog.e(LOG_TAG, "Add invoice request failed: " + throwable.getMessage());
                        }));
            }
        } else {
            // The wallet is not setup. Show setup wallet message.
            showToast(getString(R.string.demo_setupNodeFirst), Toast.LENGTH_LONG);
        }
    }

    private boolean hasLightningIncomeBalance() {
        if (BackendManager.getCurrentBackendType() == BackendConfig.BackendType.LND_HUB)
            return true;

        boolean hasActiveChannels = WalletUtil.hasOpenActiveChannels();

        if (hasActiveChannels) {
            if (WalletUtil.getMaxLightningReceiveAmount() > 0L) {
                // We have remote balances on at least one channel, so we can receive a lightning payment!
                return true;
            } else {
                mTvNoIncomingBalance.setText(R.string.receive_noIncomeBalance);
                return false;
            }
        } else {
            mTvNoIncomingBalance.setText(R.string.receive_noActiveChannels);
            return false;
        }
    }
}
