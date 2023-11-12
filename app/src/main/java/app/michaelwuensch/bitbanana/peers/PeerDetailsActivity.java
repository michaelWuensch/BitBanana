package app.michaelwuensch.bitbanana.peers;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.view.menu.MenuBuilder;

import com.github.lightningnetwork.lnd.lnrpc.DisconnectPeerRequest;
import com.github.lightningnetwork.lnd.lnrpc.Feature;
import com.github.lightningnetwork.lnd.lnrpc.Peer;
import com.github.lightningnetwork.lnd.lnrpc.TimestampedError;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.contacts.ScanContactActivity;
import app.michaelwuensch.bitbanana.lightning.LightningParser;
import app.michaelwuensch.bitbanana.tor.TorManager;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class PeerDetailsActivity extends BaseAppCompatActivity {

    static final String LOG_TAG = PeerDetailsActivity.class.getSimpleName();
    public static final int RESPONSE_CODE_OPEN_CHANNEL = 213;
    public static final int RESPONSE_CODE_DELETE_PEER = 214;
    public static final String EXTRA_PEER = "extraPeer";

    private CompositeDisposable mCompositeDisposable;
    private Peer mPeer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_details);

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

        // ToDo: Make a UI for the information that is currently only shown in log prints

        mPeer.getFeaturesMap();  // Same as getNodeInfo -> Features?
        mPeer.getBytesRecv();
        mPeer.getBytesSent();
        mPeer.getErrorsList(); // A list of errors:   Timestamp | Message String   Showed only disconnect so far.
        mPeer.getPingTime(); // Responsiveness of the peer... Unit?
        mPeer.getFlapCount(); // How often was the node down? This decreases over time to forgive a phase of high flaps.
        mPeer.getLastFlapNs(); // Last time the peer went down.
        mPeer.getAddress(); // String: Host + Port
        mPeer.getPubKey(); // String: pubkey
        mPeer.getSyncType(); // Probably not important for BitBanana

        //// General
        BBLog.w(LOG_TAG, "Pubkey: " + mPeer.getPubKey());
        BBLog.w(LOG_TAG, "Address: " + mPeer.getAddress());

        //// Reliability
        BBLog.e(LOG_TAG, "Ping time: " + mPeer.getPingTime());
        BBLog.e(LOG_TAG, "Flap count: " + mPeer.getFlapCount());
        BBLog.e(LOG_TAG, "Last flap: " + mPeer.getLastFlapNs());
        for (TimestampedError e : mPeer.getErrorsList()) {
            BBLog.e(LOG_TAG, "Error: " + e.getError());
        }

        //// Network
        // is channel partner
        // num channels
        // total public capacity
        for (Map.Entry<Integer, Feature> entry : mPeer.getFeaturesMap().entrySet()) {
            BBLog.e(LOG_TAG, "Feature: " + entry.getKey() + " " + entry.getValue().getName());
        }

        // getNodeInfo -> Custom Records?
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
                    intentDeletePeer.putExtra(ScanContactActivity.EXTRA_NODE_URI, LightningParser.parseNodeUri(mPeer.getPubKey() + "@" + mPeer.getAddress()));
                    setResult(RESPONSE_CODE_DELETE_PEER, intentDeletePeer);
                    finish();
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error disconnecting peer: " + throwable.getMessage());
                }));
    }

    private void openChannel() {
        Intent intentOpenChannel = new Intent();
        intentOpenChannel.putExtra(ScanContactActivity.EXTRA_NODE_URI, LightningParser.parseNodeUri(mPeer.getPubKey() + "@" + mPeer.getAddress()));
        setResult(RESPONSE_CODE_OPEN_CHANNEL, intentOpenChannel);
        finish();
    }
}