package app.michaelwuensch.bitbanana.wallet;


import android.os.Handler;

import com.github.lightningnetwork.lnd.lnrpc.GetStateRequest;
import com.github.lightningnetwork.lnd.lnrpc.UnlockWalletRequest;
import com.google.protobuf.ByteString;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lnd.LndBackend;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Wallet {

    private static final String LOG_TAG = Wallet.class.getSimpleName();

    private static Wallet mInstance = null;
    private final Set<InfoListener> mInfoListeners = new HashSet<>();
    private final Set<ConnectionTestListener> mConnectionTestListeners = new HashSet<>();
    private final Set<WalletLoadStateListener> mWalletLoadStateListeners = new HashSet<>();

    private WalletLoadState mWalletLoadState;
    private CurrentNodeInfo mCurrentNodeInfo;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Handler mHandler = new Handler();

    private Wallet() {
        ;
    }

    public static Wallet getInstance() {

        if (mInstance == null) {
            mInstance = new Wallet();
        }

        return mInstance;
    }

    private void setWalletLoadState(WalletLoadState walletLoadState) {
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
        Wallet_Channels.getInstance().reset();
        Wallet_TransactionHistory.getInstance().reset();
        Wallet_NodesAndPeers.getInstance().reset();
        Wallet_Bolt12Offers.getInstance().reset();

        mCurrentNodeInfo = null;
        mWalletLoadState = WalletLoadState.NOT_LOADED;

        mHandler.removeCallbacksAndMessages(null);
        compositeDisposable.clear();
    }

    public void cancelSubscriptions() {
        Wallet_Balance.getInstance().cancelSubscriptions();
        Wallet_Channels.getInstance().cancelSubscriptions();
        Wallet_TransactionHistory.getInstance().cancelSubscriptions();
        Wallet_NodesAndPeers.getInstance().cancelSubscriptions();
        Wallet_Bolt12Offers.getInstance().cancelSubscriptions();
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
            case LND_HUB:
                authLndHub();
                break;
            default:
                setWalletLoadState(WalletLoadState.UNLOCKED);
                connectionTest(true);
        }
    }

    private void authLndHub() {
        setWalletLoadState(WalletLoadState.TESTING_CONNECTION_BEFORE_UNLOCK);

        if (!BackendManager.getCurrentBackendConfig().getHostWithOverride().contains("http")) {
            // This if statement prevents a crash that could deadlock the application if something in the host address missed the protocol
            broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_HOST_UNRESOLVABLE);
            return;
        }
        // The info request works without authentication. Therefore we need another call and use balances.
        // Without doing this here we would end up with a multicall, that tries to authenticate multiple times.
        compositeDisposable.add(Wallet_Balance.getInstance().fetchBalanceSingle()
                .subscribe(response -> {
                    setWalletLoadState(WalletLoadState.UNLOCKED);
                    connectionTest(true);
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Exception authenticating for LndHub instance: " + throwable.getMessage());
                    broadcastConnectionTestResult(false, ConnectionTestListener.ERROR_AUTHENTICATION_TOKEN);
                }));
    }

    public void checkIfLndIsLocked() {
        setWalletLoadState(WalletLoadState.TESTING_CONNECTION_BEFORE_UNLOCK);
        BBLog.d(LOG_TAG, "LND lock state test.");

        if (((LndBackend) BackendManager.getCurrentBackend()).getIsAccountRestricted()) {
            BBLog.d(LOG_TAG, "Restricted account macaroon. LND is assumed to be unlocked.");
            setWalletLoadState(WalletLoadState.UNLOCKED);
            connectionTest(true);
            return;
        }

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
                        Wallet_Channels.getInstance().fetchChannels();
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

                    // Save the network info to our backend configuration if it is different
                    if (response.getNetwork() != BackendManager.getCurrentBackendConfig().getNetwork() ||
                            !response.getAvatarMaterial().equals(BackendManager.getCurrentBackendConfig().getAvatarMaterial())) {
                        BackendManager.getCurrentBackendConfig().setNetwork(response.getNetwork());
                        BackendManager.getCurrentBackendConfig().setAvatarMaterial(response.getAvatarMaterial());
                        BackendConfigsManager.getInstance().updateBackendConfig(BackendManager.getCurrentBackendConfig());
                        BackendConfigsManager.getInstance().apply();
                    }

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
            BBLog.e(LOG_TAG, "Connection interrupted.");
            broadcastConnectionTestResult(throwable.getMessage());
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
                // Fetching the data that is required before we show the wallet.
                compositeDisposable.add(Single.zip(Wallet_Balance.getInstance().fetchBalanceSingle(),
                        Wallet_Channels.getInstance().fetchChannelsSingle(),
                        (response1, response2) -> {
                            // Everything fetched, now show the wallet!
                            setWalletLoadState(WalletLoadState.WALLET_LOADED);
                            return true;
                        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(aBoolean -> {
                }, throwable -> {
                    setWalletLoadState(WalletLoadState.ERROR);
                    broadcastWalletLoadError("Exception loading required data on startup: " + throwable.getMessage());
                    BBLog.e(LOG_TAG, "Exception loading required data on startup: " + throwable.getMessage());
                }));

                //Data that can be loaded after wallet is displayed

                // Fetch the transaction history
                Wallet_TransactionHistory.getInstance().fetchTransactionHistory();

                // Fetch UTXOs
                Wallet_TransactionHistory.getInstance().fetchUTXOs();

                // Subscribe to Transaction Events
                Wallet_TransactionHistory.getInstance().subscribeToTransactions();
                Wallet_Channels.getInstance().subscribeToHtlcEvents();
                Wallet_TransactionHistory.getInstance().subscribeToInvoices();

                mHandler.postDelayed(() -> Wallet_Channels.getInstance().subscribeToChannelEvents(), 3000);

                break;
            case CORE_LIGHTNING_GRPC:
                // Fetching the data that is required before we show the wallet.
                compositeDisposable.add(Single.zip(Wallet_Balance.getInstance().fetchBalanceSingle(),
                        Wallet_Channels.getInstance().fetchChannelsSingle(), Wallet_Bolt12Offers.getInstance().fetchBolt12OffersSingle(),
                        (response1, response2, response3) -> {
                            // Everything fetched, now show the wallet!
                            setWalletLoadState(WalletLoadState.WALLET_LOADED);
                            return true;
                        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(aBoolean -> {
                }, throwable -> {
                    setWalletLoadState(WalletLoadState.ERROR);
                    broadcastWalletLoadError("Exception loading required data on startup: " + throwable.getMessage());
                    BBLog.e(LOG_TAG, "Exception loading required data on startup: " + throwable.getMessage());
                }));

                //Data that can be loaded after wallet is displayed

                // Fetch UTXOs
                Wallet_TransactionHistory.getInstance().fetchUTXOs();

                // Fetch the transaction history
                Wallet_TransactionHistory.getInstance().fetchTransactionHistory();
                break;
            case LND_HUB:
                compositeDisposable.add(Wallet_Balance.getInstance().fetchBalanceSingle()
                        .subscribe(response -> {
                            // Everything fetched, now show the wallet!
                            setWalletLoadState(WalletLoadState.WALLET_LOADED);
                        }, throwable -> {
                            setWalletLoadState(WalletLoadState.ERROR);
                            broadcastWalletLoadError("Exception loading required data on startup: " + throwable.getMessage());
                            BBLog.e(LOG_TAG, "Exception loading required data on startup: " + throwable.getMessage());
                        }));

                // Fetch the transaction history
                Wallet_TransactionHistory.getInstance().fetchTransactionHistory();
                break;
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

    /**
     * Gets the bitcoin network the wallet is connected to.
     * If the wallet is not fully loaded yet, this function will return UNKNOWN.
     */
    public BackendConfig.Network getNetwork() {
        if (getCurrentNodeInfo() == null)
            return BackendConfig.Network.UNKNOWN;
        return getCurrentNodeInfo().getNetwork();
    }

    /**
     * Gets the bitcoin network the wallet is connected to.
     * If the wallet is not fully loaded yet, this function will fall back to the saved network information from the connection config
     */
    public BackendConfig.Network getNetworkWithFallback() {
        if (getCurrentNodeInfo() == null || getCurrentNodeInfo().getNetwork() == BackendConfig.Network.UNKNOWN)
            return BackendManager.getCurrentBackendConfig().getNetwork();
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

    public void broadcastWalletLoadError(String errorMessage) {
        for (WalletLoadStateListener listener : mWalletLoadStateListeners) {
            listener.onWalletLoadError(errorMessage);
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
        int ERROR_AUTHENTICATION_TOKEN = 12;

        void onConnectionTestError(int error);

        void onConnectionTestError(String error);

        void onConnectionTestSuccess();
    }

    public interface WalletLoadStateListener {
        void onWalletLoadStateChanged(WalletLoadState state);

        void onWalletLoadError(String error);
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