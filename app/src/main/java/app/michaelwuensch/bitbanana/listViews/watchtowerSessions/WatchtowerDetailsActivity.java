package app.michaelwuensch.bitbanana.listViews.watchtowerSessions;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.listViews.contacts.ScanContactActivity;
import app.michaelwuensch.bitbanana.listViews.watchtowerSessions.itemDetails.SessionDetailBSDFragment;
import app.michaelwuensch.bitbanana.listViews.watchtowerSessions.items.WatchtowerSessionListItem;
import app.michaelwuensch.bitbanana.models.Watchtower;
import app.michaelwuensch.bitbanana.models.WatchtowerSession;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class WatchtowerDetailsActivity extends BaseAppCompatActivity implements WatchtowerSessionSelectListener {

    static final String LOG_TAG = WatchtowerDetailsActivity.class.getSimpleName();
    public static final int RESPONSE_CODE_REMOVE_WATCHTOWER = 214;
    public static final String EXTRA_WATCHTOWER = "extraWatchtower";

    private RecyclerView mRecyclerView;
    private WatchtowerSessionItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private TextView mEmptyListText;

    private CompositeDisposable mCompositeDisposable;
    private Watchtower mWatchtower;

    private ImageView mIvState;
    private TextView mTvState;
    private TextView mTvPubkey;
    private ImageView mIvPubkeyCopyIcon;
    private TextView mTVAddress;
    private ImageView mIvAddressCopyIcon;
    private Menu mMenu;
    private List<WatchtowerSessionListItem> mSessionItems;
    private TextView mTvSessionsHeading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchtower_details);

        mIvState = findViewById(R.id.stateImage);
        mTvState = findViewById(R.id.stateText);
        mTvPubkey = findViewById(R.id.remotePubKeyText);
        mIvPubkeyCopyIcon = findViewById(R.id.remotePubKeyCopyIcon);
        mTVAddress = findViewById(R.id.remoteAddress);
        mIvAddressCopyIcon = findViewById(R.id.remoteAddressCopyIcon);
        mTvSessionsHeading = findViewById(R.id.sessionsHeading);

        mCompositeDisposable = new CompositeDisposable();

        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            mWatchtower = (Watchtower) extras.getSerializable(EXTRA_WATCHTOWER);
        }

        mRecyclerView = findViewById(R.id.sessionList);
        mEmptyListText = findViewById(R.id.listEmpty);

        mSessionItems = new ArrayList<>();

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(WatchtowerDetailsActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new WatchtowerSessionItemAdapter(this);
        mRecyclerView.setAdapter(mAdapter);


        bindWatchtower(mWatchtower);
    }

    private void bindWatchtower(Watchtower watchtower) {
        setTitle(AliasManager.getInstance().getAlias(watchtower.getPubKey()));

        updateMenuVisibilities();

        if (watchtower.getIsActive()) {
            mIvState.setImageDrawable(AppCompatResources.getDrawable(WatchtowerDetailsActivity.this, R.drawable.outline_visibility_24));
            mTvState.setText(R.string.active);
            mTvState.setTextColor(getResources().getColor(R.color.green));
        } else {
            mIvState.setImageDrawable(AppCompatResources.getDrawable(WatchtowerDetailsActivity.this, R.drawable.outline_visibility_off_24));
            mTvState.setText(R.string.inactive);
            mTvState.setTextColor(getResources().getColor(R.color.red));
        }

        ////// General section
        mTvPubkey.setText(watchtower.getPubKey());
        mIvPubkeyCopyIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(WatchtowerDetailsActivity.this, "WatchtowerPubKey", watchtower.getPubKey()));
        mTVAddress.setText(watchtower.getAddress().get(0));
        mIvAddressCopyIcon.setOnClickListener(view1 -> ClipBoardUtil.copyToClipboard(WatchtowerDetailsActivity.this, "WatchtowerAddress", watchtower.getAddress().get(0)));

        updateSessionsDisplayList();
    }

    private void updateSessionsDisplayList() {


        if (Wallet.getInstance().isConnectedToNode()) {

            BBLog.v(LOG_TAG, "Update Sessions list.");

            mSessionItems.clear();
            for (WatchtowerSession session : mWatchtower.getSessions()) {
                WatchtowerSessionListItem currItem = new WatchtowerSessionListItem(session);
                mSessionItems.add(currItem);
            }

            String sessionsHeading = getString(R.string.watchtower_sessions) + " (" + mSessionItems.size() + ")";
            mTvSessionsHeading.setText(sessionsHeading);

            // Show "No sessions" if the list is empty
            if (mWatchtower.getSessions().isEmpty()) {
                mEmptyListText.setVisibility(View.VISIBLE);
            } else {
                mEmptyListText.setVisibility(View.GONE);
            }

            // Update the list view
            mAdapter.replaceAll(mSessionItems);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.watchtower_details_menu, menu);

        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;

            //noinspection RestrictedApi
            m.setOptionalIconsVisible(true);
        }

        mMenu = menu;
        updateMenuVisibilities();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_deactivate:
                deactivateWatchtower();
                break;
            case R.id.action_reactivate:
                reactivateWatchtower();
                break;
            case R.id.action_remove:
                removeWatchtower();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    private void deactivateWatchtower() {
        mCompositeDisposable.add(BackendManager.api().deactivateWatchtower(mWatchtower.getPubKey())
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(response -> {
                    BBLog.d(LOG_TAG, "Successfully deactivated watchtower. New state: " + response);
                    reloadWatchtowerInfo();
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error deactivating watchtower: " + throwable.getMessage());
                    showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                }));
    }

    private void reactivateWatchtower() {
        mCompositeDisposable.add(BackendManager.api().addWatchtower(mWatchtower.getPubKey(), mWatchtower.getAddress().get(0))
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(() -> {
                    BBLog.d(LOG_TAG, "Successfully added watchtower.");
                    reloadWatchtowerInfo();
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error reactivating watchtower: " + throwable.getMessage());
                    showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                }));
    }

    private void removeWatchtower() {
        if (mWatchtower.hasNonExhaustedSessions())
            new UserGuardian(WatchtowerDetailsActivity.this, new UserGuardian.OnGuardianConfirmedListener() {
                @Override
                public void onConfirmed() {
                    removeWatchtowerPart2();
                }

                @Override
                public void onCancelled() {

                }
            }).dumbRemoveWatchtower();
        else
            removeWatchtowerPart2();
    }

    private void removeWatchtowerPart2() {
        mCompositeDisposable.add(BackendManager.api().removeWatchtower(mWatchtower.getPubKey())
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(() -> {
                    BBLog.d(LOG_TAG, "Successfully removed watchtower.");
                    Intent intentRemoveWatchtower = new Intent();
                    intentRemoveWatchtower.putExtra(ScanContactActivity.EXTRA_NODE_URI, LightningNodeUriParser.parseNodeUri(mWatchtower.getPubKey() + "@" + mWatchtower.getAddress()));
                    setResult(RESPONSE_CODE_REMOVE_WATCHTOWER, intentRemoveWatchtower);
                    finish();
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error removing watchtower: " + throwable.getMessage());
                    showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                }));
    }

    private void reloadWatchtowerInfo() {
        mCompositeDisposable.add(BackendManager.api().getWatchtower(mWatchtower.getPubKey())
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(response -> {
                    BBLog.d(LOG_TAG, "Successfully fetched watchtower info.");
                    mWatchtower = response;
                    bindWatchtower(mWatchtower);
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Error fetching watchtower info: " + throwable.getMessage());
                    showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                }));
    }

    private void updateMenuVisibilities() {
        if (mMenu != null && mWatchtower != null) {
            mMenu.findItem(R.id.action_deactivate).setVisible(mWatchtower.getIsActive());
            mMenu.findItem(R.id.action_reactivate).setVisible(!mWatchtower.getIsActive());
        }
    }

    @Override
    public void onWatchtowerSessionSelect(Serializable session) {
        Bundle bundle = new Bundle();
        SessionDetailBSDFragment sessionDetailBSDFragment = new SessionDetailBSDFragment();
        bundle.putSerializable(SessionDetailBSDFragment.ARGS_SESSION, session);
        sessionDetailBSDFragment.setArguments(bundle);
        sessionDetailBSDFragment.show(getSupportFragmentManager(), SessionDetailBSDFragment.TAG);
    }
}
