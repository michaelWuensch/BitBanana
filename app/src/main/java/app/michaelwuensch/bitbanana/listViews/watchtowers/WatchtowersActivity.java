package app.michaelwuensch.bitbanana.listViews.watchtowers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.OwnWatchtowerActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.listViews.watchtowerSessions.WatchtowerDetailsActivity;
import app.michaelwuensch.bitbanana.listViews.watchtowers.items.WatchtowerListItem;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.Watchtower;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_NodesAndPeers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class WatchtowersActivity extends BaseAppCompatActivity implements WatchtowerSelectListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String LOG_TAG = WatchtowersActivity.class.getSimpleName();
    private static int REQUEST_CODE_ADD_WATCHTOWER = 111;
    private static int REQUEST_CODE_WATCHTOWER_ACTION = 112;

    private RecyclerView mRecyclerView;
    private WatchtowerItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private List<WatchtowerListItem> mWatchtowerItems;
    private TextView mEmptyListText;
    private TextView mClientNotActiveText;
    private View mOwnWatchtowerLayout;
    private Button mOwnWatchtowerButton;
    private LightningNodeUri mOwnWatchtowerUri;
    private CompositeDisposable mCompositeDisposable;
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_watchtowers);
        mCompositeDisposable = new CompositeDisposable();

        // Own watchtower
        mOwnWatchtowerLayout = findViewById(R.id.ownWatchtowerLayout);
        mOwnWatchtowerButton = findViewById(R.id.ownWatchtowerButton);
        mCompositeDisposable.add(BackendManager.api().getOwnWatchtowerInfo().subscribe(response -> {
                    mOwnWatchtowerLayout.setVisibility(View.VISIBLE);
                    mOwnWatchtowerUri = response;
                }
                , throwable -> {
                    BBLog.w(LOG_TAG, "Fetching own watchtower info failed." + throwable.getMessage());
                }));
        mOwnWatchtowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WatchtowersActivity.this, OwnWatchtowerActivity.class);
                intent.putExtra("lightningNodeUri", mOwnWatchtowerUri);
                startActivity(intent);
            }
        });

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        mRecyclerView = findViewById(R.id.watchtowerList);
        mEmptyListText = findViewById(R.id.listEmpty);
        mClientNotActiveText = findViewById(R.id.wtClientNotActive);

        mWatchtowerItems = new ArrayList<>();

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(WatchtowersActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new WatchtowerItemAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mFab = findViewById(R.id.fab);
        mFab.setVisibility(View.GONE);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BackendManager.hasBackendConfigs()) {
                    // Add a new peer
                    Intent intent = new Intent(WatchtowersActivity.this, ScanWatchtowerActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_ADD_WATCHTOWER);
                } else {
                    Toast.makeText(WatchtowersActivity.this, R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the list
        updateWatchtowersDisplayList();
    }

    private void updateWatchtowersDisplayList() {

        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            if (Wallet.getInstance().isConnectedToNode()) {

                BBLog.v(LOG_TAG, "Update Watchtower list.");

                mCompositeDisposable.add(BackendManager.api().listWatchtowers()
                        .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                        .subscribe(response -> {
                                    BBLog.d(LOG_TAG, "List watchtowers successful.");
                                    mFab.setVisibility(View.VISIBLE);
                                    mClientNotActiveText.setVisibility(View.GONE);
                                    mWatchtowerItems.clear();
                                    ArrayList<String> watchtowersToFetchInfo = new ArrayList<>();
                                    for (Watchtower watchtower : response) {
                                        WatchtowerListItem currItem = new WatchtowerListItem(watchtower);
                                        mWatchtowerItems.add(currItem);
                                        if (!AliasManager.getInstance().hasUpToDateAliasInfo(watchtower.getPubKey()))
                                            watchtowersToFetchInfo.add(watchtower.getPubKey());
                                    }
                                    // Show "No watchtowers" if the list is empty
                                    if (mWatchtowerItems.size() == 0) {
                                        mEmptyListText.setVisibility(View.VISIBLE);
                                    } else {
                                        mEmptyListText.setVisibility(View.GONE);
                                    }

                                    // Set number in activity title
                                    if (mWatchtowerItems.size() > 0) {
                                        String title = getResources().getString(R.string.activity_watchtowers) + " (" + mWatchtowerItems.size() + ")";
                                        setTitle(title);
                                    } else {
                                        setTitle(getResources().getString(R.string.activity_watchtowers));
                                    }

                                    // Update the list view
                                    mAdapter.replaceAll(mWatchtowerItems);


                                    // Fetch aliases for watchtowers if necessary
                                    if (watchtowersToFetchInfo.size() > 0) {
                                        BBLog.d(LOG_TAG, "Fetching node info for " + watchtowersToFetchInfo.size() + " nodes.");

                                        mCompositeDisposable.add(Observable.range(0, watchtowersToFetchInfo.size())
                                                .concatMap(i -> Observable.just(i).delay(100, TimeUnit.MILLISECONDS))
                                                .doOnNext(integer -> Wallet_NodesAndPeers.getInstance().fetchNodeInfo(watchtowersToFetchInfo.get(integer), integer == watchtowersToFetchInfo.size() - 1, true, new Wallet_NodesAndPeers.NodeInfoFetchedListener() {
                                                    @Override
                                                    public void onNodeInfoFetched(String pubkey) {
                                                        if (pubkey.equals(watchtowersToFetchInfo.get(watchtowersToFetchInfo.size() - 1)))
                                                            updateWatchtowersDisplayList();
                                                    }
                                                }))
                                                .subscribe());
                                    }
                                }
                                , throwable -> {
                                    BBLog.w(LOG_TAG, "Fetching watchtower list failed." + throwable.getMessage());
                                    mClientNotActiveText.setVisibility(View.VISIBLE);
                                    mEmptyListText.setVisibility(View.GONE);
                                }));

                // Remove refreshing symbol
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    @Override
    public void onWatchtowerSelect(Serializable watchtower) {
        if (watchtower != null) {
            Intent intentWatchtowerDetails = new Intent(this, WatchtowerDetailsActivity.class);
            intentWatchtowerDetails.putExtra(WatchtowerDetailsActivity.EXTRA_WATCHTOWER, watchtower);
            startActivityForResult(intentWatchtowerDetails, REQUEST_CODE_WATCHTOWER_ACTION);
        }
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
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
                final List<WatchtowerListItem> filteredWatchtowerList = filter(mWatchtowerItems, newText);
                mAdapter.replaceAll(filteredWatchtowerList);
                mRecyclerView.scrollToPosition(0);
                return true;
            }
        });
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    private static List<WatchtowerListItem> filter(List<WatchtowerListItem> watchtowers, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        final List<WatchtowerListItem> filteredWatchtowerList = new ArrayList<>();
        for (WatchtowerListItem watchtower : watchtowers) {
            final String text = watchtower.getAlias().toLowerCase();
            if (text.contains(lowerCaseQuery)) {
                filteredWatchtowerList.add(watchtower);
            }
        }
        return filteredWatchtowerList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(WatchtowersActivity.this, getString(R.string.settings_featureWatchtowers_summary) + "\n\n" + getString(R.string.help_dialog_watchtowers));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs() && Wallet.getInstance().isConnectedToNode()) {
            updateWatchtowersDisplayList();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_WATCHTOWER && resultCode == RESULT_OK) {
            if (data != null) {
                LightningNodeUri nodeUri = (LightningNodeUri) data.getSerializableExtra(ScanWatchtowerActivity.EXTRA_NODE_URI);
                if (nodeUri != null) {
                    mCompositeDisposable.add(BackendManager.api().addWatchtower(nodeUri.getPubKey(), nodeUri.getHost() + ":" + nodeUri.getPort())
                            .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                            .subscribe(() -> {
                                BBLog.d(LOG_TAG, "Successfully added watchtower.");
                                updateWatchtowersDisplayList();
                            }, throwable -> {
                                BBLog.e(LOG_TAG, "Error adding watchtower: " + throwable.getMessage());
                            }));
                }
            }
        }

        if (requestCode == REQUEST_CODE_WATCHTOWER_ACTION) {
            if (resultCode == WatchtowerDetailsActivity.RESPONSE_CODE_REMOVE_WATCHTOWER) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateWatchtowersDisplayList();
                    }
                }, 500);
            }
        }
    }
}
