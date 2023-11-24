package app.michaelwuensch.bitbanana.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import app.michaelwuensch.bitbanana.HomeActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.ScanActivity;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.internetConnectionStatus.NetworkUtil;
import app.michaelwuensch.bitbanana.connection.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.NodeConfigsManager;
import app.michaelwuensch.bitbanana.contacts.ManageContactsActivity;
import app.michaelwuensch.bitbanana.customView.MainBalanceView;
import app.michaelwuensch.bitbanana.customView.NodeSpinner;
import app.michaelwuensch.bitbanana.setup.SetupActivity;
import app.michaelwuensch.bitbanana.tor.TorManager;
import app.michaelwuensch.bitbanana.util.ExchangeRateUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.Wallet;


/**
 * A simple {@link Fragment} subclass.
 */
public class WalletFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener,
        Wallet.BalanceListener, Wallet.InfoListener, Wallet.LndConnectionTestListener,
        Wallet.WalletLoadedListener, ExchangeRateUtil.ExchangeRateListener, TorManager.TorErrorListener {

    private static final String LOG_TAG = WalletFragment.class.getSimpleName();

    private MainBalanceView mMainBalanceView;
    private ConstraintLayout mWalletConnectedLayout;
    private ConstraintLayout mWalletNotConnectedLayout;
    private ConstraintLayout mLoadingWalletLayout;
    private TextView mTvConnectError;
    private NodeSpinner mNodeSpinner;
    private ImageView mDrawerMenuButton;
    private TextView mWalletNameWidthDummy;
    private ImageView mStatusDot;
    private Button mBtnSetup;

    private boolean mPreferenceChangeListenerRegistered = false;
    private boolean mBalanceChangeListenerRegistered = false;
    private boolean mInfoChangeListenerRegistered = false;
    private boolean mExchangeRateListenerRegistered = false;
    private boolean mLndConnectionTestListenerRegistered = false;
    private boolean mWalletLoadedListenerRegistered = false;
    private boolean mTorErrorListenerRegistred = false;

    public WalletFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        // Get View elements
        mMainBalanceView = view.findViewById(R.id.mainBalanceView);
        mWalletConnectedLayout = view.findViewById(R.id.walletConnected);
        mWalletNotConnectedLayout = view.findViewById(R.id.ConnectionError);
        mLoadingWalletLayout = view.findViewById(R.id.loading);
        mTvConnectError = view.findViewById(R.id.connectError);
        mStatusDot = view.findViewById(R.id.statusDot);
        mNodeSpinner = view.findViewById(R.id.walletSpinner);
        mDrawerMenuButton = view.findViewById(R.id.drawerMenuButton);
        mWalletNameWidthDummy = view.findViewById(R.id.walletNameWidthDummy);
        mBtnSetup = view.findViewById(R.id.setupWallet);

        // Show loading screen
        showLoading();

        mNodeSpinner.setOnNodeSpinnerChangedListener(new NodeSpinner.OnNodeSpinnerChangedListener() {
            @Override
            public void onNodeChanged(String id, String alias) {
                // Close current connection and reset all
                LndConnection.getInstance().closeConnection();
                Wallet.getInstance().reset();
                updateTotalBalanceDisplay();

                // Save the new chosen node in prefs
                PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_NODE_CONFIG, id).commit();

                // Update status dot
                updateStatusDot(alias);

                // Show loading screen
                showLoading();

                // Clear history list
                ((HomeActivity) getActivity()).getHistoryFragment().updateHistoryDisplayList();

                // Reset drawer menu
                ((HomeActivity) getActivity()).resetDrawerNavigationMenu();

                // Open the newly selected node
                ((HomeActivity) getActivity()).openWallet();
            }
        });


        // Hide balance if the setting was chosen
        if (!PrefsUtil.getPrefs().getString(PrefsUtil.BALANCE_HIDE_TYPE, "off").equals("off")) {
            hideBalance();
        }

        // Action when clicked on menu button
        mDrawerMenuButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                ((HomeActivity) getActivity()).mDrawer.openDrawer(GravityCompat.START);
            }
        });

        // Action when clicked on "History Button"
        ImageView historyButton = view.findViewById(R.id.historyButton);
        historyButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                ((HomeActivity) getActivity()).mViewPager.setCurrentItem(1);
            }
        });


        // Action when clicked on "scan"
        View btnScan = view.findViewById(R.id.scanButton);
        btnScan.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(getActivity(), ScanActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_GENERIC_SCAN);
            }
        });

        // Action when clicked on "send"
        Button btnSend = view.findViewById(R.id.sendButton);
        btnSend.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(getActivity(), ManageContactsActivity.class);
                intent.putExtra(ManageContactsActivity.EXTRA_CONTACT_ACTIVITY_MODE, ManageContactsActivity.MODE_SEND);
                startActivityForResult(intent, 0);
            }
        });


        // Action when clicked on "receive"
        Button btnReceive = view.findViewById(R.id.receiveButton);
        btnReceive.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                ReceiveBSDFragment receiveBottomSheetDialog = new ReceiveBSDFragment();
                receiveBottomSheetDialog.show(getParentFragmentManager(), "receiveBottomSheetDialog");
            }
        });

        // Action when clicked on "setup wallet"
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
            mBtnSetup.setVisibility(View.INVISIBLE);
        }
        mBtnSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SetupActivity.class);
                intent.putExtra(RefConstants.SETUP_MODE, SetupActivity.FULL_SETUP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });


        // Action when clicked on "retry"
        Button btnReconnect = view.findViewById(R.id.reconnectBtn);
        btnReconnect.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                showLoading();
                updateStatusDot(NodeConfigsManager.getInstance().getCurrentNodeConfig().getAlias());
                // We delay the execution, to make it obvious to the user the button press had an effect.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (PrefsUtil.isTorEnabled()) {
                            if (TorManager.getInstance().isProxyRunning()) {
                                LndConnection.getInstance().reconnect();
                            } else {
                                TorManager.getInstance().restartTor();
                            }
                        } else {
                            // Start lnd connection
                            Wallet.getInstance().checkIfLndIsUnlockedAndConnect();
                        }
                    }
                }, 200);
            }
        });


        updateTotalBalanceDisplay();


        if (App.getAppContext().connectionToLNDEstablished) {
            walletLoadingCompleted();
        }

        return view;
    }

    private void walletLoadingCompleted() {

        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {

            // Show info about mode (offline, connected, error, testnet or mainnet, ...)
            onInfoUpdated(Wallet.getInstance().isInfoFetched());
        }
    }

    private void updateTotalBalanceDisplay() {
        mMainBalanceView.updateBalances();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            // Update if primary currency has been switched from this or another activity
            if (key.equals("firstCurrencyIsPrimary")) {
                updateTotalBalanceDisplay();
            }
            if (key.equals(PrefsUtil.BALANCE_HIDE_TYPE)) {
                if (PrefsUtil.getPrefs().getString(PrefsUtil.BALANCE_HIDE_TYPE, "off").equals("off")) {
                    showBalance();
                } else {
                    hideBalance();
                }
            }
            if (key.equals(PrefsUtil.CURRENT_NODE_CONFIG)) {
                updateSpinnerVisibility();
            }
        }
    }

    @Override
    public void onExchangeRatesUpdated() {
        updateTotalBalanceDisplay();
    }

    @Override
    public void onExchangeRateUpdateFailed(int error, int duration) {
        if (error == ExchangeRateUtil.ExchangeRateListener.ERROR_CLOUDFLARE_BLOCKED_TOR) {
            showError(getString(R.string.error_tor_exchange_rate), duration);
        }
    }

    @Override
    public void onBalanceUpdated() {
        updateTotalBalanceDisplay();
    }

    @Override
    public void onInfoUpdated(boolean connected) {
        if (connected) {
            mWalletConnectedLayout.setVisibility(View.VISIBLE);
            mLoadingWalletLayout.setVisibility(View.GONE);
            mWalletNotConnectedLayout.setVisibility(View.GONE);
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.green)));
            mMainBalanceView.updateNetworkInfo();
        } else {
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.red)));
            mWalletConnectedLayout.setVisibility(View.GONE);
            mLoadingWalletLayout.setVisibility(View.GONE);
            mWalletNotConnectedLayout.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (PrefsUtil.isTorEnabled() && !TorManager.getInstance().isProxyRunning()) {
            showLoading();
        }

        // Update status dot
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
            mStatusDot.setVisibility(View.VISIBLE);
            updateStatusDot(NodeConfigsManager.getInstance().getCurrentNodeConfig().getAlias());
        } else {
            mStatusDot.setVisibility(View.GONE);
        }

        // Register listeners
        if (!mPreferenceChangeListenerRegistered) {
            PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);
            mPreferenceChangeListenerRegistered = true;
        }
        if (!mBalanceChangeListenerRegistered) {
            Wallet.getInstance().registerBalanceListener(this);
            mBalanceChangeListenerRegistered = true;
        }
        if (!mInfoChangeListenerRegistered) {
            Wallet.getInstance().registerInfoListener(this);
            mInfoChangeListenerRegistered = true;
        }
        if (!mLndConnectionTestListenerRegistered) {
            Wallet.getInstance().registerLndConnectionTestListener(this);
            mLndConnectionTestListenerRegistered = true;
        }
        if (!mExchangeRateListenerRegistered) {
            ExchangeRateUtil.getInstance().registerExchangeRateListener(this);
            mExchangeRateListenerRegistered = true;
        }
        if (!mWalletLoadedListenerRegistered) {
            Wallet.getInstance().registerWalletLoadedListener(this);
            mWalletLoadedListenerRegistered = true;
        }
        if (!mTorErrorListenerRegistred) {
            TorManager.getInstance().registerTorErrorListener(this);
            mTorErrorListenerRegistred = true;
        }

        updateSpinnerVisibility();

        if (!NodeConfigsManager.getInstance().hasAnyConfigs()) {
            // If the App is not setup yet,
            // this will cause to get the status text updated. Otherwise it would be empty.
            Wallet.getInstance().simulateFetchInfoForDemo();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister listeners
        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
        Wallet.getInstance().unregisterBalanceListener(this);
        Wallet.getInstance().unregisterInfoListener(this);
        Wallet.getInstance().unregisterLndConnectionTestListener(this);
        ExchangeRateUtil.getInstance().unregisterExchangeRateListener(this);
        Wallet.getInstance().unregisterWalletLoadedListener(this);
        TorManager.getInstance().unregisterTorErrorListener(this);
    }

    public void updateSpinnerVisibility() {
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
            mNodeSpinner.updateList();
            mNodeSpinner.setVisibility(View.VISIBLE);
        } else {
            mNodeSpinner.setVisibility(View.GONE);
        }
    }

    public void showErrorAfterNotUnlocked() {
        mWalletConnectedLayout.setVisibility(View.GONE);
        mWalletNotConnectedLayout.setVisibility(View.VISIBLE);
        mLoadingWalletLayout.setVisibility(View.GONE);

        mTvConnectError.setText(R.string.error_connection_wallet_locked);
    }

    public void showBackgroundForWalletUnlock() {
        mWalletConnectedLayout.setVisibility(View.GONE);
        mWalletNotConnectedLayout.setVisibility(View.GONE);
        mLoadingWalletLayout.setVisibility(View.GONE);
    }

    public void showLoading() {
        mWalletConnectedLayout.setVisibility(View.GONE);
        mWalletNotConnectedLayout.setVisibility(View.GONE);
        mLoadingWalletLayout.setVisibility(View.VISIBLE);
        mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.banana_yellow)));
    }

    private void hideBalance() {
        mMainBalanceView.hideBalance();
    }

    private void showBalance() {
        mMainBalanceView.showBalance();
    }

    private void updateStatusDot(String walletAlias) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (NodeConfigsManager.getInstance().getCurrentNodeConfig().getUseTor()) {
            mStatusDot.setImageResource(R.drawable.tor_icon);
            mStatusDot.getLayoutParams().height = (int) metrics.scaledDensity * 20;
            mStatusDot.getLayoutParams().width = (int) metrics.scaledDensity * 20;
        } else {
            mStatusDot.setImageResource(R.drawable.ic_status_dot_black_24dp);
            mStatusDot.getLayoutParams().height = (int) metrics.scaledDensity * 8;
            mStatusDot.getLayoutParams().width = (int) metrics.scaledDensity * 8;
        }
        mStatusDot.requestLayout();

        mWalletNameWidthDummy.setText(walletAlias);
        if (NetworkUtil.getConnectivityStatus(getActivity()) == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.red)));
        } else {
            if (Wallet.getInstance().isConnectedToLND()) {
                mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.green)));
            }
        }
    }

    private void showError(String message, int duration) {
        if (((HomeActivity) getActivity()) != null) {
            ((HomeActivity) getActivity()).showError(message, duration);
        }
    }

    @Override
    public void onLndConnectError(int error) {
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
            if (error != Wallet.LndConnectionTestListener.ERROR_LOCKED) {
                onInfoUpdated(false);
                if (error == Wallet.LndConnectionTestListener.ERROR_AUTHENTICATION) {
                    mTvConnectError.setText(R.string.error_connection_invalid_macaroon2);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_TIMEOUT) {
                    mTvConnectError.setText(getResources().getString(R.string.error_connection_server_unreachable, LndConnection.getInstance().getConnectionConfig().getHost()));
                } else if (error == Wallet.LndConnectionTestListener.ERROR_UNAVAILABLE) {
                    mTvConnectError.setText(getResources().getString(R.string.error_connection_lnd_unavailable, String.valueOf(LndConnection.getInstance().getConnectionConfig().getPort())));
                } else if (error == Wallet.LndConnectionTestListener.ERROR_TOR) {
                    mTvConnectError.setText(R.string.error_connection_tor_unreachable);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_HOST_VERIFICATION) {
                    mTvConnectError.setText(R.string.error_connection_host_verification_failed);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_HOST_UNRESOLVABLE) {
                    mTvConnectError.setText(getString(R.string.error_connection_host_unresolvable, LndConnection.getInstance().getConnectionConfig().getHost()));
                } else if (error == Wallet.LndConnectionTestListener.ERROR_NETWORK_UNREACHABLE) {
                    mTvConnectError.setText(R.string.error_connection_network_unreachable);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_CERTIFICATE_NOT_TRUSTED) {
                    mTvConnectError.setText(R.string.error_connection_invalid_certificate);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_INTERNAL) {
                    mTvConnectError.setText(R.string.error_connection_internal_server);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_INTERNAL_CLEARNET) {
                    mTvConnectError.setText(R.string.error_connection_internal_server_clearnet);
                }
            }
        } else {
            onInfoUpdated(true);
        }
    }

    @Override
    public void onLndConnectError(String error) {
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
            onInfoUpdated(false);
            String errorMessage;
            if (error.toLowerCase().contains("shutdown") && error.toLowerCase().contains("channel")) {
                // We explicitly filter this error message out, as it is confusing for users.
                // They might think lightning channels are getting closed, while in reality the message is about the grpc channel
                errorMessage = getString(R.string.error_lnd_connection_failed);
            } else {
                errorMessage = getString(R.string.error_connection_unknown) + "\n\n" + error;
            }
            mTvConnectError.setText(errorMessage);
        }
    }

    @Override
    public void onLndConnectSuccess() {
    }

    @Override
    public void onLndConnectionTestStarted() {
        showLoading();
    }

    @Override
    public void onWalletLoaded() {
        walletLoadingCompleted();
        mNodeSpinner.updateList();
        mNodeSpinner.setVisibility(View.VISIBLE);
        mStatusDot.setVisibility(View.VISIBLE);
        updateStatusDot(NodeConfigsManager.getInstance().getCurrentNodeConfig().getAlias());
        mBtnSetup.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onTorBootstrappingFailed() {
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
            onInfoUpdated(false);
            mTvConnectError.setText(R.string.error_connection_tor_bootstrapping);
        } else {
            onInfoUpdated(true);
        }
    }
}
