package zapsolutions.zap;

import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.view.MenuItem;
import android.view.WindowManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import zapsolutions.zap.baseClasses.App;
import zapsolutions.zap.baseClasses.BaseAppCompatActivity;
import zapsolutions.zap.connection.LndConnection;
import zapsolutions.zap.connection.NetworkChangeReceiver;
import zapsolutions.zap.fragments.HistoryFragment;
import zapsolutions.zap.fragments.SettingsFragment;
import zapsolutions.zap.fragments.WalletFragment;
import zapsolutions.zap.connection.HttpClient;
import zapsolutions.zap.interfaces.UserGuardianInterface;
import zapsolutions.zap.util.MonetaryUtil;
import zapsolutions.zap.util.PrefsUtil;
import zapsolutions.zap.util.TimeOutUtil;
import zapsolutions.zap.util.UserGuardian;
import zapsolutions.zap.util.Wallet;
import zapsolutions.zap.util.ZapLog;

public class HomeActivity extends BaseAppCompatActivity implements LifecycleObserver,
        SharedPreferences.OnSharedPreferenceChangeListener,
        Wallet.InfoListener, Wallet.WalletLoadedListener, UserGuardianInterface {

    private static final String LOG_TAG = "Main Activity";

    private UserGuardian mUG;
    private ScheduledExecutorService mExchangeRateScheduler;
    private ScheduledExecutorService mLNDInfoScheduler;
    private NetworkChangeReceiver mNetworkChangeReceiver;
    private boolean mIsExchangeRateSchedulerRunning = false;
    private boolean mIsLNDInfoSchedulerRunning = false;
    private boolean mIsNetworkChangeReceiverRunning = false;
    private Fragment mCurrentFragment = null;
    private FragmentTransaction mFt;
    private boolean mInfoChangeListenerRegistered;
    private boolean mWalletLoadedListenerRegistered;
    private boolean mMainnetWarningShownOnce;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUG = new UserGuardian(this, this);

        // Register observer to detect if app goes to background
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // Set wallet fragment as beginning fragment
        mFt = getSupportFragmentManager().beginTransaction();
        mCurrentFragment = new WalletFragment();
        mFt.replace(R.id.mainContent, mCurrentFragment);
        mFt.commit();

        // Setup Listener
        BottomNavigationView navigation = findViewById(R.id.mainNavigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_wallet:
                    // Display the fragment as main content.
                    mFt = getSupportFragmentManager().beginTransaction();
                    mCurrentFragment = new WalletFragment();
                    mFt.replace(R.id.mainContent, mCurrentFragment);
                    //mFt.addToBackStack(null);
                    mFt.commit();
                    return true;
                case R.id.navigation_history:
                    // Display the fragment as main content.
                    mFt = getSupportFragmentManager().beginTransaction();
                    mCurrentFragment = new HistoryFragment();
                    mFt.replace(R.id.mainContent, mCurrentFragment);
                    //mFt.addToBackStack(null);
                    mFt.commit();
                    return true;
                case R.id.navigation_settings:
                    // Display the fragment as main content.
                    mFt = getSupportFragmentManager().beginTransaction();
                    mCurrentFragment = new SettingsFragment();
                    mFt.replace(R.id.mainContent, mCurrentFragment);
                    //mFt.addToBackStack(null);
                    mFt.commit();
                    return true;
            }
            return false;
        }
    };


    // This schedule keeps us up to date on exchange rates
    private void setupExchangeRateSchedule() {

        if (!mIsExchangeRateSchedulerRunning) {
            mIsExchangeRateSchedulerRunning = true;
            final JsonObjectRequest request = MonetaryUtil.getInstance().getExchangeRates();

            mExchangeRateScheduler =
                    Executors.newSingleThreadScheduledExecutor();

            mExchangeRateScheduler.scheduleAtFixedRate
                    (new Runnable() {
                        public void run() {
                            if(!MonetaryUtil.getInstance().getSecondCurrency().isBitcoin()) {
                                ZapLog.debug(LOG_TAG, "Fiat exchange rate request initiated");
                                // Adding request to request queue
                                HttpClient.getInstance().addToRequestQueue(request, "rateRequest");
                            }
                        }
                    }, 0, 3, TimeUnit.MINUTES);
        }

    }


    // This scheduled LND info request lets us know
    // if we have a working connection to LND and if we are still in sync with the network
    private void setupLNDInfoSchedule() {

        if (!mIsLNDInfoSchedulerRunning) {
            mIsLNDInfoSchedulerRunning = true;
            mLNDInfoScheduler =
                    Executors.newSingleThreadScheduledExecutor();

            mLNDInfoScheduler.scheduleAtFixedRate
                    (new Runnable() {
                        public void run() {
                            ZapLog.debug(LOG_TAG, "LND info check initiated");
                            Wallet.getInstance().fetchInfoFromLND();
                        }
                    }, 0, 30, TimeUnit.SECONDS);
        }

    }

    // Register the network status changed listener to handle network changes
    private void registerNetworkStatusChangeListener() {

        if (!mIsNetworkChangeReceiverRunning) {
            mIsNetworkChangeReceiverRunning = true;
            mNetworkChangeReceiver = new NetworkChangeReceiver();
            IntentFilter networkStatusIntentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(mNetworkChangeReceiver, networkStatusIntentFilter);
        }

    }


    // This function gets called when app is moved to foreground.
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onMoveToForeground() {
        ZapLog.debug(LOG_TAG, "Zap moved to foreground");

        if (PrefsUtil.isWalletSetup() && TimeOutUtil.getInstance().isTimedOut()) {
            // Go to PIN entry screen
            Intent intent = new Intent(this, PinEntryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {

            // start listeners and schedules
            setupExchangeRateSchedule();
            registerNetworkStatusChangeListener();

            if (!mWalletLoadedListenerRegistered) {
                Wallet.getInstance().registerWalletLoadedListener(this);
                mWalletLoadedListenerRegistered = true;
            }

            if (!mInfoChangeListenerRegistered) {
                Wallet.getInstance().registerInfoListener(this);
                mInfoChangeListenerRegistered = true;
            }

            PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

            // Restart lnd connection
            if (PrefsUtil.isWalletSetup()) {
                TimeOutUtil.getInstance().setCanBeRestarted(true);

                ZapLog.debug(LOG_TAG, "Starting to establish connections...");
                LndConnection.getInstance().restartBackgroundTasks();

                Wallet.getInstance().isLNDReachable();

            }
        }
    }

    // This function gets called when app is moved to background.
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onMoveToBackground() {

        ZapLog.debug(LOG_TAG, "Zap moved to background");

        App.getAppContext().connectionToLNDEstablished = false;

        stopListenersAndSchedules();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListenersAndSchedules();

        // Remove observer to detect if app goes to background
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
    }

    private void stopListenersAndSchedules() {
        if (TimeOutUtil.getInstance().getCanBeRestarted()) {
            TimeOutUtil.getInstance().restartTimer();
            ZapLog.debug(LOG_TAG, "PIN timer restarted");
        }
        TimeOutUtil.getInstance().setCanBeRestarted(false);

        // Unregister Wallet Loaded & Info Listener
        Wallet.getInstance().unregisterWalletLoadedListener(this);
        mWalletLoadedListenerRegistered = false;
        Wallet.getInstance().unregisterInfoListener(this);
        mInfoChangeListenerRegistered = false;

        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);

        if (mIsExchangeRateSchedulerRunning) {
            // Kill the scheduled exchange rate requests to go easy on the battery.
            mExchangeRateScheduler.shutdownNow();
            mIsExchangeRateSchedulerRunning = false;
        }

        if (mIsLNDInfoSchedulerRunning) {
            // Kill the LND info requests to go easy on the battery.
            mLNDInfoScheduler.shutdownNow();
            mIsLNDInfoSchedulerRunning = false;
        }

        if (mIsNetworkChangeReceiverRunning) {
            // Kill the Network state change listener to go easy on the battery.
            unregisterReceiver(mNetworkChangeReceiver);
            mIsNetworkChangeReceiverRunning = false;
        }

        // Kill Server Streams
        Wallet.getInstance().cancelTransactionSubscription();
        Wallet.getInstance().cancelInvoiceSubscription();
        Wallet.getInstance().cancelChannelEventSubscription();
        Wallet.getInstance().cancelChannelBackupSubscription();


        // Kill lnd connection
        if (PrefsUtil.isWalletSetup()) {
            LndConnection.getInstance().stopBackgroundTasks();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirmExit)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        HomeActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    @Override
    public void onInfoUpdated(boolean connected) {
        if ((PrefsUtil.isWalletSetup())) {
            if (!Wallet.getInstance().isTestnet() && Wallet.getInstance().isConnectedToLND()) {
                if (!mMainnetWarningShownOnce) {
                    // Show mainnet not ready warning
                    mUG.securityMainnetNotReady();
                    mMainnetWarningShownOnce = true;
                }
            }
        }
    }

    @Override
    public void guardianDialogConfirmed(String DialogName) {

    }

    @Override
    public void onWalletLoadedUpdated(boolean success, String error) {
        if (success) {
            // We managed to establish a connection to LND.
            // Now we can start to fetch all information needed from LND
            App.getAppContext().connectionToLNDEstablished = true;

            setupLNDInfoSchedule();

            // Fetch the transaction history
            Wallet.getInstance().fetchLNDTransactionHistory();

            // Fetch the channels from LND
            Wallet.getInstance().fetchOpenChannelsFromLND();
            Wallet.getInstance().fetchPendingChannelsFromLND();
            Wallet.getInstance().fetchClosedChannelsFromLND();

            Wallet.getInstance().subscribeToTransactions();
            Wallet.getInstance().subscribeToInvoices();
            Wallet.getInstance().subscribeToChannelEvents();
            Wallet.getInstance().subscribeToChannelBackup();

            ZapLog.debug(LOG_TAG, "Wallet loaded");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Update if primary currency has been switched from this or another activity
        if (key.equals(PrefsUtil.PREVENT_SCREEN_RECORDING)) {
            if (PrefsUtil.preventScreenRecording()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        }
    }
}