package app.michaelwuensch.bitbanana.listViews.logs;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.listViews.logs.items.LogListItem;
import app.michaelwuensch.bitbanana.models.BBLogItem;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LogsActivity extends BaseAppCompatActivity implements LogSelectListener, SwipeRefreshLayout.OnRefreshListener, BBLog.LogAddedListener {

    private static final String LOG_TAG = LogsActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private LogItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private List<LogListItem> mLogItems = new ArrayList<>();
    private BBButton mClearBtn;
    private BBButton mCopyAllBtn;
    private SearchView mSearchView;
    private Spinner mSpVerbosity;
    private BBLogItem.Verbosity mVerbosity = BBLogItem.Verbosity.DEBUG;
    private boolean mIsBitBananaLog = true;
    private TabLayout mTabLayoutMode;

    private TextView mEmptyListText;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        mRecyclerView = findViewById(R.id.logsList);
        mEmptyListText = findViewById(R.id.listEmpty);

        mLogItems = new ArrayList<>();

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(LogsActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new LogItemAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mClearBtn = findViewById(R.id.clearButton);
        mClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BBLog.clearInAppLog();
                mLogItems.clear();
                updateLogsDisplayList();
            }
        });

        mCopyAllBtn = findViewById(R.id.copyButton);
        mCopyAllBtn.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                new UserGuardian(LogsActivity.this, new UserGuardian.OnGuardianConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        copyCompleteLog();
                    }

                    @Override
                    public void onCancelled() {

                    }
                }).securityCopyLog();
            }
        });

        mTabLayoutMode = findViewById(R.id.logTabLayout);
        mTabLayoutMode.setVisibility(BackendManager.getCurrentBackend().supportsShowBackendLog() ? View.VISIBLE : View.GONE);
        mTabLayoutMode.getTabAt(1).setText(BackendManager.getCurrentBackend().getNodeImplementationName());

        mTabLayoutMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        mIsBitBananaLog = true;
                        mClearBtn.setButtonEnabled(true);
                        break;
                    case 1:
                        mIsBitBananaLog = false;
                        mClearBtn.setButtonEnabled(false);
                        break;
                }
                fetchLogsAndUpdateList(true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        String[] items = new String[BBLogItem.Verbosity.values().length];
        for (int i = 0; i < BBLogItem.Verbosity.values().length; i++) {
            items[i] = BBLogItem.Verbosity.values()[i].getDisplayName();
        }

        mSpVerbosity = findViewById(R.id.verbositySpinner);
        mSpVerbosity.setAdapter(new ArrayAdapter<>(LogsActivity.this, R.layout.spinner_item, items));
        mSpVerbosity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mVerbosity = BBLogItem.Verbosity.values()[position];
                PrefsUtil.editPrefs().putInt("logVerbosity", position).commit();
                final List<LogListItem> filteredLogList = filterLog();
                if (filteredLogList.isEmpty()) {
                    mEmptyListText.setVisibility(View.VISIBLE);
                } else {
                    mEmptyListText.setVisibility(View.GONE);
                }
                mAdapter.replaceAll(filteredLogList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSpVerbosity.setSelection(PrefsUtil.getPrefs().getInt("logVerbosity", 1));

        BBLog.registerLogAddedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the list
        fetchLogsAndUpdateList(false);
    }

    private void copyCompleteLog() {
        String completeLog = "";
        List<LogListItem> logs = filterLog();
        for (LogListItem log : logs)
            if (log.getLogItem().isAllInfoInMessage())
                completeLog = completeLog + log.getLogItem().getMessage() + "\n";
            else
                completeLog = completeLog + log.getLogItem().getVerbosity() + " | " + log.getLogItem().getTag() + " | " + log.getLogItem().getMessage() + "\n";
        ClipBoardUtil.copyToClipboard(LogsActivity.this, "CompleteLog", completeLog);
    }

    private void fetchLogsAndUpdateList(boolean showLoading) {
        mLogItems.clear();
        if (mIsBitBananaLog) {
            List<BBLogItem> logs = BBLog.getInAppLogItems();
            if (logs != null) {
                for (BBLogItem log : logs) {
                    LogListItem currItem = new LogListItem(log);
                    mLogItems.add(currItem);
                }
            }
            updateLogsDisplayList();
        } else {
            if (showLoading) {
                mAdapter.replaceAll(mLogItems);
                mSwipeRefreshLayout.setRefreshing(true);
            }
            compositeDisposable.add(BackendManager.api().listBackendLogs()
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                        if (response != null) {
                            for (BBLogItem log : response) {
                                LogListItem currItem = new LogListItem(log);
                                mLogItems.add(currItem);
                            }
                        }
                        updateLogsDisplayList();
                    }, throwable -> BBLog.e(LOG_TAG, "Exception in fetch logs task: " + throwable.getMessage())));
        }
    }

    private void updateLogsDisplayList() {
        mVerbosity = BBLogItem.Verbosity.values()[PrefsUtil.getPrefs().getInt("logVerbosity", 1)];

        List<LogListItem> finalLogs = filterLog();

        // Show "No Logs" if the list is empty
        if (finalLogs.isEmpty()) {
            mEmptyListText.setVisibility(View.VISIBLE);
        } else {
            mEmptyListText.setVisibility(View.GONE);
        }

        // Update the list view
        mAdapter.replaceAll(finalLogs);

        // Remove refreshing symbol
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private List<LogListItem> filterLog() {
        List<LogListItem> verbosityFilteredLogs = new ArrayList<>();
        for (LogListItem item : mLogItems) {
            if (item.getLogItem().getVerbosity().ordinal() >= mVerbosity.ordinal())
                verbosityFilteredLogs.add(item);
        }
        List<LogListItem> finalLogs;
        if (isSearchQueryActive())
            finalLogs = filterByQuery(verbosityFilteredLogs, mSearchView.getQuery().toString());
        else
            finalLogs = verbosityFilteredLogs;
        return finalLogs;
    }

    private static List<LogListItem> filterByQuery(List<LogListItem> logListItems, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        final List<LogListItem> filteredLogList = new ArrayList<>();
        for (LogListItem logListItem : logListItems) {
            String text;

            if (logListItem.getLogItem().hasTag())
                text = logListItem.getLogItem().getTag().toLowerCase() + "|" + logListItem.getLogItem().getMessage().toLowerCase();
            else
                text = logListItem.getLogItem().getMessage().toLowerCase();
            if (text.contains(lowerCaseQuery)) {
                filteredLogList.add(logListItem);
            }
        }
        return filteredLogList;
    }

    private boolean isSearchQueryActive() {
        return mSearchView != null && !mSearchView.getQuery().toString().isEmpty();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.searchButton);
        mSearchView = (SearchView) menuItem.getActionView();
        mSearchView.setQueryHint(getResources().getString(R.string.search));

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                final List<LogListItem> filteredLogList = filterLog();
                if (filteredLogList.isEmpty()) {
                    mEmptyListText.setVisibility(View.VISIBLE);
                } else {
                    mEmptyListText.setVisibility(View.GONE);
                }
                mAdapter.replaceAll(filteredLogList);
                mRecyclerView.scrollToPosition(0);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        fetchLogsAndUpdateList(false);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        BBLog.unregisterLogAddedListener(this);
        super.onDestroy();
    }

    @Override
    public void onLogSelect(Serializable log) {
        BBLogItem logItem = (BBLogItem) log;
        String logMessage;
        if (logItem.isAllInfoInMessage())
            logMessage = logItem.getMessage();
        else
            logMessage = logItem.getVerbosity().name() + " | " + logItem.getTag() + " | " + logItem.getMessage();
        ClipBoardUtil.copyToClipboard(LogsActivity.this, "log", logMessage);
    }

    @Override
    public void onLogAdded(BBLogItem logItem) {
        LogListItem item = new LogListItem(logItem);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Code must run on the main/UI thread

                if (mIsBitBananaLog) {
                    mLogItems.add(item);

                    // Only add it to the view if it is not filtered out.
                    if (item.getLogItem().getVerbosity().ordinal() >= mVerbosity.ordinal()) {
                        if (isSearchQueryActive()) {
                            String text = item.getLogItem().getTag().toLowerCase() + "|" + item.getLogItem().getMessage().toLowerCase();
                            if (!text.contains(mSearchView.getQuery().toString().toLowerCase()))
                                return;
                        }
                        mAdapter.add(item);
                        mEmptyListText.setVisibility(View.GONE);
                    }
                }
            }
        });
    }
}
