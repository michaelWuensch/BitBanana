package app.michaelwuensch.bitbanana.forwarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.lightningnetwork.lnd.lnrpc.ForwardingEvent;
import com.github.lightningnetwork.lnd.lnrpc.ForwardingHistoryRequest;
import com.google.android.material.tabs.TabLayout;
import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.channelManagement.UpdateRoutingPolicyActivity;
import app.michaelwuensch.bitbanana.connection.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.NodeConfigsManager;
import app.michaelwuensch.bitbanana.customView.SimpleAmountView;
import app.michaelwuensch.bitbanana.forwarding.listItems.DateItem;
import app.michaelwuensch.bitbanana.forwarding.listItems.ForwardingEventListItem;
import app.michaelwuensch.bitbanana.forwarding.listItems.ForwardingListItem;
import app.michaelwuensch.bitbanana.tor.TorManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ForwardingActivity extends BaseAppCompatActivity implements ForwardingEventSelectListener, SwipeRefreshLayout.OnRefreshListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = ForwardingActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private ForwardingEventItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TabLayout mTabLayoutPeriod;
    private TabLayout mDots;
    private long mPeriod = 24 * 60 * 60; // in seconds
    private SimpleAmountView mTVAmount;
    private TextView mTVUnit;
    private View mVHeaderProgress;
    private View mVHeaderSummary;
    private TextView mTvSummaryText;

    private List<ForwardingListItem> mForwardingItems;
    private List<ForwardingEvent> mTempForwardingEventsList;
    private List<ForwardingEvent> mForwardingEventsList;
    private TextView mEmptyListText;

    private long mEarnedMsats = 0;
    private long mRoutedMsats = 0;
    private boolean mIsVolume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forwarding);

        mTabLayoutPeriod = findViewById(R.id.periodTabLayout);
        mDots = findViewById(R.id.tabDots);
        mTVAmount = findViewById(R.id.amount);
        mTVUnit = findViewById(R.id.unit);
        mVHeaderProgress = findViewById(R.id.earnedFeeProgress);
        mVHeaderSummary = findViewById(R.id.forwardingHeaderSummary);
        mTvSummaryText = findViewById(R.id.forwardingSummaryText);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        mRecyclerView = findViewById(R.id.forwardingEventList);
        mEmptyListText = findViewById(R.id.listEmpty);

        mForwardingItems = new ArrayList<>();

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(ForwardingActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new ForwardingEventItemAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        // Make dots unclickable
        for (View v : mDots.getTouchables()) {
            v.setEnabled(false);
        }

        PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

        mIsVolume = PrefsUtil.getPrefs().getBoolean(PrefsUtil.ROUTING_SUMMARY_VOLUME, false);
        updateSummaryTexts();

        // display current state of the list
        updateForwardingEventDisplayList();

        mVHeaderSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsVolume = !mIsVolume;
                updateSummaryTexts();
            }
        });

        mTabLayoutPeriod.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        // 1 day
                        mPeriod = 24 * 60 * 60;
                        break;
                    case 1:
                        // 1 week
                        mPeriod = 7 * 24 * 60 * 60;
                        break;
                    case 2:
                        // 1 month
                        mPeriod = (365 / 12) * 24 * 60 * 60;
                        break;
                    case 3:
                        // 3 months
                        mPeriod = (365 / 4) * 24 * 60 * 60;
                        break;
                    case 4:
                        // 6 months
                        mPeriod = (365 / 2) * 24 * 60 * 60;
                        break;
                    case 5:
                        // 1 year
                        mPeriod = 365 * 24 * 60 * 60;
                        break;
                    case 6:
                        // all, achieved by setting the the period to the last 50 years
                        mPeriod = 50 * 365 * 24 * 60 * 60;
                        break;
                }
                refreshData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshData();
    }

    private void updateForwardingEventDisplayList() {

        // Save state, we want to keep the scroll offset after the update.
        Parcelable recyclerViewState;
        recyclerViewState = mRecyclerView.getLayoutManager().onSaveInstanceState();

        mForwardingItems.clear();

        List<ForwardingListItem> forwardingEvents = new LinkedList<>();
        Set<ForwardingListItem> dateLines = new HashSet<>();

        mEarnedMsats = 0;
        mRoutedMsats = 0;
        if (mForwardingEventsList != null) {
            // Add all relevant items the forwardingEvents list
            for (ForwardingEvent forwardingEvent : mForwardingEventsList) {
                mEarnedMsats += forwardingEvent.getFeeMsat();
                mRoutedMsats += forwardingEvent.getAmtInMsat();
                ForwardingEventListItem currItem = new ForwardingEventListItem(forwardingEvent);
                forwardingEvents.add(currItem);
            }
        }
        mForwardingItems.addAll(forwardingEvents);

        // Add the Date Lines
        for (ForwardingListItem item : forwardingEvents) {
            DateItem dateItem = new DateItem(item.getTimestampNS());
            dateLines.add(dateItem);
        }
        mForwardingItems.addAll(dateLines);

        // Show "No forwarding events" if the list is empty
        if (mForwardingItems.size() == 0) {
            mEmptyListText.setVisibility(View.VISIBLE);
        } else {
            mEmptyListText.setVisibility(View.GONE);
        }

        // Update the list view
        mAdapter.replaceAll(mForwardingItems);

        // Set number in activity title
        if (forwardingEvents.size() > 0) {
            String title = getResources().getString(R.string.activity_forwarding) + " (" + forwardingEvents.size() + ")";
            setTitle(title);
        } else {
            setTitle(getResources().getString(R.string.activity_forwarding));
        }

        updateSummaryTexts();

        // Restore state (e.g. scroll offset)
        mRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);

        refreshFinished();
    }

    private void updateSummaryTexts() {
        if (mIsVolume) {
            // Set earned amount texts
            mTVAmount.setAmount(mRoutedMsats / 1000);
            mTVUnit.setText(MonetaryUtil.getInstance().getPrimaryDisplayUnit());
            mTvSummaryText.setText(R.string.forwarding_volume_description);
            mDots.selectTab(mDots.getTabAt(1));
            PrefsUtil.editPrefs().putBoolean(PrefsUtil.ROUTING_SUMMARY_VOLUME, true).apply();
        } else {
            // Set earned amount texts
            mTVAmount.setAmount(mEarnedMsats / 1000);
            mTVUnit.setText(MonetaryUtil.getInstance().getPrimaryDisplayUnit());
            mTvSummaryText.setText(R.string.forwarding_earned_description);
            mDots.selectTab(mDots.getTabAt(0));
            PrefsUtil.editPrefs().putBoolean(PrefsUtil.ROUTING_SUMMARY_VOLUME, false).apply();
        }
    }

    @Override
    public void onRefresh() {
        refreshData();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onForwardingEventSelect(ByteString forwardingEvent) {
        // ToDo: Open details page when a forwarding event was selected.
    }

    private void refreshData() {
        if (NodeConfigsManager.getInstance().hasAnyConfigs() && LndConnection.getInstance().isConnected()) {
            setTitle(getResources().getString(R.string.activity_forwarding));
            mTVAmount.setVisibility(View.GONE);
            mVHeaderProgress.setVisibility(View.VISIBLE);
            mEmptyListText.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.INVISIBLE);
            fetchForwardingHistory(10000, mPeriod);
        } else {
            refreshFinished();
        }
    }

    private void refreshFinished() {
        mTVAmount.setVisibility(View.VISIBLE);
        mVHeaderProgress.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void fetchForwardingHistory(int pageSize, long timeframe) {
        mTempForwardingEventsList = new LinkedList<>();
        fetchForwardingHistory(pageSize, 0, timeframe);
    }

    private void fetchForwardingHistory(int pageSize, int lastOffset, long timeframe) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        if (LndConnection.getInstance().getLightningService() != null) {
            ForwardingHistoryRequest forwardingHistoryRequest = ForwardingHistoryRequest.newBuilder()
                    .setStartTime((System.currentTimeMillis() / 1000) - timeframe)
                    .setNumMaxEvents(pageSize)
                    .setIndexOffset(lastOffset)
                    .build();

            compositeDisposable.add(LndConnection.getInstance().getLightningService().forwardingHistory(forwardingHistoryRequest)
                    .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                    .subscribe(forwardingResponse -> {
                                if (forwardingResponse.getForwardingEventsList().size() == pageSize) {
                                    // The page is full, save the current list and load the next page
                                    mTempForwardingEventsList.addAll(forwardingResponse.getForwardingEventsList());
                                    fetchForwardingHistory(pageSize, forwardingResponse.getLastOffsetIndex(), timeframe);
                                } else {
                                    mTempForwardingEventsList.addAll(forwardingResponse.getForwardingEventsList());
                                    mForwardingEventsList = mTempForwardingEventsList;
                                    updateForwardingEventDisplayList();
                                    compositeDisposable.dispose();
                                }
                            }
                            , throwable -> {
                                BBLog.w(LOG_TAG, "Fetching forwarding event list failed." + throwable.getMessage());
                            }));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (FeatureManager.isEditRoutingPoliciesEnabled())
            getMenuInflater().inflate(R.menu.settings_menu, menu);
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(ForwardingActivity.this, R.string.help_dialog_forwarding);
            return true;
        }

        if (id == R.id.settingsButton) {
            Intent intentUpdateRoutingPolicy = new Intent(this, UpdateRoutingPolicyActivity.class);
            startActivity(intentUpdateRoutingPolicy);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("firstCurrencyIsPrimary")) {
            mTVUnit.setText(MonetaryUtil.getInstance().getPrimaryDisplayUnit());
        }
    }
}
