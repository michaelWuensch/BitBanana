package app.michaelwuensch.bitbanana.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

import com.google.android.material.snackbar.Snackbar;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.NumpadView;
import app.michaelwuensch.bitbanana.customView.OnChainFeeView;
import app.michaelwuensch.bitbanana.customView.UtxoOptionsView;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_Channels;

public class OpenChannelBSDFragment extends BaseBSDFragment implements UtxoOptionsView.OnUtxoSelectClickListener, Wallet_Channels.ChannelOpenUpdateListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = OpenChannelBSDFragment.class.getSimpleName();
    public static final String ARGS_NODE_URI = "NODE_URI";

    private ActivityResultLauncher<Intent> mActivityResultLauncher;

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private ConstraintLayout mContentTopLayout;
    private ConstraintLayout mOpenChannelInputLayout;
    private NumpadView mNumpad;
    private EditText mEtAmount;
    private TextView mTvUnit;
    private boolean mAmountValid = false;
    private BBButton mOpenChannelButton;
    private TextView mTvNodeAlias;
    private AmountView mTvOnChainFunds;
    private LightningNodeUri mLightningNodeUri;
    private OnChainFeeView mOnChainFeeView;
    private CheckBox mPrivateCheckbox;
    private View mVSwitchImage;
    private long mValueChannelCapacity;
    private boolean mBlockOnInputChanged;
    private long mOnChainUnconfirmed;
    private long mOnChainConfirmed;
    private UtxoOptionsView mUtxoOptionsView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_open_channel, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mProgressView = view.findViewById(R.id.paymentProgressLayout);
        mOpenChannelInputLayout = view.findViewById(R.id.openChannelInputLayout);
        mNumpad = view.findViewById(R.id.numpadView);
        mTvNodeAlias = view.findViewById(R.id.nodeAliasText);
        mTvOnChainFunds = view.findViewById(R.id.onChainFunds);
        mEtAmount = view.findViewById(R.id.localAmount);
        mTvUnit = view.findViewById(R.id.localAmountUnit);
        mOpenChannelButton = view.findViewById(R.id.openChannelButton);
        mOnChainFeeView = view.findViewById(R.id.sendFeeOnChainLayout);
        mPrivateCheckbox = view.findViewById(R.id.privateCheckBox);
        mVSwitchImage = view.findViewById(R.id.localAmountSwitchUnitImage);

        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mBSDScrollableMainView.setTitle(R.string.channel_open);
        mResultView.setOnOkListener(this::dismiss);
        mNumpad.bindEditText(mEtAmount);
        mUtxoOptionsView = view.findViewById(R.id.utxoOptions);

        mUtxoOptionsView.setVisibility(FeatureManager.isUtxoSelectionOnChannelOpenEnabled() ? View.VISIBLE : View.GONE);
        mUtxoOptionsView.setUtxoSelectClickListener(this);

        PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

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

        mOnChainFeeView.initialSetup();
        setFeeFailure();

        Wallet_Channels.getInstance().registerChannelOpenUpdateListener(this);

        if (getArguments() != null) {
            mLightningNodeUri = (LightningNodeUri) getArguments().getSerializable(ARGS_NODE_URI);
            setAlias(mLightningNodeUri);
        }

        setAvailableFunds();
        ImageButton privateHelpButton = view.findViewById(R.id.privateHelpButton);
        if (FeatureManager.isHelpButtonsEnabled()) {
            privateHelpButton.setVisibility(View.VISIBLE);
            privateHelpButton.setOnClickListener(view1 -> HelpDialogUtil.showDialog(getActivity(), R.string.help_dialog_private_channels));
        } else {
            privateHelpButton.setVisibility(View.GONE);
        }

        // Input validation for the amount field.
        mEtAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // remove the last inputted character if not valid
                if (!mAmountValid) {
                    mNumpad.removeOneDigit();
                }
            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before,
                                      int count) {
                if (mBlockOnInputChanged)
                    return;

                // validate input
                mAmountValid = MonetaryUtil.getInstance().validateCurrentCurrencyInput(arg0.toString(), false);

                // calculate fees
                if (mAmountValid) {
                    mValueChannelCapacity = MonetaryUtil.getInstance().convertCurrentCurrencyTextInputToMsat(mEtAmount.getText().toString());
                    calculateTransactionSize();
                } else {
                    setFeeFailure();
                }
            }
        });

        // Action when clicked on receive unit
        if (MonetaryUtil.getInstance().hasMoreThanOneCurrency()) {
            mVSwitchImage.setVisibility(View.VISIBLE);
            LinearLayout llUnit = view.findViewById(R.id.sendUnitLayout);
            llUnit.setOnClickListener(v -> {
                mBlockOnInputChanged = true;
                MonetaryUtil.getInstance().switchToNextCurrency();
                mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mValueChannelCapacity, false));
                mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
                setAvailableFunds();
                mBlockOnInputChanged = false;
            });
        } else {
            mVSwitchImage.setVisibility(View.GONE);
        }

        mNumpad.setVisibility(View.VISIBLE);

        mOpenChannelButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (!mAmountValid || mEtAmount.getText().toString().equals(".")) {
                    // no real amount
                    showError(getResources().getString(R.string.amount_invalid), Snackbar.LENGTH_LONG);
                    return;
                }

                // ToDo: values are from LND. Make it generic? Support Wumbo channels?
                long minSendAmount = 20000 * 1000L;
                long absoluteMaxSendAmount = 17666215 * 1000L;
                long maxSendAmount = absoluteMaxSendAmount;

                if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                    long onChainAvailable = Wallet_Balance.getInstance().getBalances().onChainConfirmed();
                    long onChainUnconfirmed = Wallet_Balance.getInstance().getBalances().onChainUnconfirmed();

                    if (onChainAvailable < maxSendAmount) {
                        maxSendAmount = onChainAvailable;
                    }

                    if (mValueChannelCapacity < minSendAmount) {
                        // amount is to small
                        String message = getResources().getString(R.string.min_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(minSendAmount, false);
                        showError(message, Snackbar.LENGTH_LONG);
                        return;
                    }

                    if (mValueChannelCapacity > maxSendAmount) {
                        // amount is to big
                        if (mValueChannelCapacity > absoluteMaxSendAmount) {
                            String message = getResources().getString(R.string.max_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(absoluteMaxSendAmount, false);
                            showError(message, Snackbar.LENGTH_LONG);
                            return;
                        } else {
                            if (mValueChannelCapacity < (onChainAvailable + onChainUnconfirmed)) {
                                String message = getResources().getString(R.string.error_funds_not_confirmed_yet);
                                showError(message, 10000);
                                return;
                            } else {
                                String message = getResources().getString(R.string.error_insufficient_on_chain_funds) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(onChainAvailable + onChainUnconfirmed, false);
                                showError(message, Snackbar.LENGTH_LONG);
                                return;
                            }
                        }
                    }

                } else {
                    // you need to setup wallet to open a channel
                    showError(getResources().getString(R.string.error_channel_open_node_setup), Snackbar.LENGTH_LONG);
                    return;
                }

                if (mOnChainFeeView.isLowerThanMinimum()) {
                    new UserGuardian(getContext(), new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onConfirmed() {
                            performChannelOpen();
                        }

                        @Override
                        public void onCancelled() {

                        }
                    }).securityLowOnChainFee((int) mOnChainFeeView.getSatPerVByteFee());
                } else {
                    performChannelOpen();
                }
            }
        });

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            // We have to call this delayed, as otherwise it will still bring up the softKeyboard
            mEtAmount.requestFocus();
        }, 600);

        // deactivate default keyboard for number input.
        mEtAmount.setShowSoftInputOnFocus(false);

        // set unit to current primary unit
        mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());

        return view;
    }

    private void performChannelOpen() {
        switchToProgressScreen();
        Wallet_Channels.getInstance().openChannel(mLightningNodeUri, mValueChannelCapacity, mOnChainFeeView.getSatPerVByteFee(), mPrivateCheckbox.isChecked(), mUtxoOptionsView.getSelectedUTXOs());
    }

    private void setAlias(LightningNodeUri lightningNodeUri) {
        String alias = AliasManager.getInstance().getAliasWithoutPubkey(lightningNodeUri.getPubKey());
        alias = alias + " (" + lightningNodeUri.getPubKey() + ")";
        mTvNodeAlias.setText(alias);
    }

    private void switchToProgressScreen() {
        mProgressView.setVisibility(View.VISIBLE);
        mOpenChannelInputLayout.setVisibility(View.INVISIBLE);
        mProgressView.startSpinning();
        mBSDScrollableMainView.animateTitleOut();
    }

    private void switchToFailedScreen(String error) {
        mProgressView.spinningFinished(false);
        TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
        mOpenChannelInputLayout.setVisibility(View.GONE);
        mResultView.setVisibility(View.VISIBLE);

        // Set failed states
        mResultView.setHeading(R.string.channel_open_error, false);
        mResultView.setDetailsText(error);
    }

    private void switchToSuccessScreen() {
        mProgressView.spinningFinished(true);
        TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
        mOpenChannelInputLayout.setVisibility(View.GONE);
        mResultView.setVisibility(View.VISIBLE);
        mResultView.setHeading(R.string.success, true);
        mResultView.setDetailsText(R.string.channel_open_success);
    }

    private void setAvailableFunds() {
        long available = Wallet_Balance.getInstance().getBalances().onChainConfirmed();
        mOnChainConfirmed = available;
        mOnChainUnconfirmed = Wallet_Balance.getInstance().getBalances().onChainUnconfirmed();
        mTvOnChainFunds.setLabelText(getString(R.string.available) + ": ");
        mTvOnChainFunds.setLabelVisibility(true);
        mTvOnChainFunds.setAmountMsat(available);
    }

    private void showError(String message, int duration) {
        Snackbar msg = Snackbar.make(getView().findViewById(R.id.coordinator), message, duration);
        View sbView = msg.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
        msg.show();
    }

    @Override
    public void onDestroy() {
        Wallet_Channels.getInstance().unregisterChannelOpenUpdateListener(this);
        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onChannelOpenUpdate(LightningNodeUri lightningNodeUri, int status, String message) {

        if (mLightningNodeUri.getPubKey().equals(lightningNodeUri.getPubKey())) {
            if (status == Wallet_Channels.ChannelOpenUpdateListener.SUCCESS) {
                // fetch channels after open
                Wallet_Channels.getInstance().updateLNDChannelsWithDebounce();
                getActivity().runOnUiThread(this::switchToSuccessScreen);
            } else {
                getActivity().runOnUiThread(() -> switchToFailedScreen(getDetailedErrorMessage(status, message)));
            }
        }
    }

    private String getDetailedErrorMessage(int error, String message) {
        switch (error) {
            case ERROR_GET_PEERS_TIMEOUT:
                return getString(R.string.error_get_peers_timeout);
            case ERROR_GET_PEERS:
                return getString(R.string.error_get_peers);
            case ERROR_CONNECTION_TIMEOUT:
                return getString(R.string.error_connect_peer_timeout);
            case ERROR_CONNECTION_REFUSED:
                return getString(R.string.error_connect_peer_refused);
            case ERROR_CONNECTION_SELF:
                return getString(R.string.error_connect_peer_self);
            case ERROR_CONNECTION_NO_HOST:
                return getString(R.string.error_connect_peer_no_host);
            case ERROR_CONNECTION:
                return getString(R.string.error_connect_peer);
            case ERROR_CHANNEL_TIMEOUT:
                return getString(R.string.error_channel_open_timeout);
            case ERROR_CHANNEL_PENDING_MAX:
                return getString(R.string.error_channel_open_pending_max);
            case ERROR_CHANNEL_OPEN:
            default:
                return getString(R.string.error_channel_open, message);
        }
    }


    private void calculateTransactionSize() {
        setCalculatingFee();
        estimateOnChainTransactionSize(mValueChannelCapacity);
    }

    /**
     * Show progress while calculating fee
     */
    private void setCalculatingFee() {
        mOnChainFeeView.onCalculating();
    }


    /**
     * Show fee calculation failure
     */
    private void setFeeFailure() {
        mOnChainFeeView.onSizeCalculationFailure();
    }


    /**
     * This function is used to calculate the expected on chain fee.
     */
    private void estimateOnChainTransactionSize(long amount) {
        if (!BackendManager.getCurrentBackend().supportsAbsoluteOnChainFeeEstimation()) {
            setFeeFailure();
            return;
        }

        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            // We choose a dummy bech32 address. The fee amount depends only on the address type.
            String address;
            switch (Wallet.getInstance().getNetwork()) {
                case TESTNET:
                    address = "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx";
                    break;
                case REGTEST:
                    address = "bcrt1qsdtedxkv2mdgtstsv9fhyq03dsv9dyu5qmeh2w";
                    break;
                default:
                    address = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"; // Mainnet
            }

            getCompositeDisposable().add(BackendManager.api().getTransactionSizeVByte(address, amount)
                    .subscribe(response -> mOnChainFeeView.onSizeCalculatedSuccess(((long) (response * 1.5))), // ToDo: channel open transactions are larger than normal transactions. We go with this factor until we find a better method for estimating channel opens. It will also depend on the channel type.
                            throwable -> {
                                BBLog.w(TAG, "Exception in on-chain transaction size request task.");
                                BBLog.w(TAG, throwable.getMessage());
                                setFeeFailure();
                            }));
        } else {
            setFeeFailure();
        }
    }

    @Override
    public long onSelectUtxosClicked() {
        return mValueChannelCapacity;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key != null) {
            if (key.equals(PrefsUtil.CURRENT_CURRENCY_INDEX)) {
                mEtAmount.setText(MonetaryUtil.getInstance().msatsToCurrentCurrencyTextInputString(mValueChannelCapacity, false));
                mTvUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());
                setAvailableFunds();
            }
        }
    }
}
