package app.michaelwuensch.bitbanana.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import com.google.android.material.snackbar.Snackbar;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.BBAmountInput;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBExpandableTextInfoBox;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.ClearFocusListener;
import app.michaelwuensch.bitbanana.customView.OnChainFeeView;
import app.michaelwuensch.bitbanana.customView.UtxoOptionsView;
import app.michaelwuensch.bitbanana.listViews.utxos.UTXOsActivity;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannelRequest;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_Channels;

public class OpenChannelBSDFragment extends BaseBSDFragment implements UtxoOptionsView.OnUtxoViewButtonListener, Wallet_Channels.ChannelOpenUpdateListener, ClearFocusListener {

    public static final String TAG = OpenChannelBSDFragment.class.getSimpleName();
    public static final String ARGS_NODE_URI = "NODE_URI";

    private ActivityResultLauncher<Intent> mActivityResultLauncher;

    private BSDScrollableMainView mBSDScrollableMainView;
    private BBAmountInput mAmountInput;
    private BBExpandableTextInfoBox mNodeView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private ConstraintLayout mContentTopLayout;
    private LinearLayout mOpenChannelInputLayout;
    private BBButton mOpenChannelButton;
    private LightningNodeUri mLightningNodeUri;
    private OnChainFeeView mOnChainFeeView;
    private CheckBox mPrivateCheckbox;

    private UtxoOptionsView mUtxoOptionsView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_open_channel, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mAmountInput = view.findViewById(R.id.amountInput);
        mNodeView = view.findViewById(R.id.nodeView);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mProgressView = view.findViewById(R.id.paymentProgressLayout);
        mOpenChannelInputLayout = view.findViewById(R.id.openChannelInputLayout);
        mOpenChannelButton = view.findViewById(R.id.openChannelButton);
        mOnChainFeeView = view.findViewById(R.id.sendFeeOnChainLayout);
        mPrivateCheckbox = view.findViewById(R.id.privateCheckBox);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mBSDScrollableMainView.setTitle(R.string.channel_open);
        mResultView.setOnOkListener(this::dismiss);
        mUtxoOptionsView = view.findViewById(R.id.utxoOptions);

        // Initialize the ActivityResultLauncher
        mActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        // Pass result to the custom view
                        mUtxoOptionsView.handleActivityResult(data);

                        // Update Amount View
                        long selectedAmount = data.getLongExtra(UTXOsActivity.EXTRA_TOTAL_SELECTED_UTXO_AMOUNT, 0);
                        mAmountInput.updateUtxoSelectionAmount(selectedAmount);

                        // Update On-Chain Fee view
                        mOnChainFeeView.setUtxosSelectedFlag(selectedAmount > 0);
                    }
                }
        );

        mUtxoOptionsView.setActivityResultLauncher(mActivityResultLauncher);
        mUtxoOptionsView.setVisibility(FeatureManager.isUtxoSelectionOnChannelOpenEnabled() ? View.VISIBLE : View.GONE);
        mUtxoOptionsView.setUtxoViewButtonListener(this);

        mOnChainFeeView.initialSetup();
        mOnChainFeeView.setClearFocusListener(this);
        setFeeFailure();

        mAmountInput.setupView();
        mAmountInput.setSendAllEnabled(true);
        mAmountInput.setOnChain(true);
        mAmountInput.setOnAmountInputActionListener(new BBAmountInput.OnAmountInputActionListener() {
            @Override
            public boolean onAfterTextChanged(String newText, long amount, boolean isFixedAmount, boolean isOnChain) {
                if (amount <= 0)
                    return false;

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

                    if (amount > maxSendAmount) {
                        // amount is to big
                        if (amount > absoluteMaxSendAmount) {
                            String message = getResources().getString(R.string.max_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(absoluteMaxSendAmount, false);
                            showError(message, Snackbar.LENGTH_LONG);
                            return false;
                        } else {
                            if (amount < (onChainAvailable + onChainUnconfirmed)) {
                                String message = getResources().getString(R.string.error_funds_not_confirmed_yet);
                                showError(message, 10000);
                                return false;
                            } else {
                                String message = getResources().getString(R.string.error_insufficient_on_chain_funds) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(onChainAvailable + onChainUnconfirmed, false);
                                showError(message, Snackbar.LENGTH_LONG);
                                return false;
                            }
                        }
                    }
                    return true;
                } else
                    return true;
            }

            @Override
            public void onInputValidityChanged(boolean valid) {
                mOpenChannelButton.setButtonEnabled(valid);
            }

            @Override
            public void onInputChanged(boolean valid) {
                if (valid)
                    calculateTransactionSize();
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
        mAmountInput.requestFocusDelayed();

        Wallet_Channels.getInstance().registerChannelOpenUpdateListener(this);

        if (getArguments() != null) {
            mLightningNodeUri = (LightningNodeUri) getArguments().getSerializable(ARGS_NODE_URI);
            setAlias(mLightningNodeUri);
        }

        ImageButton privateHelpButton = view.findViewById(R.id.privateHelpButton);
        if (FeatureManager.isHelpButtonsEnabled()) {
            privateHelpButton.setVisibility(View.VISIBLE);
            privateHelpButton.setOnClickListener(view1 -> HelpDialogUtil.showDialog(getActivity(), R.string.help_dialog_private_channels));
        } else {
            privateHelpButton.setVisibility(View.GONE);
        }

        mOpenChannelButton.setButtonEnabled(false);
        mOpenChannelButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {

                // ToDo: values are from LND. Make it generic?
                long minSendAmount = 20000 * 1000L;

                if (mAmountInput.getAmount() < minSendAmount) {
                    // amount is to small
                    String message = getResources().getString(R.string.min_amount) + " " + MonetaryUtil.getInstance().getCurrentCurrencyDisplayStringFromMSats(minSendAmount, false);
                    showError(message, Snackbar.LENGTH_LONG);
                    return;
                }

                if (!BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
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

        return view;
    }

    private void performChannelOpen() {
        hideKeyboard();
        switchToProgressScreen();
        OpenChannelRequest openChannelRequest = OpenChannelRequest.newBuilder()
                .setNodePubKey(mLightningNodeUri.getPubKey())
                .setAmount(mAmountInput.getAmount())
                .setUseAllFunds(mAmountInput.getSendAllChecked())
                .setSatPerVByte(mOnChainFeeView.getSatPerVByteFee())
                .setPrivate(mPrivateCheckbox.isChecked())
                .setUTXOs(mUtxoOptionsView.getSelectedUTXOs())
                .build();
        Wallet_Channels.getInstance().openChannel(mLightningNodeUri, openChannelRequest);
    }

    private void setAlias(LightningNodeUri lightningNodeUri) {
        String alias = AliasManager.getInstance().getAliasWithoutPubkey(lightningNodeUri.getPubKey());
        alias = alias + " (" + lightningNodeUri.getPubKey() + ")";
        mNodeView.setContent(alias);
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

    @Override
    public void onDestroy() {
        Wallet_Channels.getInstance().unregisterChannelOpenUpdateListener(this);
        super.onDestroy();
    }

    @Override
    public void onChannelOpenUpdate(String nodePubKey, int status, String message) {

        if (mLightningNodeUri.getPubKey().equals(nodePubKey)) {
            if (status == Wallet_Channels.ChannelOpenUpdateListener.SUCCESS) {
                // fetch channels after open
                Wallet_Channels.getInstance().updateChannelsWithDebounce();
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
        estimateOnChainTransactionSize(mAmountInput.getAmount());
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
        if (mAmountInput.getSendAllChecked())
            return 0;
        else
            return mAmountInput.getAmount();
    }

    @Override
    public void onResetUtxoViewClicked() {
        mAmountInput.updateUtxoSelectionAmount(0);
        mOnChainFeeView.setUtxosSelectedFlag(false);
    }

    @Override
    public void onSelectAllUTXOsToggled(boolean newIsChecked) {

    }

    @Override
    public void onClearFocus() {
        mAmountInput.clearFocus();
    }
}
