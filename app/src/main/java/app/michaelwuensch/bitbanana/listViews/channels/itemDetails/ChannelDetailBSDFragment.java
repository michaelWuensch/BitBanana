package app.michaelwuensch.bitbanana.listViews.channels.itemDetails;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.listViews.channels.items.ChannelListItem;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.BlockExplorer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Channels;

public class ChannelDetailBSDFragment extends BaseBSDFragment implements Wallet_Channels.ChannelCloseUpdateListener {

    public static final String TAG = ChannelDetailBSDFragment.class.getSimpleName();
    public static final String ARGS_CHANNEL = "CHANNEL";
    public static final String ARGS_TYPE = "TYPE";

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private ConstraintLayout mContentTopLayout;

    private TextView mNodeAlias;
    private ImageView mStatusDot;
    private TextView mRemotePubKey;
    private ProgressBar mBalanceBarLocal;
    private ProgressBar mBalanceBarRemote;
    private AmountView mLocalBalance;
    private AmountView mRemoteBalance;
    private TextView mChannelVisibilityLabel;
    private TextView mChannelVisibility;
    private ImageView mChannelVisibilitySeparatorLine;
    private TextView mFundingTx;
    private BBButton mCloseChannelButton;

    private ConstraintLayout mChannelDetailsLayout;
    private ConstraintLayout mClosingTxLayout;
    private TextView mClosingTxText;
    private ImageView mClosingTxCopyIcon;
    private TextView mForceClosingTxTimeLabel;
    private TextView mForceClosingTxTimeText;
    private String mChannelPoint;
    private TextView mTvClosingTypeLabel;
    private TextView mTvClosingType;
    private ImageView mIvClosingTypeSeparator;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_channeldetail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mProgressView = view.findViewById(R.id.paymentProgressLayout);
        mNodeAlias = view.findViewById(R.id.nodeAlias);
        mStatusDot = view.findViewById(R.id.statusDot);
        mRemotePubKey = view.findViewById(R.id.remotePubKeyText);
        mBalanceBarLocal = view.findViewById(R.id.balanceBarLocal);
        mBalanceBarRemote = view.findViewById(R.id.balanceBarRemote);
        mLocalBalance = view.findViewById(R.id.localBalance);
        mRemoteBalance = view.findViewById(R.id.remoteBalance);
        mChannelVisibilityLabel = view.findViewById(R.id.channelVisibilityLabel);
        mChannelVisibility = view.findViewById(R.id.channelVisibility);
        mChannelVisibilitySeparatorLine = view.findViewById(R.id.separator_4);
        mFundingTx = view.findViewById(R.id.fundingTxText);
        mChannelDetailsLayout = view.findViewById(R.id.channelDetailsLayout);
        mClosingTxLayout = view.findViewById(R.id.closingTxLayout);
        mClosingTxText = view.findViewById(R.id.closingTxText);
        mClosingTxCopyIcon = view.findViewById(R.id.closingTxCopyIcon);
        mCloseChannelButton = view.findViewById(R.id.channelCloseButton);
        mForceClosingTxTimeLabel = view.findViewById(R.id.closingTxTimeLabel);
        mForceClosingTxTimeText = view.findViewById(R.id.closingTxTimeText);
        mTvClosingType = view.findViewById(R.id.closeType);
        mTvClosingTypeLabel = view.findViewById(R.id.closeTypeLabel);
        mIvClosingTypeSeparator = view.findViewById(R.id.separator_close_type);

        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mBSDScrollableMainView.setTitleVisibility(false);
        mResultView.setOnOkListener(this::dismiss);

        ImageView remotePublicKeyIcon = view.findViewById(R.id.remotePubKeyCopyIcon);
        ImageView fundingTxIcon = view.findViewById(R.id.fundingTxCopyIcon);

        if (getArguments() != null) {
            int type = getArguments().getInt(ARGS_TYPE);
            try {
                switch (type) {
                    case ChannelListItem.TYPE_OPEN_CHANNEL:
                        bindOpenChannel((OpenChannel) getArguments().getSerializable(ARGS_CHANNEL));
                        break;
                    case ChannelListItem.TYPE_PENDING_CHANNEL:
                        bindPendingChannel((PendingChannel) getArguments().getSerializable(ARGS_CHANNEL));
                        break;
                    case ChannelListItem.TYPE_CLOSED_CHANNEL:
                        bindClosedChannel((ClosedChannel) getArguments().getSerializable(ARGS_CHANNEL));
                }
            } catch (NullPointerException exception) {
                BBLog.e(TAG, "Failed to parse channel.", exception);
                dismiss();
            }
        }

        remotePublicKeyIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(getContext(), "remotePubKey", mRemotePubKey.getText()));
        fundingTxIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(getContext(), "fundingTransaction", mFundingTx.getText()));
        mFundingTx.setOnClickListener(view1 -> new BlockExplorer().showTransaction(mFundingTx.getText().toString(), getActivity()));

        mBSDScrollableMainView.setOnMoreListener(new BSDScrollableMainView.OnMoreListener() {
            @Override
            public void onMore() {
                if (getArguments() != null) {
                    Intent intent = new Intent(getActivity(), AdvancedChannelDetailsActivity.class);
                    intent.putExtras(getArguments());
                    getActivity().startActivity(intent);
                }
            }
        });

        return view;
    }

    private void bindOpenChannel(OpenChannel channel) {
        mBSDScrollableMainView.setMoreButtonVisibility(true);
        mNodeAlias.setText(AliasManager.getInstance().getAlias(channel.getRemotePubKey()));
        mRemotePubKey.setText(channel.getRemotePubKey());
        mFundingTx.setText(channel.getFundingOutpoint().getTransactionID());

        // register for channel close events and keep channel point for later comparison
        Wallet_Channels.getInstance().registerChannelCloseUpdateListener(this);
        mChannelPoint = channel.getFundingOutpoint().toString();

        showChannelVisibility(channel.isPrivate());

        if (FeatureManager.isCloseChannelEnabled()) {
            showClosingButton(channel, !channel.isActive(), channel.getLocalChannelConstraints().getSelfDelay());
        }

        if (channel.isActive()) {
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.green)));
        } else {
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.gray)));
        }

        long availableCapacity = channel.getCapacity() - channel.getCommitFee();
        setBalances(channel.getLocalBalance(), channel.getRemoteBalance(), availableCapacity);
    }

    private void bindPendingChannel(PendingChannel channel) {
        mBSDScrollableMainView.setMoreButtonVisibility(true);

        mNodeAlias.setText(AliasManager.getInstance().getAlias(channel.getRemotePubKey()));
        mRemotePubKey.setText(channel.getRemotePubKey());
        mFundingTx.setText(channel.getFundingOutpoint().getTransactionID());

        // Status dot
        mStatusDot.setVisibility(View.VISIBLE);
        switch (channel.getPendingType()) {
            case PENDING_OPEN:
                mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.banana_yellow)));
                break;
            case PENDING_CLOSE:
            case PENDING_FORCE_CLOSE:
                mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.red)));
                break;
            default:
                mStatusDot.setVisibility(View.GONE);
        }

        setBalances(channel.getLocalBalance(), channel.getRemoteBalance(), channel.getCapacity());

        if (channel.hasCloseTransactionId())
            showClosingTransaction(channel.getCloseTransactionId());
        if (channel.getPendingType() == PendingChannel.PendingType.PENDING_FORCE_CLOSE && channel.hasBlocksTilMaturity())
            showForceClosingTime(channel.getBlocksTilMaturity());
    }

    private void bindClosedChannel(ClosedChannel channel) {
        mBSDScrollableMainView.setMoreButtonVisibility(true);
        mStatusDot.setVisibility(View.GONE);
        mNodeAlias.setText(AliasManager.getInstance().getAlias(channel.getRemotePubKey()));
        mRemotePubKey.setText(channel.getRemotePubKey());
        mFundingTx.setText(channel.getFundingOutpoint().getTransactionID());
        if (channel.hasCloseTransactionId())
            showClosingTransaction(channel.getCloseTransactionId());
        if (channel.isHasCloseType())
            showChannelCloseType(channel);
        setBalances(channel.getLocalBalance(), channel.getCapacity() - channel.getLocalBalance(), channel.getCapacity());
    }

    private void setBalances(long local, long remote, long capacity) {
        float localBarValue = (float) ((double) local / (double) capacity);
        float remoteBarValue = (float) ((double) remote / (double) capacity);

        mBalanceBarLocal.setProgress((int) (localBarValue * 100f));
        mBalanceBarRemote.setProgress((int) (remoteBarValue * 100f));

        mLocalBalance.setAmountMsat(local);
        mRemoteBalance.setAmountMsat(remote);
    }


    private void showChannelCloseType(ClosedChannel closedChannel) {
        String closeTypeLabel = "";
        if (closedChannel.getCloseType() == ClosedChannel.CloseType.COOPERATIVE_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_coop);
        } else if (closedChannel.getCloseType() == ClosedChannel.CloseType.FORCE_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_force_close);
        } else if (closedChannel.getCloseType() == ClosedChannel.CloseType.BREACH_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_breach);
        } else {
            closeTypeLabel = closedChannel.getCloseType().name();
        }
        mTvClosingType.setText(closeTypeLabel);
        mTvClosingType.setVisibility(View.VISIBLE);
        mTvClosingTypeLabel.setVisibility(View.VISIBLE);
        mIvClosingTypeSeparator.setVisibility(View.VISIBLE);
    }

    private void showChannelVisibility(boolean isPrivate) {
        mChannelVisibilityLabel.setVisibility(View.VISIBLE);
        mChannelVisibility.setVisibility(View.VISIBLE);
        mChannelVisibilitySeparatorLine.setVisibility(View.VISIBLE);
        mChannelVisibility.setText(isPrivate ? R.string.channel_visibility_private : R.string.channel_visibility_public);
    }

    private void showClosingTransaction(String closingTransaction) {
        mClosingTxLayout.setVisibility(View.VISIBLE);
        mClosingTxText.setText(closingTransaction);
        mClosingTxText.setOnClickListener(view1 -> new BlockExplorer().showTransaction(closingTransaction, getActivity()));
        mClosingTxCopyIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(getContext(), "closingTransaction", closingTransaction));
    }

    private void showForceClosingTime(int maturity) {
        String expiryText = TimeFormatUtil.formattedDuration(maturity * 10 * 60, getContext()).toLowerCase();
        mForceClosingTxTimeLabel.setVisibility(View.VISIBLE);
        mForceClosingTxTimeText.setVisibility(View.VISIBLE);
        mForceClosingTxTimeText.setText(expiryText);
    }

    private void showClosingButton(OpenChannel channel, boolean forceClose, int csvDelay) {
        mCloseChannelButton.setVisibility(View.VISIBLE);
        mCloseChannelButton.setText(forceClose ? getString(R.string.channel_close_force) : getString(R.string.channel_close));
        mCloseChannelButton.setOnClickListener(view1 -> closeChannel(channel, forceClose, csvDelay));
    }

    private void closeChannel(OpenChannel channel, boolean force, int csvDelay) {
        String lockUpTime = TimeFormatUtil.formattedDuration(csvDelay * 10 * 60, getContext()).toLowerCase();
        new AlertDialog.Builder(getContext())
                .setTitle(force ? R.string.channel_close_force : R.string.channel_close)
                .setMessage(getString(force ? R.string.channel_close_force_confirmation : R.string.channel_close_confirmation, mNodeAlias.getText(), lockUpTime))
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    switchToProgressScreen();
                    Wallet_Channels.getInstance().closeChannel(channel, force);
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                })
                .show();
    }

    private void switchToProgressScreen() {
        mProgressView.setVisibility(View.VISIBLE);
        mChannelDetailsLayout.setVisibility(View.INVISIBLE);
        mProgressView.startSpinning();
        mBSDScrollableMainView.animateTitleOut();
    }

    private void switchToFinishScreen(boolean success, String error) {
        TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
        mResultView.setVisibility(View.VISIBLE);
        mProgressView.spinningFinished(success);
        mChannelDetailsLayout.setVisibility(View.GONE);

        if (success) {
            mResultView.setHeading(R.string.success, true);
            mResultView.setDetailsText(R.string.channel_close_success);
        } else {
            mResultView.setHeading(R.string.channel_close_error, false);
            mResultView.setDetailsText(error);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        Wallet_Channels.getInstance().unregisterChannelCloseUpdateListener(this);
        super.onDismiss(dialog);
    }

    @Override
    public void onChannelCloseUpdate(String channelPoint, int status, String message) {
        BBLog.d(TAG, "Channel close: " + channelPoint + " status=(" + status + ")");

        if (getActivity() != null && mChannelPoint.equals(channelPoint)) {

            // fetch channels after closing finished
            Wallet_Channels.getInstance().updateLNDChannelsWithDebounce();

            getActivity().runOnUiThread(() -> {
                if (status == Wallet_Channels.ChannelCloseUpdateListener.SUCCESS) {
                    switchToFinishScreen(true, null);
                } else {
                    switchToFinishScreen(false, getDetailedErrorMessage(status, message));
                }
            });
        }
    }

    private String getDetailedErrorMessage(int error, String message) {
        switch (error) {
            case ERROR_PEER_OFFLINE:
                return getString(R.string.error_channel_close_offline);
            case ERROR_CHANNEL_TIMEOUT:
                return getString(R.string.error_channel_close_timeout);
            case ERROR_CHANNEL_CLOSE:
            default:
                return getString(R.string.error_channel_close, message);
        }
    }
}
