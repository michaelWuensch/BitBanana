package app.michaelwuensch.bitbanana.listViews.utxos;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.listViews.utxos.itemDetails.UTXODetailBSDFragment;
import app.michaelwuensch.bitbanana.listViews.utxos.items.UTXOListItem;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_Components;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class UTXOsActivity extends BaseAppCompatActivity implements UTXOSelectListener, SwipeRefreshLayout.OnRefreshListener, Wallet_Components.UtxoSubscriptionListener {

    private static final String LOG_TAG = UTXOsActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private List<UTXOListItem> mUTXOItems;
    private TextView mEmptyListText;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_utxos);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        mRecyclerView = findViewById(R.id.utxoList);
        mEmptyListText = findViewById(R.id.listEmpty);

        mUTXOItems = new ArrayList<>();

        Wallet_Components.getInstance().registerUtxoSubscriptionListener(this);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(UTXOsActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new UTXOItemAdapter(mUTXOItems, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the list
        updateUTXOsDisplayList();
        Wallet_Components.getInstance().fetchUTXOs();
    }

    private void updateUTXOsDisplayList() {
        List<Utxo> utxos = Wallet_Components.getInstance().mUTXOsList;
        mUTXOItems.clear();
        if (utxos != null) {
            for (Utxo utxo : utxos) {
                UTXOListItem currItem = new UTXOListItem(utxo);
                mUTXOItems.add(currItem);
            }
            // Show "No UTXOs" if the list is empty
            if (mUTXOItems.size() == 0) {
                mEmptyListText.setVisibility(View.VISIBLE);
            } else {
                mEmptyListText.setVisibility(View.GONE);
            }

            // Update the list view
            mAdapter.notifyDataSetChanged();
        }
        // Remove refreshing symbol
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onUtxoSelect(Serializable utxo) {
        Bundle bundle = new Bundle();

        if (utxo != null) {
            UTXODetailBSDFragment utxoDetailBSDFragment = new UTXODetailBSDFragment();
            bundle.putSerializable(UTXODetailBSDFragment.ARGS_UTXO, utxo);
            utxoDetailBSDFragment.setArguments(bundle);
            utxoDetailBSDFragment.show(getSupportFragmentManager(), UTXODetailBSDFragment.TAG);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(UTXOsActivity.this, R.string.help_dialog_utxos);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs() && Wallet.getInstance().isConnectedToNode()) {
            Wallet_Components.getInstance().fetchUTXOs();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        Wallet_Components.getInstance().unregisterUtxoSubscriptionListener(this);
        super.onDestroy();
    }

    @Override
    public void onUtxoListUpdated() {
        updateUTXOsDisplayList();
    }
}
