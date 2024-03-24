package app.michaelwuensch.bitbanana.listViews.channels.itemDetails;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.math.BigDecimal;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBExpandablePropertyView;
import app.michaelwuensch.bitbanana.listViews.channels.UpdateRoutingPolicyActivity;
import app.michaelwuensch.bitbanana.listViews.channels.items.ChannelListItem;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.RoutingPolicy;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
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
    private BBExpandablePropertyView mDetailChannelType;
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
    private ShortChannelId mShortChannelId;
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
        mDetailChannelType = findViewById(R.id.commitmentType);
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
            mChannelType = extras.getInt(ChannelDetailBSDFragment.ARGS_TYPE);

            try {
                switch (mChannelType) {
                    case ChannelListItem.TYPE_OPEN_CHANNEL:
                        bindOpenChannel((OpenChannel) extras.getSerializable(ChannelDetailBSDFragment.ARGS_CHANNEL));
                        break;
                    case ChannelListItem.TYPE_PENDING_CHANNEL:
                        bindPendingChannel((PendingChannel) extras.getSerializable(ChannelDetailBSDFragment.ARGS_CHANNEL));
                        break;
                    case ChannelListItem.TYPE_CLOSED_CHANNEL:
                        bindClosedChannel((ClosedChannel) extras.getSerializable(ChannelDetailBSDFragment.ARGS_CHANNEL));
                        break;
                }
            } catch (NullPointerException exception) {
                BBLog.e(LOG_TAG, "Failed to parse channel.", exception);
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChannelType == ChannelListItem.TYPE_OPEN_CHANNEL)
            fetchChannelInfo(mShortChannelId);
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
                intentUpdateRoutingPolicy.putExtra(UpdateRoutingPolicyActivity.EXTRA_ROUTING_POLICY, mLocalRoutingPolicy);
                startActivity(intentUpdateRoutingPolicy);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindOpenChannel(OpenChannel channel) {
        mAlias = AliasManager.getInstance().getAlias(channel.getRemotePubKey());
        setTitle(mAlias);

        // capacity
        mDetailCapacity.setAmountValueMsat(channel.getCapacity());
        mDetailCapacity.setVisibility(View.VISIBLE);

        // activity
        String activity = UtilFunctions.roundDouble((channel.getActivity() * 100), 2) + "%";
        mDetailActivity.setValue(activity);
        mDetailActivity.setVisibility(View.VISIBLE);

        // channel lifetime
        int openHeight = channel.getShortChannelId().getBlockHeight();
        int currentHeight = Wallet.getInstance().getCurrentNodeInfo().getBlockHeight();
        int ageInBlocks = currentHeight - openHeight;
        String duration = TimeFormatUtil.formattedBlockDuration(ageInBlocks, AdvancedChannelDetailsActivity.this);
        mDetailChannelLifetime.setValue(duration);
        mDetailChannelLifetime.setVisibility(View.VISIBLE);

        // visibility
        String visibility;
        if (channel.isPrivate()) {
            visibility = getString(R.string.channel_visibility_private);
        } else {
            visibility = getString(R.string.channel_visibility_public);
        }
        mDetailVisibility.setValue(visibility);
        mDetailVisibility.setVisibility(View.VISIBLE);

        // channel type
        // ToDo: Improve channel types, to work with CoreLightning as well.
        if (channel.hasChannelType()) {
            mDetailChannelType.setValue(channel.getChannelType());
            mDetailChannelType.setVisibility(View.VISIBLE);
        }

        // initiator
        String initiator;
        if (channel.isInitiator()) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setValue(initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);

        // commit fee
        if (channel.hasCommitFee()) {
            mDetailCommitFee.setCanBlur(false);
            mDetailCommitFee.setAmountValueMsat(channel.getCommitFee());
            mDetailCommitFee.setVisibility(View.VISIBLE);
        }

        // time lock
        long timeLockInSeconds = (long) channel.getLocalChannelConstraints().getSelfDelay() * 10 * 60;
        String timeLock = channel.getLocalChannelConstraints().getSelfDelay() + " (" + TimeFormatUtil.formattedDurationShort(timeLockInSeconds, AdvancedChannelDetailsActivity.this) + ")";
        mDetailTimeLock.setValue(timeLock);
        mDetailTimeLock.setVisibility(View.VISIBLE);

        // local reserve amount
        mDetailLocalReserve.setAmountValueMsat(channel.getLocalChannelConstraints().getChannelReserve());
        mDetailLocalReserve.setVisibility(View.VISIBLE);

        // remote reserve amount
        mDetailRemoteReserve.setAmountValueMsat(channel.getRemoteChannelConstraints().getChannelReserve());
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

        mShortChannelId = channel.getShortChannelId();
        fetchChannelInfo(mShortChannelId);
    }

    private void bindPendingChannel(PendingChannel channel) {
        mAlias = AliasManager.getInstance().getAlias(channel.getRemotePubKey());
        setTitle(mAlias);

        // capacity
        mDetailCapacity.setAmountValueMsat(channel.getCapacity());
        mDetailCapacity.setVisibility(View.VISIBLE);

        // commitment type
        mDetailChannelType.setValue(channel.getChannelType());
        mDetailChannelType.setVisibility(View.VISIBLE);

        // initiator
        String initiator;
        if (channel.isInitiator()) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setValue(initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);

        // visibility
        String visibility;
        if (channel.isPrivate()) {
            visibility = getString(R.string.channel_visibility_private);
        } else {
            visibility = getString(R.string.channel_visibility_public);
        }
        mDetailVisibility.setValue(visibility);
        mDetailVisibility.setVisibility(View.VISIBLE);

        mTvLocalRoutingPolicyHeading.setVisibility(View.GONE);
        mTvRemoteRoutingPolicyHeading.setVisibility(View.GONE);

        // commit fee
        if (channel.hasCommitFee()) {
            mDetailCommitFee.setAmountValueMsat(channel.getCommitFee());
            mDetailCommitFee.setVisibility(View.VISIBLE);
        }
    }

    private void bindClosedChannel(ClosedChannel channel) {
        mAlias = AliasManager.getInstance().getAlias(channel.getRemotePubKey());
        setTitle(mAlias);

        // capacity
        mDetailCapacity.setAmountValueMsat(channel.getCapacity());
        mDetailCapacity.setVisibility(View.VISIBLE);

        // channel lifetime
        if (channel.hasCloseHeight()) {
            int openHeight = channel.getShortChannelId().getBlockHeight();
            int closeHeight = channel.getCloseHeight();
            int ageInBlocks = closeHeight - openHeight;
            String duration = TimeFormatUtil.formattedBlockDuration(ageInBlocks, AdvancedChannelDetailsActivity.this);
            mDetailChannelLifetime.setContent(R.string.advanced_channel_details_lifetime_closed, duration, R.string.advanced_channel_details_explanation_lifetime_closed);
            mDetailChannelLifetime.setVisibility(View.VISIBLE);
        }

        // initiator
        String initiator;
        if (channel.isOpenInitiator()) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setValue(initiator);
        mDetailInitiator.setVisibility(View.VISIBLE);

        // close initiator
        String closeInitiator;
        if (channel.isCloseInitiator()) {
            closeInitiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            closeInitiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailCloseInitiator.setValue(closeInitiator);
        mDetailCloseInitiator.setVisibility(View.VISIBLE);

        // visibility
        if (channel.hasPrivate()) {
            String visibility;
            if (channel.isPrivate()) {
                visibility = getString(R.string.channel_visibility_private);
            } else {
                visibility = getString(R.string.channel_visibility_public);
            }
            mDetailVisibility.setValue(visibility);
            mDetailVisibility.setVisibility(View.VISIBLE);
        }

        // close type
        if (channel.isHasCloseType()) {
            String closeTypeLabel = "";
            if (channel.getCloseType() == ClosedChannel.CloseType.COOPERATIVE_CLOSE) {
                closeTypeLabel = getResources().getString(R.string.channel_close_type_coop);
            } else if (channel.getCloseType() == ClosedChannel.CloseType.FORCE_CLOSE) {
                closeTypeLabel = getResources().getString(R.string.channel_close_type_force_close);
            } else if (channel.getCloseType() == ClosedChannel.CloseType.BREACH_CLOSE) {
                closeTypeLabel = getResources().getString(R.string.channel_close_type_breach);
            } else {
                closeTypeLabel = channel.getCloseType().name();
            }
            mDetailCloseType.setValue(closeTypeLabel);
            mDetailCloseType.setVisibility(View.VISIBLE);
        }


        mTvLocalRoutingPolicyHeading.setVisibility(View.GONE);
        mTvRemoteRoutingPolicyHeading.setVisibility(View.GONE);
    }

    public void fetchChannelInfo(ShortChannelId chanID) {
        // Retrieve channel info from LND with gRPC (async)
        if (Wallet.getInstance().isConnectedToNode()) {
            mCompositeDisposable.add(BackendManager.api().getPublicChannelInfo(chanID)
                    .subscribe(response -> {
                        RoutingPolicy localPolicy;
                        RoutingPolicy remotePolicy;
                        if (response.getNode1PubKey().equals(Wallet.getInstance().getCurrentNodeInfo().getLightningNodeUris()[0].getPubKey())) {
                            localPolicy = response.getNode1RoutingPolicy();
                            remotePolicy = response.getNode2RoutingPolicy();
                        } else {
                            localPolicy = response.getNode2RoutingPolicy();
                            remotePolicy = response.getNode1RoutingPolicy();
                        }
                        mLocalRoutingPolicy = localPolicy;

                        BigDecimal localFeeRate = BigDecimal.valueOf((double) (localPolicy.getFeeRate()) / 10000.0).stripTrailingZeros();
                        String localFee = MonetaryUtil.getInstance().getDisplayStringFromMsats(localPolicy.getFeeBase()) + "\n+ " + localFeeRate.toPlainString() + " %";
                        mDetailLocalRoutingFee.setValue(localFee);
                        mDetailLocalMinHTLC.setAmountValueMsat(localPolicy.getMinHTLC());
                        mDetailLocalMaxHTLC.setAmountValueMsat(localPolicy.getMaxHTLC());
                        long localTimeLockInSeconds = (long) localPolicy.getDelay() * 10 * 60;
                        String localTimeLock = localPolicy.getDelay() + " (" + TimeFormatUtil.formattedDurationShort(localTimeLockInSeconds, AdvancedChannelDetailsActivity.this) + ")";
                        mDetailLocalTimelockDelta.setValue(localTimeLock);

                        BigDecimal remoteFeeRate = BigDecimal.valueOf((double) (remotePolicy.getFeeRate()) / 10000.0).stripTrailingZeros();
                        String remoteFee = MonetaryUtil.getInstance().getDisplayStringFromMsats(remotePolicy.getFeeBase()) + "\n+ " + remoteFeeRate.toPlainString() + " %";
                        mDetailRemoteRoutingFee.setValue(remoteFee);
                        mDetailRemoteMinHTLC.setAmountValueMsat(remotePolicy.getMinHTLC());
                        mDetailRemoteMaxHTLC.setAmountValueMsat(remotePolicy.getMaxHTLC());
                        long remoteTimeLockInSeconds = (long) remotePolicy.getDelay() * 10 * 60;
                        String remoteTimeLock = remotePolicy.getDelay() + " (" + TimeFormatUtil.formattedDurationShort(remoteTimeLockInSeconds, AdvancedChannelDetailsActivity.this) + ")";
                        mDetailRemoteTimelockDelta.setValue(remoteTimeLock);

                    }, throwable -> {
                        BBLog.w(LOG_TAG, "Exception in fetch chanInfo task: " + throwable.getMessage());
                    }));
        }
    }
}