package app.michaelwuensch.bitbanana.channelManagement;

import android.os.Bundle;
import android.view.View;

import com.github.lightningnetwork.lnd.lnrpc.ChanInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Channel;
import com.github.lightningnetwork.lnd.lnrpc.ChannelCloseSummary;
import com.github.lightningnetwork.lnd.lnrpc.Initiator;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.math.BigDecimal;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.channelManagement.listItems.ChannelListItem;
import app.michaelwuensch.bitbanana.connection.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.customView.AdvancedChannelDetailView;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.util.UtilFunctions;
import app.michaelwuensch.bitbanana.util.Wallet;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class AdvancedChannelDetailsActivity extends BaseAppCompatActivity {

    static final String LOG_TAG = AdvancedChannelDetailsActivity.class.getSimpleName();
    private String mAlias;
    private AdvancedChannelDetailView mDetailCapacity;
    private AdvancedChannelDetailView mDetailActivity;
    private AdvancedChannelDetailView mDetailChannelLifetime;
    private AdvancedChannelDetailView mDetailVisibility;
    private AdvancedChannelDetailView mDetailInitiator;
    private AdvancedChannelDetailView mDetailCloseInitiator;
    private AdvancedChannelDetailView mDetailCloseType;
    private AdvancedChannelDetailView mDetailTimeLock;
    private AdvancedChannelDetailView mDetailCommitFee;
    private AdvancedChannelDetailView mDetailLocalRoutingFee;
    private AdvancedChannelDetailView mDetailRemoteRoutingFee;
    private AdvancedChannelDetailView mDetailLocalReserve;
    private AdvancedChannelDetailView mDetailRemoteReserve;
    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_channel_details);

        mDetailCapacity = findViewById(R.id.capacity);
        mDetailActivity = findViewById(R.id.activity);
        mDetailChannelLifetime = findViewById(R.id.channelLifetime);
        mDetailVisibility = findViewById(R.id.visibility);
        mDetailInitiator = findViewById(R.id.initiator);
        mDetailCloseInitiator = findViewById(R.id.closeInitiator);
        mDetailCloseType = findViewById(R.id.closeType);
        mDetailTimeLock = findViewById(R.id.timeLock);
        mDetailCommitFee = findViewById(R.id.commitFee);
        mDetailLocalRoutingFee = findViewById(R.id.localRoutingFee);
        mDetailRemoteRoutingFee = findViewById(R.id.remoteRoutingFee);
        mDetailLocalReserve = findViewById(R.id.localReserve);
        mDetailRemoteReserve = findViewById(R.id.remoteReserve);

        mCompositeDisposable = new CompositeDisposable();

        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            ByteString channelString = (ByteString) extras.getSerializable(ChannelDetailBSDFragment.ARGS_CHANNEL);
            int type = extras.getInt(ChannelDetailBSDFragment.ARGS_TYPE);

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
                        break;
                }
            } catch (InvalidProtocolBufferException | NullPointerException exception) {
                BBLog.e(LOG_TAG, "Failed to parse channel.", exception);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    private void bindOpenChannel(ByteString channelString) throws InvalidProtocolBufferException {
        Channel channel = Channel.parseFrom(channelString);
        mAlias = AliasManager.getInstance().getAlias(channel.getRemotePubkey());
        setTitle(mAlias);

        // capacity
        String capacity = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getCapacity());
        mDetailCapacity.setContent(R.string.channel_capacity, capacity, R.string.advanced_channel_details_explanation_capacity);
        mDetailCapacity.setVisibility(View.VISIBLE);

        // activity
        String activity = UtilFunctions.roundDouble(((double) (channel.getTotalSatoshisSent() + channel.getTotalSatoshisReceived()) / channel.getCapacity() * 100), 2) + "%";
        mDetailActivity.setContent(R.string.advanced_channel_details_activity, activity, R.string.advanced_channel_details_explanation_activity);
        mDetailActivity.setVisibility(View.VISIBLE);

        // visibility
        String visibility;
        if (channel.getPrivate()) {
            visibility = getString(R.string.channel_visibility_private);
        } else {
            visibility = getString(R.string.channel_visibility_public);
        }
        mDetailVisibility.setContent(R.string.channel_visibility, visibility, R.string.advanced_channel_details_explanation_visibility);
        mDetailVisibility.setVisibility(View.VISIBLE);

        // initiator
        String initiator;
        if (channel.getInitiator()) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setContent(R.string.advanced_channel_details_initiator, initiator, R.string.advanced_channel_details_explanation_initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);

        // commit fee
        String commitFee = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getCommitFee());
        mDetailCommitFee.setContent(R.string.advanced_channel_details_commit_fee, commitFee, R.string.advanced_channel_details_explanation_commit_fee);
        mDetailCommitFee.setVisibility(View.VISIBLE);

        // time lock
        long timeLockInSeconds = channel.getLocalConstraints().getCsvDelay() * 10 * 60;
        String timeLock = String.valueOf(channel.getLocalConstraints().getCsvDelay()) + " (" + TimeFormatUtil.formattedDurationShort(timeLockInSeconds, AdvancedChannelDetailsActivity.this) + ")";
        mDetailTimeLock.setContent(R.string.advanced_channel_details_time_lock, timeLock, R.string.advanced_channel_details_explanation_time_lock);
        mDetailTimeLock.setVisibility(View.VISIBLE);

        // local fee
        mDetailLocalRoutingFee.setContent(R.string.advanced_channel_details_local_routing_fee, "- msat\n - %", R.string.advanced_channel_details_explanation_local_routing_fee);
        mDetailLocalRoutingFee.setVisibility(View.VISIBLE);

        // remote fee
        mDetailRemoteRoutingFee.setContent(R.string.advanced_channel_details_remote_routing_fee, "- msat\n - %", R.string.advanced_channel_details_explanation_remote_routing_fee);
        mDetailRemoteRoutingFee.setVisibility(View.VISIBLE);

        // local reserve amount
        String localReserve = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getLocalConstraints().getChanReserveSat());
        mDetailLocalReserve.setContent(R.string.advanced_channel_details_local_channel_reserve, localReserve, R.string.advanced_channel_details_explanation_local_channel_reserve);
        mDetailLocalReserve.setVisibility(View.VISIBLE);

        // remote reserve amount
        String remoteReserve = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getRemoteConstraints().getChanReserveSat());
        mDetailRemoteReserve.setContent(R.string.advanced_channel_details_remote_channel_reserve, remoteReserve, R.string.advanced_channel_details_explanation_remote_channel_reserve);
        mDetailRemoteReserve.setVisibility(View.VISIBLE);

        fetchChannelInfo(channel.getChanId());
    }

    private void bindPendingOpenChannel(ByteString channelString) throws InvalidProtocolBufferException {
        PendingChannelsResponse.PendingOpenChannel channel = PendingChannelsResponse.PendingOpenChannel.parseFrom(channelString);

        bindPendingChannel(channel.getChannel());

        // visibility
        String visibility;
        if (channel.getChannel().getPrivate()) {
            visibility = getString(R.string.channel_visibility_private);
        } else {
            visibility = getString(R.string.channel_visibility_public);
        }
        mDetailVisibility.setContent(R.string.channel_visibility, visibility, R.string.advanced_channel_details_explanation_visibility);
        mDetailVisibility.setVisibility(View.VISIBLE);

        // commit fee
        String commitFee = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getCommitFee());
        mDetailCommitFee.setContent(R.string.advanced_channel_details_commit_fee, commitFee, R.string.advanced_channel_details_explanation_commit_fee);
        mDetailCommitFee.setVisibility(View.VISIBLE);

        // local reserve amount
        String localReserve = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getChannel().getLocalChanReserveSat());
        mDetailLocalReserve.setContent(R.string.advanced_channel_details_local_channel_reserve, localReserve, R.string.advanced_channel_details_explanation_local_channel_reserve);
        mDetailCommitFee.setVisibility(View.VISIBLE);

        // remote reserve amount
        String remoteReserve = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getChannel().getRemoteChanReserveSat());
        mDetailRemoteReserve.setContent(R.string.advanced_channel_details_remote_channel_reserve, remoteReserve, R.string.advanced_channel_details_explanation_remote_channel_reserve);
        mDetailCommitFee.setVisibility(View.VISIBLE);
    }

    private void bindWaitingCloseChannel(ByteString channelString) throws InvalidProtocolBufferException {
        PendingChannelsResponse.WaitingCloseChannel channel = PendingChannelsResponse.WaitingCloseChannel.parseFrom(channelString);
        bindPendingChannel(channel.getChannel());
    }

    private void bindPendingCloseChannel(ByteString channelString) throws InvalidProtocolBufferException {
        PendingChannelsResponse.ClosedChannel channel = PendingChannelsResponse.ClosedChannel.parseFrom(channelString);
        bindPendingChannel(channel.getChannel());
    }

    private void bindForceClosingChannel(ByteString channelString) throws InvalidProtocolBufferException {
        PendingChannelsResponse.ForceClosedChannel channel = PendingChannelsResponse.ForceClosedChannel.parseFrom(channelString);
        bindPendingChannel(channel.getChannel());
    }

    private void bindPendingChannel(PendingChannelsResponse.PendingChannel channel) {
        mAlias = AliasManager.getInstance().getAlias(channel.getRemoteNodePub());
        setTitle(mAlias);

        // capacity
        String capacity = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getCapacity());
        mDetailCapacity.setContent(R.string.channel_capacity, capacity, R.string.advanced_channel_details_explanation_capacity);
        mDetailCapacity.setVisibility(View.VISIBLE);

        // initiator
        String initiator;
        if (channel.getInitiator() == Initiator.INITIATOR_LOCAL) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setContent(R.string.advanced_channel_details_initiator, initiator, R.string.advanced_channel_details_explanation_initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);
    }

    private void bindClosedChannel(ByteString channelString) throws InvalidProtocolBufferException {
        ChannelCloseSummary channel = ChannelCloseSummary.parseFrom(channelString);
        mAlias = AliasManager.getInstance().getAlias(channel.getRemotePubkey());
        setTitle(mAlias);

        // capacity
        String capacity = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getCapacity());
        mDetailCapacity.setContent(R.string.channel_capacity, capacity, R.string.advanced_channel_details_explanation_capacity);
        mDetailCapacity.setVisibility(View.VISIBLE);

        // channel lifetime
        // ToDo: Find out how we can get openHeight if we are not channel initiator.
        /*
        if (channel.getOpenInitiator() == Initiator.INITIATOR_LOCAL) {
            int openHeight = Wallet.getInstance().getChannelOpenBlockHeight(channel.getChannelPoint());
            BBLog.e(LOG_TAG, "open height: " + openHeight);
            int closeHeight = channel.getCloseHeight();
            int ageInBlocks = closeHeight - openHeight;
            String duration = TimeFormatUtil.formattedBlockDuration(ageInBlocks, AdvancedChannelDetailsActivity.this);
            mDetailChannelLifetime.setContent(R.string.advanced_channel_details_lifetime, duration, R.string.advanced_channel_details_explanation_lifetime);
        } else {
            mDetailChannelLifetime.setVisibility(View.GONE);
        }
         */

        // initiator
        String initiator;
        if (channel.getOpenInitiator() == Initiator.INITIATOR_LOCAL) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setContent(R.string.advanced_channel_details_initiator, initiator, R.string.advanced_channel_details_explanation_initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);

        // close initiator
        String closeInitiator;
        if (channel.getCloseInitiator() == Initiator.INITIATOR_LOCAL) {
            closeInitiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            closeInitiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailCloseInitiator.setContent(R.string.advanced_channel_details_close_initiator, closeInitiator, R.string.advanced_channel_details_explanation_close_initiator);
        mDetailCloseInitiator.setVisibility(View.VISIBLE);

        // close type
        String closeTypeLabel = "";
        if (channel.getCloseType() == ChannelCloseSummary.ClosureType.COOPERATIVE_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_coop);
        } else if (channel.getCloseType() == ChannelCloseSummary.ClosureType.LOCAL_FORCE_CLOSE || channel.getCloseType() == ChannelCloseSummary.ClosureType.REMOTE_FORCE_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_force_close);
        } else if (channel.getCloseType() == ChannelCloseSummary.ClosureType.BREACH_CLOSE) {
            closeTypeLabel = getResources().getString(R.string.channel_close_type_breach);
        } else {
            closeTypeLabel = channel.getCloseType().name();
        }
        mDetailCloseType.setContent(R.string.channel_close_type, closeTypeLabel, R.string.advanced_channel_details_explanation_close_type);
        mDetailCloseType.setVisibility(View.VISIBLE);
    }

    public void fetchChannelInfo(long chanID) {
        // Retrieve channel info from LND with gRPC (async)
        if (LndConnection.getInstance().getLightningService() != null) {
            mCompositeDisposable.add(LndConnection.getInstance().getLightningService().getChanInfo(ChanInfoRequest.newBuilder().setChanId(chanID).build())
                    .subscribe(chanInfoResponse -> {
                        if (chanInfoResponse.getNode1Pub().equals(Wallet.getInstance().getNodeUris()[0].getPubKey())) {
                            // Our node is Node1
                            if (chanInfoResponse.hasNode1Policy()) {
                                BigDecimal localFeeRate = BigDecimal.valueOf((double) (chanInfoResponse.getNode1Policy().getFeeRateMilliMsat()) / 10000.0).stripTrailingZeros();
                                String localFee = chanInfoResponse.getNode1Policy().getFeeBaseMsat() + " msat\n+ " + localFeeRate.toPlainString() + " %";
                                mDetailLocalRoutingFee.setValue(localFee);
                            }
                            if (chanInfoResponse.hasNode2Policy()) {
                                BigDecimal remoteFeeRate = BigDecimal.valueOf((double) (chanInfoResponse.getNode2Policy().getFeeRateMilliMsat()) / 10000.0).stripTrailingZeros();
                                String remoteFee = chanInfoResponse.getNode2Policy().getFeeBaseMsat() + " msat\n+ " + remoteFeeRate.toPlainString() + " %";
                                mDetailRemoteRoutingFee.setValue(remoteFee);
                            }
                        } else {
                            // Our node is Node2
                            if (chanInfoResponse.hasNode1Policy()) {
                                BigDecimal remoteFeeRate = BigDecimal.valueOf((double) (chanInfoResponse.getNode1Policy().getFeeRateMilliMsat()) / 10000.0).stripTrailingZeros();
                                String remoteFee = chanInfoResponse.getNode1Policy().getFeeBaseMsat() + " msat\n+ " + remoteFeeRate.toPlainString() + " %";
                                mDetailRemoteRoutingFee.setValue(remoteFee);
                            }
                            if (chanInfoResponse.hasNode2Policy()) {
                                BigDecimal localFeeRate = BigDecimal.valueOf((double) (chanInfoResponse.getNode2Policy().getFeeRateMilliMsat()) / 10000.0).stripTrailingZeros();
                                String localFee = chanInfoResponse.getNode2Policy().getFeeBaseMsat() + " msat\n+ " + localFeeRate.toPlainString() + " %";
                                mDetailLocalRoutingFee.setValue(localFee);
                            }
                        }
                    }, throwable -> {
                        BBLog.w(LOG_TAG, "Exception in fetch chanInfo task: " + throwable.getMessage());
                    }));
        }
    }
}