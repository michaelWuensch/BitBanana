package app.michaelwuensch.bitbanana.wallet;


import android.os.Handler;

import com.github.lightningnetwork.lnd.lnrpc.GetStateRequest;
import com.github.lightningnetwork.lnd.lnrpc.UnlockWalletRequest;
import com.google.protobuf.ByteString;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Wallet {

    private static final String LOG_TAG = Wallet.class.getSimpleName();

    private static Wallet mInstance = null;
    private final Set<InfoListener> mInfoListeners = new HashSet<>();
    private final Set<ConnectionTestListener> mConnectionTestListeners = new HashSet<>();
    private final Set<WalletLoadStateListener> mWalletLoadStateListeners = new HashSet<>();

    private WalletLoadState mWalletLoadState;
    // ToDo: remove when fetching data is done correctly
    public boolean mIsWalletReady = false;
    private CurrentNodeInfo mCurrentNodeInfo;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Handler mHandler = new Handler();

    private Wallet() {
        ;
    }

    public static Wallet getInstance() {

        if (mInstance == null) {
            mInstance = new Wallet();
        }

        return mInstance;
    }

    // ToDo: make this private once possible
    public void setWalletLoadState(WalletLoadState walletLoadState) {
        if (mWalletLoadState != walletLoadState) {
            mWalletLoadState = walletLoadState;
            BBLog.v(LOG_TAG, "New wallet load state: " + walletLoadState);
            broadcastWalletLoadState(walletLoadState);
        }
    }

    public WalletLoadState getCurrentWalletLoadState() {
        return mWalletLoadState;
    }

    /**
     * Use this to reset the wallet information when the wallet was switched.
     */
    public void reset() {
        Wallet_Balance.getInstance().reset();
        Wallet_Components.getInstance().reset();

        mIsWalletReady = false;
        mCurrentNodeInfo = null;
        mWalletLoadState = WalletLoadState.NOT_LOADED;

        mHandler.removeCallbacksAndMessages(null);
        compositeDisposable.clear();
    }

    /**
     * This is the function to call to initiate loading of a wallet after a backend was activated.
     */
    public void open() {
        switch (BackendManager.getCurrentBackendConfig().getBackendType()) {
            case LND_GRPC:
                checkIfLndIsLocked();
                break;
            default:
                setWalletLoadState(WalletLoadState.UNLOCKED);
                connectionTest(true);
        }
    }

    public void checkIfLndIsLocked() {
        setWalletLoadState(WalletLoadState.TESTING_CONNECTION_BEFORE_UNLOCK);
        BBLog.d(LOG_TAG, "LND lock state test.");

        compositeDisposable.add(LndConnection.getInstance().getStateService().getState(GetStateRequest.newBuilder().build())
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(getStateResponse -> {

                    switch (getStateResponse.getState()) {
                        case LOCKED:
                            BBLog.d(LOG_TAG, "LND is locked.");
                            setWalletLoadState(WalletLoadState.LOCKED);
                            broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_LOCKED);
                            break;
                        default:
                            BBLog.d(LOG_TAG, "LND is unlocked.");
                            setWalletLoadState(WalletLoadState.UNLOCKED);
                            connectionTest(true);
                    }
                }, throwable -> {
                    // If something in our connection is wrong, we cannot reach the state service and end here.
                    // We use the same error handling as in our connection test to display meaningful errors.
                    BBLog.d(LOG_TAG, "Error testing if LND is locked. Reason: " + throwable.getMessage()); // There is a hard coded proxy connect timeout of 30 seconds that we cannot change. With tor we often reach this.
                    errorHandling(throwable);
                }));
    }

    /**
     * Call this if the daemon is running, but the wallet is not unlocked yet.
     *
     * @param password
     */
    public void unlockWallet(String password) {
        UnlockWalletRequest unlockRequest = UnlockWalletRequest.newBuilder()
                .setWalletPassword(ByteString.copyFrom(password.getBytes()))
                .build();

        compositeDisposable.add(LndConnection.getInstance().getWalletUnlockerService().unlockWallet(unlockRequest)
                .subscribe(unlockWalletResponse -> {
                    BBLog.d(LOG_TAG, "successfully unlocked");
                    setWalletLoadState(WalletLoadState.UNLOCKED);
                    setWalletLoadState(WalletLoadState.RECONNECT_AFTER_UNLOCK);

                    // We have to reset the connection, because until you unlock the wallet, there is no Lightning rpc service available.
                    // Thus we could not connect to it with previous channel, so we reset the connection and connect to all services when unlocked.
                    LndConnection.getInstance().restartConnection();

                    mHandler.postDelayed(() -> {
                        // We have to call this delayed, as without it, it will show as unconnected until the wallet button is hit again.
                        // ToDo: Create a routine that retries this until successful
                        connectionTest(true);
                    }, 10000);

                    mHandler.postDelayed(() -> {
                        // The channels are already fetched before, but they are all showed and saved as offline right after unlocking.
                        // That's why we update it again 10 seconds later.
                        // ToDo: Create a routine that retries this until successful
                        Wallet_Components.getInstance().fetchChannelsFromLND();
                    }, 12000);
                }, throwable -> {
                    BBLog.e(LOG_TAG, throwable.getMessage());
                    // Show password prompt again after error
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_LOCKED);
                }));
    }


    /**
     * Makes a simple getInfo request and observes the response.
     * The getInfo request should be done periodically anyway, therefore this combines fetching the node info with testing the connection.
     * If this request finishes without an error, our connection is successfully established.
     * All listeners registered to ConnectionTestListener will be informed about the result.
     * All listeners to info update will be informed about the new node info.
     */
    public void connectionTest(boolean loadWalletOnSuccess) {
        if (loadWalletOnSuccess) {
            setWalletLoadState(WalletLoadState.UNLOCKED);
            setWalletLoadState(WalletLoadState.TESTING_CONNECTION);
        }

        compositeDisposable.add(BackendManager.api().getCurrentNodeInfo()
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    mCurrentNodeInfo = response;
                    broadcastInfoUpdate();
                    broadcastConnectionTestResult(true, -1);
                    if (loadWalletOnSuccess) {
                        setWalletLoadState(WalletLoadState.CONNECTION_SUCCESS);
                        fetchWalletData();
                    }
                }, throwable -> {
                    errorHandling(throwable);
                }));
    }

    private void errorHandling(Throwable throwable) {
        // ToDo: This is just error handling for LND, adapt for CoreLightning if necessary.
        if (throwable.getMessage().toLowerCase().contains("unavailable") && !throwable.getMessage().toLowerCase().contains(".onion")) {
            BBLog.e(LOG_TAG, "Service unavailable");
            if (throwable.getCause() != null) {
                if (throwable.getCause().getMessage().toLowerCase().contains("cannot verify hostname")) {
                    // This is the case if:
                    // - The hostname used to initiate the lnd connection (the hostname from the lndconnect string) does not match with the hostname in the provided certificate.
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_HOST_VERIFICATION);
                } else if (throwable.getCause().getMessage().toLowerCase().contains("unable to resolve host")) {
                    // This is the case if:
                    // - We have an internet or network connection, but the desired host is not resolvable.
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_HOST_UNRESOLVABLE);
                } else if (throwable.getCause().getMessage().toLowerCase().contains("enetunreach")) {
                    // This is the case if:
                    // - We have no internet or network connection at all.
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_NETWORK_UNREACHABLE);
                } else if (throwable.getCause().getMessage().toLowerCase().contains("econnrefused")) {
                    // This is the case if:
                    // - LND daemon is not running
                    // - An incorrect port is used
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_UNAVAILABLE);
                } else if (throwable.getCause().getMessage().toLowerCase().contains("trust anchor")) {
                    // This is the case if:
                    // - tor is not used and no certificate is provided or a wrong certificate is provided
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_CERTIFICATE_NOT_TRUSTED);
                } else {
                    // Unknown error. Print what gets returned directly, always english.
                    broadcastConnectionTestResult(throwable.getCause().getMessage());
                }
            } else if (throwable.getMessage().toLowerCase().contains("404") && PrefsUtil.isTorEnabled()) {
                // This is the case if:
                // - Tor is turned on, but the host cannot be resolved
                broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_HOST_UNRESOLVABLE);
            } else if (throwable.getMessage().toLowerCase().contains("500") && PrefsUtil.isTorEnabled()) {
                if (BackendManager.getCurrentBackendConfig().isTorHostAddress()) {
                    // This is the case if:
                    // - Tor is turned on and an incorrect port is used.
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_INTERNAL);
                } else {
                    // This is the case if:
                    // - happened for a user that used wireguard and connected to a clearnet node. Disabling tor solved connection issues.
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_INTERNAL_CLEARNET);
                }
            } else {
                // Unknown error. Print what gets returned directly, always english.
                broadcastConnectionTestResult(throwable.getMessage());
            }
        } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
            // This is the case if:
            // - The server is not reachable at all. (e.g. wrong IP Address or server offline)
            BBLog.e(LOG_TAG, "Cannot reach remote");
            broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_TIMEOUT);
        } else if (throwable.getMessage().toLowerCase().contains("verification failed")) {
            // This is the case if:
            // - The macaroon is invalid
            BBLog.e(LOG_TAG, "Macaroon is invalid!");
            broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_AUTHENTICATION);
        } else if (throwable.getMessage().contains("UNKNOWN")) {
            // This is the case if:
            // - The macaroon has wrong encoding
            BBLog.e(LOG_TAG, "Macaroon is invalid!");
            broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_AUTHENTICATION);
        } else if (throwable.getMessage().contains(".onion")) {
            // This is the case if:
            // - Tor is not active in the settings and the user tries to connect to a tor node.
            BBLog.e(LOG_TAG, "Cannot resolve onion address!");
            broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_TOR);
        } else if (throwable.getMessage().toLowerCase().contains("interrupted")) {
            BBLog.e(LOG_TAG, "Test if LND is reachable was interrupted.");
            broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_INTERRUPTED);
        } else {
            // Unknown error. Print what gets returned directly, always english.
            BBLog.e(LOG_TAG, "Unknown connection error..");
            broadcastConnectionTestResult(throwable.getMessage());
        }
        BBLog.e(LOG_TAG, throwable.getMessage());
        if (throwable.getCause() != null) {
            BBLog.e(LOG_TAG, throwable.getCause().getMessage());
            throwable.getCause().printStackTrace();
        }
    }

    /**
     * This function will initiate fetching all necessary data from our connected node
     * and set the WalletLoadState to WALLET_LOADED once the minimum required data is fetched to display
     * the first screen.
     */
    public void fetchWalletData() {
        broadcastWalletLoadState(WalletLoadState.FETCHING_DATA);

        switch (BackendManager.getCurrentBackendType()) {
            case LND_GRPC:

                // Fetch the transaction history
                Wallet_Components.getInstance().fetchLNDTransactionHistory();
                Wallet_Balance.getInstance().fetchBalances();
                Wallet_Components.getInstance().fetchChannelsFromLND();

                // Fetch UTXOs
                Wallet_Components.getInstance().fetchUTXOs();

                // Subscribe to Transaction Events
                Wallet_Components.getInstance().subscribeToTransactions();
                Wallet_Components.getInstance().subscribeToHtlcEvents();
                Wallet_Components.getInstance().subscribeToInvoices();

                if (mHandler != null) {
                    mHandler.postDelayed(() -> Wallet_Components.getInstance().subscribeToChannelEvents(), 3000);
                }
                break;
            case CORE_LIGHTNING_GRPC:
                Wallet_Components.getInstance().mChannelsFetched = true;
                Wallet_Balance.getInstance().fetchBalances();

                // Fetch UTXOs
                Wallet_Components.getInstance().fetchUTXOs();
        }
    }

    public boolean isInfoFetched() {
        return mCurrentNodeInfo != null;
    }

    /**
     * Returns if there is/was a working connection to the node.
     */
    public boolean isConnectedToNode() {
        return mWalletLoadState == WalletLoadState.CONNECTION_SUCCESS ||
                mWalletLoadState == WalletLoadState.FETCHING_DATA ||
                mWalletLoadState == WalletLoadState.WALLET_LOADED;
    }

    public CurrentNodeInfo getCurrentNodeInfo() {
        return mCurrentNodeInfo;
    }

    public BaseBackendConfig.Network getNetwork() {
        if (getCurrentNodeInfo() == null)
            return BaseBackendConfig.Network.UNKNOWN;
        return getCurrentNodeInfo().getNetwork();
    }

    /**
     * Notify all listeners about the lnd connection test result.
     *
     * @param success true if successful
     * @param error   one of LndConnectionTestListener errors
     */
    public void broadcastConnectionTestResult(boolean success, int error) {
        if (success) {
            for (ConnectionTestListener listener : mConnectionTestListeners) {
                listener.onConnectionTestSuccess();
            }
        } else {
            for (ConnectionTestListener listener : mConnectionTestListeners) {
                listener.onConnectionTestError(error);
            }
        }
    }

    public void broadcastConnectionTestResult(String errorMessage) {
        for (ConnectionTestListener listener : mConnectionTestListeners) {
            listener.onConnectionTestError(errorMessage);
        }
    }

    public void registerConnectionTestListener(ConnectionTestListener listener) {
        mConnectionTestListeners.add(listener);
    }

    public void unregisterConnectionTestListener(ConnectionTestListener listener) {
        mConnectionTestListeners.remove(listener);
    }


    /**
     * Notify all listeners to info updates.
     */
    private void broadcastInfoUpdate() {
        for (InfoListener listener : mInfoListeners) {
            listener.onInfoUpdated();
        }
    }

    public void registerInfoListener(InfoListener listener) {
        mInfoListeners.add(listener);
    }

    public void unregisterInfoListener(InfoListener listener) {
        mInfoListeners.remove(listener);
    }


    public void broadcastWalletLoadState(WalletLoadState walletLoadState) {
        for (WalletLoadStateListener listener : mWalletLoadStateListeners) {
            listener.onWalletLoadStateChanged(walletLoadState);
        }
    }

    public void registerWalletLoadStateListener(WalletLoadStateListener listener) {
        mWalletLoadStateListeners.add(listener);
    }

    public void unregisterWalletLoadStateListener(WalletLoadStateListener listener) {
        mWalletLoadStateListeners.remove(listener);
    }

    public interface ConnectionTestListener {

        int ERROR_LOCKED = 0;
        int ERROR_INTERRUPTED = 1;
        int ERROR_TIMEOUT = 2;
        int ERROR_UNAVAILABLE = 3;
        int ERROR_AUTHENTICATION = 4;
        int ERROR_TOR = 5;
        int ERROR_HOST_VERIFICATION = 6;
        int ERROR_HOST_UNRESOLVABLE = 7;
        int ERROR_NETWORK_UNREACHABLE = 8;
        int ERROR_CERTIFICATE_NOT_TRUSTED = 9;
        int ERROR_INTERNAL = 10;
        int ERROR_INTERNAL_CLEARNET = 11;

        void onConnectionTestError(int error);

        void onConnectionTestError(String error);

        void onConnectionTestSuccess();
    }

    public interface WalletLoadStateListener {
        void onWalletLoadStateChanged(WalletLoadState state);
    }

    public interface InfoListener {
        void onInfoUpdated();
    }

    public enum WalletLoadState {
        NOT_LOADED,
        TESTING_CONNECTION_BEFORE_UNLOCK,
        LOCKED,
        UNLOCKED,
        RECONNECT_AFTER_UNLOCK,
        TESTING_CONNECTION,
        CONNECTION_SUCCESS,
        FETCHING_DATA,
        WALLET_LOADED,
        ERROR;

        public static Wallet.WalletLoadState parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return NOT_LOADED;
            }
        }
    }
}