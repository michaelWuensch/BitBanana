package app.michaelwuensch.bitbanana.listViews.logs;

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
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.listViews.logs.items.LogListItem;
import app.michaelwuensch.bitbanana.models.BBLogItem;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LogsActivity extends BaseAppCompatActivity implements LogSelectListener, SwipeRefreshLayout.OnRefreshListener, BBLog.LogAddedListener {

    private static final String LOG_TAG = LogsActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private LogItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private List<LogListItem> mLogItems;
    private BBButton mClearBtn;
    private BBButton mCopyAllBtn;

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
                updateLogsDisplayList();
            }
        });

        mCopyAllBtn = findViewById(R.id.copyButton);
        mCopyAllBtn.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                String completeLog = "";
                for (LogListItem log : mLogItems)
                    completeLog = completeLog + log.getLogItem().getVerbosity() + " | " + log.getLogItem().getTag() + " | " + log.getLogItem().getMessage() + "\n";
                ClipBoardUtil.copyToClipboard(LogsActivity.this, "CompleteLog", completeLog);
            }
        });

        BBLog.registerLogAddedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the list
        updateLogsDisplayList();
    }

    private void updateLogsDisplayList() {
        List<BBLogItem> logs = BBLog.getInAppLogItems();
        mLogItems.clear();
        if (logs != null) {
            for (BBLogItem log : logs) {
                LogListItem currItem = new LogListItem(log);
                mLogItems.add(currItem);
            }
            // Show "No Logs" if the list is empty
            if (mLogItems.isEmpty()) {
                mEmptyListText.setVisibility(View.VISIBLE);
            } else {
                mEmptyListText.setVisibility(View.GONE);
            }

            // Update the list view
            mAdapter.replaceAll(mLogItems);
        }
        // Remove refreshing symbol
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        updateLogsDisplayList();
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
        ClipBoardUtil.copyToClipboard(LogsActivity.this, "log", logItem.getVerbosity().name() + " | " + logItem.getTag() + " | " + logItem.getMessage());
    }

    @Override
    public void onLogAdded(BBLogItem logItem) {
        LogListItem item = new LogListItem(logItem);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Code that must run on the main/UI thread
                mAdapter.add(item);
                mEmptyListText.setVisibility(View.GONE);
            }
        });
    }
}
