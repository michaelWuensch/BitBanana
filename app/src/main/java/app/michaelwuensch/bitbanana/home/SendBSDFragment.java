package app.michaelwuensch.bitbanana.home;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.customView.BBAmountInput;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBExpandableTextInfoBox;
import app.michaelwuensch.bitbanana.customView.BBTextInputBox;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.ClearFocusListener;
import app.michaelwuensch.bitbanana.customView.LightningFeeView;
import app.michaelwuensch.bitbanana.customView.OnChainFeeView;
import app.michaelwuensch.bitbanana.customView.PickChannelsView;
import app.michaelwuensch.bitbanana.customView.UtxoOptionsView;
import app.michaelwuensch.bitbanana.listViews.utxos.UTXOsActivity;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.models.FetchInvoiceFromOfferRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.models.SendOnChainPaymentRequest;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;


public class SendBSDFragment extends BaseBSDFragment implements UtxoOptionsView.OnUtxoViewButtonListener, ClearFocusListener, PickChannelsView.OnPickChannelViewButtonListener {

    private static final String LOG_TAG = SendBSDFragment.class.getSimpleName();

    private ActivityResultLauncher<Intent> mActivityResultLauncherSelectUTXOs;
    private ActivityResultLauncher<Intent> mActivityResultLauncherSelectChannel;

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDProgressView mProgressScreen;
    private BSDResultView mResultView;
    private BBAmountInput mAmountInput;
    private BBExpandableTextInfoBox mPayeeView;
    private ConstraintLayout mContentTopLayout;
    private LinearLayout mInputLayout;
    private BBExpandableTextInfoBox mDescriptionView;
    private OnChainFeeView mOnChainFeeView;
    private LightningFeeView mLightningFeeView;
    private BBButton mBtnSend;
    private BBButton mFallbackButton;
    private BBTextInputBox mPcvComment;
    private UtxoOptionsView mUtxoOptionsView;
    private PickChannelsView mPickChannelsView;

    private DecodedBolt11 mDecodedBolt11;
    private DecodedBolt12 mDecodedBolt12;
    private Bip21Invoice mFallbackOnChainInvoice;
    private String mDescription;
    private String mOnChainAddress;
    private boolean mOnChain;
    private boolean mIsBolt12Offer;
    private long mFixedAmount;
    private Handler mHandler;

    private String mKeysendPubkey;
    private boolean mIsKeysend;

    public static SendBSDFragment createLightningDialog(DecodedBolt11 decodedBolt11, Bip21Invoice fallbackOnChainInvoice) {
        Intent intent = new Intent();
        intent.putExtra("keysend", false);
        intent.putExtra("onChain", false);
        intent.putExtra("lnPaymentRequest", decodedBolt11);
        intent.putExtra("fallbackOnChainInvoice", fallbackOnChainInvoice);
        intent.putExtra("isBolt12Offer", false);
        SendBSDFragment sendBottomSheetDialog = new SendBSDFragment();
        sendBottomSheetDialog.setArguments(intent.getExtras());
        return sendBottomSheetDialog;
    }

    public static SendBSDFragment createBolt12OfferDialog(DecodedBolt12 decodedBolt12, Bip21Invoice fallbackOnChainInvoice) {
        Intent intent = new Intent();
        intent.putExtra("keysend", false);
        intent.putExtra("onChain", false);
        intent.putExtra("isBolt12Offer", true);
        intent.putExtra("decodedBolt12", decodedBolt12);
        intent.putExtra("fallbackOnChainInvoice", fallbackOnChainInvoice);
        SendBSDFragment sendBottomSheetDialog = new SendBSDFragment();
        sendBottomSheetDialog.setArguments(intent.getExtras());
        return sendBottomSheetDialog;
    }

    public static SendBSDFragment createOnChainDialog(Bip21Invoice onChainInvoice) {
        Intent intent = new Intent();
        intent.putExtra("keysend", false);
        intent.putExtra("onChain", true);
        intent.putExtra("onChainAddress", onChainInvoice.getAddress());
        intent.putExtra("onChainAmount", onChainInvoice.getAmount());
        intent.putExtra("onChainMessage", onChainInvoice.getMessage());
        SendBSDFragment sendBottomSheetDialog = new SendBSDFragment();
        sendBottomSheetDialog.setArguments(intent.getExtras());
        return sendBottomSheetDialog;
    }

    public static SendBSDFragment createKeysendDialog(String pubkey) {
        Intent intent = new Intent();
        intent.putExtra("onChain", false);
        intent.putExtra("keysend", true);
        intent.putExtra("keysendPubkey", pubkey);
        SendBSDFragment sendBottomSheetDialog = new SendBSDFragment();
        sendBottomSheetDialog.setArguments(intent.getExtras());
        return sendBottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle args = getArguments();
        mOnChain = args.getBoolean("onChain");
        mIsKeysend = args.getBoolean("keysend");
        mIsBolt12Offer = args.getBoolean("isBolt12Offer");

        if (mOnChain) {
            mFixedAmount = args.getLong("onChainAmount");
            mOnChainAddress = args.getString("onChainAddress");
            mDescription = args.getString("onChainMessage");
        } else if (mIsBolt12Offer) {
            mDecodedBolt12 = (DecodedBolt12) args.getSerializable("decodedBolt12");
            mFallbackOnChainInvoice = (Bip21Invoice) args.getSerializable("fallbackOnChainInvoice");
        } else {
            if (mIsKeysend) {
                mKeysendPubkey = args.getString("keysendPubkey");
            } else {
                mDecodedBolt11 = (DecodedBolt11) args.getSerializable("lnPaymentRequest");
                mFallbackOnChainInvoice = (Bip21Invoice) args.getSerializable("fallbackOnChainInvoice");
            }
        }

        View view = inflater.inflate(R.layout.bsd_send, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mProgressScreen = view.findViewById(R.id.paymentProgressLayout);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mInputLayout = view.findViewById(R.id.inputLayout);
        mDescriptionView = view.findViewById(R.id.descriptionView);
        mOnChainFeeView = view.findViewById(R.id.sendFeeOnChainLayout);
        mLightningFeeView = view.findViewById(R.id.sendFeeLightningLayout);
        mBtnSend = view.findViewById(R.id.sendButton);
        mFallbackButton = view.findViewById(R.id.fallbackButton);
        mPayeeView = view.findViewById(R.id.payeeView);
        mPcvComment = view.findViewById(R.id.paymentComment);
        mUtxoOptionsView = view.findViewById(R.id.utxoOptions);
        mPickChannelsView = view.findViewById(R.id.pickChannels);
        mAmountInput = view.findViewById(R.id.amountInput);

        mPayeeView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        mPayeeView.setClearFocusListener(this);
        mDescriptionView.setClearFocusListener(this);

        mAmountInput.setupView();
        mAmountInput.setOnAmountInputActionListener(new BBAmountInput.OnAmountInputActionListener() {
            @Override
            public boolean onAfterTextChanged(String newText, long amount, boolean isFixedAmount, boolean isOnChain) {
                if (newText.equals(".") || amount == 0)
                    return false;

                // make text red if input is too large
                long maxSendable;
                if (isOnChain) {
                    maxSendable = Wallet_Balance.getInstance().getBalances().onChainConfirmed();
                } else {
                    maxSendable = WalletUtil.getMaxLightningSendAmount();
                }

                if (amount > maxSendable) {
                    if (isOnChain) {
                        if (amount < Wallet_Balance.getInstance().getBalances().onChainTotal()) {
                            String message = getResources().getString(R.string.error_funds_not_confirmed_yet);
                            showError(message, 10000);
                        } else {
                            String message = getResources().getString(R.string.error_insufficient_on_chain_funds) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(Wallet_Balance.getInstance().getBalances().onChainTotal(), false);
                            showError(message, 4000);
                        }
                    } else {
                        String message = getResources().getString(R.string.error_insufficient_lightning_sending_liquidity) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(maxSendable, true);
                        showError(message, 6000);
                    }
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public void onInputValidityChanged(boolean valid) {
                mBtnSend.setButtonEnabled(valid);
            }

            @Override
            public void onInputChanged(boolean valid) {
                if (valid)
                    calculateFee();
                else
                    setFeeFailure();
            }

            @Override
            public void onSendAllCheckboxChanged(boolean checked) {
                mOnChainFeeView.setSendAllFlag(checked);
            }

            @Override
            public void onError(String message, int duration) {
                showError(message, duration);
            }
        });

        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mResultView.setOnOkListener(this::dismiss);

        mHandler = new Handler();

        // Initialize the ActivityResultLauncher for UTXO selection
        mActivityResultLauncherSelectUTXOs = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        // Pass result to the utxo view
                        mUtxoOptionsView.handleActivityResult(data);

                        // Update Amount View
                        long selectedAmount = data.getLongExtra(UTXOsActivity.EXTRA_TOTAL_SELECTED_UTXO_AMOUNT, 0);
                        mAmountInput.updateUtxoSelectionAmount(selectedAmount);

                        // Update On-Chain Fee view
                        mOnChainFeeView.setUtxosSelectedFlag(selectedAmount > 0);
                    }
                }
        );

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

        mUtxoOptionsView.setActivityResultLauncher(mActivityResultLauncherSelectUTXOs);
        mUtxoOptionsView.setClearFocusListener(this);

        mPickChannelsView.setActivityResultLauncher(mActivityResultLauncherSelectChannel);
        mPickChannelsView.setClearFocusListener(this);

        if (mOnChain) {
            mAmountInput.setOnChain(true);
            mAmountInput.setSendAllEnabled(true);
            mPayeeView.setContent(mOnChainAddress);
            mOnChainFeeView.initialSetup();
            mOnChainFeeView.setClearFocusListener(this);

            mOnChainFeeView.setVisibility(View.VISIBLE);
            mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_on_chain);
            mResultView.setTypeIcon(R.drawable.ic_onchain_black_24dp);
            mProgressScreen.setProgressTypeIcon(R.drawable.ic_onchain_black_24dp);
            mBSDScrollableMainView.setTitle(R.string.send_onChainPayment);
            mPcvComment.setVisibility(View.GONE);
            mUtxoOptionsView.setVisibility(FeatureManager.isUtxoSelectionOnSendEnabled() ? View.VISIBLE : View.GONE);
            mUtxoOptionsView.setUtxoViewButtonListener(this);
            mPickChannelsView.setVisibility(View.GONE);

            if (mDescription == null) {
                mDescriptionView.setVisibility(View.GONE);
            } else {
                mDescriptionView.setVisibility(View.VISIBLE);
                mDescriptionView.setContent(mDescription);
            }

            if (mFixedAmount != 0L) {
                mAmountInput.setFixedAmount(mFixedAmount);
            } else {
                // No specific amount was requested. Let User input an amount.
                mBtnSend.setButtonEnabled(false);
                setFeeFailure();
                mAmountInput.requestFocusDelayed();
            }

            // Action when clicked on "Send payment"
            mBtnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideKeyboard();
                    BBLog.d(LOG_TAG, "Trying to send on-chain payment...");
                    // Send on-chain payment

                    if (getSendAmount() != 0L) {

                        if (mOnChainFeeView.isLowerThanMinimum()) {
                            new UserGuardian(getContext(), new UserGuardian.OnGuardianConfirmedListener() {
                                @Override
                                public void onConfirmed() {
                                    performOnChainSend();
                                }

                                @Override
                                public void onCancelled() {

                                }
                            }).securityLowOnChainFee((int) mOnChainFeeView.getSatPerVByteFee());
                        } else {
                            performOnChainSend();
                        }
                    } else {
                        // Send amount == 0
                        Toast.makeText(getActivity(), "Send amount is to small.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            // Lightning Payment
            mAmountInput.setOnChain(false);
            mAmountInput.setSendAllEnabled(false);
            mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_lightning);
            mResultView.setTypeIcon(R.drawable.ic_nav_wallet_black_24dp);
            mProgressScreen.setProgressTypeIcon(R.drawable.ic_nav_wallet_black_24dp);
            mBSDScrollableMainView.setTitle(R.string.send_lightningPayment);
            mUtxoOptionsView.setVisibility(View.GONE);
            mPickChannelsView.setVisibility(FeatureManager.isChannelPickingOnSendEnabled() ? View.VISIBLE : View.GONE);
            mPickChannelsView.setPickChannelsViewButtonListener(this);

            if (mIsBolt12Offer) {
                // Bolt 12
                if (mDecodedBolt12.getIssuer() == null || mDecodedBolt12.getIssuer().isEmpty()) {
                    mPayeeView.setVisibility(View.GONE);
                } else {
                    mPayeeView.setVisibility(View.VISIBLE);
                    mPayeeView.setContent(mDecodedBolt12.getIssuer());
                }

                mPcvComment.setupCharLimit(200);
                mPcvComment.setVisibility(View.VISIBLE);

                if (mDecodedBolt12.getDescription() == null || mDecodedBolt12.getDescription().isEmpty()) {
                    mDescriptionView.setVisibility(View.GONE);
                } else {
                    mDescriptionView.setVisibility(View.VISIBLE);
                    mDescriptionView.setContent(mDecodedBolt12.getDescription());
                }

                if (mDecodedBolt12.hasAmountSpecified()) {
                    // A specific amount was requested. We are not allowed to change the amount
                    mAmountInput.setFixedAmount(mDecodedBolt12.getAmount());
                } else {
                    // No specific amount was requested. Let User input an amount.
                    mBtnSend.setButtonEnabled(false);
                    setFeeFailure();
                    mAmountInput.requestFocusDelayed();
                }

                // Action when clicked on "Send payment"
                mBtnSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideKeyboard();
                        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs())
                            fetchInvoiceFromOffer();
                        else
                            Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Bolt 11
                mLightningFeeView.setVisibility(BackendManager.getCurrentBackend().supportsRoutingFeeEstimation() ? View.VISIBLE : View.GONE);

                if (mIsKeysend) {
                    mPayeeView.setContent(ContactsManager.getInstance().getNameByContactData(mKeysendPubkey));
                    mPcvComment.setupCharLimit(200);
                    mPcvComment.setVisibility(View.VISIBLE);
                    mPickChannelsView.setLastHopEnabled(false);
                } else {
                    mPayeeView.setContent(ContactsManager.getInstance().getNameByContactData(mDecodedBolt11.getDestinationPubKey()));
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

                if (mIsKeysend || mDecodedBolt11.hasNoDescription()) {
                    mDescriptionView.setVisibility(View.GONE);
                } else {
                    mDescriptionView.setVisibility(View.VISIBLE);
                    mDescriptionView.setContent(mDecodedBolt11.getDescription());
                }

                if (mIsKeysend || mDecodedBolt11.hasNoAmountSpecified()) {
                    mBtnSend.setButtonEnabled(false);
                    setFeeFailure();
                    mAmountInput.requestFocusDelayed();
                } else {
                    // A specific amount was requested. We are not allowed to change the amount
                    mAmountInput.setFixedAmount(mDecodedBolt11.getAmountRequested());
                }

                // Action when clicked on "Send payment"
                mBtnSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideKeyboard();
                        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs())
                            prepareStandardBolt11Payment();
                        else
                            Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        mFallbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((HomeActivity) getActivity()).analyzeString(mFallbackOnChainInvoice.toString());
                dismiss();
            }
        });

        return view;
    }

    private long getSendAmount() {
        return mAmountInput.getAmount();
    }

    private void performOnChainSend() {
        long sendAmount = getSendAmount();
        mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(sendAmount, false));

        switchToSendProgressScreen();

        SendOnChainPaymentRequest sendOnChainPaymentRequest = SendOnChainPaymentRequest.newBuilder()
                .setAddress(mOnChainAddress)
                .setAmount(sendAmount)
                .setSatPerVByte(mOnChainFeeView.getSatPerVByteFee())
                .setUTXOs(mUtxoOptionsView.getSelectedUTXOs())
                .setSendAll(mAmountInput.getSendAllChecked())
                .build();

        getCompositeDisposable().add(BackendManager.api().sendOnChainPayment(sendOnChainPaymentRequest)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    BBLog.d(LOG_TAG, "On-chain payment successful.");

                    // updated the history, so it is shown the next time the user views it
                    Wallet_TransactionHistory.getInstance().updateOnChainTransactionHistory();

                    // show success animation
                    mHandler.postDelayed(() -> switchToSuccessScreen(), 500);
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
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Exception in send coins request task.");
                    BBLog.e(LOG_TAG, throwable.getMessage());

                    String errorPrefix = getResources().getString(R.string.error).toUpperCase() + ":";
                    String errormessage = throwable.getMessage().replace("UNKNOWN:", errorPrefix);
                    mHandler.postDelayed(() -> switchToFailedScreen(errormessage), 300);
                }));
    }

    @Override
    public void onDestroyView() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void showFeeAlertDialog(String message) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getContext())
                .setTitle(R.string.fee_limit_title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> prepareStandardBolt11Payment())
                .setNegativeButton(R.string.no, (dialog, whichButton) -> {
                });
        Dialog dlg = adb.create();
        // Apply FLAG_SECURE to dialog to prevent screen recording
        if (PrefsUtil.isScreenRecordingPrevented()) {
            dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        dlg.show();
    }

    private void fetchInvoiceFromOffer() {
        switchToSendProgressScreen();
        mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(getSendAmount(), true));

        FetchInvoiceFromOfferRequest.Builder requestBuilder = FetchInvoiceFromOfferRequest.newBuilder()
                .setDecodedBolt12(mDecodedBolt12)
                .setAmount(getSendAmount());

        if (!(mPcvComment.getData() == null || mPcvComment.getData().isEmpty()))
            requestBuilder.setComment(mPcvComment.getData());

        getCompositeDisposable().add(BackendManager.api().fetchInvoiceFromBolt12Offer(requestBuilder.build())
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                            prepareBolt12Payment(response);
                        }
                        , throwable -> {
                            BBLog.w(LOG_TAG, "Fetching invoice for offer failed: " + throwable.getMessage());
                            mHandler.postDelayed(() -> switchToFailedScreen(throwable.getMessage()), 300);
                        }));
    }

    private void prepareBolt12Payment(String fetchedInvoice) {
        SendLnPaymentRequest sendLnPaymentRequest = PaymentUtil.prepareBolt12InvoicePayment(fetchedInvoice, getSendAmount());
        sendLightningPayment(sendLnPaymentRequest);
    }

    private void prepareStandardBolt11Payment() {
        switchToSendProgressScreen();
        mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(getSendAmount(), true));

        SendLnPaymentRequest sendLnPaymentRequest = null;
        if (mIsKeysend)
            sendLnPaymentRequest = PaymentUtil.prepareKeysendPayment(mKeysendPubkey, getSendAmount(), mPcvComment.getData(), mPickChannelsView.getFirstHop(), mPickChannelsView.getLastHopPubkey());
        else {
            sendLnPaymentRequest = PaymentUtil.prepareBolt11InvoicePayment(mDecodedBolt11, getSendAmount(), mPickChannelsView.getFirstHop(), mPickChannelsView.getLastHopPubkey(), -1);
        }
        sendLightningPayment(sendLnPaymentRequest);
    }

    private void sendLightningPayment(SendLnPaymentRequest lnPaymentRequest) {
        PaymentUtil.sendLnPayment(lnPaymentRequest, getCompositeDisposable(), new PaymentUtil.OnPaymentResult() {
            @Override
            public void onSuccess(SendLnPaymentResponse sendLnPaymentResponse) {
                mHandler.postDelayed(() -> switchToSuccessScreen(), 300);
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
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_keysend_not_enabled_on_remote);
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

    private void switchToSendProgressScreen() {
        mProgressScreen.setVisibility(View.VISIBLE);
        mInputLayout.setVisibility(View.INVISIBLE);
        mProgressScreen.startSpinning();
        mBSDScrollableMainView.animateTitleOut();
    }

    private void switchToSuccessScreen() {
        mProgressScreen.spinningFinished(true);
        TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
        mInputLayout.setVisibility(View.GONE);
        mResultView.setVisibility(View.VISIBLE);
        mResultView.setHeading(R.string.send_success, true);
    }

    private void switchToFailedScreen(String error) {
        mProgressScreen.spinningFinished(false);
        TransitionManager.beginDelayedTransition(mContentTopLayout);
        mInputLayout.setVisibility(View.GONE);
        mResultView.setVisibility(View.VISIBLE);

        // Set failed states
        mResultView.setHeading(R.string.send_fail, false);
        mResultView.setDetailsText(error);

        if (!mOnChain && mFallbackOnChainInvoice != null) {
            mFallbackButton.setVisibility(View.VISIBLE);
        }
    }

    private void calculateFee() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            setCalculatingFee();
            if (mOnChain) {
                estimateOnChainTransactionSize(mOnChainAddress, getSendAmount());
            } else {
                estimateRoutingFee();
            }
        } else {
            setFeeFailure();
        }
    }

    /**
     * Show progress while calculating fee
     */
    private void setCalculatingFee() {
        if (mOnChain) {
            mOnChainFeeView.onCalculating();
        } else {
            mLightningFeeView.onCalculating();
        }
    }

    /**
     * Show the calculated fee
     */
    private void setCalculatedFeeAmountOnChain(long vByte) {
        mOnChainFeeView.onSizeCalculatedSuccess(vByte);
    }

    private void setCalculatedFeeAmountLightning(long amount) {
        double calculatedFeePercent = (amount / (double) getSendAmount());
        String feePercentageString = "(" + String.format("%.2f", calculatedFeePercent * 100) + "%)";
        mLightningFeeView.setAmountMsat(amount, feePercentageString, false);
    }

    /**
     * Show fee calculation failure
     */
    private void setFeeFailure() {
        if (mOnChain) {
            mOnChainFeeView.onSizeCalculationFailure();
        } else {
            mLightningFeeView.onFeeFailure();
        }
    }

    /**
     * This function is used to calculate the expected on chain fee.
     */
    private void estimateOnChainTransactionSize(String address, long amount) {
        if (!BackendManager.getCurrentBackend().supportsAbsoluteOnChainFeeEstimation()) {
            setFeeFailure();
            return;
        }
        getCompositeDisposable().add(BackendManager.api().getTransactionSizeVByte(address, amount)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> setCalculatedFeeAmountOnChain((long) response.doubleValue()),
                        throwable -> {
                            BBLog.w(LOG_TAG, "Exception in on-chain transaction size request task.");
                            BBLog.w(LOG_TAG, throwable.getMessage());
                            setFeeFailure();
                        }));
    }

    private void estimateRoutingFee() {
        if (getSendAmount() == 0 || !BackendManager.getCurrentBackend().supportsRoutingFeeEstimation()) {
            setFeeFailure();
            return;
        }
        String pubKey;
        if (mIsKeysend)
            pubKey = mKeysendPubkey;
        else
            pubKey = mDecodedBolt11.getDestinationPubKey();
        getCompositeDisposable().add(BackendManager.api().estimateRoutingFee(pubKey, getSendAmount())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> setCalculatedFeeAmountLightning(response),
                        throwable -> {
                            BBLog.w(LOG_TAG, "Exception in lightning routing fee request task.");
                            BBLog.w(LOG_TAG, throwable.getMessage());
                            setFeeFailure();
                        }));
    }

    @Override
    public long onSelectUtxosClicked() {
        if (mAmountInput.getSendAllChecked())
            return 0;
        else
            return getSendAmount();
    }

    @Override
    public void onResetUtxoViewClicked() {
        mAmountInput.updateUtxoSelectionAmount(0);
        mOnChainFeeView.setUtxosSelectedFlag(false);
    }

    @Override
    public void onClearFocus() {
        mAmountInput.clearFocus();
        hideKeyboard();
    }

    @Override
    public long onSelectChannelClicked() {
        return getSendAmount();
    }

    @Override
    public void onResetPickedChannelClicked() {

    }
}