package app.michaelwuensch.bitbanana.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.GeneratedRequestActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.NumpadView;
import app.michaelwuensch.bitbanana.listViews.channels.ManageChannelsActivity;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.WalletUtil;


public class ReceiveBSDFragment extends BaseBSDFragment {

    private static final String LOG_TAG = ReceiveBSDFragment.class.getSimpleName();

    private BSDScrollableMainView mBSDScrollableMainView;
    private ConstraintLayout mContentTopLayout;
    private BBButton mBtnLn;
    private BBButton mBtnOnChain;
    private View mChooseTypeView;
    private View mReceiveAmountView;
    private EditText mEtAmount;
    private EditText mEtMemo;
    private TextView mTvUnit;
    private View mMemoView;
    private NumpadView mNumpad;
    private BBButton mBtnNext;
    private BBButton mBtnGenerateRequest;
    private boolean mOnChain;
    private TextView mTvNoIncomingBalance;
    private BBButton mBtnManageChannels;
    private View mViewNoIncomingBalance;
    private boolean mAmountValid = true;
    private long mReceiveAmount;
    private boolean mBlockOnInputChanged;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_receive, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mBtnLn = view.findViewById(R.id.lnBtn);
        mBtnOnChain = view.findViewById(R.id.onChainBtn);
        mChooseTypeView = view.findViewById(R.id.chooseTypeLayout);
        mReceiveAmountView = view.findViewById(R.id.receiveInputsView);
        mEtAmount = view.findViewById(R.id.receiveAmount);
        mTvUnit = view.findViewById(R.id.receiveUnit);
        mEtMemo = view.findViewById(R.id.receiveMemo);
        mMemoView = view.findViewById(R.id.receiveMemoTopLayout);
        mNumpad = view.findViewById(R.id.numpadView);
        mBtnNext = view.findViewById(R.id.nextButton);
        mBtnGenerateRequest = view.findViewById(R.id.generateRequestButton);
        mTvNoIncomingBalance = view.findViewById(R.id.noIncomingChannelBalanceText);
        mViewNoIncomingBalance = view.findViewById(R.id.noIncomingChannelBalanceView);
        mBtnManageChannels = view.findViewById(R.id.manageChannels);

        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setHelpButtonVisibility(true);
        mBSDScrollableMainView.setHelpMessage(R.string.help_dialog_LightningVsOnChain);
        mBSDScrollableMainView.setTitle(R.string.receive);

        mNumpad.bindEditText(mEtAmount);

        // add "optional" hint to optional fields
        mEtAmount.setHint(getResources().getString(R.string.amount) + " (" + getResources().getString(R.string.optional) + ")");
        mEtMemo.setHint(getResources().getString(R.string.memo) + " (" + getResources().getString(R.string.optional) + ")");

        // deactivate default Keyboard for number input.
        mEtAmount.setShowSoftInputOnFocus(false);

        // set unit to current primary unit
        mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());


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

        // Action when clicked on "next" button
        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNumpad.setVisibility(View.GONE);
                mBtnNext.setVisibility(View.GONE);
                mMemoView.setVisibility(View.VISIBLE);
                mBtnGenerateRequest.setVisibility(View.VISIBLE);

                mEtAmount.setEnabled(false);
                mEtMemo.requestFocus();
                showKeyboard();
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


        // Action when clicked on receive unit
        if (MonetaryUtil.getInstance().hasMoreThanOneCurrency()) {
            LinearLayout llUnit = view.findViewById(R.id.receiveUnitLayout);
            llUnit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBlockOnInputChanged = true;
                    MonetaryUtil.getInstance().switchToNextCurrency();
                    mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mReceiveAmount, !mOnChain));
                    mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
                    mBlockOnInputChanged = false;
                }
            });
        } else {
            view.findViewById(R.id.receiveSwitchUnitImage).setVisibility(View.GONE);
        }


        // Input validation for the amount field.
        mEtAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {

                // remove the last inputted character if not valid
                if (!mAmountValid) {
                    mNumpad.removeOneDigit();
                }

                // make text red if input is too large
                if (mOnChain) {
                    // always make it white, we have no limit for on-chain
                    mEtAmount.setTextColor(getResources().getColor(R.color.white));
                } else {
                    long maxReceivable;
                    if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                        maxReceivable = WalletUtil.getMaxLightningReceiveAmount();
                    } else {
                        maxReceivable = 500000000000000L;
                    }
                    if (!mEtAmount.getText().toString().equals(".")) {
                        if (mReceiveAmount > maxReceivable) {
                            mEtAmount.setTextColor(getResources().getColor(R.color.red));
                            String maxAmount = getResources().getString(R.string.max_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(maxReceivable, true);
                            Toast.makeText(getActivity(), maxAmount, Toast.LENGTH_SHORT).show();
                            mBtnNext.setButtonEnabled(false);
                        } else if (mReceiveAmount == 0 && !FeatureManager.isBolt11WithoutAmountEnabled()) {
                            // Disable 0 sat ln invoices
                            mBtnNext.setButtonEnabled(false);
                        } else {
                            mEtAmount.setTextColor(getResources().getColor(R.color.white));
                            mBtnNext.setButtonEnabled(true);
                        }
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
                mAmountValid = MonetaryUtil.getInstance().validateCurrentCurrencyInput(arg0.toString(), !mOnChain);
                if (mAmountValid) {
                    mReceiveAmount = MonetaryUtil.getInstance().convertCurrentCurrencyTextInputToMsat(arg0.toString());
                }
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
        if (BackendManager.getCurrentBackendType() == BackendConfig.BackendType.LND_HUB) {
            onGenerateRequestClicked();
            return;
        }

        // Manage visibilities and animation
        AutoTransition autoTransition = new AutoTransition();
        autoTransition.setDuration(200);
        TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView(), autoTransition);
        mBSDScrollableMainView.setHelpButtonVisibility(false);
        mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_on_chain);
        mBSDScrollableMainView.setTitle(R.string.receive_on_chain_request);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mReceiveAmountView.setVisibility(View.VISIBLE);
        mNumpad.setVisibility(View.VISIBLE);
        mChooseTypeView.setVisibility(View.GONE);
        mBtnNext.setVisibility(View.VISIBLE);
        mMemoView.setVisibility(View.GONE);

        // Request focus on amount input
        mEtAmount.requestFocus();
    }

    private void switchToLightning() {
        mOnChain = false;
        boolean canReceiveLightningPayment = hasLightningIncomeBalance() || !BackendConfigsManager.getInstance().hasAnyBackendConfigs();

        // Manage visibilities and animation
        AutoTransition autoTransition = new AutoTransition();
        autoTransition.setDuration(200);
        TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView(), autoTransition);
        mBSDScrollableMainView.setHelpButtonVisibility(false);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_lightning);
        mBSDScrollableMainView.setTitle(R.string.receive_lightning_request);

        mChooseTypeView.setVisibility(View.GONE);
        mMemoView.setVisibility(View.GONE);
        if (FeatureManager.isBolt11WithoutAmountEnabled()) {
            mBtnNext.setButtonEnabled(true);
        } else {
            mBtnNext.setButtonEnabled(false);
        }
        mEtAmount.setHint(getResources().getString(R.string.amount));

        if (canReceiveLightningPayment) {
            mTvNoIncomingBalance.setVisibility(View.GONE);
            mReceiveAmountView.setVisibility(View.VISIBLE);
            mNumpad.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            // Request focus on amount input, delayed to prevent system keyboard from popping up
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mEtAmount.requestFocus();
                }
            }, 500);
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

    private void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    private void generateRequest() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            // The wallet is setup. Communicate with LND and generate the request.
            if (mOnChain) {

                // generate onChain request

                NewOnChainAddressRequest.Type addressType;
                String addressTypeString = PrefsUtil.getPrefs().getString("btcAddressType", "bech32m");
                if (addressTypeString.equals("bech32")) {
                    addressType = NewOnChainAddressRequest.Type.SEGWIT;
                } else {
                    if (addressTypeString.equals("bech32m")) {
                        addressType = NewOnChainAddressRequest.Type.TAPROOT;
                    } else {
                        addressType = NewOnChainAddressRequest.Type.SEGWIT_COMPATIBILITY;
                    }
                }

                NewOnChainAddressRequest newOnChainAddressRequest = NewOnChainAddressRequest.newBuilder()
                        .setType(addressType)
                        .setUnused(true)
                        .build();

                BBLog.d(LOG_TAG, "OnChain generating...");
                getCompositeDisposable().add(BackendManager.api().getNewOnchainAddress(newOnChainAddressRequest)
                        .subscribe(response -> {
                            Bip21Invoice bip21Invoice = Bip21Invoice.newBuilder()
                                    .setAddress(response)
                                    .setAmount(mReceiveAmount)
                                    .setMessage(mEtMemo.getText().toString())
                                    .build();
                            Intent intent = new Intent(getActivity(), GeneratedRequestActivity.class);
                            intent.putExtra("onChain", mOnChain);
                            intent.putExtra("bip21Invoice", bip21Invoice);
                            startActivity(intent);
                            dismiss();
                        }, throwable -> {
                            Toast.makeText(getActivity(), R.string.receive_generateRequest_failed, Toast.LENGTH_SHORT).show();
                            BBLog.e(LOG_TAG, "New address request failed: " + throwable.fillInStackTrace());
                        }));

            } else {
                // generate lightning request
                CreateInvoiceRequest invoiceRequest = CreateInvoiceRequest.newBuilder()
                        .setAmount(mReceiveAmount)
                        .setDescription(mEtMemo.getText().toString())
                        .setExpiry(Long.parseLong(PrefsUtil.getPrefs().getString("lightning_expiry", "86400"))) // in seconds
                        .setIncludeRouteHints(PrefsUtil.getPrefs().getBoolean("includePrivateChannelHints", true))
                        .build();

                getCompositeDisposable().add(BackendManager.api().createInvoice(invoiceRequest)
                        .subscribe(response -> {

                            Intent intent = new Intent(getActivity(), GeneratedRequestActivity.class);
                            intent.putExtra("onChain", mOnChain);
                            intent.putExtra("lnInvoice", InvoiceUtil.decodeBolt11(response.getBolt11()));
                            startActivity(intent);
                            dismiss();
                        }, throwable -> {
                            Toast.makeText(getActivity(), R.string.receive_generateRequest_failed, Toast.LENGTH_SHORT).show();
                            BBLog.e(LOG_TAG, "Add invoice request failed: " + throwable.getMessage());
                        }));
            }
        } else {
            // The wallet is not setup. Show setup wallet message.
            Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_LONG).show();
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
