package app.michaelwuensch.bitbanana.channelManagement;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

import com.github.lightningnetwork.lnd.lnrpc.Channel;
import com.github.lightningnetwork.lnd.lnrpc.ChannelCloseSummary;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.channelManagement.listItems.ChannelListItem;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.fragments.BaseBSDFragment;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.BlockExplorer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.util.Wallet;

public class ChannelDetailBSDFragment extends BaseBSDFragment implements Wallet.ChannelCloseUpdateListener {

    static final String TAG = ChannelDetailBSDFragment.class.getSimpleName();
    static final String ARGS_CHANNEL = "CHANNEL";
    static final String ARGS_TYPE = "TYPE";

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
    private Button mCloseChannelButton;

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
            ByteString channelString = (ByteString) getArguments().getSerializable(ARGS_CHANNEL);
            int type = getArguments().getInt(ARGS_TYPE);

            try {
                switch (type) {
                    case ChannelListItem.TYPE_OPEN_CHANNEL:
                        bindOpenChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_PENDING_OPEN_CHANNEL:
                        bindPendingOpenChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_WAITING_CLOSE_CHANNEL:
                        bindWaitingCloseChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_PENDING_CLOSING_CHANNEL:
                        bindPendingCloseChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_PENDING_FORCE_CLOSING_CHANNEL:
                        bindForceClosingChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_CLOSED_CHANNEL:
                        bindClosedChannel(channelString);
                }
            } catch (InvalidProtocolBufferException | NullPointerException exception) {
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

    private void bindOpenChannel(ByteString channelString) throws InvalidProtocolBufferException {
        Channel channel = Channel.parseFrom(channelString);
        mBSDScrollableMainView.setMoreButtonVisibility(true);
        mNodeAlias.setText(AliasManager.getInstance().getAlias(channel.getRemotePubkey()));
        mRemotePubKey.setText(channel.getRemotePubkey());
        mFundingTx.setText(channel.getChannelPoint().substring(0, channel.getChannelPoint().indexOf(':')));

        // register for channel close events and keep channel point for later comparison
        Wallet.getInstance().registerChannelCloseUpdateListener(this);
        mChannelPoint = channel.getChannelPoint();

        showChannelVisibility(channel.getPrivate());
        showClosingButton(!channel.getActive(), channel.getCsvDelay());

        if (channel.getActive()) {
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.green)));
        } else {
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.gray)));
        }

        long availableCapacity = channel.getCapacity() - channel.getCommitFee();
        setBalances(channel.getLocalBalance(), channel.getRemoteBalance(), availableCapacity);
    }

    private void bindPendingOpenChannel(ByteString channelString) throws InvalidProtocolBufferException {
        PendingChannelsResponse.PendingOpenChannel pendingOpenChannel = PendingChannelsResponse.PendingOpenChannel.parseFrom(channelString);
        mBSDScrollableMainView.setMoreButtonVisibility(true);

        setBasicInformation(pendingOpenChannel.getChannel().getRemoteNodePub(),
                R.color.banana_yellow,
                pendingOpenChannel.getChannel().getChannelPoint());

        setBalances(pendingOpenChannel.getChannel().getLocalBalance(), pendingOpenChannel.getChannel().getRemoteBalance(), pendingOpenChannel.getChannel().getCapacity());
    }

    private void bindWaitingCloseChannel(ByteString channelString) throws InvalidProtocolBufferException {
        PendingChannelsResponse.WaitingCloseChannel waitingCloseChannel = PendingChannelsResponse.WaitingCloseChannel.parseFrom(channelString);
        mBSDScrollableMainView.setMoreButtonVisibility(true);

        setBasicInformation(waitingCloseChannel.getChannel().getRemoteNodePub(),
                R.color.red,
                waitingCloseChannel.getChannel().getChannelPoint());

        setBalances(waitingCloseChannel.getChannel().getLocalBalance(), waitingCloseChannel.getChannel().getRemoteBalance(), waitingCloseChannel.getChannel().getCapacity());
    }

    private void bindPendingCloseChannel(ByteString channelString) throws InvalidProtocolBufferException {
        PendingChannelsResponse.ClosedChannel pendingCloseChannel = PendingChannelsResponse.ClosedChannel.parseFrom(channelString);
        mBSDScrollableMainView.setMoreButtonVisibility(true);

        showClosingTransaction(pendingCloseChannel.getClosingTxid());

        setBasicInformation(pendingCloseChannel.getChannel().getRemoteNodePub(),
                R.color.red,
                pendingCloseChannel.getChannel().getChannelPoint());

        setBalances(pendingCloseChannel.getChannel().getLocalBalance(), pendingCloseChannel.getChannel().getRemoteBalance(), pendingCloseChannel.getChannel().getCapacity());
    }

    private void bindForceClosingChannel(ByteString channelString) throws InvalidProtocolBufferException {
        PendingChannelsResponse.ForceClosedChannel forceClosedChannel = PendingChannelsResponse.ForceClosedChannel.parseFrom(channelString);
        mBSDScrollableMainView.setMoreButtonVisibility(true);

        showClosingTransaction(forceClosedChannel.getClosingTxid());

        setBasicInformation(forceClosedChannel.getChannel().getRemoteNodePub(),
                R.color.red,
                forceClosedChannel.getChannel().getChannelPoint());

        showForceClosingTime(forceClosedChannel.getBlocksTilMaturity());
        setBalances(forceClosedChannel.getChannel().getLocalBalance(), forceClosedChannel.getChannel().getRemoteBalance(), forceClosedChannel.getChannel().getCapacity());
    }

    private void bindClosedChannel(ByteString channelString) throws InvalidProtocolBufferException {
        ChannelCloseSummary channelCloseSummary = ChannelCloseSummary.parseFrom(channelString);
        mBSDScrollableMainView.setMoreButtonVisibility(true);
        mStatusDot.setVisibility(View.GONE);

        showClosingTransaction(channelCloseSummary.getClosingTxHash());
        showChannelCloseType(channelCloseSummary);

        setBasicInformation(channelCloseSummary.getRemotePubkey(),
                R.color.gray,
                channelCloseSummary.getChannelPoint());

        setBalances(channelCloseSummary.getSettledBalance(), channelCloseSummary.getCapacity() - channelCloseSummary.getSettledBalance(), channelCloseSummary.getCapacity());
    }

    private void setBasicInformation(@NonNull String remoteNodePublicKey, int statusDot, @NonNull String channelPoint) {
        mNodeAlias.setText(AliasManager.getInstance().getAlias(remoteNodePublicKey));
        mRemotePubKey.setText(remoteNodePublicKey);
        mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), statusDot)));
        mFundingTx.setText(channelPoint.substring(0, channelPoint.indexOf(':')));
    }

    private void setBalances(long local, long remote, long capacity) {
        float localBarValue = (float) ((double) local / (double) capacity);
        float remoteBarValue = (float) ((double) remote / (double) capacity);

        mBalanceBarLocal.setProgress((int) (localBarValue * 100f));
        mBalanceBarRemote.setProgress((int) (remoteBarValue * 100f));

        mLocalBalance.setAmount(local);
        mRemoteBalance.setAmount(remote);
    }

    private void showChannelCloseType(ChannelCloseSummary channelCloseSummary) {
        mTvClosingType.setVisibility(View.VISIBLE);
        mTvClosingTypeLabel.setVisibility(View.VISIBLE);
        mIvClosingTypeSeparator.setVisibility(View.VISIBLE);
        // close type
        String closeTypeLabel = "";
        if (channelCloseSummary.getCloseType() == ChannelCloseSummary.ClosureType.COOPERATIVE_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_coop);
        } else if (channelCloseSummary.getCloseType() == ChannelCloseSummary.ClosureType.LOCAL_FORCE_CLOSE || channelCloseSummary.getCloseType() == ChannelCloseSummary.ClosureType.REMOTE_FORCE_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_force_close);
        } else if (channelCloseSummary.getCloseType() == ChannelCloseSummary.ClosureType.BREACH_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_breach);
        } else {
            closeTypeLabel = channelCloseSummary.getCloseType().toString();
        }
        mTvClosingType.setText(closeTypeLabel);
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

    private void showClosingButton(boolean forceClose, int csvDelay) {
        mCloseChannelButton.setVisibility(View.VISIBLE);
        mCloseChannelButton.setText(forceClose ? getText(R.string.channel_close_force) : getText(R.string.channel_close));
        mCloseChannelButton.setOnClickListener(view1 -> closeChannel(forceClose, csvDelay));
    }

    private void closeChannel(boolean force, int csvDelay) {
        String lockUpTime = TimeFormatUtil.formattedDuration(csvDelay * 10 * 60, getContext()).toLowerCase();
        new AlertDialog.Builder(getContext())
                .setTitle(force ? R.string.channel_close_force : R.string.channel_close)
                .setMessage(getString(force ? R.string.channel_close_force_confirmation : R.string.channel_close_confirmation, mNodeAlias.getText(), lockUpTime))
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    switchToProgressScreen();
                    Wallet.getInstance().closeChannel(mChannelPoint, force);
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
        Wallet.getInstance().unregisterChannelCloseUpdateListener(this);
        super.onDismiss(dialog);
    }

    @Override
    public void onChannelCloseUpdate(String channelPoint, int status, String message) {
        BBLog.d(TAG, "Channel close: " + channelPoint + " status=(" + status + ")");

        if (getActivity() != null && mChannelPoint.equals(channelPoint)) {

            // fetch channels after closing finished
            Wallet.getInstance().updateLNDChannelsWithDebounce();

            getActivity().runOnUiThread(() -> {
                if (status == Wallet.ChannelCloseUpdateListener.SUCCESS) {
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
