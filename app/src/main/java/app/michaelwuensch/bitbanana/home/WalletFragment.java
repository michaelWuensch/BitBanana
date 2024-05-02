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
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.connection.internetConnectionStatus.NetworkUtil;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.connection.vpn.VPNUtil;
import app.michaelwuensch.bitbanana.customView.MainBalanceView;
import app.michaelwuensch.bitbanana.customView.NodeSpinner;
import app.michaelwuensch.bitbanana.listViews.contacts.ManageContactsActivity;
import app.michaelwuensch.bitbanana.setup.SetupActivity;
import app.michaelwuensch.bitbanana.util.ExchangeRateUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;


/**
 * A simple {@link Fragment} subclass.
 */
public class WalletFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener,
        Wallet_Balance.BalanceListener, Wallet.InfoListener, Wallet.ConnectionTestListener,
        Wallet.WalletLoadStateListener, ExchangeRateUtil.ExchangeRateListener, TorManager.TorErrorListener, BackendManager.BackendStateChangedListener {

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
    private Button mSendButton;
    private ImageView mCustodialButton;

    private boolean mPreferenceChangeListenerRegistered = false;
    private boolean mBalanceChangeListenerRegistered = false;
    private boolean mInfoChangeListenerRegistered = false;
    private boolean mExchangeRateListenerRegistered = false;
    private boolean mConnectionTestListenerRegistered = false;
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
        mCustodialButton = view.findViewById(R.id.custodialButton);

        // Show loading screen
        showLoadingScreen();

        mNodeSpinner.setOnNodeSpinnerChangedListener(new NodeSpinner.OnNodeSpinnerChangedListener() {
            @Override
            public void onNodeChanged(String id) {
                BackendManager.activateBackendConfig(BackendConfigsManager.getInstance().getBackendConfigById(id), getActivity(), false);
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

        mCustodialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new UserGuardian(getContext()).securityCustodialLndHubInfoButton();
            }
        });


        // Action when clicked on "scan"
        View btnScan = view.findViewById(R.id.scanButton);
        btnScan.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(getActivity(), ScanActivity.class);
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_GENERIC_SCAN);
            }
        });

        // Action when clicked on "send"
        mSendButton = view.findViewById(R.id.sendButton);

        mSendButton.setOnClickListener(new OnSingleClickListener() {
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
                VPNUtil.openVpnAppSettings(BackendManager.getCurrentBackendConfig().getVpnConfig(), getContext());
            }
        });


        // Action when clicked on "retry"
        Button btnReconnect = view.findViewById(R.id.reconnectBtn);
        btnReconnect.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                mTvLoadingText.setText("");
                showLoadingScreen();
                updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig());
                BackendManager.activateCurrentBackendConfig(getActivity(), true);
            }
        });

        updateTotalBalanceDisplay();

        return view;
    }

    private void walletLoadingCompleted() {
        showWalletScreen();
        mNodeSpinner.updateList();
        mNodeSpinner.setVisibility(View.VISIBLE);
        mStatusDot.setVisibility(View.VISIBLE);
        updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig());
        mBtnSetup.setVisibility(View.INVISIBLE);
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
    public void onInfoUpdated() {

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
            updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig());
        } else {
            mStatusDot.setVisibility(View.GONE);
        }

        // Register listeners
        if (!mPreferenceChangeListenerRegistered) {
            PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);
            mPreferenceChangeListenerRegistered = true;
        }
        if (!mBalanceChangeListenerRegistered) {
            Wallet_Balance.getInstance().registerBalanceListener(this);
            mBalanceChangeListenerRegistered = true;
        }
        if (!mInfoChangeListenerRegistered) {
            Wallet.getInstance().registerInfoListener(this);
            mInfoChangeListenerRegistered = true;
        }
        if (!mConnectionTestListenerRegistered) {
            Wallet.getInstance().registerConnectionTestListener(this);
            mConnectionTestListenerRegistered = true;
        }
        if (!mExchangeRateListenerRegistered) {
            ExchangeRateUtil.getInstance().registerExchangeRateListener(this);
            mExchangeRateListenerRegistered = true;
        }
        if (!mWalletLoadedListenerRegistered) {
            Wallet.getInstance().registerWalletLoadStateListener(this);
            mWalletLoadedListenerRegistered = true;
        }
        if (!mTorErrorListenerRegistered) {
            TorManager.getInstance().registerTorErrorListener(this);
            mTorErrorListenerRegistered = true;
        }
        if (!mBackendStateListenerRegistered) {
            BackendManager.registerBackendStateChangedListener(this);
            mBackendStateListenerRegistered = true;
        }

        updateSpinnerVisibility();

        if (!BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            // if nothing is connected we want to always show the wallet screen
            showWalletScreen();
        }
    }

    public void updateToFeatures() {
        if (FeatureManager.isSendingEnabled()) {
            mSendButton.setEnabled(true);
            mSendButton.setTextColor(getResources().getColor(R.color.banana_yellow));
        } else {
            mSendButton.setEnabled(false);
            mSendButton.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister listeners
        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
        Wallet_Balance.getInstance().unregisterBalanceListener(this);
        Wallet.getInstance().unregisterInfoListener(this);
        Wallet.getInstance().unregisterConnectionTestListener(this);
        ExchangeRateUtil.getInstance().unregisterExchangeRateListener(this);
        Wallet.getInstance().unregisterWalletLoadStateListener(this);
        TorManager.getInstance().unregisterTorErrorListener(this);
        BackendManager.unregisterBackendStateChangedListener(this);
    }

    public void updateSpinnerVisibility() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            mNodeSpinner.updateList();
            mNodeSpinner.setVisibility(View.VISIBLE);
        } else {
            mNodeSpinner.setVisibility(View.GONE);
        }
    }

    public void showConnectionErrorScreen() {
        mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.red)));
        mWalletConnectedLayout.setVisibility(View.GONE);
        mLoadingWalletLayout.setVisibility(View.GONE);
        mWalletNotConnectedLayout.setVisibility(View.VISIBLE);
        mBtnVpnSettings.setVisibility(View.GONE);
    }

    public void showErrorAfterNotUnlockedScreen() {
        showConnectionErrorScreen();
        mTvConnectError.setText(R.string.error_connection_wallet_locked);
    }

    public void showBackgroundForWalletUnlockScreen() {
        mWalletConnectedLayout.setVisibility(View.GONE);
        mWalletNotConnectedLayout.setVisibility(View.GONE);
        mLoadingWalletLayout.setVisibility(View.GONE);
    }

    public void showLoadingScreen() {
        mWalletConnectedLayout.setVisibility(View.GONE);
        mWalletNotConnectedLayout.setVisibility(View.GONE);
        mLoadingWalletLayout.setVisibility(View.VISIBLE);
        mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.banana_yellow)));
    }

    public void showWalletScreen() {
        mWalletConnectedLayout.setVisibility(View.VISIBLE);
        mLoadingWalletLayout.setVisibility(View.GONE);
        mWalletNotConnectedLayout.setVisibility(View.GONE);
        mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.green)));
    }

    private void updateCustodialWarningVisibility() {
        mCustodialButton.setVisibility(BackendManager.getCurrentBackendType() == BackendConfig.BackendType.LND_HUB ? View.VISIBLE : View.GONE);
    }

    private void showCustodialWarning() {
        switch (BackendManager.getCurrentBackendType()) {
            case LND_HUB:
                new UserGuardian(getContext()).securityCustodialLndHub();
                break;
        }
    }

    private void hideBalance() {
        mMainBalanceView.hideBalance();
    }

    private void showBalance() {
        mMainBalanceView.showBalance();
    }

    private void updateStatusDot(BackendConfig backendConfig) {
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

        String walletAlias;
        if (backendConfig.getNetwork() == BackendConfig.Network.MAINNET || backendConfig.getNetwork() == BackendConfig.Network.UNKNOWN || backendConfig.getNetwork() == null)
            walletAlias = backendConfig.getAlias();
        else
            walletAlias = backendConfig.getAlias() + " (" + backendConfig.getNetwork().getDisplayName() + ")";
        mWalletNameWidthDummy.setText(walletAlias);
        if (NetworkUtil.getConnectivityStatus(getActivity()) == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
            mStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.red)));
        } else {
            if (Wallet.getInstance().isConnectedToNode()) {
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
    public void onConnectionTestError(int error) {
        if (!BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            showWalletScreen();
            return;
        }

        mBtnVpnSettings.setVisibility(View.GONE);

        if (error != Wallet.ConnectionTestListener.ERROR_LOCKED) {
            showConnectionErrorScreen();
            if (error == Wallet.ConnectionTestListener.ERROR_AUTHENTICATION) {
                mTvConnectError.setText(R.string.error_connection_invalid_macaroon2);
            } else if (error == Wallet.ConnectionTestListener.ERROR_TIMEOUT) {
                mTvConnectError.setText(getResources().getString(R.string.error_connection_server_unreachable, BackendManager.getCurrentBackendConfig().getHost()));
            } else if (error == Wallet.ConnectionTestListener.ERROR_UNAVAILABLE) {
                mTvConnectError.setText(getResources().getString(R.string.error_connection_lnd_unavailable, String.valueOf(BackendManager.getCurrentBackendConfig().getPort())));
            } else if (error == Wallet.ConnectionTestListener.ERROR_TOR) {
                mTvConnectError.setText(R.string.error_connection_tor_unreachable);
            } else if (error == Wallet.ConnectionTestListener.ERROR_HOST_VERIFICATION) {
                mTvConnectError.setText(R.string.error_connection_host_verification_failed);
            } else if (error == Wallet.ConnectionTestListener.ERROR_HOST_UNRESOLVABLE) {
                mTvConnectError.setText(getString(R.string.error_connection_host_unresolvable, BackendManager.getCurrentBackendConfig().getHost()));
            } else if (error == Wallet.ConnectionTestListener.ERROR_NETWORK_UNREACHABLE) {
                mTvConnectError.setText(R.string.error_connection_network_unreachable);
            } else if (error == Wallet.ConnectionTestListener.ERROR_CERTIFICATE_NOT_TRUSTED) {
                mTvConnectError.setText(R.string.error_connection_invalid_certificate);
            } else if (error == Wallet.ConnectionTestListener.ERROR_INTERNAL) {
                mTvConnectError.setText(R.string.error_connection_internal_server);
            } else if (error == Wallet.ConnectionTestListener.ERROR_INTERNAL_CLEARNET) {
                mTvConnectError.setText(R.string.error_connection_internal_server_clearnet);
            } else if (error == Wallet.ConnectionTestListener.ERROR_AUTHENTICATION_TOKEN) {
                mTvConnectError.setText(R.string.error_connection_rest_authentication);
            }
        }
    }

    @Override
    public void onConnectionTestError(String error) {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            showConnectionErrorScreen();
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
    public void onConnectionTestSuccess() {
        mMainBalanceView.updateNetworkInfo();
        if (Wallet.getInstance().getCurrentWalletLoadState() == Wallet.WalletLoadState.WALLET_LOADED) {
            showWalletScreen();
        }
    }

    @Override
    public void onWalletLoadStateChanged(Wallet.WalletLoadState walletLoadState) {
        switch (walletLoadState) {
            case NOT_LOADED:
                break;
            case TESTING_CONNECTION_BEFORE_UNLOCK:
                mTvLoadingText.setText(R.string.wallet_load_sate_testing_connection);
                break;
            case LOCKED:
                break;
            case UNLOCKED:
                break;
            case RECONNECT_AFTER_UNLOCK:
                mTvLoadingText.setText(R.string.wallet_load_state_reconnect_after_unlock);
                break;
            case TESTING_CONNECTION:
                mTvLoadingText.setText(R.string.wallet_load_sate_testing_connection);
                break;
            case CONNECTION_SUCCESS:
                break;
            case FETCHING_DATA:
                mTvLoadingText.setText(R.string.wallet_load_state_fetching_data);
                break;
            case WALLET_LOADED:
                updateToFeatures();
                updateCustodialWarningVisibility();
                showCustodialWarning();
                walletLoadingCompleted();
        }
    }

    @Override
    public void onTorBootstrappingFailed() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            showConnectionErrorScreen();
            mTvConnectError.setText(R.string.error_connection_tor_bootstrapping);
        }
    }

    @Override
    public void onBackendStateChanged(BackendManager.BackendState backendState) {
        switch (backendState) {
            case DISCONNECTING:
                mTvLoadingText.setText(R.string.connection_state_disconnecting);
                if (BackendConfigsManager.getInstance().getBackendConfigById(mNodeSpinner.getSelectedNodeId()) != null)
                    updateStatusDot(BackendConfigsManager.getInstance().getBackendConfigById(mNodeSpinner.getSelectedNodeId()));

                // Show loading screen
                showLoadingScreen();
                break;
            case NO_BACKEND_SELECTED:
                updateTotalBalanceDisplay();
                updateCustodialWarningVisibility();
                // Clear history list
                ((HomeActivity) getActivity()).getHistoryFragment().updateHistoryDisplayList();
                break;
            case ACTIVATING_BACKEND:
                if (BackendManager.hasBackendConfigs())
                    updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig());
                break;
            case STARTING_VPN:
                mTvLoadingText.setText(R.string.connection_state_starting_vpn);
                break;
            case STARTING_TOR:
                mTvLoadingText.setText(R.string.connection_state_starting_tor);
                break;
            case TOR_CONNECTED:
                updateStatusDot(BackendConfigsManager.getInstance().getCurrentBackendConfig());
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
        showConnectionErrorScreen();
        mTvConnectError.setText(message);

        switch (errorCode) {
            case BackendManager.ERROR_VPN_UNKNOWN_START_ISSUE:
                mBtnVpnSettings.setVisibility(View.VISIBLE);
                break;
            default:
                mBtnVpnSettings.setVisibility(View.GONE);
        }
    }
}
