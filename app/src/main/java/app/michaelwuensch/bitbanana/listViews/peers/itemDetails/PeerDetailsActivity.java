package app.michaelwuensch.bitbanana.listViews.peers.itemDetails;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.view.menu.MenuBuilder;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBExpandablePropertyView;
import app.michaelwuensch.bitbanana.listViews.contacts.ScanContactActivity;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.LnFeature;
import app.michaelwuensch.bitbanana.models.Peer;
import app.michaelwuensch.bitbanana.models.TimestampedMessage;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Channels;
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
            mPeer = (Peer) extras.getSerializable(EXTRA_PEER);
        }

        setTitle(AliasManager.getInstance().getAlias(mPeer.getPubKey()));

        ////// General section
        mTvPubkey.setText(mPeer.getPubKey());
        mIvPubkeyCopyIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(PeerDetailsActivity.this, "remotePubKey", mPeer.getPubKey()));
        mTVAddress.setText(mPeer.getAddress());
        mIvAddressCopyIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(PeerDetailsActivity.this, "remoteAddress", mPeer.getAddress()));

        ////// Reliability section
        if (mPeer.hasPing())
            mPing.setValue(mPeer.getPing() / 1000L + " ms");
        else
            mPing.setVisibility(View.GONE);
        if (mPeer.hasFlapCount())
            mFlapCount.setValue(String.valueOf(mPeer.getFlapCount()));
        else
            mFlapCount.setVisibility(View.GONE);

        if (mPeer.hasLastFlap()) {
            if (mPeer.getLastFlapTimestamp() == 0) {
                mLastFlap.setValue(getResources().getString(R.string.fee_not_available));
            } else {
                long durationSinceLastFlap = System.currentTimeMillis() - (mPeer.getLastFlapTimestamp() / 1000000L);
                String durationString = TimeFormatUtil.formattedDuration(durationSinceLastFlap / 1000L, PeerDetailsActivity.this);
                mLastFlap.setValue(durationString);
            }
        } else {
            mLastFlap.setVisibility(View.GONE);
        }

        // Errors
        mErrors.setValue(String.valueOf(mPeer.getErrorMessages().size()));
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, getResources().getConfiguration().getLocales().get(0));
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT, getResources().getConfiguration().getLocales().get(0));

        ArrayList<String> errorsAsStrings = new ArrayList<>();
        for (TimestampedMessage message : mPeer.getErrorMessages()) {
            String formattedDate = df.format(new Date(message.getTimestamp() * 1000L));
            String formattedTime = tf.format(new Date(message.getTimestamp() * 1000L));
            errorsAsStrings.add(formattedDate + ", " + formattedTime + ":\n" + message.getMessage() + "\n\n");
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
        for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList()) {
            if (c.getRemotePubKey().equals(mPeer.getPubKey())) {
                numChans++;
            }
        }
        for (PendingChannel c : Wallet_Channels.getInstance().getPendingChannelsList()) {
            if (c.getRemotePubKey().equals(mPeer.getPubKey())) {
                numChans++;
            }
        }
        mChannelsWithYou.setValue(String.valueOf(numChans));


        // Channel count & total Capacity
        mCompositeDisposable.add(BackendManager.api().getNodeInfo(mPeer.getPubKey())
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribe(response -> {
                    if (response.hasNumChannels()) {
                        mTotalChannels.setVisibility(View.VISIBLE);
                        mTotalChannels.setValue(String.valueOf(response.getNumChannels()));
                    } else {
                        mTotalChannels.setVisibility(View.GONE);
                    }
                    if (response.hasTotalCapacity()) {
                        mTotalCapacity.setVisibility(View.VISIBLE);
                        mTotalCapacity.setAmountValueMsat(response.getTotalCapacity());
                        mTotalCapacity.setCanBlur(false);
                    } else {
                        mTotalCapacity.setVisibility(View.GONE);
                    }
                }, throwable -> {
                    BBLog.w(LOG_TAG, "Fetching node info failed. Exception in get node info (" + mPeer.getPubKey() + ") request task: " + throwable.getMessage());
                    mTotalChannels.setValue(getResources().getString(R.string.fee_not_available));
                    mTotalCapacity.mVsDetailsValueSwitcher.showNext();
                    mTotalCapacity.setValue(getResources().getString(R.string.fee_not_available));
                }));

        // Feature list
        if (mPeer.hasFeatures()) {
            mFeatures.setValue(String.valueOf(mPeer.getFeatures().size()));
            ArrayList<String> featuresAsStrings = new ArrayList<>();
            for (LnFeature feature : mPeer.getFeatures()) {
                String padding = "";
                if (feature.getFeatureNumber() < 10)
                    padding = "         ";
                else if (feature.getFeatureNumber() < 100)
                    padding = "       ";
                else if (feature.getFeatureNumber() < 1000)
                    padding = "     ";
                else
                    padding = "  ";
                featuresAsStrings.add(feature.getFeatureNumber() + padding + feature.getName() + "\n");
            }
            featuresAsStrings.sort(new NumericStringComparator());
            String featureList = "";
            for (String entry : featuresAsStrings) {
                featureList = featureList + entry;
            }
            mFeatures.setExplanation(featureList);
        } else {
            mFeatures.setVisibility(View.GONE);
        }
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
        mCompositeDisposable.add(BackendManager.api().disconnectPeer(mPeer.getPubKey())
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribe(() -> {
                    BBLog.d(LOG_TAG, "Successfully disconnected peer");
                    Intent intentDeletePeer = new Intent();
                    intentDeletePeer.putExtra(ScanContactActivity.EXTRA_NODE_URI, LightningNodeUriParser.parseNodeUri(mPeer.getPubKey() + "@" + mPeer.getAddress()));
                    setResult(RESPONSE_CODE_DELETE_PEER, intentDeletePeer);
                    finish();
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error disconnecting peer: " + throwable.getMessage());
                    showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                }));
    }

    private void openChannel() {
        Intent intentOpenChannel = new Intent();
        intentOpenChannel.putExtra(ScanContactActivity.EXTRA_NODE_URI, LightningNodeUriParser.parseNodeUri(mPeer.getPubKey() + "@" + mPeer.getAddress()));
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