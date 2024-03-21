package app.michaelwuensch.bitbanana.listViews.channels;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.CustomViewPager;
import app.michaelwuensch.bitbanana.fragments.OpenChannelBSDFragment;
import app.michaelwuensch.bitbanana.listViews.channels.itemDetails.ChannelDetailBSDFragment;
import app.michaelwuensch.bitbanana.listViews.channels.items.ChannelListItem;
import app.michaelwuensch.bitbanana.listViews.channels.items.ClosedChannelItem;
import app.michaelwuensch.bitbanana.listViews.channels.items.OpenChannelItem;
import app.michaelwuensch.bitbanana.listViews.channels.items.PendingChannelItem;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelBSDFragment;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_Channels;

public class ManageChannelsActivity extends BaseAppCompatActivity implements ChannelSelectListener, SwipeRefreshLayout.OnRefreshListener, Wallet_Channels.ChannelsUpdatedSubscriptionListener {

    private static final String LOG_TAG = ManageChannelsActivity.class.getSimpleName();

    private static int REQUEST_CODE_OPEN_CHANNEL = 100;

    private TextView mEmptyListText;
    private ChannelSummaryView mChannelSummaryView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private List<ChannelListItem> mChannelItems;
    private List<ChannelListItem> mClosedChannelItems;
    private String mCurrentSearchString = "";
    private CustomViewPager mViewPager;
    private ChannelsPagerAdapter mPagerAdapter;
    private boolean isOpenChannelView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_channels);

        Wallet_Channels.getInstance().registerChannelsUpdatedSubscriptionListener(this);

        mChannelSummaryView = findViewById(R.id.channelSummary);
        mEmptyListText = findViewById(R.id.listEmpty);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));

        // Setup view pager
        mViewPager = findViewById(R.id.channel_list_viewpager);
        mPagerAdapter = new ChannelsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(mViewPager, true);

        mChannelItems = new ArrayList<>();
        mClosedChannelItems = new ArrayList<>();

        FloatingActionButton fab = findViewById(R.id.fab);

        if (FeatureManager.isOpenChannelEnabled()) {
            fab.setOnClickListener(view -> {
                Intent intent = new Intent(ManageChannelsActivity.this, ScanNodePubKeyActivity.class);
                startActivityForResult(intent, REQUEST_CODE_OPEN_CHANNEL);
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                isOpenChannelView = position == 0;
                updateActivityTitle();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // This prevents the swipeRefreshLayout to not interfere on swiping
                toggleRefreshing(state == ViewPager.SCROLL_STATE_IDLE);
            }
        });


        // Display the current state of channels
        updateChannelsView();

        // Fetch channels from Node. This will automatically update the view when finished.
        // This is necessary, as we might display outdated data otherwise.
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            if (Wallet.getInstance().isConnectedToNode()) {
                Wallet_Channels.getInstance().fetchChannels();
            }
        }
    }

    private void updateChannelsView() {
        mChannelItems.clear();
        mClosedChannelItems.clear();

        List<ChannelListItem> offlineChannels = new ArrayList<>();

        long outbound = 0;
        long inbound = 0;
        long unavailable = 0;

        // Add all open channel items
        if (Wallet_Channels.getInstance().getOpenChannelsList() != null) {
            for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList()) {
                OpenChannelItem openChannelItem = new OpenChannelItem(c);
                if (c.isActive()) {
                    outbound += openChannelItem.getChannel().getLocalBalance();
                    inbound += openChannelItem.getChannel().getRemoteBalance();
                } else {
                    unavailable += openChannelItem.getChannel().getLocalBalance() + openChannelItem.getChannel().getRemoteBalance();
                }
                mChannelItems.add(openChannelItem);
            }
        }

        // Add all pending channel items
        if (Wallet_Channels.getInstance().getPendingChannelsList() != null) {
            for (PendingChannel c : Wallet_Channels.getInstance().getPendingChannelsList()) {
                PendingChannelItem pendingChannelItem = new PendingChannelItem(c);
                switch (c.getPendingType()) {
                    case PENDING_OPEN:
                        mChannelItems.add(pendingChannelItem);
                        break;
                    case PENDING_CLOSE:
                    case PENDING_FORCE_CLOSE:
                        mClosedChannelItems.add(pendingChannelItem);
                        break;
                }
            }
        }

        // Add all closed channel items
        if (Wallet_Channels.getInstance().getClosedChannelsList() != null) {
            for (ClosedChannel c : Wallet_Channels.getInstance().getClosedChannelsList()) {
                ClosedChannelItem closedChannelItem = new ClosedChannelItem(c);
                mClosedChannelItems.add(closedChannelItem);
            }
        }

        // Update channel summary
        mChannelSummaryView.updateBalances(outbound, inbound, unavailable);

        // Update items in recycler views
        if (mCurrentSearchString.isEmpty()) {
            mPagerAdapter.getOpenChannelsList().replaceAllItems(mChannelItems);
            mPagerAdapter.getClosedChannelsList().replaceAllItems(mClosedChannelItems);
        } else {
            final List<ChannelListItem> filteredChannelList = filter(mChannelItems, mCurrentSearchString);
            final List<ChannelListItem> filteredClosedChannelList = filter(mClosedChannelItems, mCurrentSearchString);
            mPagerAdapter.getOpenChannelsList().replaceAllItems(filteredChannelList);
            mPagerAdapter.getClosedChannelsList().replaceAllItems(filteredClosedChannelList);
        }

        updateActivityTitle();
    }

    private void updateActivityTitle() {
        // Update activity title and empty channel display.
        if (isOpenChannelView) {
            if (mChannelItems.size() > 0) {
                String title = getResources().getString(R.string.activity_manage_channels) + " (" + mChannelItems.size() + ")";
                setTitle(title);
                mEmptyListText.setVisibility(View.GONE);
            } else {
                setTitle(getResources().getString(R.string.activity_manage_channels));
                mEmptyListText.setVisibility(View.VISIBLE);
            }
        } else {
            if (mClosedChannelItems.size() > 0) {
                String title = getResources().getString(R.string.activity_manage_channels_closed) + " (" + mClosedChannelItems.size() + ")";
                setTitle(title);
                mEmptyListText.setVisibility(View.GONE);
            } else {
                setTitle(getResources().getString(R.string.activity_manage_channels_closed));
                mEmptyListText.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onChannelSelect(Serializable channel, int type) {
        if (channel != null) {
            ChannelDetailBSDFragment channelDetailBSDFragment = new ChannelDetailBSDFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(ChannelDetailBSDFragment.ARGS_CHANNEL, channel);
            bundle.putInt(ChannelDetailBSDFragment.ARGS_TYPE, type);
            channelDetailBSDFragment.setArguments(bundle);
            channelDetailBSDFragment.show(getSupportFragmentManager(), ChannelDetailBSDFragment.TAG);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OPEN_CHANNEL && resultCode == ScanNodePubKeyActivity.RESULT_CODE_NODE_URI) {
            if (data != null) {
                LightningNodeUri nodeUri = (LightningNodeUri) data.getSerializableExtra(ScanNodePubKeyActivity.EXTRA_NODE_URI);

                OpenChannelBSDFragment openChannelBSDFragment = new OpenChannelBSDFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(OpenChannelBSDFragment.ARGS_NODE_URI, nodeUri);
                openChannelBSDFragment.setArguments(bundle);
                openChannelBSDFragment.show(getSupportFragmentManager(), OpenChannelBSDFragment.TAG);
            }
        }

        if (requestCode == REQUEST_CODE_OPEN_CHANNEL && resultCode == ScanNodePubKeyActivity.RESULT_CODE_LNURL_CHANNEL) {
            if (data != null) {
                LnUrlChannelResponse channelResponse = (LnUrlChannelResponse) data.getSerializableExtra(ScanNodePubKeyActivity.EXTRA_CHANNEL_RESPONSE);
                LnUrlChannelBSDFragment lnUrlChannelBSDFragment = LnUrlChannelBSDFragment.createLnURLChannelDialog(channelResponse);
                lnUrlChannelBSDFragment.show(getSupportFragmentManager(), LnUrlChannelBSDFragment.TAG);
            }
        }
    }

    @Override
    protected void onDestroy() {
        Wallet_Channels.getInstance().unregisterChannelsUpdatedSubscriptionListener(this);

        super.onDestroy();
    }

    @Override
    public void onChannelsUpdated() {
        runOnUiThread(this::updateChannelsView);
        mSwipeRefreshLayout.setRefreshing(false);
        BBLog.d(LOG_TAG, "Channels updated!");
    }

    @Override
    public void onRefresh() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs() && Wallet.getInstance().isConnectedToNode()) {
            Wallet_Channels.getInstance().fetchChannels();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
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
                mCurrentSearchString = newText;
                final List<ChannelListItem> filteredChannelList = filter(mChannelItems, newText);
                mPagerAdapter.getOpenChannelsList().replaceAllItems(filteredChannelList);
                mPagerAdapter.getOpenChannelsList().scrollToPosition(0);

                final List<ChannelListItem> filteredClosedChannelList = filter(mClosedChannelItems, newText);
                mPagerAdapter.getClosedChannelsList().replaceAllItems(filteredClosedChannelList);
                mPagerAdapter.getClosedChannelsList().scrollToPosition(0);

                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private List<ChannelListItem> filter(List<ChannelListItem> items, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        final List<ChannelListItem> filteredItemList = new ArrayList<>();
        for (ChannelListItem item : items) {
            String text;
            String pubkey;

            switch (item.getType()) {
                case ChannelListItem.TYPE_OPEN_CHANNEL:
                    pubkey = ((OpenChannelItem) item).getChannel().getRemotePubKey();
                    text = pubkey + AliasManager.getInstance().getAlias(pubkey);
                    break;
                case ChannelListItem.TYPE_PENDING_CHANNEL:
                    pubkey = ((PendingChannelItem) item).getChannel().getRemotePubKey();
                    text = pubkey + AliasManager.getInstance().getAlias(pubkey);
                    break;
                case ChannelListItem.TYPE_CLOSED_CHANNEL:
                    pubkey = ((ClosedChannelItem) item).getChannel().getRemotePubKey();
                    text = pubkey + AliasManager.getInstance().getAlias(pubkey);
                    break;
                default:
                    text = "";
            }

            if (text.toLowerCase().contains(lowerCaseQuery)) {
                filteredItemList.add(item);
            }
        }
        return filteredItemList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialogWithLink(ManageChannelsActivity.this, R.string.help_dialog_channels, "LIGHTNINGNETWORk.PLUS", RefConstants.URL_LNPLUS);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class ChannelsPagerAdapter extends FragmentPagerAdapter {
        private ChannelListFragment mOpenChannelsList;
        private ChannelListFragment mClosedChannelsList;

        public ChannelsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mOpenChannelsList = new ChannelListFragment();
            mOpenChannelsList.setChannelSelectListener(ManageChannelsActivity.this);
            mClosedChannelsList = new ChannelListFragment();
            mClosedChannelsList.setChannelSelectListener(ManageChannelsActivity.this);
        }

        @Override
        public Fragment getItem(int pos) {
            switch (pos) {
                case 0:
                    return mOpenChannelsList;
                case 1:
                    return mClosedChannelsList;
                default:
                    return mOpenChannelsList;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        public ChannelListFragment getOpenChannelsList() {
            return mOpenChannelsList;
        }

        public ChannelListFragment getClosedChannelsList() {
            return mClosedChannelsList;
        }
    }

    private void toggleRefreshing(boolean enabled) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(enabled);
        }
    }
}
