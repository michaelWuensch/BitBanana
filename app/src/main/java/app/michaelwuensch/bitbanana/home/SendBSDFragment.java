package app.michaelwuensch.bitbanana.home;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.ExpandableTextView;
import app.michaelwuensch.bitbanana.customView.LightningFeeView;
import app.michaelwuensch.bitbanana.customView.NumpadView;
import app.michaelwuensch.bitbanana.customView.OnChainFeeView;
import app.michaelwuensch.bitbanana.customView.PaymentCommentView;
import app.michaelwuensch.bitbanana.customView.UtxoOptionsView;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.models.FetchInvoiceFromOfferRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.models.SendOnChainPaymentRequest;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.DebounceHandler;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;


public class SendBSDFragment extends BaseBSDFragment implements UtxoOptionsView.OnUtxoSelectClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = SendBSDFragment.class.getSimpleName();

    private ActivityResultLauncher<Intent> mActivityResultLauncher;

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDProgressView mProgressScreen;
    private BSDResultView mResultView;
    private ConstraintLayout mContentTopLayout;
    private ConstraintLayout mInputLayout;
    private EditText mEtAmount;
    private ExpandableTextView mExtvMemo;
    private TextView mTvUnit;
    private OnChainFeeView mOnChainFeeView;
    private LightningFeeView mLightningFeeView;
    private NumpadView mNumpad;
    private Button mBtnSend;
    private Button mFallbackButton;
    private ImageView mPayeeLine;
    private TextView mPayeeLabel;
    private TextView mPayee;
    private PaymentCommentView mPcvComment;
    private UtxoOptionsView mUtxoOptionsView;

    private DecodedBolt11 mDecodedBolt11;
    private DecodedBolt12 mDecodedBolt12;
    private Bip21Invoice mFallbackOnChainInvoice;
    private String mMemo;
    private String mOnChainAddress;
    private boolean mOnChain;
    private boolean mIsBolt12Offer;
    private long mFixedAmount;
    private Handler mHandler;
    private boolean mAmountValid = true;
    private String mKeysendPubkey;
    private boolean mIsKeysend;
    private long mSendAmount;
    private boolean mBlockOnInputChanged;
    private View mRootView;
    private DebounceHandler mFeeCaclulationDebounceHandler = new DebounceHandler();

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
            mMemo = args.getString("onChainMessage");
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
        mRootView = view;

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mProgressScreen = view.findViewById(R.id.paymentProgressLayout);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mInputLayout = view.findViewById(R.id.inputLayout);
        mEtAmount = view.findViewById(R.id.sendAmount);
        mTvUnit = view.findViewById(R.id.sendUnit);
        mExtvMemo = view.findViewById(R.id.sendMemo);
        mOnChainFeeView = view.findViewById(R.id.sendFeeOnChainLayout);
        mLightningFeeView = view.findViewById(R.id.sendFeeLightningLayout);
        mNumpad = view.findViewById(R.id.numpadView);
        mBtnSend = view.findViewById(R.id.sendButton);
        mFallbackButton = view.findViewById(R.id.fallbackButton);
        mPayeeLine = view.findViewById(R.id.line);
        mPayeeLabel = view.findViewById(R.id.payeeSourceLabel);
        mPayee = view.findViewById(R.id.sendPayee);
        mPcvComment = view.findViewById(R.id.paymentComment);
        mUtxoOptionsView = view.findViewById(R.id.utxoOptions);

        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mResultView.setOnOkListener(this::dismiss);

        mHandler = new Handler();
        PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

        mNumpad.bindEditText(mEtAmount);

        // Initialize the ActivityResultLauncher
        mActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        // Pass result to the custom view
                        mUtxoOptionsView.handleActivityResult(data);
                    }
                }
        );
        mUtxoOptionsView.setActivityResultLauncher(mActivityResultLauncher);


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
                }

                if (!mEtAmount.getText().toString().equals(".")) {
                    // make text red if input is too large
                    long maxSendable;
                    if (mOnChain) {
                        maxSendable = Wallet_Balance.getInstance().getBalances().onChainConfirmed();
                    } else {
                        maxSendable = WalletUtil.getMaxLightningSendAmount();
                    }

                    if (mSendAmount > maxSendable) {
                        if (mOnChain) {
                            if (mSendAmount < Wallet_Balance.getInstance().getBalances().onChainTotal()) {
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
                        mEtAmount.setTextColor(getResources().getColor(R.color.red));
                        setSendButtonEnabled(false);
                    } else {
                        mEtAmount.setTextColor(getResources().getColor(R.color.white));
                        setSendButtonEnabled(true);
                    }
                    if (mSendAmount == 0 && mFixedAmount == 0L) {
                        setSendButtonEnabled(false);
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
                    mSendAmount = MonetaryUtil.getInstance().convertCurrentCurrencyTextInputToMsat(arg0.toString());
                    calculateFee();
                } else {
                    setFeeFailure();
                }
            }
        });


        if (mOnChain) {
            mPayee.setText(mOnChainAddress);
            mOnChainFeeView.initialSetup();

            mOnChainFeeView.setVisibility(View.VISIBLE);
            mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_on_chain);
            mResultView.setTypeIcon(R.drawable.ic_onchain_black_24dp);
            mProgressScreen.setProgressTypeIcon(R.drawable.ic_onchain_black_24dp);
            mBSDScrollableMainView.setTitle(R.string.send_onChainPayment);
            mPcvComment.setVisibility(View.GONE);
            mUtxoOptionsView.setVisibility(FeatureManager.isUtxoSelectionOnSendEnabled() ? View.VISIBLE : View.GONE);
            mUtxoOptionsView.setUtxoSelectClickListener(this);

            if (mMemo == null) {
                mExtvMemo.setVisibility(View.GONE);
            } else {
                mExtvMemo.setVisibility(View.VISIBLE);
                mExtvMemo.setContent(R.string.memo, mMemo);
            }

            if (mFixedAmount != 0L) {
                // A specific amount was requested. We are not allowed to change the amount.
                mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, !mOnChain));
                mEtAmount.clearFocus();
                mEtAmount.setFocusable(false);
                mEtAmount.setEnabled(false);
            } else {
                // No specific amount was requested. Let User input an amount.
                mNumpad.setVisibility(View.VISIBLE);
                setSendButtonEnabled(false);
                setFeeFailure();

                mHandler.postDelayed(() -> {
                    // We have to call this delayed, as otherwise it will still bring up the softKeyboard
                    mEtAmount.requestFocus();
                }, 600);
            }

            // Action when clicked on "Send payment"
            mBtnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    BBLog.d(LOG_TAG, "Trying to send on-chain payment...");
                    // Send on-chain payment

                    if (getOnChainSendAmount() != 0L) {

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
            mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_lightning);
            mResultView.setTypeIcon(R.drawable.ic_nav_wallet_black_24dp);
            mProgressScreen.setProgressTypeIcon(R.drawable.ic_nav_wallet_black_24dp);
            mBSDScrollableMainView.setTitle(R.string.send_lightningPayment);
            mUtxoOptionsView.setVisibility(View.GONE);

            if (mIsBolt12Offer) {
                // Bolt 12
                if (mDecodedBolt12.getIssuer() == null || mDecodedBolt12.getIssuer().isEmpty()) {
                    mPayeeLine.setVisibility(View.GONE);
                    mPayeeLabel.setVisibility(View.GONE);
                    mPayee.setVisibility(View.GONE);
                } else {
                    mPayee.setVisibility(View.VISIBLE);
                    mPayee.setText(mDecodedBolt12.getIssuer());
                }

                mPcvComment.setupCharLimit(200);
                mPcvComment.setVisibility(View.VISIBLE);

                if (mDecodedBolt12.getDescription() == null || mDecodedBolt12.getDescription().isEmpty()) {
                    mExtvMemo.setVisibility(View.GONE);
                } else {
                    mExtvMemo.setVisibility(View.VISIBLE);
                    mExtvMemo.setContent(R.string.memo, mDecodedBolt12.getDescription());
                }

                if (mDecodedBolt12.hasAmountSpecified()) {
                    // A specific amount was requested. We are not allowed to change the amount
                    mFixedAmount = mDecodedBolt12.getAmount();
                    mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, !mOnChain));
                    mEtAmount.clearFocus();
                    mEtAmount.setFocusable(false);
                } else {
                    // No specific amount was requested. Let User input an amount.
                    mNumpad.setVisibility(View.VISIBLE);
                    setSendButtonEnabled(false);
                    setFeeFailure();

                    mHandler.postDelayed(() -> {
                        // We have to call this delayed, as otherwise it will still bring up the softKeyboard
                        mEtAmount.requestFocus();
                    }, 600);
                }

                // Action when clicked on "Send payment"
                mBtnSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                    mPayee.setText(ContactsManager.getInstance().getNameByContactData(mKeysendPubkey));
                    mPcvComment.setupCharLimit(200);
                    mPcvComment.setVisibility(View.VISIBLE);
                } else {
                    mPayee.setText(ContactsManager.getInstance().getNameByContactData(mDecodedBolt11.getDestinationPubKey()));
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

                if (mIsKeysend || mDecodedBolt11.hasNoDescription()) {
                    mExtvMemo.setVisibility(View.GONE);
                } else {
                    mExtvMemo.setVisibility(View.VISIBLE);
                    mExtvMemo.setContent(R.string.memo, mDecodedBolt11.getDescription());
                }

                if (mIsKeysend || mDecodedBolt11.hasNoAmountSpecified()) {
                    // No specific amount was requested. Let User input an amount.
                    mNumpad.setVisibility(View.VISIBLE);
                    setSendButtonEnabled(false);
                    setFeeFailure();

                    mHandler.postDelayed(() -> {
                        // We have to call this delayed, as otherwise it will still bring up the softKeyboard
                        mEtAmount.requestFocus();
                    }, 600);
                } else {
                    // A specific amount was requested. We are not allowed to change the amount
                    mFixedAmount = mDecodedBolt11.getAmountRequested();
                    mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, !mOnChain));
                    mEtAmount.clearFocus();
                    mEtAmount.setFocusable(false);
                }

                // Action when clicked on "Send payment"
                mBtnSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs())
                            prepareStandardBolt11Payment();
                        else
                            Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }


        // Action when clicked on unit
        if (MonetaryUtil.getInstance().hasMoreThanOneCurrency()) {
            LinearLayout llUnit = view.findViewById(R.id.sendUnitLayout);
            llUnit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBlockOnInputChanged = true;
                    MonetaryUtil.getInstance().switchToNextCurrency();
                    if (mFixedAmount == 0L) {
                        mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mSendAmount, !mOnChain));
                    } else {
                        mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, !mOnChain));
                    }
                    mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
                    mBlockOnInputChanged = false;
                }
            });
        } else {
            view.findViewById(R.id.sendSwitchUnitImage).setVisibility(View.GONE);
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

    private long getOnChainSendAmount() {
        long sendAmount = 0L;
        if (mFixedAmount != 0L) {
            sendAmount = mFixedAmount;
        } else {
            sendAmount = mSendAmount;
        }
        return sendAmount;
    }

    private void performOnChainSend() {
        long sendAmount = getOnChainSendAmount();
        mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(sendAmount, false));

        switchToSendProgressScreen();

        SendOnChainPaymentRequest sendOnChainPaymentRequest = SendOnChainPaymentRequest.newBuilder()
                .setAddress(mOnChainAddress)
                .setAmount(sendAmount)
                .setSatPerVByte(mOnChainFeeView.getSatPerVByteFee())
                .setUTXOs(mUtxoOptionsView.getSelectedUTXOs())
                .build();

        getCompositeDisposable().add(BackendManager.api().sendOnChainPayment(sendOnChainPaymentRequest)
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
        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);

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
        mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(getLightningPaymentAmountMSat(), true));

        FetchInvoiceFromOfferRequest.Builder requestBuilder = FetchInvoiceFromOfferRequest.newBuilder()
                .setDecodedBolt12(mDecodedBolt12)
                .setAmount(getLightningPaymentAmountMSat());

        if (!(mPcvComment.getData() == null || mPcvComment.getData().isEmpty()))
            requestBuilder.setComment(mPcvComment.getData());

        getCompositeDisposable().add(BackendManager.api().fetchInvoiceFromBolt12Offer(requestBuilder.build())
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribe(response -> {
                            prepareBolt12Payment(response);
                        }
                        , throwable -> {
                            BBLog.w(LOG_TAG, "Fetching invoice for offer failed: " + throwable.getMessage());
                            mHandler.postDelayed(() -> switchToFailedScreen(throwable.getMessage()), 300);
                        }));
    }

    private void prepareBolt12Payment(String fetchedInvoice) {
        SendLnPaymentRequest sendLnPaymentRequest = PaymentUtil.prepareBolt12InvoicePayment(fetchedInvoice, getLightningPaymentAmountMSat());
        sendLightningPayment(sendLnPaymentRequest);
    }

    private void prepareStandardBolt11Payment() {
        switchToSendProgressScreen();
        mResultView.setDetailsText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(getLightningPaymentAmountMSat(), true));

        SendLnPaymentRequest sendLnPaymentRequest = null;
        if (mIsKeysend)
            sendLnPaymentRequest = PaymentUtil.prepareKeysendPayment(mKeysendPubkey, mSendAmount, mPcvComment.getData());
        else {
            sendLnPaymentRequest = PaymentUtil.prepareBolt11InvoicePayment(mDecodedBolt11, getLightningPaymentAmountMSat());
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
                estimateOnChainTransactionSize(mOnChainAddress, getOnChainSendAmount());
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
        double calculatedFeePercent = (amount / (double) getLightningPaymentAmountMSat());
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
                .subscribe(response -> setCalculatedFeeAmountOnChain((long) response.doubleValue()),
                        throwable -> {
                            BBLog.w(LOG_TAG, "Exception in on-chain transaction size request task.");
                            BBLog.w(LOG_TAG, throwable.getMessage());
                            setFeeFailure();
                        }));
    }

    private void estimateRoutingFee() {
        if (getLightningPaymentAmountMSat() == 0 || !BackendManager.getCurrentBackend().supportsRoutingFeeEstimation()) {
            setFeeFailure();
            return;
        }
        String pubKey;
        if (mIsKeysend)
            pubKey = mKeysendPubkey;
        else
            pubKey = mDecodedBolt11.getDestinationPubKey();
        getCompositeDisposable().add(BackendManager.api().estimateRoutingFee(pubKey, getLightningPaymentAmountMSat())
                .subscribe(response -> setCalculatedFeeAmountLightning(response),
                        throwable -> {
                            BBLog.w(LOG_TAG, "Exception in lightning routing fee request task.");
                            BBLog.w(LOG_TAG, throwable.getMessage());
                            setFeeFailure();
                        }));
    }

    private long getLightningPaymentAmountMSat() {
        if (mIsBolt12Offer) {
            if (mDecodedBolt12.hasAmountSpecified())
                return mDecodedBolt12.getAmount();
            else
                return mSendAmount;
        } else {
            if (mIsKeysend || mDecodedBolt11.hasNoAmountSpecified()) {
                return mSendAmount;
            } else {
                return mDecodedBolt11.getAmountRequested();
            }
        }
    }

    private void showError(String message, int duration) {
        Snackbar msg = Snackbar.make(mRootView.findViewById(R.id.coordinator), message, duration);
        View sbView = msg.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
        msg.show();
    }

    private void setSendButtonEnabled(boolean enabled) {
        if (enabled) {
            mBtnSend.setEnabled(true);
            mBtnSend.setTextColor(getResources().getColor(R.color.banana_yellow));
        } else {
            mBtnSend.setEnabled(false);
            mBtnSend.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    @Override
    public long onSelectUtxosClicked() {
        return getOnChainSendAmount();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key != null) {
            if (key.equals(PrefsUtil.CURRENT_CURRENCY_INDEX)) {
                if (mFixedAmount == 0L) {
                    mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mSendAmount, !mOnChain));
                } else {
                    mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mFixedAmount, !mOnChain));
                }
                mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
            }
        }
    }
}