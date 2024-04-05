package app.michaelwuensch.bitbanana.listViews.forwardings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.github.lightningnetwork.lnd.lnrpc.ForwardingEvent;
import com.google.android.material.tabs.TabLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.customView.CustomViewPager;
import app.michaelwuensch.bitbanana.listViews.channels.UpdateRoutingPolicyActivity;
import app.michaelwuensch.bitbanana.listViews.forwardings.items.DateItem;
import app.michaelwuensch.bitbanana.listViews.forwardings.items.ForwardingEventListItem;
import app.michaelwuensch.bitbanana.listViews.forwardings.items.ForwardingListItem;
import app.michaelwuensch.bitbanana.models.Forward;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ForwardingActivity extends BaseAppCompatActivity implements ForwardingEventSelectListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String LOG_TAG = ForwardingActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private ForwardingEventItemAdapter mForwardingEventListAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TabLayout mTabLayoutPeriod;
    private long mPeriod = 24 * 60 * 60; // in seconds

    private List<ForwardingListItem> mForwardingItems;
    private List<ForwardingEvent> mTempForwardingEventsList;
    private List<Forward> mForwardsList;
    private TextView mEmptyListText;

    private CustomViewPager mSummaryViewPager;
    private ForwardingActivity.SummaryPagerAdapter mSummaryPagerAdapter;

    private long mEarnedMsats = 0;
    private long mRoutedMsats = 0;
    private int mRoutingEventsCount = 0;
    private boolean mIsVolume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forwarding);

        mTabLayoutPeriod = findViewById(R.id.periodTabLayout);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        mRecyclerView = findViewById(R.id.forwardingEventList);
        mEmptyListText = findViewById(R.id.listEmpty);

        mForwardingItems = new ArrayList<>();

        // Setup summary view pager
        mSummaryViewPager = findViewById(R.id.summary_viewpager);
        mSummaryPagerAdapter = new ForwardingActivity.SummaryPagerAdapter(getSupportFragmentManager());
        mSummaryViewPager.setAdapter(mSummaryPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(mSummaryViewPager, true);

        mSummaryViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateSummaryTexts();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(ForwardingActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mForwardingEventListAdapter = new ForwardingEventItemAdapter(this);
        mRecyclerView.setAdapter(mForwardingEventListAdapter);

        // display current state of the list
        updateForwardingEventDisplayList();

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
                        // all, achieved by setting the period to the last 50 years
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
        if (mForwardsList != null) {
            // Add all relevant items the forwardingEvents list
            for (Forward forwardingEvent : mForwardsList) {
                mEarnedMsats += forwardingEvent.getFee();
                mRoutedMsats += forwardingEvent.getAmountIn();
                ForwardingEventListItem currItem = new ForwardingEventListItem(forwardingEvent);
                forwardingEvents.add(currItem);
            }
        }
        mForwardingItems.addAll(forwardingEvents);
        mRoutingEventsCount = forwardingEvents.size();

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
        mForwardingEventListAdapter.replaceAll(mForwardingItems);

        // Set number in activity title
        if (mRoutingEventsCount > 0) {
            String title = getResources().getString(R.string.activity_forwarding) + " (" + mRoutingEventsCount + ")";
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
        if (mSummaryPagerAdapter.getAmountEarnedFragment() != null)
            mSummaryPagerAdapter.getAmountEarnedFragment().setAmountMSat(mEarnedMsats);
        if (mSummaryPagerAdapter.getRoutedVolumeFragment() != null) {
            mSummaryPagerAdapter.getRoutedVolumeFragment().setMsatPrecision(false);
            mSummaryPagerAdapter.getRoutedVolumeFragment().setAmountMSat(mRoutedMsats);
        }
        if (mSummaryPagerAdapter.getAverageEarnedFragment() != null)
            mSummaryPagerAdapter.getAverageEarnedFragment().setAmountMSat(mEarnedMsats / Math.max(mRoutingEventsCount, 1));
        if (mSummaryPagerAdapter.getAverageVolumeFragment() != null) {
            mSummaryPagerAdapter.getAverageVolumeFragment().setMsatPrecision(false);
            mSummaryPagerAdapter.getAverageVolumeFragment().setAmountMSat(mRoutedMsats / Math.max(mRoutingEventsCount, 1));
        }
        if (mSummaryPagerAdapter.getAverageEventsPerDayFragment() != null && mForwardsList != null) {
            long period;
            if (mTabLayoutPeriod.getSelectedTabPosition() == 6) {
                // when "all" is selected, we use the period between the first forwarding event that occurred and now to calculate the daily average.
                long now = System.currentTimeMillis();
                long firstForwarding = mForwardsList.get(0).getTimestampNs() / 1000000L;
                period = (now - firstForwarding) / 1000;
            } else {
                period = mPeriod;
            }
            double daysInPeriod = (period / (24.0 * 60.0 * 60.0));
            mSummaryPagerAdapter.getAverageEventsPerDayFragment().overrideValue(String.format(getResources().getConfiguration().getLocales().get(0), "%.2g%n", mRoutingEventsCount / daysInPeriod));
        }
    }

    @Override
    public void onRefresh() {
        refreshData();
    }


    @Override
    public void onForwardingEventSelect(Serializable forwardingEvent) {
        // ToDo: Open details page when a forwarding event was selected.
    }

    private void refreshData() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs() && Wallet.getInstance().isConnectedToNode()) {
            setTitle(getResources().getString(R.string.activity_forwarding));
            if (mSummaryPagerAdapter.getAmountEarnedFragment() != null)
                mSummaryPagerAdapter.getAmountEarnedFragment().setInProgress(true);
            if (mSummaryPagerAdapter.getRoutedVolumeFragment() != null)
                mSummaryPagerAdapter.getRoutedVolumeFragment().setInProgress(true);
            if (mSummaryPagerAdapter.getAverageEarnedFragment() != null)
                mSummaryPagerAdapter.getAverageEarnedFragment().setInProgress(true);
            if (mSummaryPagerAdapter.getAverageVolumeFragment() != null)
                mSummaryPagerAdapter.getAverageVolumeFragment().setInProgress(true);
            if (mSummaryPagerAdapter.getAverageEventsPerDayFragment() != null)
                mSummaryPagerAdapter.getAverageEventsPerDayFragment().setInProgress(true);
            mEmptyListText.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.INVISIBLE);
            fetchForwardingHistory(10000, (System.currentTimeMillis() / 1000) - mPeriod);
        } else {
            refreshFinished();
        }
    }

    private void refreshFinished() {
        if (mSummaryPagerAdapter.getAmountEarnedFragment() != null)
            mSummaryPagerAdapter.getAmountEarnedFragment().setInProgress(false);
        if (mSummaryPagerAdapter.getRoutedVolumeFragment() != null)
            mSummaryPagerAdapter.getRoutedVolumeFragment().setInProgress(false);
        if (mSummaryPagerAdapter.getAverageEarnedFragment() != null)
            mSummaryPagerAdapter.getAverageEarnedFragment().setInProgress(false);
        if (mSummaryPagerAdapter.getAverageVolumeFragment() != null)
            mSummaryPagerAdapter.getAverageVolumeFragment().setInProgress(false);
        if (mSummaryPagerAdapter.getAverageEventsPerDayFragment() != null)
            mSummaryPagerAdapter.getAverageEventsPerDayFragment().setInProgress(false);
        mRecyclerView.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void fetchForwardingHistory(int pageSize, long startTime) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(BackendManager.api().listForwards(0, pageSize, startTime)
                .timeout(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier(), TimeUnit.SECONDS)
                .subscribe(response -> {
                            mForwardsList = response;
                            compositeDisposable.dispose();
                            updateForwardingEventDisplayList();
                        }
                        , throwable -> {
                            BBLog.w(LOG_TAG, "Fetching forwarding event list failed." + throwable.getMessage());
                        }));
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

    public class SummaryPagerAdapter extends FragmentPagerAdapter {
        private ForwardingSummaryFragment mAmountEarned;
        private ForwardingSummaryFragment mRoutedVolume;
        private ForwardingSummaryFragment mAverageEarned;
        private ForwardingSummaryFragment mAverageVolume;
        private ForwardingSummaryFragment mAverageEventsPerDay;


        public SummaryPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mAmountEarned = new ForwardingSummaryFragment(ForwardingSummaryFragment.TYPE_AMOUNT_EARNED);
            mRoutedVolume = new ForwardingSummaryFragment(ForwardingSummaryFragment.TYPE_ROUTED_VOLUME);
            mAverageEarned = new ForwardingSummaryFragment(ForwardingSummaryFragment.TYPE_AVG_EARNED);
            mAverageVolume = new ForwardingSummaryFragment(ForwardingSummaryFragment.TYPE_AVG_ROUTED);
            mAverageEventsPerDay = new ForwardingSummaryFragment(ForwardingSummaryFragment.TYPE_AVG_EVENTS_PER_DAY);
        }

        @Override
        public Fragment getItem(int pos) {
            switch (pos) {
                case 0:
                    return mAmountEarned;
                case 1:
                    return mRoutedVolume;
                case 2:
                    return mAverageEarned;
                case 3:
                    return mAverageVolume;
                default:
                    return mAverageEventsPerDay;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

        public ForwardingSummaryFragment getAmountEarnedFragment() {
            return mAmountEarned;
        }

        public ForwardingSummaryFragment getRoutedVolumeFragment() {
            return mRoutedVolume;
        }

        public ForwardingSummaryFragment getAverageEarnedFragment() {
            return mAverageEarned;
        }

        public ForwardingSummaryFragment getAverageVolumeFragment() {
            return mAverageVolume;
        }

        public ForwardingSummaryFragment getAverageEventsPerDayFragment() {
            return mAverageEventsPerDay;
        }
    }
}