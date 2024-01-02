package app.michaelwuensch.bitbanana.listViews.peers.itemDetails;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.view.menu.MenuBuilder;

import com.github.lightningnetwork.lnd.lnrpc.Channel;
import com.github.lightningnetwork.lnd.lnrpc.DisconnectPeerRequest;
import com.github.lightningnetwork.lnd.lnrpc.Feature;
import com.github.lightningnetwork.lnd.lnrpc.NodeInfoRequest;
import com.github.lightningnetwork.lnd.lnrpc.Peer;
import com.github.lightningnetwork.lnd.lnrpc.PendingChannelsResponse;
import com.github.lightningnetwork.lnd.lnrpc.TimestampedError;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.lnd.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.customView.BBExpandablePropertyView;
import app.michaelwuensch.bitbanana.listViews.contacts.ScanContactActivity;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUirParser;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.util.Wallet;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class PeerDetailsActivity extends BaseAppCompatActivity {

    static final String LOG_TAG = PeerDetailsActivity.class.getSimpleName();
    public static final int RESPONSE_CODE_OPEN_CHANNEL = 213;
    public static final int RESPONSE_CODE_DELETE_PEER = 214;
    public static final String EXTRA_PEER = "extraPeer";

    private CompositeDisposable mCompositeDisposable;
    private Peer mPeer;

    private TextView mTvPubkey;
    private ImageView mIvPubkeyCopyIcon;
    private TextView mTVAddress;
    private ImageView mIvAddressCopyIcon;
    private BBExpandablePropertyView mPing;
    private BBExpandablePropertyView mFlapCount;
    private BBExpandablePropertyView mLastFlap;
    private BBExpandablePropertyView mErrors;

    private BBExpandablePropertyView mChannelsWithYou;
    private BBExpandablePropertyView mTotalChannels;
    private BBExpandablePropertyView mTotalCapacity;
    private BBExpandablePropertyView mFeatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_details);

        mTvPubkey = findViewById(R.id.remotePubKeyText);
        mIvPubkeyCopyIcon = findViewById(R.id.remotePubKeyCopyIcon);
        mTVAddress = findViewById(R.id.remoteAddress);
        mIvAddressCopyIcon = findViewById(R.id.remoteAddressCopyIcon);
        mPing = findViewById(R.id.ping);
        mLastFlap = findViewById(R.id.lastFlap);
        mFlapCount = findViewById(R.id.flapCount);
        mErrors = findViewById(R.id.errors);
        mChannelsWithYou = findViewById(R.id.mutualChannels);
        mTotalChannels = findViewById(R.id.totalChannels);
        mTotalCapacity = findViewById(R.id.totalCapacity);
        mFeatures = findViewById(R.id.features);


        mCompositeDisposable = new CompositeDisposable();

        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            ByteString PeerString = (ByteString) extras.getSerializable(EXTRA_PEER);
            try {
                mPeer = Peer.parseFrom(PeerString);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }

        setTitle(AliasManager.getInstance().getAlias(mPeer.getPubKey()));

        ////// General section
        mTvPubkey.setText(mPeer.getPubKey());
        mIvPubkeyCopyIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(PeerDetailsActivity.this, "remotePubKey", mPeer.getPubKey()));
        mTVAddress.setText(mPeer.getAddress());
        mIvAddressCopyIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(PeerDetailsActivity.this, "remoteAddress", mPeer.getAddress()));

        ////// Reliability section
        mPing.setValue(mPeer.getPingTime() / 1000L + " ms");
        mFlapCount.setValue(String.valueOf(mPeer.getFlapCount()));
        if (mPeer.getLastFlapNs() == 0) {
            mLastFlap.setValue(getResources().getString(R.string.fee_not_available));
        } else {
            long durationSinceLastFlap = System.currentTimeMillis() - (mPeer.getLastFlapNs() / 1000000L);
            String durationString = TimeFormatUtil.formattedDuration(durationSinceLastFlap / 1000L, PeerDetailsActivity.this);
            mLastFlap.setValue(durationString);
        }

        // Errors
        mErrors.setValue(String.valueOf(mPeer.getErrorsCount()));
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, getResources().getConfiguration().getLocales().get(0));
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT, getResources().getConfiguration().getLocales().get(0));

        ArrayList<String> errorsAsStrings = new ArrayList<>();
        for (TimestampedError e : mPeer.getErrorsList()) {
            String formattedDate = df.format(new Date(e.getTimestamp() * 1000L));
            String formattedTime = tf.format(new Date(e.getTimestamp() * 1000L));
            errorsAsStrings.add(formattedDate + ", " + formattedTime + ":\n" + e.getError() + "\n\n");
        }
        Collections.reverse(errorsAsStrings);
        String errorsString = "";
        for (String e : errorsAsStrings) {

            errorsString = errorsString + e;
        }
        mErrors.setExplanation(errorsString);


        ////// Network Section
        // Channels with node
        int numChans = 0;
        for (Channel c : Wallet.getInstance().mOpenChannelsList) {
            if (c.getRemotePubkey().equals(mPeer.getPubKey())) {
                numChans++;
            }
        }
        for (PendingChannelsResponse.PendingOpenChannel c : Wallet.getInstance().mPendingOpenChannelsList) {
            if (c.getChannel().getRemoteNodePub().equals(mPeer.getPubKey())) {
                numChans++;
            }
        }
        mChannelsWithYou.setValue(String.valueOf(numChans));


        // Channel count & total Capacity
        NodeInfoRequest nodeInfoRequest = NodeInfoRequest.newBuilder()
                .setPubKey(mPeer.getPubKey())
                .build();

        mCompositeDisposable.add(LndConnection.getInstance().getLightningService().getNodeInfo(nodeInfoRequest)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .subscribe(nodeInfo -> {
                    mTotalChannels.setValue(String.valueOf(nodeInfo.getNumChannels()));
                    mTotalCapacity.setAmountValueSat(nodeInfo.getTotalCapacity());
                    mTotalCapacity.setCanBlur(false);
                    // ToDo: Check if customRecords are useful
                    // nodeInfo.getNode().getCustomRecordsMap()
                }, throwable -> {
                    BBLog.w(LOG_TAG, "Fetching node info failed. Exception in get node info (" + mPeer.getPubKey() + ") request task: " + throwable.getMessage());
                    mTotalChannels.setValue(getResources().getString(R.string.fee_not_available));
                    mTotalCapacity.mVsDetailsValueSwitcher.showNext();
                    mTotalCapacity.setValue(getResources().getString(R.string.fee_not_available));
                }));

        // Feature list
        mFeatures.setValue(String.valueOf(mPeer.getFeaturesMap().size()));
        ArrayList<String> featuresAsStrings = new ArrayList<>();
        for (Map.Entry<Integer, Feature> entry : mPeer.getFeaturesMap().entrySet()) {
            String padding = "";
            if (entry.getKey() < 10)
                padding = "         ";
            else if (entry.getKey() < 100)
                padding = "       ";
            else if (entry.getKey() < 1000)
                padding = "     ";
            else
                padding = "  ";
            featuresAsStrings.add(entry.getKey() + padding + entry.getValue().getName() + "\n");
        }
        featuresAsStrings.sort(new NumericStringComparator());
        String featureList = "";
        for (String entry : featuresAsStrings) {
            featureList = featureList + entry;
        }
        mFeatures.setExplanation(featureList);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.peer_details_menu, menu);

        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;

            //noinspection RestrictedApi
            m.setOptionalIconsVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_open_channel:
                openChannel();
                break;
            case R.id.action_disconnect:
                disconnect();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    private void disconnect() {
        BBLog.e(LOG_TAG, "disconnect called");
        DisconnectPeerRequest dpr = DisconnectPeerRequest.newBuilder()
                .setPubKey(mPeer.getPubKey())
                .build();
        mCompositeDisposable.add(LndConnection.getInstance().getLightningService().disconnectPeer(dpr)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .subscribe(disconnectPeerResponse -> {
                    BBLog.d(LOG_TAG, "Successfully disconnected peer");
                    Intent intentDeletePeer = new Intent();
                    intentDeletePeer.putExtra(ScanContactActivity.EXTRA_NODE_URI, LightningNodeUirParser.parseNodeUri(mPeer.getPubKey() + "@" + mPeer.getAddress()));
                    setResult(RESPONSE_CODE_DELETE_PEER, intentDeletePeer);
                    finish();
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error disconnecting peer: " + throwable.getMessage());
                }));
    }

    private void openChannel() {
        Intent intentOpenChannel = new Intent();
        intentOpenChannel.putExtra(ScanContactActivity.EXTRA_NODE_URI, LightningNodeUirParser.parseNodeUri(mPeer.getPubKey() + "@" + mPeer.getAddress()));
        setResult(RESPONSE_CODE_OPEN_CHANNEL, intentOpenChannel);
        finish();
    }

    class NumericStringComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            // Extract numeric parts from the strings
            Integer num1 = Integer.parseInt(s1.split(" ")[0]);
            Integer num2 = Integer.parseInt(s2.split(" ")[0]);

            // Compare numeric parts, if available
            if (num1 != null && num2 != null) {
                return num1.compareTo(num2);
            }

            // If numeric parts are not available, compare the strings lexicographically
            return s1.compareTo(s2);
        }
    }

}