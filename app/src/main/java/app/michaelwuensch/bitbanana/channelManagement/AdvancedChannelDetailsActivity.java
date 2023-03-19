package app.michaelwuensch.bitbanana.channelManagement;

import android.os.Bundle;

import com.github.lightningnetwork.lnd.lnrpc.ChanInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Channel;
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
    private AdvancedChannelDetailView mDetailVisibility;
    private AdvancedChannelDetailView mDetailInitiator;
    private AdvancedChannelDetailView mDetailChannelLifetime;
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
        mDetailVisibility = findViewById(R.id.visibility);
        mDetailInitiator = findViewById(R.id.initiator);
        mDetailChannelLifetime = findViewById(R.id.channelLifetime);
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
                    // ToDo: The following channel types do not support advanced details so far
                    case ChannelListItem.TYPE_PENDING_OPEN_CHANNEL:
                        //bindPendingOpenChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_WAITING_CLOSE_CHANNEL:
                        //bindWaitingCloseChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_PENDING_CLOSING_CHANNEL:
                        //bindPendingCloseChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_PENDING_FORCE_CLOSING_CHANNEL:
                        //bindForceClosingChannel(channelString);
                        break;
                    case ChannelListItem.TYPE_CLOSED_CHANNEL:
                        //bindClosedChannel(channelString);
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

        // activity
        String activity = UtilFunctions.roundDouble(((double) (channel.getTotalSatoshisSent() + channel.getTotalSatoshisReceived()) / channel.getCapacity() * 100), 2) + "%";
        mDetailActivity.setContent(R.string.advanced_channel_details_activity, activity, R.string.advanced_channel_details_explanation_activity);

        // channel lifetime
        // ToDo: find out how to get the data for channel lifetime
        /*
        byte[] chanID = UtilFunctions.longToBytes(channel.getChanId());
        String fundingTxId = channel.getChannelPoint().substring(0, channel.getChannelPoint().indexOf(':'));
        mDetailChannelLifetime.setContent(R.string.advanced_channel_details_lifetime, "1y 2d", R.string.advanced_channel_details_explanation_lifetime);
         */

        // visibility
        String visibility;
        if (channel.getPrivate()) {
            visibility = getString(R.string.channel_visibility_private);
        } else {
            visibility = getString(R.string.channel_visibility_public);
        }
        mDetailVisibility.setContent(R.string.channel_visibility, visibility, R.string.advanced_channel_details_explanation_visibility);

        // initiator
        String initiator;
        if (channel.getInitiator()) {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_you);
        } else {
            initiator = getResources().getString(R.string.advanced_channel_details_initiator_peer);
        }
        mDetailInitiator.setContent(R.string.advanced_channel_details_initiator, initiator, R.string.advanced_channel_details_explanation_initiator);

        // commit fee
        String commitFee = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getCommitFee());
        mDetailCommitFee.setContent(R.string.advanced_channel_details_commit_fee, commitFee, R.string.advanced_channel_details_explanation_commit_fee);

        // time lock
        long timeLockInSeconds = channel.getLocalConstraints().getCsvDelay() * 10 * 60;
        String timeLock = String.valueOf(channel.getLocalConstraints().getCsvDelay()) + " (" + TimeFormatUtil.formattedDurationShort(timeLockInSeconds, AdvancedChannelDetailsActivity.this) + ")";
        mDetailTimeLock.setContent(R.string.advanced_channel_details_time_lock, timeLock, R.string.advanced_channel_details_explanation_time_lock);

        // local fee
        mDetailLocalRoutingFee.setContent(R.string.advanced_channel_details_local_routing_fee, "- msat\n - %", R.string.advanced_channel_details_explanation_local_routing_fee);

        // remote fee
        mDetailRemoteRoutingFee.setContent(R.string.advanced_channel_details_remote_routing_fee, "- msat\n - %", R.string.advanced_channel_details_explanation_remote_routing_fee);

        // local reserve amount
        String localReserve = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getLocalConstraints().getChanReserveSat());
        mDetailLocalReserve.setContent(R.string.advanced_channel_details_local_channel_reserve, localReserve, R.string.advanced_channel_details_explanation_local_channel_reserve);

        // remote reserve amount
        String remoteReserve = MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(channel.getRemoteConstraints().getChanReserveSat());
        mDetailRemoteReserve.setContent(R.string.advanced_channel_details_remote_channel_reserve, remoteReserve, R.string.advanced_channel_details_explanation_remote_channel_reserve);

        fetchChannelInfo(channel.getChanId());
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