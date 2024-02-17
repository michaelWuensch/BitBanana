package app.michaelwuensch.bitbanana.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
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

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.internetConnectionStatus.NetworkUtil;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.connection.vpn.VPNUtil;
import app.michaelwuensch.bitbanana.customView.MainBalanceView;
import app.michaelwuensch.bitbanana.customView.NodeSpinner;
import app.michaelwuensch.bitbanana.listViews.contacts.ManageContactsActivity;
import app.michaelwuensch.bitbanana.setup.SetupActivity;
import app.michaelwuensch.bitbanana.util.BackendSwitcher;
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
        Wallet.WalletLoadedListener, ExchangeRateUtil.ExchangeRateListener, TorManager.TorErrorListener, BackendSwitcher.BackendStateChangedListener {

    private static final String LOG_TAG = WalletFragment.class.getSimpleName();

    private MainBalanceView mMainBalanceView;
    private ConstraintLayout mWalletConnectedLayout;
    private ConstraintLayout mWalletNotConnectedLayout;
    private ConstraintLayout mLoadingWalletLayout;
    private TextView mTvConnectError;
    private TextView mTvLoadingText;
    private NodeSpinner mNodeSpinner;
    private ImageView mDrawerMenuButton;
    private TextView mWalletNameWidthDummy;
    private ImageView mStatusDot;
    private Button mBtnSetup;
    private Button mBtnVpnSettings;

    private boolean mPreferenceChangeListenerRegistered = false;
    private boolean mBalanceChangeListenerRegistered = false;
    private boolean mInfoChangeListenerRegistered = false;
    private boolean mExchangeRateListenerRegistered = false;
    private boolean mLndConnectionTestListenerRegistered = false;
    private boolean mWalletLoadedListenerRegistered = false;
    private boolean mTorErrorListenerRegistered = false;
    private boolean mBackendStateListenerRegistered = false;

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
        mTvLoadingText = view.findViewById(R.id.loadingText);
        mStatusDot = view.findViewById(R.id.statusDot);
        mNodeSpinner = view.findViewById(R.id.walletSpinner);
        mDrawerMenuButton = view.findViewById(R.id.drawerMenuButton);
        mWalletNameWidthDummy = view.findViewById(R.id.walletNameWidthDummy);
        mBtnSetup = view.findViewById(R.id.setupWallet);
        mBtnVpnSettings = view.findViewById(R.id.vpnSettingsButton);

        // Show loading screen
        showLoading();

        mNodeSpinner.setOnNodeSpinnerChangedListener(new NodeSpinner.OnNodeSpinnerChangedListener() {
            @Override
            public void onNodeChanged(String id) {
                BackendSwitcher.activateBackendConfig(BackendConfigsManager.getInstance().getBackendConfigById(id), getActivity(), false);
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
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
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

        mBtnVpnSettings.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                VPNUtil.openVpnAppSettings(BackendSwitcher.getCurrentBackendConfig().getVpnConfig(), getContext());
            }
        });


        // Action when clicked on "retry"
        Button btnReconnect = view.findViewById(R.id.reconnectBtn);
        btnReconnect.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                mTvLoadingText.setText("");
                showLoading();
                updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig().getAlias());
                BackendSwitcher.activateCurrentBackendConfig(getActivity(), true);
            }
        });


        updateTotalBalanceDisplay();


        if (App.getAppContext().connectionToLNDEstablished) {
            walletLoadingCompleted();
        }

        return view;
    }

    private void walletLoadingCompleted() {

        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {

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
            if (key.equals(PrefsUtil.CURRENT_BACKEND_CONFIG)) {
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
            mBtnVpnSettings.setVisibility(View.GONE);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (PrefsUtil.isTorEnabled() && !TorManager.getInstance().isProxyRunning()) {
            TorManager.getInstance().startTor();
        }

        // Update status dot
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            mStatusDot.setVisibility(View.VISIBLE);
            updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig().getAlias());
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
        if (!mTorErrorListenerRegistered) {
            TorManager.getInstance().registerTorErrorListener(this);
            mTorErrorListenerRegistered = true;
        }
        if (!mBackendStateListenerRegistered) {
            BackendSwitcher.registerBackendStateChangedListener(this);
            mBackendStateListenerRegistered = true;
        }

        updateSpinnerVisibility();

        if (!BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
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
        BackendSwitcher.unregisterBackendStateChangedListener(this);
    }

    public void updateSpinnerVisibility() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
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
        mBtnVpnSettings.setVisibility(View.GONE);

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
        if (BackendConfigsManager.getInstance().getCurrentBackendConfig().getUseTor() && TorManager.getInstance().isProxyRunning()) {
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
        mBtnVpnSettings.setVisibility(View.GONE);
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            if (error != Wallet.LndConnectionTestListener.ERROR_LOCKED) {
                onInfoUpdated(false);
                if (error == Wallet.LndConnectionTestListener.ERROR_AUTHENTICATION) {
                    mTvConnectError.setText(R.string.error_connection_invalid_macaroon2);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_TIMEOUT) {
                    mTvConnectError.setText(getResources().getString(R.string.error_connection_server_unreachable, BackendSwitcher.getCurrentBackendConfig().getHost()));
                } else if (error == Wallet.LndConnectionTestListener.ERROR_UNAVAILABLE) {
                    mTvConnectError.setText(getResources().getString(R.string.error_connection_lnd_unavailable, String.valueOf(BackendSwitcher.getCurrentBackendConfig().getPort())));
                } else if (error == Wallet.LndConnectionTestListener.ERROR_TOR) {
                    mTvConnectError.setText(R.string.error_connection_tor_unreachable);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_HOST_VERIFICATION) {
                    mTvConnectError.setText(R.string.error_connection_host_verification_failed);
                } else if (error == Wallet.LndConnectionTestListener.ERROR_HOST_UNRESOLVABLE) {
                    mTvConnectError.setText(getString(R.string.error_connection_host_unresolvable, BackendSwitcher.getCurrentBackendConfig().getHost()));
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
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
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
            mBtnVpnSettings.setVisibility(View.GONE);
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
        updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig().getAlias());
        mBtnSetup.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onTorBootstrappingFailed() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            onInfoUpdated(false);
            mTvConnectError.setText(R.string.error_connection_tor_bootstrapping);
            mBtnVpnSettings.setVisibility(View.GONE);
        } else {
            onInfoUpdated(true);
        }
    }

    @Override
    public void onBackendStateChanged(BackendSwitcher.BackendState backendState) {
        switch (backendState) {
            case DISCONNECTING:
                mTvLoadingText.setText(R.string.connection_state_disconnecting);
                if (BackendConfigsManager.getInstance().getBackendConfigById(mNodeSpinner.getSelectedNodeId()) != null)
                    updateStatusDot(BackendConfigsManager.getInstance().getBackendConfigById(mNodeSpinner.getSelectedNodeId()).getAlias());
                // Show loading screen
                showLoading();
                break;
            case NO_BACKEND_SELECTED:
                updateTotalBalanceDisplay();
                // Clear history list
                ((HomeActivity) getActivity()).getHistoryFragment().updateHistoryDisplayList();
                break;
            case ACTIVATING_BACKEND:
                updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig().getAlias());
                break;
            case STARTING_VPN:
                mTvLoadingText.setText(R.string.connection_state_starting_vpn);
                break;
            case STARTING_TOR:
                mTvLoadingText.setText(R.string.connection_state_starting_tor);
                break;
            case TOR_CONNECTED:
                updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig().getAlias());
                break;
            case CONNECTING_TO_BACKEND:
                mTvLoadingText.setText(R.string.connection_state_connecting);
                break;
            case BACKEND_CONNECTED:
                break;
            case ERROR:
                break;
        }
    }

    @Override
    public void onBackendStateError(String message, int errorCode) {
        onInfoUpdated(false);
        mTvConnectError.setText(message);

        switch (errorCode) {
            case BackendSwitcher.ERROR_VPN_UNKNOWN_START_ISSUE:
                mBtnVpnSettings.setVisibility(View.VISIBLE);
                break;
            default:
                mBtnVpnSettings.setVisibility(View.GONE);
        }
    }
}
