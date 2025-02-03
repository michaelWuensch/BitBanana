package app.michaelwuensch.bitbanana.listViews.utxos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.listViews.utxos.itemDetails.UTXODetailBSDFragment;
import app.michaelwuensch.bitbanana.listViews.utxos.items.UTXOListItem;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class UTXOsActivity extends BaseAppCompatActivity implements UTXOSelectListener, SwipeRefreshLayout.OnRefreshListener, Wallet_TransactionHistory.UtxoSubscriptionListener {

    public static final String EXTRA_UTXO_ACTIVITY_MODE = "utxoActivityMode";
    public static final String EXTRA_UTXO_SELECTED = "selectedUTXOs";
    public static final String EXTRA_UTXO_PRESELECTED = "preselectedUTXOs";
    public static final String EXTRA_TRANSACTION_AMOUNT = "transactionAmount";
    public static final int MODE_VIEW = 0;
    public static final int MODE_SELECT = 1;

    private static final String LOG_TAG = UTXOsActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private UTXOItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private List<UTXOListItem> mUTXOItems;
    private List<Outpoint> mPreselectedUTXOs = null;
    private TextView mEmptyListText;
    private Button mBtnConfirm;
    private AmountView mTotalSelectedAmount;
    private View mSelectionLayout;
    private TextView mTotalSelectedAmountLabel;
    private int mMode;
    private long mTransactionAmountMSat;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_utxos);


        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mMode = extras.getInt(EXTRA_UTXO_ACTIVITY_MODE);
            mPreselectedUTXOs = (List<Outpoint>) extras.getSerializable(EXTRA_UTXO_PRESELECTED);
            mTransactionAmountMSat = extras.getLong(EXTRA_TRANSACTION_AMOUNT);
        } else {
            mMode = 0;
        }

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        mRecyclerView = findViewById(R.id.utxoList);
        mEmptyListText = findViewById(R.id.listEmpty);
        mBtnConfirm = findViewById(R.id.confirmButton);
        mTotalSelectedAmount = findViewById(R.id.totalSelectedAmount);
        mSelectionLayout = findViewById(R.id.selectionLayout);
        mTotalSelectedAmountLabel = findViewById(R.id.totalSelectedAmountLabel);

        mUTXOItems = new ArrayList<>();

        Wallet_TransactionHistory.getInstance().registerUtxoSubscriptionListener(this);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(UTXOsActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new UTXOItemAdapter(mUTXOItems, this, mMode);
        mRecyclerView.setAdapter(mAdapter);

        switch (mMode) {
            case MODE_VIEW:
                setupViewMode();
                break;
            case MODE_SELECT:
                setupSelectMode();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the list
        updateUTXOsDisplayList();
        Wallet_TransactionHistory.getInstance().fetchUTXOs();
    }

    private void updateUTXOsDisplayList() {
        List<Utxo> utxos = Wallet_TransactionHistory.getInstance().getUTXOList();
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

            // Apply preselection if necessary
            if (mPreselectedUTXOs != null && !mPreselectedUTXOs.isEmpty()) {
                mAdapter.setPreselectedItems(mPreselectedUTXOs);
                mPreselectedUTXOs = null;
                updateUiToSelection();
            }

            // Update the list view
            mAdapter.notifyDataSetChanged();
        }
        // Remove refreshing symbol
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setupViewMode() {
        mSelectionLayout.setVisibility(View.GONE);
    }

    private void setupSelectMode() {
        String title = getString(R.string.select) + " ...";
        setTitle(title);
        mSelectionLayout.setVisibility(View.VISIBLE);
        mTotalSelectedAmount.setAmountMsat(0);
        mTotalSelectedAmount.setTextColor(ContextCompat.getColor(UTXOsActivity.this, R.color.red));
        mTotalSelectedAmountLabel.setText(getResources().getString(R.string.total) + ":");
        setConfirmButtonEnabled(false);
        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultIntent = new Intent();
                List<Utxo> selectedUTXOs = new ArrayList<>();
                for (UTXOListItem utxoListItem : mAdapter.getSelectedItems())
                    selectedUTXOs.add(utxoListItem.getUtxo());
                resultIntent.putExtra(EXTRA_UTXO_SELECTED, (Serializable) selectedUTXOs);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void setConfirmButtonEnabled(boolean enabled) {
        if (enabled) {
            mBtnConfirm.setEnabled(true);
            mBtnConfirm.setTextColor(getResources().getColor(R.color.banana_yellow));
        } else {
            mBtnConfirm.setEnabled(false);
            mBtnConfirm.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    @Override
    public void onUtxoSelect(Serializable utxo) {
        switch (mMode) {
            case MODE_VIEW:
                Bundle bundle = new Bundle();
                if (utxo != null) {
                    UTXODetailBSDFragment utxoDetailBSDFragment = new UTXODetailBSDFragment();
                    bundle.putSerializable(UTXODetailBSDFragment.ARGS_UTXO, utxo);
                    utxoDetailBSDFragment.setArguments(bundle);
                    utxoDetailBSDFragment.show(getSupportFragmentManager(), UTXODetailBSDFragment.LOG_TAG);
                }
                break;
            case MODE_SELECT:
                updateUiToSelection();
                break;
        }
    }

    private void updateUiToSelection() {
        if (mAdapter.getSelectedItems().isEmpty()) {
            setConfirmButtonEnabled(false);
            mTotalSelectedAmount.setAmountMsat(0);
            mTotalSelectedAmount.setTextColor(ContextCompat.getColor(UTXOsActivity.this, R.color.red));
        } else {
            long totalAmt = 0;
            for (UTXOListItem item : mAdapter.getSelectedItems())
                totalAmt += item.getUtxo().getAmount();
            mTotalSelectedAmount.setAmountMsat(totalAmt);
            if (totalAmt >= mTransactionAmountMSat) {
                setConfirmButtonEnabled(true);
                mTotalSelectedAmount.setTextColor(ContextCompat.getColor(UTXOsActivity.this, R.color.green));
            } else {
                setConfirmButtonEnabled(false);
                mTotalSelectedAmount.setTextColor(ContextCompat.getColor(UTXOsActivity.this, R.color.red));
            }
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
            switch (mMode) {
                case MODE_VIEW:
                    HelpDialogUtil.showDialog(UTXOsActivity.this, getString(R.string.help_dialog_utxos) + "\n\n" + getString(R.string.help_dialog_utxos_locking));
                    break;
                case MODE_SELECT:
                    HelpDialogUtil.showDialog(UTXOsActivity.this, getString(R.string.help_dialog_utxos) + "\n\n" + getString(R.string.help_dialog_utxos_select));
                    break;
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs() && Wallet.getInstance().isConnectedToNode()) {
            Wallet_TransactionHistory.getInstance().fetchUTXOs();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        Wallet_TransactionHistory.getInstance().unregisterUtxoSubscriptionListener(this);
        super.onDestroy();
    }

    @Override
    public void onUtxoListUpdated() {
        updateUTXOsDisplayList();
    }
}
