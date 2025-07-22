package app.michaelwuensch.bitbanana.listViews.channels;


import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.customView.BBRebalance_ChannelView;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.SelectedChannel;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.wallet.Wallet_Channels;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class RebalanceActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = RebalanceActivity.class.getSimpleName();

    public static final String EXTRA_CHANNEL_A = "channel1";

    private TextView mChannelALabel;
    private TextView mChannelBLabel;
    private BBRebalance_ChannelView mChannelViewA;
    private BBRebalance_ChannelView mChannelViewB;
    private BBRebalance_ChannelView mChannelViewASuccess;
    private BBRebalance_ChannelView mChannelViewBSuccess;
    private BBButton mChannelASelectButton;
    private BBButton mChannelBSelectButton;
    private SeekBar mSlider;
    private BBInputFieldView mMaxFeeInput;
    private TextView mAmountLabel;
    private AmountView mRebalanceAmount;
    private AmountView mRebalanceFee;
    private BBButton mResetButton;
    private BBButton mRebalanceButton;
    private ConstraintLayout mContentLayout;
    private View mResultView;
    private TextView mResultDetails;
    private View mInputLayout;
    private BBButton mSuccessOkButton;
    private AmountView mSuccessFee;

    private OpenChannel mChannelA;
    private OpenChannel mChannelB;
    private boolean isAtoB;
    private DecodedBolt11 mDecodedBolt11;
    private ActivityResultLauncher<Intent> mActivityResultLauncher;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rebalance_channels);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mChannelA = (OpenChannel) extras.getSerializable(EXTRA_CHANNEL_A);
        }

        mChannelALabel = findViewById(R.id.channelALabel);
        mChannelBLabel = findViewById(R.id.channelBLabel);
        mChannelViewA = findViewById(R.id.channelA);
        mChannelViewB = findViewById(R.id.channelB);
        mChannelViewASuccess = findViewById(R.id.channelASuccessScreen);
        mChannelViewBSuccess = findViewById(R.id.channelBSuccessScreen);
        mChannelASelectButton = findViewById(R.id.channelASelectButton);
        mChannelBSelectButton = findViewById(R.id.channelBSelectButton);
        mSlider = findViewById(R.id.slider);
        mMaxFeeInput = findViewById(R.id.maxFeeInput);
        mAmountLabel = findViewById(R.id.amountAndDirectionLabel);
        mRebalanceAmount = findViewById(R.id.rebalanceAmount);
        mRebalanceFee = findViewById(R.id.rebalanceFee);
        mResetButton = findViewById(R.id.resetButton);
        mRebalanceButton = findViewById(R.id.rebalanceButton);
        mContentLayout = findViewById(R.id.contentLayout);
        mInputLayout = findViewById(R.id.inputLayout);
        mSuccessOkButton = findViewById(R.id.okButton);
        mSuccessFee = findViewById(R.id.successFee);
        mResultView = findViewById(R.id.resultContent);
        mResultDetails = findViewById(R.id.resultDetails);

        mChannelViewA.setVisibility(View.GONE);
        mChannelViewB.setVisibility(View.GONE);
        mSlider.setEnabled(false);
        mRebalanceButton.setButtonEnabled(false);
        mChannelASelectButton.setText(getString(R.string.select) + " ...");
        mChannelBSelectButton.setText(getString(R.string.select) + " ...");

        // Initialize the ActivityResultLauncher for Channel selection
        mActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        // Pass result to the pick channels view
                        handleActivityResult(data);
                    }
                }
        );

        mChannelALabel.setText(getString(R.string.channel) + " A");
        mChannelBLabel.setText(getString(R.string.channel) + " B");

        mRebalanceAmount.setAmountMsat(0);
        mRebalanceFee.setAmountMsat(0);

        if (mChannelA != null) {
            mChannelViewA.setChannel(mChannelA);
            mChannelViewASuccess.setChannel(mChannelA);
            mChannelASelectButton.setVisibility(View.GONE);
            mChannelViewA.setVisibility(View.VISIBLE);
        }

        mChannelASelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RebalanceActivity.this, ManageChannelsActivity.class);
                intent.putExtra(ManageChannelsActivity.EXTRA_CHANNELS_ACTIVITY_MODE, ManageChannelsActivity.MODE_SELECT);
                intent.putExtra(ManageChannelsActivity.EXTRA_SELECTION_TYPE, ManageChannelsActivity.SELECTION_TYPE_REBALANCE_A);
                mActivityResultLauncher.launch(intent);
            }
        });

        mChannelBSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RebalanceActivity.this, ManageChannelsActivity.class);
                intent.putExtra(ManageChannelsActivity.EXTRA_CHANNELS_ACTIVITY_MODE, ManageChannelsActivity.MODE_SELECT);
                intent.putExtra(ManageChannelsActivity.EXTRA_SELECTION_TYPE, ManageChannelsActivity.SELECTION_TYPE_REBALANCE_B);
                mActivityResultLauncher.launch(intent);
            }
        });

        mSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateBalancing(i * 1000L);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mMaxFeeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mMaxFeeInput.setDescriptionDetail("(" + getResources().getString(R.string.percent) + ")");
        String lightning_feeLimit = PrefsUtil.getPrefs().getString(PrefsUtil.REBALANCE_FEE_LIMIT_PERCENT, "0.5%");
        String feePercent = lightning_feeLimit.replace("%", "");
        mMaxFeeInput.setValue(feePercent);
        mMaxFeeInput.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                PrefsUtil.editPrefs().putString(PrefsUtil.REBALANCE_FEE_LIMIT_PERCENT, charSequence.toString() + "%").apply();
                if (mChannelA != null && mChannelB != null)
                    updateBalancing(mSlider.getProgress() * 1000L);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSlider.setProgress(0);
                if (mChannelA != null & mChannelB != null)
                    updateBalancing(0);
            }
        });

        mRebalanceButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                rebalanceClicked();
            }
        });
    }

    private void handleActivityResult(Intent data) {
        OpenChannel selectedChannel = (OpenChannel) data.getSerializableExtra(ManageChannelsActivity.EXTRA_SELECTED_CHANNEL);
        int type = data.getIntExtra(ManageChannelsActivity.EXTRA_SELECTION_TYPE, -1);

        if (type == ManageChannelsActivity.SELECTION_TYPE_REBALANCE_A) {
            mChannelA = selectedChannel;
            mChannelViewA.setChannel(mChannelA);
            mChannelViewASuccess.setChannel(mChannelA);
            mChannelASelectButton.setVisibility(View.GONE);
            mChannelViewA.setVisibility(View.VISIBLE);
            if (mChannelB != null) {
                setupSlider();
            }
        }

        if (type == ManageChannelsActivity.SELECTION_TYPE_REBALANCE_B) {
            mChannelB = selectedChannel;
            mChannelViewB.setChannel(mChannelB);
            mChannelViewBSuccess.setChannel(mChannelB);
            mChannelBSelectButton.setVisibility(View.GONE);
            mChannelViewB.setVisibility(View.VISIBLE);
            if (mChannelA != null) {
                setupSlider();
            }
        }
    }

    private float getMaxFeePercent() {
        if (mMaxFeeInput.getData() == null || mMaxFeeInput.getData().isEmpty() || mMaxFeeInput.getData().equals("."))
            return 0;

        return Float.parseFloat(mMaxFeeInput.getData());
    }

    private void setupSlider() {
        long maxAtoB = Math.min(Math.max(mChannelA.getLocalBalance() - mChannelA.getLocalChannelConstraints().getChannelReserve(), 0), Math.max(mChannelB.getRemoteBalance() - mChannelB.getRemoteChannelConstraints().getChannelReserve(), 0));
        maxAtoB = maxAtoB / 1000L;  // truncate to sat
        long maxBtoA = Math.min(Math.max(mChannelB.getLocalBalance() - mChannelB.getLocalChannelConstraints().getChannelReserve(), 0), Math.max(mChannelA.getRemoteBalance() - mChannelA.getRemoteChannelConstraints().getChannelReserve(), 0));
        maxBtoA = maxBtoA / 1000L; // truncate to sat
        mSlider.setMin(-1 * (int) maxBtoA);
        mSlider.setMax((int) maxAtoB);
        mSlider.setProgress(0);
        mSlider.setEnabled(true);
    }

    private void updateBalancing(long i) {
        mRebalanceAmount.setAmountMsat(Math.abs(i));
        mChannelViewA.setBalances(mChannelA.getLocalBalance() - i, mChannelA.getRemoteBalance() + i);
        mChannelViewB.setBalances(mChannelB.getLocalBalance() + i, mChannelB.getRemoteBalance() - i);

        if (i == 0) {
            mRebalanceButton.setButtonEnabled(false);
            mAmountLabel.setText(getString(R.string.amount));
            mRebalanceFee.setAmountMsat(0);
        } else if (i > 0) {
            isAtoB = true;
            mRebalanceButton.setButtonEnabled(true);
            mAmountLabel.setText(getString(R.string.amount) + " (A --> B)");
            if (mMaxFeeInput.getData() != null)
                mRebalanceFee.setAmountMsat((long) (i * (getMaxFeePercent() / 100)));
            else
                mRebalanceFee.setAmountMsat(0);
        } else {
            isAtoB = false;
            mRebalanceButton.setButtonEnabled(true);
            mAmountLabel.setText(getString(R.string.amount) + " (B --> A)");
            if (mMaxFeeInput.getData() != null)
                mRebalanceFee.setAmountMsat((long) (Math.abs(i) * (getMaxFeePercent() / 100)));
            else
                mRebalanceFee.setAmountMsat(0);
        }
    }

    private void rebalanceClicked() {
        // Validate input
        if (mMaxFeeInput.getData() == null) {
            showError(getString(R.string.error_channel_rebalance_no_fee_rate), RefConstants.ERROR_DURATION_MEDIUM);
            return;
        }
        if (getMaxFeePercent() > 5.0) {
            showError(getString(R.string.error_routing_policy_fee_rate_too_high, 5.0), RefConstants.ERROR_DURATION_MEDIUM);
            return;
        }

        mRebalanceButton.showProgress();
        mSlider.setEnabled(false);
        mMaxFeeInput.setEnabled(false);

        if (mDecodedBolt11 != null && mDecodedBolt11.getAmountRequested() == mRebalanceAmount.getAmount() && !mDecodedBolt11.isExpired()) {
            BBLog.v(LOG_TAG, "Reusing existing invoice.");
            prepareLightningPayment();
        } else {
            createInvoiceRequest();
        }
    }

    private void createInvoiceRequest() {
        BBLog.v(LOG_TAG, "Create new bolt11 invoice...");

        CreateInvoiceRequest request = CreateInvoiceRequest.newBuilder()
                .setAmount(mRebalanceAmount.getAmount())
                .setDescription("Channel rebalance")
                .setExpiry(300)
                .build();

        mCompositeDisposable.add(BackendManager.api().createInvoice(request)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                    BBLog.v(LOG_TAG, "Invoice created successfully.");
                    String bolt11 = response.getBolt11();
                    mDecodedBolt11 = InvoiceUtil.decodeBolt11(bolt11);
                    prepareLightningPayment();
                }, throwable -> {
                    showError(throwable.getMessage(), 3000);
                    BBLog.e(LOG_TAG, "Failed to create invoice for rebalancing: " + throwable.getMessage());
                }));
    }

    private void prepareLightningPayment() {
        SelectedChannel firstHop = isAtoB
                ? SelectedChannel.newBuilder().setShortChannelId(mChannelA.getShortChannelId()).setRemotePubKey(mChannelA.getRemotePubKey()).build()
                : SelectedChannel.newBuilder().setShortChannelId(mChannelB.getShortChannelId()).setRemotePubKey(mChannelB.getRemotePubKey()).build();
        SelectedChannel lastHop = isAtoB
                ? SelectedChannel.newBuilder().setShortChannelId(mChannelB.getShortChannelId()).setRemotePubKey(mChannelB.getRemotePubKey()).build()
                : SelectedChannel.newBuilder().setShortChannelId(mChannelA.getShortChannelId()).setRemotePubKey(mChannelA.getRemotePubKey()).build();
        SendLnPaymentRequest sendLnPaymentRequest = PaymentUtil.prepareBolt11InvoicePayment(mDecodedBolt11, mDecodedBolt11.getAmountRequested(), firstHop, lastHop, getMaxFeePercent());
        sendLightningPayment(sendLnPaymentRequest);
    }

    private void sendLightningPayment(SendLnPaymentRequest lnPaymentRequest) {
        BBLog.v(LOG_TAG, "Trying to pay the circular payment...");
        PaymentUtil.sendLnPayment(lnPaymentRequest, mCompositeDisposable, new PaymentUtil.OnPaymentResult() {
            @Override
            public void onSuccess(SendLnPaymentResponse sendLnPaymentResponse) {
                BBLog.i(LOG_TAG, "Rebalance success!");
                showSuccessScreen(sendLnPaymentResponse);
                Wallet_Channels.getInstance().updateChannelsWithDebounce();
            }

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onError(String error, SendLnPaymentResponse sendLnPaymentResponse, int duration) {
                mRebalanceButton.hideProgress();
                mSlider.setEnabled(true);
                mMaxFeeInput.setEnabled(true);
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
                                errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_no_route, getMaxFeePercent());
                            break;
                        case INSUFFICIENT_FUNDS:
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_channel_rebalance_insufficient_funds);
                            break;
                        case INCORRECT_PAYMENT_DETAILS:
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_keysend_not_enabled_on_remote);
                            break;
                        case CANCELED:
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_canceled);
                            break;
                        default:
                            if (sendLnPaymentResponse.getFailureMessage() != null)
                                errorMessage = errorPrefix + "\n\n" + sendLnPaymentResponse.getFailureMessage();
                            else
                                errorMessage = errorPrefix + "\n\n" + error;
                            break;
                    }
                } else {
                    errorMessage = errorPrefix + "\n\n" + error;
                }
                showError(errorMessage, duration);
            }
        });
    }

    private void showSuccessScreen(SendLnPaymentResponse sendLnPaymentResponse) {
        TransitionManager.beginDelayedTransition(mContentLayout);
        mSuccessOkButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                finish();
            }
        });
        mResultDetails.setText(getString(R.string.fee_paid) + ":");
        mSuccessFee.setAmountMsat(sendLnPaymentResponse.getFee());
        mInputLayout.setVisibility(View.GONE);
        mSuccessOkButton.setVisibility(View.VISIBLE);
        mResultView.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // The final value you want to interpolate to
                final long finalValue = mSlider.getProgress() * 1000L;

                // Create an animator that goes from 0f to 1f over 1 second
                ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
                animator.setDuration(1000);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());

                animator.addUpdateListener(animation -> {
                    // Get the "completion fraction" from the animator, which goes from 0 to 1
                    float fraction = (float) animation.getAnimatedValue();

                    // Interpolate i based on the fraction. When fraction=0, i=0; when fraction=1, i=finalValue
                    long i = (long) (finalValue * fraction);

                    mChannelViewASuccess.setBalances(mChannelA.getLocalBalance() - i, mChannelA.getRemoteBalance() + i);
                    mChannelViewBSuccess.setBalances(mChannelB.getLocalBalance() + i, mChannelB.getRemoteBalance() - i);
                });

                // Finally, start the animation
                animator.start();
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(RebalanceActivity.this, getString(R.string.help_dialog_rebalancing));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
