package app.michaelwuensch.bitbanana.home;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.viewpager.widget.ViewPager;

import com.github.lightningnetwork.lnd.lnrpc.PayReq;
import com.google.android.material.internal.NavigationMenuView;
import com.google.android.material.navigation.NavigationView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.BuildConfig;
import app.michaelwuensch.bitbanana.IdentityActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backup.BackupActivity;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.internetConnectionStatus.NetworkChangeReceiver;
import app.michaelwuensch.bitbanana.customView.CustomViewPager;
import app.michaelwuensch.bitbanana.customView.UserAvatarView;
import app.michaelwuensch.bitbanana.fragments.ChooseNodeActionBSDFragment;
import app.michaelwuensch.bitbanana.fragments.OpenChannelBSDFragment;
import app.michaelwuensch.bitbanana.listViews.backendConfigs.ManageBackendConfigsActivity;
import app.michaelwuensch.bitbanana.listViews.channels.ManageChannelsActivity;
import app.michaelwuensch.bitbanana.listViews.contacts.ManageContactsActivity;
import app.michaelwuensch.bitbanana.listViews.contacts.ScanContactActivity;
import app.michaelwuensch.bitbanana.listViews.contacts.itemDetails.ContactDetailsActivity;
import app.michaelwuensch.bitbanana.listViews.forwardings.ForwardingActivity;
import app.michaelwuensch.bitbanana.listViews.peers.PeersActivity;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.TransactionHistoryFragment;
import app.michaelwuensch.bitbanana.listViews.utxos.UTXOsActivity;
import app.michaelwuensch.bitbanana.lnurl.auth.LnUrlAuthBSDFragment;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelBSDFragment;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayBSDFragment;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawBSDFragment;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.models.LNAddress;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.settings.SettingsActivity;
import app.michaelwuensch.bitbanana.signVerify.SignVerifyActivity;
import app.michaelwuensch.bitbanana.support.SupportActivity;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.BackendSwitcher;
import app.michaelwuensch.bitbanana.util.BitcoinStringAnalyzer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.ExchangeRateUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.NfcUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PinScreenUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.Version;
import app.michaelwuensch.bitbanana.util.Wallet;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class HomeActivity extends BaseAppCompatActivity implements LifecycleObserver,
        SharedPreferences.OnSharedPreferenceChangeListener,
        Wallet.InfoListener, Wallet.LndConnectionTestListener,
        Wallet.WalletLoadedListener, NavigationView.OnNavigationItemSelectedListener, FeatureManager.FeatureChangedListener {

    // Activity Result codes
    public static final int REQUEST_CODE_PAYMENT = 101;
    public static final int REQUEST_CODE_LNURL_WITHDRAW = 102;
    public static final int REQUEST_CODE_GENERIC_SCAN = 103;
    public static final int RESULT_CODE_PAYMENT = 201;
    public static final int RESULT_CODE_LNURL_WITHDRAW = 202;
    public static final int RESULT_CODE_GENERIC_SCAN = 203;

    private static final String LOG_TAG = HomeActivity.class.getSimpleName();
    private Handler mHandler;
    private InputMethodManager mInputMethodManager;
    private ScheduledExecutorService mExchangeRateScheduler;
    private ScheduledExecutorService mLNDInfoScheduler;
    private NetworkChangeReceiver mNetworkChangeReceiver;
    private boolean mIsExchangeRateSchedulerRunning = false;
    private boolean mIsLNDInfoSchedulerRunning = false;
    private boolean mIsNetworkChangeReceiverRunning = false;

    private boolean mInfoChangeListenerRegistered;
    private boolean mLndConnectionTestListenerRegistered;
    private boolean mWalletLoadedListener;
    private boolean mIsFirstUnlockAttempt = true;
    private AlertDialog mUnlockDialog;
    private NfcAdapter mNfcAdapter;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    public DrawerLayout mDrawer;
    private NavigationView mNavigationView;
    public CustomViewPager mViewPager;
    private HomePagerAdapter mPagerAdapter;
    private Handler mBackgroundCloseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        mInputMethodManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        mHandler = new Handler();


        // Setup navigation drawer menu
        mDrawer = findViewById(R.id.drawerLayout);
        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.getHeaderView(0).findViewById(R.id.headerButton).setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                    if (Wallet.getInstance().isConnectedToLND()) {
                        if (Wallet.getInstance().getNodeUris().length > 0) {
                            Intent intent = new Intent(HomeActivity.this, IdentityActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(HomeActivity.this, R.string.error_node_not_yet_ready, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(HomeActivity.this, R.string.demo_setupNodeFirstAvatar, Toast.LENGTH_LONG).show();
                }
            }
        });
        mDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                NavigationMenuView navigationMenuView = (NavigationMenuView) mNavigationView.getChildAt(0);
                // Blends in the scrollbar on menu open.
                navigationMenuView.setScrollBarDefaultDelayBeforeFade(1500);
                navigationMenuView.scrollBy(0, 0);
            }
        });
        TextView appVersion = findViewById(R.id.appVersion);
        String appVersionString = "BitBanana version:  " + BuildConfig.VERSION_NAME + ", build: " + BuildConfig.VERSION_CODE;
        appVersion.setText(appVersionString);
        TextView lndVersion = findViewById(R.id.lndVersion);
        String lndVersionString = "LND version: " + Wallet.getInstance().getLNDVersionString().split(" commit")[0];
        lndVersion.setText(lndVersionString);


        // Setup view pager
        mViewPager = findViewById(R.id.viewPager);
        mPagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        // Make navigation drawer menu open on a left swipe on the first page of the pager.
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int times = 0;
            private int times2 = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0 && positionOffset == 0 && positionOffsetPixels == 0) {
                    times++;
                    if (times >= 3) {
                        if (times2 < 3) {
                            mDrawer.openDrawer(GravityCompat.START);
                            mViewPager.setSwipeable(false);
                        }
                    }
                } else {
                    times2++;
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    // Close search on history fragment if we go to main fragment
                    if (mPagerAdapter != null && mPagerAdapter.getHistoryFragment() != null && mPagerAdapter.getHistoryFragment().getSearchView() != null) {
                        mPagerAdapter.getHistoryFragment().getSearchView().setQuery("", false);
                        mPagerAdapter.getHistoryFragment().getSearchView().setIconified(true); // close the search editor and show search icon again
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    times = 0;
                    times2 = 0;
                }
            }
        });


        mUnlockDialog = buildUnlockDialog();
        mBackgroundCloseHandler = new Handler();

        // Register observer to detect if app goes to background
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    // This schedule keeps us up to date on exchange rates
    private void setupExchangeRateSchedule() {

        if (!mIsExchangeRateSchedulerRunning) {
            mIsExchangeRateSchedulerRunning = true;

            mExchangeRateScheduler =
                    Executors.newSingleThreadScheduledExecutor();

            mExchangeRateScheduler.scheduleAtFixedRate
                    (new Runnable() {
                        public void run() {
                            ExchangeRateUtil.getInstance().getExchangeRates();
                        }
                    }, 10, RefConstants.EXCHANGE_RATE_PERIOD, RefConstants.EXCHANGE_RATE_PERIOD_UNIT);
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
                            if (BackendSwitcher.getBackendState() == BackendSwitcher.BackendState.BACKEND_CONNECTED) {
                                BBLog.v(LOG_TAG, "Scheduled LND info check initiated.");
                                Wallet.getInstance().fetchInfoFromLND();
                            }
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
        BBLog.d(LOG_TAG, "BitBanana moved to foreground");

        // Test if PIN screen should be shown.
        PinScreenUtil.askForAccess(this, () -> {
            continueMoveToForeground();
        });
    }

    private void continueMoveToForeground() {
        mBackgroundCloseHandler.removeCallbacksAndMessages(null);
        // start listeners and schedules
        setupExchangeRateSchedule();
        registerNetworkStatusChangeListener();

        if (!mLndConnectionTestListenerRegistered) {
            Wallet.getInstance().registerLndConnectionTestListener(this);
            mLndConnectionTestListenerRegistered = true;
        }

        if (!mWalletLoadedListener) {
            Wallet.getInstance().registerWalletLoadedListener(this);
        }

        if (!mInfoChangeListenerRegistered) {
            Wallet.getInstance().registerInfoListener(this);
            mInfoChangeListenerRegistered = true;
        }
        FeatureManager.registerFeatureChangedListener(this);

        PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);

        // We delay the actual opening of the wallet so that the wallet fragment has time to be loaded first
        new Handler().postDelayed(() -> openWallet(), 300);
    }

    public void openWallet() {
        BackendSwitcher.activateCurrentBackendConfig(HomeActivity.this, false);

        // ToDo: move to state events
        updateDrawerNavigationMenuVisibilities();

        // Check if BitBanana was started from an URI link or by NFC.
        if (App.getAppContext().getUriSchemeData() != null) {
            // Only check for connecting wallets. Other operations need a wallet fully loaded.
            if (UriUtil.isLNDConnectUri(App.getAppContext().getUriSchemeData())) {
                analyzeString(App.getAppContext().getUriSchemeData());
                App.getAppContext().setUriSchemeData(null);
            }
        }
    }

    // This function gets called when app is moved to background.
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onMoveToBackground() {
        BBLog.d(LOG_TAG, "BitBanana moved to background");

        // ToDo: check if this works here!
        if (TimeOutUtil.getInstance().getCanBeRestarted()) {
            TimeOutUtil.getInstance().restartTimer();
        }
        TimeOutUtil.getInstance().setCanBeRestarted(false);

        App.getAppContext().connectionToLNDEstablished = false;

        mBackgroundCloseHandler.postDelayed(() -> stopListenersAndSchedules(), RefConstants.DISCONNECT_TIMEOUT * 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BBLog.d(LOG_TAG, "Destroying HomeActivity.");
        stopListenersAndSchedules();
        // Remove observer to detect if app goes to background
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
    }

    private void stopListenersAndSchedules() {

        // Unregister Handler, Wallet Loaded & Info Listener
        Wallet.getInstance().unregisterLndConnectionTestListener(this);
        mLndConnectionTestListenerRegistered = false;
        Wallet.getInstance().unregisterWalletLoadedListener(this);
        mWalletLoadedListener = false;
        Wallet.getInstance().unregisterInfoListener(this);
        mInfoChangeListenerRegistered = false;
        FeatureManager.unregisterFeatureChangedListener(this);

        mHandler.removeCallbacksAndMessages(null);

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
        Wallet.getInstance().cancelSubscriptions();

        BackendSwitcher.deactivateCurrentBackendConfig(this, false, false);
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else if (mViewPager.getCurrentItem() == 1) {
            mViewPager.setCurrentItem(0);
        } else {
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

    }

    @Override
    public void onInfoUpdated(boolean connected) {

    }

    @Override
    public void onLndConnectError(int error) {
        if (error == Wallet.LndConnectionTestListener.ERROR_LOCKED) {

            if (mUnlockDialog != null && !mUnlockDialog.isShowing()) {
                mUnlockDialog.show();
            }

            mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            mPagerAdapter.getWalletFragment().showBackgroundForWalletUnlock();

            if (!mIsFirstUnlockAttempt) {
                Toast.makeText(HomeActivity.this, R.string.error_wrong_password, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onLndConnectError(String error) {

    }

    @Override
    public void onLndConnectSuccess() {
        // We managed to establish a connection to LND.
        // Now we can start to fetch all information needed from LND
        App.getAppContext().connectionToLNDEstablished = true;

        // Warn the user if an old LND version is used.
        if (Wallet.getInstance().getLNDVersion().compareTo(new Version("0.16.0")) < 0) {
            new UserGuardian(this).securityOldLndVersion("v0.16.0-beta");
        }

        // Fetch the transaction history
        Wallet.getInstance().fetchLNDTransactionHistory();
        Wallet.getInstance().fetchBalanceFromLND();
        Wallet.getInstance().fetchChannelsFromLND();

        // Fetch UTXOs
        Wallet.getInstance().fetchUTXOs();
        Wallet.getInstance().fetchLockedUTXOs();

        // Subscribe to Transaction Events
        Wallet.getInstance().subscribeToTransactions();
        Wallet.getInstance().subscribeToHtlcEvents();
        Wallet.getInstance().subscribeToInvoices();

        if (mHandler != null) {
            mHandler.postDelayed(() -> Wallet.getInstance().subscribeToChannelEvents(), 3000);
        }

        updateDrawerNavigationMenu();

        setupLNDInfoSchedule();
    }

    @Override
    public void onLndConnectionTestStarted() {

    }

    private AlertDialog buildUnlockDialog() {
        // Show unlock dialog
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.unlock_wallet);
        adb.setCancelable(false);
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_input_password, null, false);

        final EditText input = viewInflated.findViewById(R.id.input);
        input.setShowSoftInputOnFocus(true);
        input.requestFocus();

        adb.setView(viewInflated);

        adb.setPositiveButton(R.string.ok, (dialog, which) -> {
            mPagerAdapter.getWalletFragment().showLoading();
            Wallet.getInstance().unlockWallet(input.getText().toString());
            mInputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            mIsFirstUnlockAttempt = false;
            dialog.dismiss();
        });
        adb.setNegativeButton(R.string.cancel, (dialog, which) -> {
            InputMethodManager inputMethodManager = (InputMethodManager) HomeActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            mPagerAdapter.getWalletFragment().showErrorAfterNotUnlocked();
            mIsFirstUnlockAttempt = true;
            dialog.cancel();
        });

        return adb.create();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            // Update if primary currency has been switched from this or another activity
            if (key.equals(PrefsUtil.PREVENT_SCREEN_RECORDING)) {
                if (PrefsUtil.isScreenRecordingPrevented()) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            PendingIntent pendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(
                        this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(
                        this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            }
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, NfcUtil.IntentFilters(), null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NfcUtil.readTag(this, intent, new NfcUtil.OnNfcResponseListener() {
            @Override
            public void onSuccess(String payload) {
                if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                    analyzeString(payload);
                } else {
                    BBLog.d(LOG_TAG, "Wallet not setup.");
                    Toast.makeText(HomeActivity.this, R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void analyzeString(String input) {
        BitcoinStringAnalyzer.analyze(HomeActivity.this, compositeDisposable, input, new BitcoinStringAnalyzer.OnDataDecodedListener() {
            @Override
            public void onValidLightningInvoice(PayReq paymentRequest, String invoice) {
                if (paymentRequest.getNumSatoshis() == 0) {
                    // Warn about 0 sat invoices
                    new UserGuardian(HomeActivity.this, new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onGuardianConfirmed() {
                            SendBSDFragment sendBSDFragment = SendBSDFragment.createLightningDialog(paymentRequest, invoice, null);
                            sendBSDFragment.showDelayed(getSupportFragmentManager(), "sendBottomSheetDialog");
                        }
                    }).securityZeroAmountInvoice();
                } else {
                    SendBSDFragment sendBSDFragment = SendBSDFragment.createLightningDialog(paymentRequest, invoice, null);
                    sendBSDFragment.showDelayed(getSupportFragmentManager(), "sendBottomSheetDialog");
                }
            }

            @Override
            public void onValidBitcoinInvoice(String address, long amount, String message, String lightningInvoice) {
                if (lightningInvoice == null) {
                    SendBSDFragment sendBSDFragment = SendBSDFragment.createOnChainDialog(address, amount, message);
                    sendBSDFragment.show(getSupportFragmentManager(), "sendBottomSheetDialog");
                } else {
                    InvoiceUtil.readInvoice(HomeActivity.this, compositeDisposable, lightningInvoice, new InvoiceUtil.OnReadInvoiceCompletedListener() {
                        @Override
                        public void onValidLightningInvoice(PayReq paymentRequest, String invoice) {
                            if (Wallet.getInstance().getMaxLightningSendAmount() < paymentRequest.getNumSatoshis()) {
                                // Not enough funds available in channels to send this lightning payment. Fallback to onChain.
                                SendBSDFragment sendBSDFragment = SendBSDFragment.createOnChainDialog(address, amount, message);
                                sendBSDFragment.showDelayed(getSupportFragmentManager(), "sendBottomSheetDialog");
                                Toast.makeText(HomeActivity.this, R.string.on_chain_fallback_insufficient_funds, Toast.LENGTH_LONG).show();
                            } else {
                                if (paymentRequest.getNumSatoshis() == 0) {
                                    // Warn about 0 sat invoices
                                    new UserGuardian(HomeActivity.this, new UserGuardian.OnGuardianConfirmedListener() {
                                        @Override
                                        public void onGuardianConfirmed() {
                                            String amountString = MonetaryUtil.getInstance().satsToBitcoinString(amount);
                                            String onChainInvoice = InvoiceUtil.generateBitcoinInvoice(address, amountString, message, null);
                                            SendBSDFragment sendBSDFragment = SendBSDFragment.createLightningDialog(paymentRequest, invoice, onChainInvoice);
                                            sendBSDFragment.showDelayed(getSupportFragmentManager(), "sendBottomSheetDialog");
                                        }
                                    }).securityZeroAmountInvoice();
                                } else {
                                    String amountString = MonetaryUtil.getInstance().satsToBitcoinString(amount);
                                    String onChainInvoice = InvoiceUtil.generateBitcoinInvoice(address, amountString, message, null);
                                    SendBSDFragment sendBSDFragment = SendBSDFragment.createLightningDialog(paymentRequest, invoice, onChainInvoice);
                                    sendBSDFragment.showDelayed(getSupportFragmentManager(), "sendBottomSheetDialog");
                                }
                            }
                        }

                        @Override
                        public void onValidBitcoinInvoice(String address, long amount, String message, String lightningInvoice) {
                            // never reached
                        }

                        @Override
                        public void onError(String error, int duration, int errorCode) {
                            // If the added lightning parameter contains an invalid lightning invoice, we fall back to the onChain invoice.
                            BBLog.d(LOG_TAG, "Falling back to onChain Invoice: " + error);
                            SendBSDFragment sendBSDFragment = SendBSDFragment.createOnChainDialog(address, amount, message);
                            sendBSDFragment.showDelayed(getSupportFragmentManager(), "sendBottomSheetDialog");
                            if (errorCode == InvoiceUtil.ERROR_INVOICE_EXPIRED)
                                Toast.makeText(HomeActivity.this, R.string.on_chain_fallback_expired, Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(HomeActivity.this, R.string.on_chain_fallback_invalid_data, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onNoInvoiceData() {
                            // If the added lightning parameter contains an invalid lightning invoice, we fall back to the onChain invoice.
                            SendBSDFragment sendBSDFragment = SendBSDFragment.createOnChainDialog(address, amount, message);
                            sendBSDFragment.showDelayed(getSupportFragmentManager(), "sendBottomSheetDialog");
                            Toast.makeText(HomeActivity.this, R.string.on_chain_fallback_invalid_data, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse) {
                LnUrlWithdrawBSDFragment lnUrlWithdrawBSDFragment = LnUrlWithdrawBSDFragment.createWithdrawDialog(withdrawResponse);
                lnUrlWithdrawBSDFragment.show(getSupportFragmentManager(), "lnurlWithdrawBottomSheetDialog");
            }

            @Override
            public void onValidLnUrlChannel(LnUrlChannelResponse channelResponse) {
                LnUrlChannelBSDFragment lnUrlChannelBSDFragment = LnUrlChannelBSDFragment.createLnURLChannelDialog(channelResponse);
                lnUrlChannelBSDFragment.show(getSupportFragmentManager(), "lnurlChannelBottomSheetDialog");
            }

            @Override
            public void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse) {
                showError(getResources().getString(R.string.lnurl_unsupported_type), RefConstants.ERROR_DURATION_SHORT);
            }

            @Override
            public void onValidLnUrlPay(LnUrlPayResponse payResponse) {
                LnUrlPayBSDFragment lnUrlPayBSDFragment = LnUrlPayBSDFragment.createLnUrlPayDialog(payResponse);
                lnUrlPayBSDFragment.show(getSupportFragmentManager(), "lnurlPayBottomSheetDialog");
            }

            @Override
            public void onValidLnUrlAuth(URL url) {
                LnUrlAuthBSDFragment lnUrlAuthBSDFragment = LnUrlAuthBSDFragment.createLnUrlAuthDialog(url);
                lnUrlAuthBSDFragment.show(getSupportFragmentManager(), "lnurlAuthBottomSheetDialog");
            }

            @Override
            public void onValidInternetIdentifier(LnUrlPayResponse payResponse) {
                LnUrlPayBSDFragment lnUrlPayBSDFragment = LnUrlPayBSDFragment.createLnUrlPayDialog(payResponse);
                lnUrlPayBSDFragment.show(getSupportFragmentManager(), "lnurlPayBottomSheetDialog");
            }

            @Override
            public void onValidLndConnectString(BaseBackendConfig baseBackendConfig) {
                addWallet(baseBackendConfig);
            }

            @Override
            public void onValidLndHubConnectString(BaseBackendConfig baseBackendConfig) {
                addWallet(baseBackendConfig);
            }

            @Override
            public void onValidBTCPayConnectData(BaseBackendConfig baseBackendConfig) {
                addWallet(baseBackendConfig);
            }

            @Override
            public void onValidNodeUri(LightningNodeUri nodeUri) {
                ChooseNodeActionBSDFragment chooseNodeActionBSDFragment = ChooseNodeActionBSDFragment.createChooseActionDialog(nodeUri);
                chooseNodeActionBSDFragment.show(getSupportFragmentManager(), "choseNodeActionDialog");
            }

            @Override
            public void onValidURL(String urlAsString) {
                URL url = null;
                try {
                    url = new URL(urlAsString);
                } catch (MalformedURLException e) {
                    // never reached
                }
                String dialogMessage = getString(R.string.dialog_open_url, url.getProtocol() + "://" + url.getHost() + "/ ...");
                new AlertDialog.Builder(HomeActivity.this)
                        .setMessage(dialogMessage)
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlAsString));
                            startActivity(browserIntent);
                        }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
                        }).show();
            }

            @Override
            public void onError(String error, int duration) {
                showError(error, duration);
            }

            @Override
            public void onNoReadableData() {
                showError(getString(R.string.string_analyzer_unrecognized_data), RefConstants.ERROR_DURATION_SHORT);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {
            case HomeActivity.RESULT_CODE_GENERIC_SCAN:
                // This gets executed if readable data was found using the generic scanner
                if (data != null) {
                    analyzeString(data.getExtras().getString(ScanActivity.EXTRA_GENERIC_SCAN_DATA));
                }
                break;
            case ContactDetailsActivity.RESPONSE_CODE_OPEN_CHANNEL:
                if (data != null) {
                    LightningNodeUri nodeUri = (LightningNodeUri) data.getSerializableExtra(ScanContactActivity.EXTRA_NODE_URI);
                    OpenChannelBSDFragment openChannelBSDFragment = new OpenChannelBSDFragment();
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(OpenChannelBSDFragment.ARGS_NODE_URI, nodeUri);
                    openChannelBSDFragment.setArguments(bundle);
                    openChannelBSDFragment.show(getSupportFragmentManager(), OpenChannelBSDFragment.TAG);
                    if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                        mDrawer.closeDrawer(GravityCompat.START);
                    }
                }
                break;
            case ContactDetailsActivity.RESPONSE_CODE_SEND_MONEY:
                if (data != null) {
                    LightningNodeUri nodeUri = (LightningNodeUri) data.getSerializableExtra(ScanContactActivity.EXTRA_NODE_URI);
                    if (nodeUri != null) {
                        SendBSDFragment sendBSDFragment = SendBSDFragment.createKeysendDialog(nodeUri.getPubKey());
                        sendBSDFragment.show(getSupportFragmentManager(), "sendBottomSheetDialog");
                    } else {
                        LNAddress lnAddress = (LNAddress) data.getSerializableExtra(ScanContactActivity.EXTRA_LN_ADDRESS);
                        analyzeString(lnAddress.toString());
                    }
                    if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                        mDrawer.closeDrawer(GravityCompat.START);
                    }
                }
                break;
        }
    }

    private void addWallet(BaseBackendConfig baseBackendConfig) {
        new UserGuardian(HomeActivity.this, () -> {
            RemoteConnectUtil.saveRemoteConfiguration(HomeActivity.this, baseBackendConfig, null, new RemoteConnectUtil.OnSaveRemoteConfigurationListener() {

                @Override
                public void onSaved(String id) {
                    if (BackendConfigsManager.getInstance().getAllBackendConfigs(false).size() == 1) {
                        // This was the first wallet that was added. Open it immediately.
                        mPagerAdapter.getWalletFragment().showLoading();
                        openWallet();
                    } else {
                        new AlertDialog.Builder(HomeActivity.this)
                                .setMessage(R.string.node_added)
                                .setCancelable(true)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                }).show();
                    }
                }

                @Override
                public void onError(String error, int duration) {
                    showError(error, duration);
                }
            });
        }).securityConnectToRemoteServer(baseBackendConfig.getHost());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.drawerChannels) {
            Intent intentChannels = new Intent(this, ManageChannelsActivity.class);
            startActivity(intentChannels);
        } else if (id == R.id.drawerRouting) {
            Intent intentRouting = new Intent(this, ForwardingActivity.class);
            startActivity(intentRouting);
        } else if (id == R.id.drawerUTXOs) {
            Intent intentUTXOs = new Intent(this, UTXOsActivity.class);
            startActivity(intentUTXOs);
        } else if (id == R.id.drawerPeers) {
            Intent intentPeers = new Intent(this, PeersActivity.class);
            startActivityForResult(intentPeers, 0);
        } else if (id == R.id.drawerSignVerify) {
            Intent intentSignVerify = new Intent(this, SignVerifyActivity.class);
            startActivity(intentSignVerify);
        } else if (id == R.id.drawerSettings) {
            Intent intentSettings = new Intent(this, SettingsActivity.class);
            startActivity(intentSettings);
        } else if (id == R.id.drawerNodes) {
            Intent intentWallets = new Intent(this, ManageBackendConfigsActivity.class);
            startActivity(intentWallets);
        } else if (id == R.id.drawerContacts) {
            Intent intentContacts = new Intent(this, ManageContactsActivity.class);
            intentContacts.putExtra(ManageContactsActivity.EXTRA_CONTACT_ACTIVITY_MODE, ManageContactsActivity.MODE_MANAGE);
            startActivityForResult(intentContacts, 0);
        } else if (id == R.id.drawerBackup) {
            Intent intentBackup = new Intent(this, BackupActivity.class);
            startActivity(intentBackup);
        } else if (id == R.id.drawerSupport) {
            Intent intentSupport = new Intent(this, SupportActivity.class);
            startActivity(intentSupport);
        }
        return true;
    }

    public void updateDrawerNavigationMenu() {
        UserAvatarView userAvatarView = mNavigationView.getHeaderView(0).findViewById(R.id.userAvatarView);
        userAvatarView.setupWithNodeUri(Wallet.getInstance().getNodeUris()[0], false);
        TextView userWalletName = mNavigationView.getHeaderView(0).findViewById(R.id.userWalletName);
        userWalletName.setText(BackendConfigsManager.getInstance().getCurrentBackendConfig().getAlias());
        TextView lndVersion = findViewById(R.id.lndVersion);
        String lndVersionString = "lnd version: " + Wallet.getInstance().getLNDVersionString().split(" commit")[0];
        lndVersion.setText(lndVersionString);
        updateDrawerNavigationMenuVisibilities();
    }

    public void resetDrawerNavigationMenu() {
        UserAvatarView userAvatarView = mNavigationView.getHeaderView(0).findViewById(R.id.userAvatarView);
        userAvatarView.reset();
        TextView userWalletName = mNavigationView.getHeaderView(0).findViewById(R.id.userWalletName);
        userWalletName.setText(getResources().getString(R.string.notConnected));
        TextView lndVersion = findViewById(R.id.lndVersion);
        String lndVersionString = "lnd version: " + Wallet.getInstance().getLNDVersionString().split(" commit")[0];
        lndVersion.setText(lndVersionString);
        updateDrawerNavigationMenuVisibilities();
    }

    public void updateDrawerNavigationMenuVisibilities() {
        Menu drawerMenu = mNavigationView.getMenu();
        drawerMenu.findItem(R.id.drawerChannels).setVisible(FeatureManager.isChannelManagementEnabled());
        drawerMenu.findItem(R.id.drawerRouting).setVisible(FeatureManager.isRoutingListViewEnabled());
        drawerMenu.findItem(R.id.drawerUTXOs).setVisible(FeatureManager.isUTXOListViewEnabled());
        drawerMenu.findItem(R.id.drawerPeers).setVisible(FeatureManager.isPeersListViewEnabled());
        drawerMenu.findItem(R.id.drawerSignVerify).setVisible(FeatureManager.isSignVerifyEnabled());
        drawerMenu.findItem(R.id.drawerNodeSection).setVisible(FeatureManager.isChannelManagementEnabled() || FeatureManager.isUTXOListViewEnabled() || FeatureManager.isRoutingListViewEnabled() || FeatureManager.isSignVerifyEnabled());
        drawerMenu.findItem(R.id.drawerContacts).setVisible(FeatureManager.isContactsEnabled());
    }

    public TransactionHistoryFragment getHistoryFragment() {
        return mPagerAdapter.getHistoryFragment();
    }

    @Override
    public void onWalletLoaded() {
        BBLog.d(LOG_TAG, "Wallet loaded");

        // Check if BitBanana was started from an URI link or by NFC.
        if (App.getAppContext().getUriSchemeData() != null) {
            analyzeString(App.getAppContext().getUriSchemeData());
            App.getAppContext().setUriSchemeData(null);
        } else {
            // Do we have any clipboard data that BitBanana can read?
            ClipBoardUtil.performClipboardScan(HomeActivity.this, compositeDisposable, new ClipBoardUtil.OnClipboardScanProceedListener() {
                @Override
                public void onProceed(String content) {
                    analyzeString(content);
                }

                @Override
                public void onError(String error, int duration) {
                    showError(error, duration);
                }
            });
        }
    }

    @Override
    public void onFeatureChanged() {
        updateDrawerNavigationMenuVisibilities();
    }


    private class HomePagerAdapter extends FragmentPagerAdapter {
        private WalletFragment mWalletFragment;
        private TransactionHistoryFragment mHistoryFragment;

        public HomePagerAdapter(FragmentManager fm) {
            super(fm);
            mWalletFragment = new WalletFragment();
            mHistoryFragment = new TransactionHistoryFragment();
        }

        @Override
        public Fragment getItem(int pos) {
            switch (pos) {

                case 0:
                    return mWalletFragment;
                case 1:
                    return mHistoryFragment;
                default:
                    return mWalletFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        public WalletFragment getWalletFragment() {
            return mWalletFragment;
        }

        public TransactionHistoryFragment getHistoryFragment() {
            return mHistoryFragment;
        }
    }
}
