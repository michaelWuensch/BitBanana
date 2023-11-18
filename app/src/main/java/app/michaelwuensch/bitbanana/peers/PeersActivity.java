package app.michaelwuensch.bitbanana.peers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.lightningnetwork.lnd.lnrpc.ListPeersRequest;
import com.github.lightningnetwork.lnd.lnrpc.Peer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.NodeConfigsManager;
import app.michaelwuensch.bitbanana.contacts.ScanContactActivity;
import app.michaelwuensch.bitbanana.lightning.LightningNodeUri;
import app.michaelwuensch.bitbanana.tor.TorManager;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.Wallet;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class PeersActivity extends BaseAppCompatActivity implements PeerSelectListener, SwipeRefreshLayout.OnRefreshListener, Wallet.PeerUpdateListener {

    private static final String LOG_TAG = PeersActivity.class.getSimpleName();
    private static int REQUEST_CODE_ADD_PEER = 111;
    private static int REQUEST_CODE_PEER_ACTION = 112;

    private RecyclerView mRecyclerView;
    private PeerItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private List<PeerListItem> mPeersItems;
    private TextView mEmptyListText;
    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_peers);
        mCompositeDisposable = new CompositeDisposable();

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        mRecyclerView = findViewById(R.id.peerList);
        mEmptyListText = findViewById(R.id.listEmpty);

        mPeersItems = new ArrayList<>();

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(PeersActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new PeerItemAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Add a new peer
                Intent intent = new Intent(PeersActivity.this, ScanPeerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivityForResult(intent, REQUEST_CODE_ADD_PEER);
            }
        });

        // Register listeners
        Wallet.getInstance().registerPeerUpdateListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the list
        updatePeersDisplayList();
    }

    private void updatePeersDisplayList() {

        ListPeersRequest listPeersRequest = ListPeersRequest.newBuilder()
                .build();

        mCompositeDisposable.add(LndConnection.getInstance().getLightningService().listPeers(listPeersRequest)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .subscribe(listPeersResponse -> {
                            mPeersItems.clear();
                            ArrayList<String> peersToFetchInfo = new ArrayList<>();
                            for (Peer peer : listPeersResponse.getPeersList()) {
                                PeerListItem currItem = new PeerListItem(peer);
                                mPeersItems.add(currItem);
                                if (!AliasManager.getInstance().hasUpToDateAliasInfo(peer.getPubKey()))
                                    peersToFetchInfo.add(peer.getPubKey());
                            }
                            // Show "No peers" if the list is empty
                            if (mPeersItems.size() == 0) {
                                mEmptyListText.setVisibility(View.VISIBLE);
                            } else {
                                mEmptyListText.setVisibility(View.GONE);
                            }

                            // Set number in activity title
                            if (mPeersItems.size() > 0) {
                                String title = getResources().getString(R.string.activity_peers) + " (" + mPeersItems.size() + ")";
                                setTitle(title);
                            } else {
                                setTitle(getResources().getString(R.string.activity_peers));
                            }

                            // Update the list view
                            mAdapter.replaceAll(mPeersItems);


                            // Fetch aliases for peers if necessary
                            if (peersToFetchInfo.size() > 0) {
                                BBLog.d(LOG_TAG, "Fetching node info for " + peersToFetchInfo.size() + " nodes.");

                                mCompositeDisposable.add(Observable.range(0, peersToFetchInfo.size())
                                        .concatMap(i -> Observable.just(i).delay(100, TimeUnit.MILLISECONDS))
                                        .doOnNext(integer -> Wallet.getInstance().fetchNodeInfoFromLND(peersToFetchInfo.get(integer), integer == peersToFetchInfo.size() - 1, true, null))
                                        .subscribe());
                            }

                        }
                        , throwable -> {
                            BBLog.w(LOG_TAG, "Fetching peer list failed." + throwable.getMessage());
                        }));


        // Remove refreshing symbol
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onPeerSelect(ByteString peer) {
        Bundle bundle = new Bundle();

        if (peer != null) {
            Intent intentPeerDetails = new Intent(this, PeerDetailsActivity.class);
            intentPeerDetails.putExtra(PeerDetailsActivity.EXTRA_PEER, peer);
            startActivityForResult(intentPeerDetails, REQUEST_CODE_PEER_ACTION);
        }
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        Wallet.getInstance().unregisterPeerUpdateListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.searchButton);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint(getResources().getString(R.string.search));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                final List<PeerListItem> filteredPeersList = filter(mPeersItems, newText);
                mAdapter.replaceAll(filteredPeersList);
                mRecyclerView.scrollToPosition(0);
                return true;
            }
        });
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    private static List<PeerListItem> filter(List<PeerListItem> peers, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        final List<PeerListItem> filteredPeerList = new ArrayList<>();
        for (PeerListItem peer : peers) {
            final String text = peer.getAlias().toLowerCase();
            if (text.contains(lowerCaseQuery)) {
                filteredPeerList.add(peer);
            }
        }
        return filteredPeerList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(PeersActivity.this, R.string.help_dialog_peers);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        if (NodeConfigsManager.getInstance().hasAnyConfigs() && LndConnection.getInstance().isConnected()) {
            updatePeersDisplayList();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_PEER && resultCode == RESULT_OK) {
            if (data != null) {
                LightningNodeUri nodeUri = (LightningNodeUri) data.getSerializableExtra(ScanContactActivity.EXTRA_NODE_URI);
                if (nodeUri != null) {
                    Wallet.getInstance().connectPeer(nodeUri, false, 0, 0, false);
                }
            }
        }

        if (requestCode == REQUEST_CODE_PEER_ACTION) {
            if (resultCode == PeerDetailsActivity.RESPONSE_CODE_DELETE_PEER) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updatePeersDisplayList();
                    }
                }, 500);

            }

            if (resultCode == PeerDetailsActivity.RESPONSE_CODE_OPEN_CHANNEL) {
                if (data != null) {
                    LightningNodeUri nodeUri = (LightningNodeUri) data.getSerializableExtra(ScanContactActivity.EXTRA_NODE_URI);
                    Intent intent = new Intent();
                    intent.putExtra(ScanContactActivity.EXTRA_NODE_URI, nodeUri);
                    setResult(resultCode, intent);
                    finish();
                }
            }
        }
    }

    @Override
    public void onConnectedToPeer() {
        updatePeersDisplayList();
    }
}
