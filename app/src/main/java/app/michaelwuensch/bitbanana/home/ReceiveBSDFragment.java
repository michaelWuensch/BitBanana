package app.michaelwuensch.bitbanana.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.utils.widget.ImageFilterView;

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
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.qrCodeGen.QRCodeGenerator;
import app.michaelwuensch.bitbanana.setup.QuickReceiveSetup;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import app.michaelwuensch.bitbanana.wallet.QuickReceiveConfig;
import app.michaelwuensch.bitbanana.wallet.Wallet_Bolt12Offers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;


public class ReceiveBSDFragment extends BaseBSDFragment {

    private static final String LOG_TAG = ReceiveBSDFragment.class.getSimpleName();

    private BSDScrollableMainView mBSDScrollableMainView;
    private BBAmountInput mAmountInput;
    private BBButton mBtnLn;
    private BBButton mBtnOnChain;
    private BBButton mBtnCopy;
    private BBButton mBtnShare;
    private BBButton mBtnCustomize;
    private QuickReceiveConfig mQuickReceiveConfig;
    private View mQuickReceiveQRLayout;
    private ImageFilterView mIvQRCode;
    private ProgressBar mPbQRSpinner;
    private Bitmap mBmpQRCode;
    private View mChooseTypeView;
    private View mCustomizeRequestLayout;
    private BBTextInputBox mDescriptionView;
    private BBButton mBtnGenerateRequest;
    private boolean mOnChain;
    private TextView mTvNoIncomingBalance;
    private BBButton mBtnManageChannels;
    private View mViewNoIncomingBalance;
    private View mContentTopLayout;
    private String mFinalShareString;
    private String mFinalQRCodeString;
    private ActivityResultLauncher<Intent> mActivityResultLauncherQuickReceiveSetup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_receive, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mAmountInput = view.findViewById(R.id.amountInput);
        mQuickReceiveQRLayout = view.findViewById(R.id.quickReceiveQRLayout);
        mIvQRCode = view.findViewById(R.id.quickReceiveQRCode);
        mPbQRSpinner = view.findViewById(R.id.quickReceiveProgressBar);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mDescriptionView = view.findViewById(R.id.descriptionView);
        mBtnLn = view.findViewById(R.id.lnBtn);
        mBtnCustomize = view.findViewById(R.id.customizeBtn);
        mBtnCopy = view.findViewById(R.id.copyBtn);
        mBtnShare = view.findViewById(R.id.shareBtn);
        mCustomizeRequestLayout = view.findViewById(R.id.customizeRequestLayout);
        mBtnOnChain = view.findViewById(R.id.onChainBtn);
        mChooseTypeView = view.findViewById(R.id.chooseTypeLayout);
        mBtnGenerateRequest = view.findViewById(R.id.generateRequestButton);
        mTvNoIncomingBalance = view.findViewById(R.id.noIncomingChannelBalanceText);
        mViewNoIncomingBalance = view.findViewById(R.id.noIncomingChannelBalanceView);
        mBtnManageChannels = view.findViewById(R.id.manageChannels);

        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setHelpMessage(R.string.help_dialog_LightningVsOnChain);
        mBSDScrollableMainView.setTitle(R.string.receive);
        mBSDScrollableMainView.setMoreButtonVisibility(true);
        mBSDScrollableMainView.setMoreButtonStyle(BSDScrollableMainView.MoreButtonStyle.OPTIONS);

        mBSDScrollableMainView.setOnMoreListener(new BSDScrollableMainView.OnMoreListener() {
            @Override
            public void onMore() {
                Intent intent = new Intent(getActivity(), QuickReceiveSetup.class);
                mActivityResultLauncherQuickReceiveSetup.launch(intent);
            }
        });

        mBtnCustomize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToCustomization(BackendManager.getCurrentBackend().supportsOnChainReceive() && BackendManager.getCurrentBackend().supportsBolt11Receive());
            }
        });

        setupQuickReceive();

        // Initialize the ActivityResultLauncher for the QuickReceive setup
        mActivityResultLauncherQuickReceiveSetup = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    setupQuickReceive();
                }
        );

        // Action when clicked on "share"
        mBtnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, mFinalShareString);
                shareIntent.setType("text/plain");
                String title = getResources().getString(R.string.shareDialogTitle);
                startActivity(Intent.createChooser(shareIntent, title));
            }
        });

        // Action when clicked on "copy"
        mBtnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipBoardUtil.copyToClipboard(getContext(), "Request", mFinalShareString);
            }
        });

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

        return view;
    }

    private void setupQuickReceive() {
        mBSDScrollableMainView.setHelpButtonVisibility(false);
        mBSDScrollableMainView.setTitle(R.string.receive);
        mBSDScrollableMainView.setTitleIconVisibility(false);
        mCustomizeRequestLayout.setVisibility(View.GONE);
        mChooseTypeView.setVisibility(View.GONE);
        mQuickReceiveQRLayout.setVisibility(View.VISIBLE);

        // Prepare the quickReceiveQRCode loading state
        Bitmap whiteSquare = Bitmap.createBitmap(750, 750, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteSquare);
        canvas.drawColor(getResources().getColor(R.color.sea_blue_gradient));
        mIvQRCode.setImageBitmap(whiteSquare);

        setQuickReceiveReady(false);

        // Setup quick receive view
        if (!BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            switchToCustomization(false);
        } else {
            mQuickReceiveConfig = BackendConfigsManager.getInstance().getCurrentBackendConfig().getQuickReceiveConfig();
            switch (mQuickReceiveConfig.getQuickReceiveType()) {
                case OFF:
                    switchToCustomization(false);
                    break;
                case LN_ADDRESS:
                    mFinalShareString = mQuickReceiveConfig.getLnAddress();
                    mFinalQRCodeString = UriUtil.URI_PREFIX_LIGHTNING + mQuickReceiveConfig.getLnAddress();
                    mBmpQRCode = QRCodeGenerator.bitmapFromText(mFinalQRCodeString, 750);
                    mIvQRCode.setImageBitmap(mBmpQRCode);
                    setQuickReceiveReady(true);
                    break;
                case BOLT12:
                    Bolt12Offer tempOffer = Wallet_Bolt12Offers.getInstance().getBolt12OfferById(mQuickReceiveConfig.getBolt12ID());
                    if (tempOffer == null) {
                        switchToCustomization(false);
                        break;
                    }
                    mFinalShareString = UriUtil.URI_PREFIX_LIGHTNING + tempOffer.getDecodedBolt12().getBolt12String();
                    mFinalQRCodeString = mFinalShareString;
                    mBmpQRCode = QRCodeGenerator.bitmapFromText(mFinalQRCodeString, 750);
                    mIvQRCode.setImageBitmap(mBmpQRCode);
                    setQuickReceiveReady(true);
                    break;
                case ON_CHAIN_ADDRESS:
                case ON_CHAIN_AND_LN_ADDRESS:
                case ON_CHAIN_AND_BOLT12:
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

                    // ToDo: cln uses new address every time...
                    NewOnChainAddressRequest newOnChainAddressRequest = NewOnChainAddressRequest.newBuilder()
                            .setType(addressType)
                            .setUnused(true)
                            .build();

                    getCompositeDisposable().add(BackendManager.api().getNewOnchainAddress(newOnChainAddressRequest)
                            .subscribe(response -> {

                                Bip21Invoice.Builder bip21InvoiceBuilder = Bip21Invoice.newBuilder()
                                        .setAddress(response);

                                switch (mQuickReceiveConfig.getQuickReceiveType()) {
                                    case ON_CHAIN_ADDRESS:
                                        mFinalShareString = response;
                                        break;
                                    case ON_CHAIN_AND_LN_ADDRESS:
                                        bip21InvoiceBuilder.setLightning(mQuickReceiveConfig.getLnAddress());
                                        mFinalShareString = bip21InvoiceBuilder.build().toString();
                                        break;
                                    case ON_CHAIN_AND_BOLT12:
                                        Bolt12Offer tempOffer2 = Wallet_Bolt12Offers.getInstance().getBolt12OfferById(mQuickReceiveConfig.getBolt12ID());
                                        if (tempOffer2 != null) {
                                            bip21InvoiceBuilder.setOffer(tempOffer2.getDecodedBolt12().getBolt12String());
                                        }
                                        mFinalShareString = bip21InvoiceBuilder.build().toString();
                                        break;
                                }
                                mFinalQRCodeString = bip21InvoiceBuilder.build().toString();
                                mBmpQRCode = QRCodeGenerator.bitmapFromText(mFinalQRCodeString, 750);
                                mIvQRCode.setImageBitmap(mBmpQRCode);
                                setQuickReceiveReady(true);
                            }, throwable -> {
                                showError(getString(R.string.receive_generateRequest_failed), 3000);
                                BBLog.e(LOG_TAG, "New address request failed: " + throwable.fillInStackTrace());
                            }));
                    break;
            }
        }
    }

    private void setQuickReceiveReady(boolean ready) {
        mBtnCopy.setButtonEnabled(ready);
        mBtnShare.setButtonEnabled(ready);
        mPbQRSpinner.setVisibility(ready ? View.GONE : View.VISIBLE);
    }

    private void switchToCustomization(boolean animated) {
        // Manage visibilities and animation
        if (animated) {
            AutoTransition autoTransition = new AutoTransition();
            autoTransition.setDuration(200);
            TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView(), autoTransition);
        }

        if (!(BackendManager.getCurrentBackend().supportsOnChainReceive() && BackendManager.getCurrentBackend().supportsBolt11Receive())) {
            if (BackendManager.getCurrentBackend().supportsOnChainReceive())
                switchToOnChain();
            if (BackendManager.getCurrentBackend().supportsBolt11Receive())
                switchToLightning();
        } else {
            switchToSelectPaymentType();
        }
    }

    private void switchToSelectPaymentType() {
        mBSDScrollableMainView.setHelpButtonVisibility(true);
        mBSDScrollableMainView.setHelpButtonVisibility(true);
        mQuickReceiveQRLayout.setVisibility(View.GONE);
        mChooseTypeView.setVisibility(View.VISIBLE);
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
        mQuickReceiveQRLayout.setVisibility(View.GONE);

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
        mQuickReceiveQRLayout.setVisibility(View.GONE);

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
            mBtnGenerateRequest.showProgress();
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
                            mBtnGenerateRequest.hideProgress();
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
                            mBtnGenerateRequest.hideProgress();
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
