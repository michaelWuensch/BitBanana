package app.michaelwuensch.bitbanana.listViews.channels.itemDetails;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.lightningnetwork.lnd.lnrpc.ChanInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Channel;
import com.github.lightningnetwork.lnd.lnrpc.ChannelCloseSummary;
import com.github.lightningnetwork.lnd.lnrpc.Initiator;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse;
import com.github.lightningnetwork.lnd.lnrpc.RoutingPolicy;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.math.BigDecimal;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBExpandablePropertyView;
import app.michaelwuensch.bitbanana.listViews.channels.UpdateRoutingPolicyActivity;
import app.michaelwuensch.bitbanana.listViews.channels.items.ChannelListItem;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.util.UtilFunctions;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class AdvancedChannelDetailsActivity extends BaseAppCompatActivity {

    static final String LOG_TAG = AdvancedChannelDetailsActivity.class.getSimpleName();
    private String mAlias;
    private BBExpandablePropertyView mDetailCapacity;
    private BBExpandablePropertyView mDetailActivity;
    private BBExpandablePropertyView mDetailChannelLifetime;
    private BBExpandablePropertyView mDetailVisibility;
    private BBExpandablePropertyView mDetailCommitmentType;
    private BBExpandablePropertyView mDetailInitiator;
    private BBExpandablePropertyView mDetailCloseInitiator;
    private BBExpandablePropertyView mDetailCloseType;
    private BBExpandablePropertyView mDetailTimeLock;
    private BBExpandablePropertyView mDetailCommitFee;
    private BBExpandablePropertyView mDetailLocalReserve;
    private BBExpandablePropertyView mDetailRemoteReserve;
    private BBExpandablePropertyView mDetailLocalRoutingFee;
    private BBExpandablePropertyView mDetailLocalTimelockDelta;
    private BBExpandablePropertyView mDetailLocalMinHTLC;
    private BBExpandablePropertyView mDetailLocalMaxHTLC;
    private BBExpandablePropertyView mDetailRemoteRoutingFee;
    private BBExpandablePropertyView mDetailRemoteTimelockDelta;
    private BBExpandablePropertyView mDetailRemoteMinHTLC;
    private BBExpandablePropertyView mDetailRemoteMaxHTLC;
    private TextView mTvLocalRoutingPolicyHeading;
    private TextView mTvRemoteRoutingPolicyHeading;
    private int mChannelType;
    private long mChanId;
    private RoutingPolicy mLocalRoutingPolicy;


    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_channel_details);

        mDetailCapacity = findViewById(R.id.capacity);
        mDetailActivity = findViewById(R.id.activity);
        mDetailChannelLifetime = findViewById(R.id.channelLifetime);
        mDetailVisibility = findViewById(R.id.visibility);
        mDetailCommitmentType = findViewById(R.id.commitmentType);
        mDetailInitiator = findViewById(R.id.initiator);
        mDetailCloseInitiator = findViewById(R.id.closeInitiator);
        mDetailCloseType = findViewById(R.id.closeType);
        mDetailTimeLock = findViewById(R.id.timeLock);
        mDetailCommitFee = findViewById(R.id.commitFee);
        mDetailLocalReserve = findViewById(R.id.localReserve);
        mDetailRemoteReserve = findViewById(R.id.remoteReserve);
        mDetailLocalRoutingFee = findViewById(R.id.localRoutingFee);
        mDetailLocalTimelockDelta = findViewById(R.id.localTimelockDelta);
        mDetailLocalMinHTLC = findViewById(R.id.localMinHtlc);
        mDetailLocalMaxHTLC = findViewById(R.id.localMaxHtlc);
        mDetailRemoteRoutingFee = findViewById(R.id.remoteRoutingFee);
        mDetailRemoteTimelockDelta = findViewById(R.id.remoteTimelockDelta);
        mDetailRemoteMinHTLC = findViewById(R.id.remoteMinHtlc);
        mDetailRemoteMaxHTLC = findViewById(R.id.remoteMaxHtlc);
        mTvLocalRoutingPolicyHeading = findViewById(R.id.localRoutingPolicyHeading);
        mTvRemoteRoutingPolicyHeading = findViewById(R.id.remoteRoutingPolicyHeading);

        mCompositeDisposable = new CompositeDisposable();

        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            ByteString channelString = (ByteString) extras.getSerializable(ChannelDetailBSDFragment.ARGS_CHANNEL);
            mChannelType = extras.getInt(ChannelDetailBSDFragment.ARGS_TYPE);

            try {
                switch (mChannelType) {
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
    protected void onResume() {
        super.onResume();
        if (mChannelType == ChannelListItem.TYPE_OPEN_CHANNEL)
            fetchChannelInfo(mChanId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (FeatureManager.isEditRoutingPoliciesEnabled() && mChannelType == ChannelListItem.TYPE_OPEN_CHANNEL)
            getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.settingsButton) {
            if (mLocalRoutingPolicy != null) {
                Intent intentUpdateRoutingPolicy = new Intent(this, UpdateRoutingPolicyActivity.class);
                if (getIntent().getExtras() != null)
                    intentUpdateRoutingPolicy.putExtras(getIntent().getExtras());
                intentUpdateRoutingPolicy.putExtra(UpdateRoutingPolicyActivity.EXTRA_ROUTING_POLICY, mLocalRoutingPolicy.toByteString());
                startActivity(intentUpdateRoutingPolicy);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindOpenChannel(ByteString channelString) throws InvalidProtocolBufferException {
        Channel channel = Channel.parseFrom(channelString);
        mAlias = AliasManager.getInstance().getAlias(channel.getRemotePubkey());
        setTitle(mAlias);

        // capacity
        mDetailCapacity.setAmountValueSat(channel.getCapacity());
        mDetailCapacity.setVisibility(View.VISIBLE);

        // activity
        String activity = UtilFunctions.roundDouble(((double) (channel.getTotalSatoshisSent() + channel.getTotalSatoshisReceived()) / channel.getCapacity() * 100), 2) + "%";
        mDetailActivity.setValue(activity);
        mDetailActivity.setVisibility(View.VISIBLE);

        // channel lifetime
        int openHeight = UtilFunctions.getBlockHeightFromChanID(channel.getChanId());
        int currentHeight = Wallet.getInstance().getCurrentNodeInfo().getBlockHeight();
        int ageInBlocks = currentHeight - openHeight;
        String duration = TimeFormatUtil.formattedBlockDuration(ageInBlocks, AdvancedChannelDetailsActivity.this);
        mDetailChannelLifetime.setValue(duration);
        mDetailChannelLifetime.setVisibility(View.VISIBLE);

        // visibility
        String visibility;
        if (channel.getPrivate()) {
            visibility = getString(R.string.channel_visibility_private);
        } else {
            visibility = getString(R.string.channel_visibility_public);
        }
        mDetailVisibility.setValue(visibility);
        mDetailVisibility.setVisibility(View.VISIBLE);

        // commitment type
        mDetailCommitmentType.setValue(channel.getCommitmentType().name());
        mDetailCommitmentType.setVisibility(View.VISIBLE);

        // initiator
        String initiator;
        if (channel.getInitiator()) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setValue(initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);

        // commit fee
        mDetailCommitFee.setCanBlur(false);
        mDetailCommitFee.setAmountValueSat(channel.getCommitFee());
        mDetailCommitFee.setVisibility(View.VISIBLE);

        // time lock
        long timeLockInSeconds = channel.getLocalConstraints().getCsvDelay() * 10 * 60;
        String timeLock = channel.getLocalConstraints().getCsvDelay() + " (" + TimeFormatUtil.formattedDurationShort(timeLockInSeconds, AdvancedChannelDetailsActivity.this) + ")";
        mDetailTimeLock.setValue(timeLock);
        mDetailTimeLock.setVisibility(View.VISIBLE);

        // local reserve amount
        mDetailLocalReserve.setAmountValueSat(channel.getLocalConstraints().getChanReserveSat());
        mDetailLocalReserve.setVisibility(View.VISIBLE);

        // remote reserve amount
        mDetailRemoteReserve.setAmountValueSat(channel.getRemoteConstraints().getChanReserveSat());
        mDetailRemoteReserve.setVisibility(View.VISIBLE);

        // local routing policy heading
        String localRoutingPolicyHeading = getString(R.string.activity_routing_policy) + " (" + getString(R.string.you) + ")";
        mTvLocalRoutingPolicyHeading.setText(localRoutingPolicyHeading);

        // local fee
        mDetailLocalRoutingFee.setValue("- msat\n - %");
        mDetailLocalRoutingFee.setVisibility(View.VISIBLE);

        // local time lock delta
        mDetailLocalTimelockDelta.setVisibility(View.VISIBLE);

        // local min HTLC
        mDetailLocalMinHTLC.setVisibility(View.VISIBLE);

        // local max HTLC
        mDetailLocalMaxHTLC.setVisibility(View.VISIBLE);

        // remote routing policy heading
        String remoteRoutingPolicyHeading = getString(R.string.activity_routing_policy) + " (" + getString(R.string.peer) + ")";
        mTvRemoteRoutingPolicyHeading.setText(remoteRoutingPolicyHeading);

        // remote fee
        mDetailRemoteRoutingFee.setValue("- msat\n - %");
        mDetailRemoteRoutingFee.setVisibility(View.VISIBLE);

        // remote time lock delta
        mDetailRemoteTimelockDelta.setVisibility(View.VISIBLE);

        // remote min HTLC
        mDetailRemoteMinHTLC.setVisibility(View.VISIBLE);

        // remote max HTLC
        mDetailRemoteMaxHTLC.setVisibility(View.VISIBLE);

        mChanId = channel.getChanId();
        fetchChannelInfo(mChanId);
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
        mDetailVisibility.setValue(visibility);
        mDetailVisibility.setVisibility(View.VISIBLE);

        // commit fee
        mDetailCommitFee.setAmountValueSat(channel.getCommitFee());
        mDetailCommitFee.setVisibility(View.VISIBLE);

        // local reserve amount
        mDetailLocalReserve.setAmountValueSat(channel.getChannel().getLocalChanReserveSat());
        mDetailCommitFee.setVisibility(View.VISIBLE);

        // remote reserve amount
        mDetailRemoteReserve.setAmountValueSat(channel.getChannel().getRemoteChanReserveSat());
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
        mDetailCapacity.setAmountValueSat(channel.getCapacity());
        mDetailCapacity.setVisibility(View.VISIBLE);

        // commitment type
        mDetailCommitmentType.setValue(channel.getCommitmentType().name());
        mDetailCommitmentType.setVisibility(View.VISIBLE);

        // initiator
        String initiator;
        if (channel.getInitiator() == Initiator.INITIATOR_LOCAL) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setValue(initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);

        mTvLocalRoutingPolicyHeading.setVisibility(View.GONE);
        mTvRemoteRoutingPolicyHeading.setVisibility(View.GONE);
    }

    private void bindClosedChannel(ByteString channelString) throws InvalidProtocolBufferException {
        ChannelCloseSummary channel = ChannelCloseSummary.parseFrom(channelString);
        mAlias = AliasManager.getInstance().getAlias(channel.getRemotePubkey());
        setTitle(mAlias);

        // capacity
        mDetailCapacity.setAmountValueSat(channel.getCapacity());
        mDetailCapacity.setVisibility(View.VISIBLE);

        // channel lifetime
        int openHeight = UtilFunctions.getBlockHeightFromChanID(channel.getChanId());
        int closeHeight = channel.getCloseHeight();
        int ageInBlocks = closeHeight - openHeight;
        String duration = TimeFormatUtil.formattedBlockDuration(ageInBlocks, AdvancedChannelDetailsActivity.this);
        mDetailChannelLifetime.setContent(R.string.advanced_channel_details_lifetime_closed, duration, R.string.advanced_channel_details_explanation_lifetime_closed);
        mDetailChannelLifetime.setVisibility(View.VISIBLE);

        // initiator
        String initiator;
        if (channel.getOpenInitiator() == Initiator.INITIATOR_LOCAL) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setValue(initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);

        // close initiator
        String closeInitiator;
        if (channel.getCloseInitiator() == Initiator.INITIATOR_LOCAL) {
            closeInitiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            closeInitiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailCloseInitiator.setValue(closeInitiator);
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
        mDetailCloseType.setValue(closeTypeLabel);
        mDetailCloseType.setVisibility(View.VISIBLE);

        mTvLocalRoutingPolicyHeading.setVisibility(View.GONE);
        mTvRemoteRoutingPolicyHeading.setVisibility(View.GONE);
    }

    public void fetchChannelInfo(long chanID) {
        // Retrieve channel info from LND with gRPC (async)
        if (LndConnection.getInstance().getLightningService() != null) {
            mCompositeDisposable.add(LndConnection.getInstance().getLightningService().getChanInfo(ChanInfoRequest.newBuilder().setChanId(chanID).build())
                    .subscribe(chanInfoResponse -> {
                        RoutingPolicy localPolicy;
                        RoutingPolicy remotePolicy;
                        if (chanInfoResponse.getNode1Pub().equals(Wallet.getInstance().getCurrentNodeInfo().getLightningNodeUris()[0].getPubKey())) {
                            localPolicy = chanInfoResponse.getNode1Policy();
                            remotePolicy = chanInfoResponse.getNode2Policy();
                        } else {
                            localPolicy = chanInfoResponse.getNode2Policy();
                            remotePolicy = chanInfoResponse.getNode1Policy();
                        }
                        mLocalRoutingPolicy = localPolicy;
                        if (chanInfoResponse.hasNode1Policy()) {
                            BigDecimal localFeeRate = BigDecimal.valueOf((double) (localPolicy.getFeeRateMilliMsat()) / 10000.0).stripTrailingZeros();
                            String localFee = MonetaryUtil.getInstance().getDisplayStringFromMsats(localPolicy.getFeeBaseMsat()) + "\n+ " + localFeeRate.toPlainString() + " %";
                            mDetailLocalRoutingFee.setValue(localFee);
                            mDetailLocalMinHTLC.setAmountValueMsat(localPolicy.getMinHtlc());
                            mDetailLocalMaxHTLC.setAmountValueMsat(localPolicy.getMaxHtlcMsat());
                            long timeLockInSeconds = localPolicy.getTimeLockDelta() * 10 * 60;
                            String timeLock = localPolicy.getTimeLockDelta() + " (" + TimeFormatUtil.formattedDurationShort(timeLockInSeconds, AdvancedChannelDetailsActivity.this) + ")";
                            mDetailLocalTimelockDelta.setValue(timeLock);

                        }
                        if (chanInfoResponse.hasNode2Policy()) {
                            BigDecimal remoteFeeRate = BigDecimal.valueOf((double) (remotePolicy.getFeeRateMilliMsat()) / 10000.0).stripTrailingZeros();
                            String remoteFee = MonetaryUtil.getInstance().getDisplayStringFromMsats(remotePolicy.getFeeBaseMsat()) + "\n+ " + remoteFeeRate.toPlainString() + " %";
                            mDetailRemoteRoutingFee.setValue(remoteFee);
                            mDetailRemoteMinHTLC.setAmountValueMsat(remotePolicy.getMinHtlc());
                            mDetailRemoteMaxHTLC.setAmountValueMsat(remotePolicy.getMaxHtlcMsat());
                            long timeLockInSeconds = remotePolicy.getTimeLockDelta() * 10 * 60;
                            String timeLock = remotePolicy.getTimeLockDelta() + " (" + TimeFormatUtil.formattedDurationShort(timeLockInSeconds, AdvancedChannelDetailsActivity.this) + ")";
                            mDetailRemoteTimelockDelta.setValue(timeLock);
                        }
                    }, throwable -> {
                        BBLog.w(LOG_TAG, "Exception in fetch chanInfo task: " + throwable.getMessage());
                    }));
        }
    }
}