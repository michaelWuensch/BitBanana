package app.michaelwuensch.bitbanana.listViews.bolt12offers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.listViews.bolt12offers.itemDetails.Bolt12OfferDetailsActivity;
import app.michaelwuensch.bitbanana.listViews.bolt12offers.items.Bolt12OfferListItem;
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_Bolt12Offers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Bolt12OffersActivity extends BaseAppCompatActivity implements Bolt12OfferSelectListener, SwipeRefreshLayout.OnRefreshListener, Wallet_Bolt12Offers.Bolt12OffersSubscriptionListener {

    private static final String LOG_TAG = Bolt12OffersActivity.class.getSimpleName();
    public static final String EXTRA_BOLT12_OFFERS_ACTIVITY_MODE = "bolt12OffersActivityMode";
    public static final String EXTRA_SELECTED_OFFER = "selectedOffer";
    public static final int MODE_VIEW = 0;
    public static final int MODE_SELECT = 1;

    private RecyclerView mRecyclerView;
    private Bolt12OfferItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private List<Bolt12OfferListItem> mBolt12OffersItems;
    private TextView mEmptyListText;
    private CompositeDisposable mCompositeDisposable;
    private int mMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_bolt12_offers);
        mCompositeDisposable = new CompositeDisposable();

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mMode = extras.getInt(EXTRA_BOLT12_OFFERS_ACTIVITY_MODE);
        } else {
            mMode = 0;
        }

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        mRecyclerView = findViewById(R.id.bolt12OffersList);
        mEmptyListText = findViewById(R.id.listEmpty);

        mBolt12OffersItems = new ArrayList<>();

        Wallet_Bolt12Offers.getInstance().registerBolt12OffersSubscriptionListener(this);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(Bolt12OffersActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new Bolt12OfferItemAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        switch (mMode) {
            case MODE_VIEW:
                setupViewMode();
                break;
            case MODE_SELECT:
                setupSelectMode();
                break;
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BackendManager.hasBackendConfigs()) {
                    // Add a new offer
                    Intent intent = new Intent(Bolt12OffersActivity.this, CreateBolt12OfferActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(Bolt12OffersActivity.this, R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupViewMode() {
    }

    private void setupSelectMode() {
        String title = getString(R.string.select) + " ...";
        setTitle(title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the list
        updateBolt12OffersDisplayList();
        fetchBolt12Offers();
    }

    private void fetchBolt12Offers() {
        BBLog.d(LOG_TAG, "Updating bolt12 offers list...");
        Wallet_Bolt12Offers.getInstance().fetchBolt12Offers();
    }

    private void updateBolt12OffersDisplayList() {
        mBolt12OffersItems.clear();
        if (Wallet_Bolt12Offers.getInstance().getBolt12OffersList() != null) {
            for (Bolt12Offer offer : Wallet_Bolt12Offers.getInstance().getBolt12OffersList()) {
                Bolt12OfferListItem currItem = new Bolt12OfferListItem(offer);
                mBolt12OffersItems.add(currItem);
            }
        }

        // Show "No payment codes" if the list is empty
        if (mBolt12OffersItems.isEmpty()) {
            mEmptyListText.setVisibility(View.VISIBLE);
        } else {
            mEmptyListText.setVisibility(View.GONE);
        }

        // Update the list view
        mAdapter.replaceAll(mBolt12OffersItems);

        // Remove refreshing symbol
        mSwipeRefreshLayout.setRefreshing(false);

        BBLog.d(LOG_TAG, "Bolt12 offers list successfully updated.");
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        Wallet_Bolt12Offers.getInstance().unregisterBolt12OffersSubscriptionListener(this);
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
                final List<Bolt12OfferListItem> filteredBolt12OffersList = filter(mBolt12OffersItems, newText);
                mAdapter.replaceAll(filteredBolt12OffersList);
                mRecyclerView.scrollToPosition(0);
                return true;
            }
        });
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    private static List<Bolt12OfferListItem> filter(List<Bolt12OfferListItem> offerListItems, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        final List<Bolt12OfferListItem> filteredBolt12OffersList = new ArrayList<>();
        for (Bolt12OfferListItem offerListItem : offerListItems) {
            final String text = offerListItem.getBolt12Offer().getLabel() + offerListItem.getBolt12Offer().getDecodedBolt12().getDescription();
            if (text.contains(lowerCaseQuery)) {
                filteredBolt12OffersList.add(offerListItem);
            }
        }
        return filteredBolt12OffersList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(Bolt12OffersActivity.this, R.string.help_dialog_offers);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs() && Wallet.getInstance().isConnectedToNode()) {
            fetchBolt12Offers();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onOfferSelect(Serializable bolt12Offer) {
        switch (mMode) {
            case MODE_VIEW:
                if (bolt12Offer != null) {
                    Intent intentOfferDetails = new Intent(this, Bolt12OfferDetailsActivity.class);
                    intentOfferDetails.putExtra("bolt12offer", bolt12Offer);
                    startActivity(intentOfferDetails);
                }
                break;
            case MODE_SELECT:
                if (bolt12Offer != null) {
                    Bolt12Offer bolt12OfferDeserialized = (Bolt12Offer) bolt12Offer;
                    if (!bolt12OfferDeserialized.getIsActive()) {
                        showError(getString(R.string.error_selecting_inactive_bolt12_offer), 4000);
                        return;
                    }

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_SELECTED_OFFER, bolt12Offer);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
        }
    }


    @Override
    public void onQrCodeSelect(Serializable bolt12Offer) {
        if (bolt12Offer != null) {
            Intent intentQDetails = new Intent(this, Bolt12QRActivity.class);
            intentQDetails.putExtra("bolt12offer", bolt12Offer);
            startActivity(intentQDetails);
        }
    }

    @Override
    public void onBolt12OffersListUpdated() {
        updateBolt12OffersDisplayList();
    }
}
