package app.michaelwuensch.bitbanana.connection.tor;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;

import io.matthewnelson.kmp.tor.resource.exec.tor.ResourceLoaderTorExec;
import io.matthewnelson.kmp.tor.runtime.Action;
import io.matthewnelson.kmp.tor.runtime.TorRuntime;
import io.matthewnelson.kmp.tor.runtime.RuntimeEvent;

import io.matthewnelson.kmp.tor.runtime.core.OnEvent;
import io.matthewnelson.kmp.tor.runtime.core.TorEvent;
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption;
import io.matthewnelson.kmp.tor.runtime.core.config.builder.BuilderScopePort;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Singleton to manage Tor.
 */
public class TorManager {

    private static TorManager mTorManagerInstance;
    private static final String LOG_TAG = TorManager.class.getSimpleName();

    private final Set<TorErrorListener> mTorErrorListeners = new HashSet<>();

    private int mHttpProxyPort;
    private int mSocksProxyPort;
    private boolean isProxyRunning = false;
    private boolean isConnecting = false;

    private static TorRuntime.Environment mTorEnv = getmTorEnv();
    private static TorRuntime mTorRuntime = getmTorRuntime();

    private TorManager() {
    }

    public static synchronized TorManager getInstance() {
        if (mTorManagerInstance == null) {
            mTorManagerInstance = new TorManager();
        }
        return mTorManagerInstance;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public int getHttpProxyPort() {
        return mHttpProxyPort;
    }

    public void setHttpProxyPort(int httpProxyPort) {
        this.mHttpProxyPort = httpProxyPort;
    }

    public int getSocksProxyPort() {
        return mSocksProxyPort;
    }

    public void setSocksProxyPort(int socksProxyPort) {
        this.mSocksProxyPort = socksProxyPort;
    }

    public boolean isProxyRunning() {
        return isProxyRunning;
    }

    public void setIsProxyRunning(boolean proxyRunning) {
        if (!proxyRunning)
            isConnecting = false;
        isProxyRunning = proxyRunning;
    }

    public void setIsConnecting(boolean connecting) {
        isConnecting = connecting;
    }

    private static TorRuntime.Environment getmTorEnv() {
        if (mTorEnv == null) {
            TorRuntime.Environment env = TorRuntime.Environment.Builder(
                    /* workDirectory  */ new File(App.getAppContext().getFilesDir(), "kmptor"),
                    /* cacheDirectory */ new File(App.getAppContext().getCacheDir(), "kmptor"),
                    /* loader         */ ResourceLoaderTorExec::getOrCreate,
                    /* block          */ b -> {
                        // Configure further if needed...
                    }
            );
            env.debug = true;
            return env;
        } else {
            return mTorEnv;
        }
    }

    private static TorRuntime getmTorRuntime() {
        if (mTorRuntime == null) {
            // Create the Tor runtime
            TorRuntime runtime = TorRuntime.Builder(mTorEnv, b -> {

                RuntimeEvent.entries().forEach(event -> {
                    TorManager.getInstance().onTorRuntimeEvent(b, event);
                });

                b.config((c, environment) -> {
                    // SocksPort configuration
                    c.configure(TorOption.__SocksPort.INSTANCE, p -> {
                        p.auto();
                        p.flagsSocks(f -> f.OnionTrafficOnly = false);
                    });

                    // Unix domain socket configuration
                    try {
                        c.configure(TorOption.__SocksPort.INSTANCE, p -> {
                            p.unixSocket(new File(environment.workDirectory, "socks.sock"));
                        });
                    } catch (UnsupportedOperationException ignored) {
                        // Not supported on this platform
                    }

                    // HTTP Tunnel Port configuration
                    c.configure(TorOption.__HTTPTunnelPort.INSTANCE, BuilderScopePort.HTTPTunnel::auto);
                });

                b.required(TorEvent.ERR.INSTANCE);
                b.required(TorEvent.WARN.INSTANCE);
            });

            // Add shutdown hook for stopping the daemon
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Action.stopDaemonSync(runtime);
                    BBLog.i(LOG_TAG, "Tor stopped successfully because of shutdown hook.");
                } catch (Throwable e) {
                    BBLog.e(LOG_TAG, "Stopping tor because of shutdown hook failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }));

            return runtime;
        } else {
            return mTorRuntime;
        }
    }


    private void onTorRuntimeEvent(TorRuntime.BuilderScope b, RuntimeEvent<?> event) {
        if (event instanceof RuntimeEvent.LISTENERS) {
            b.observerStatic(
                    /* event    */ event,
                    /* executor */ OnEvent.Executor.Immediate.INSTANCE,
                    /* onEvent  */ data -> {
                        BBLog.i(LOG_TAG, data.toString());
                    }
            );
            RuntimeEvent.LISTENERS listenersEvent = (RuntimeEvent.LISTENERS) event;
            b.observerStatic(
                    /* event    */ listenersEvent,
                    /* executor */ OnEvent.Executor.Immediate.INSTANCE,
                    /* onEvent  */ data -> {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (data.http.isEmpty() && data.socks.isEmpty()) {
                                TorManager.getInstance().setIsProxyRunning(false);
                            } else {

                                if (!data.http.isEmpty()) {
                                    int port = Integer.valueOf(data.http.toArray()[0].toString().split(":")[1]);
                                    TorManager.getInstance().setHttpProxyPort(port);
                                }
                                if (!data.socks.isEmpty()) {
                                    int port = Integer.valueOf(data.socks.toArray()[0].toString().split(":")[1]);
                                    TorManager.getInstance().setSocksProxyPort(port);
                                }

                                TorManager.getInstance().setIsProxyRunning(true);
                                TorManager.getInstance().setIsConnecting(false);

                                // restart HTTP Client
                                HttpClient.getInstance().restartHttpClient();

                                // Continue backend connection process if it waited for Tor connection to be established
                                if (BackendManager.getCurrentBackendConfig() != null && BackendManager.getCurrentBackendConfig().getUseTor() && BackendManager.getBackendState() == BackendManager.BackendState.STARTING_TOR) {
                                    BackendManager.activateBackendConfig4();
                                }
                            }
                        });
                    }
            );
        } else if (event instanceof RuntimeEvent.ERROR) {
            b.observerStatic(
                    /* event    */ event,
                    /* executor */ OnEvent.Executor.Immediate.INSTANCE,
                    /* onEvent  */ data -> {
                        BBLog.e(LOG_TAG, data.toString());
                    }
            );
        } else {
            // log all runtime events
            b.observerStatic(
                    /* event    */ event,
                    /* executor */ OnEvent.Executor.Immediate.INSTANCE,
                    /* onEvent  */ data -> {
                        BBLog.v(LOG_TAG, data.toString());
                    }
            );
        }
    }

    private Completable startDaemon() {
        // Handle runtime lifecycle with RxJava
        return Completable.create(emitter -> {
            mTorRuntime.enqueue(
                    /* action    */ Action.StartDaemon,
                    /* onFailure */ emitter::onError,
                    /* onSuccess */ success -> emitter.onComplete()
            );
        }).subscribeOn(Schedulers.io());
    }

    private Completable stopDaemon() {
        // Handle runtime lifecycle with RxJava
        return Completable.create(emitter -> {
            mTorRuntime.enqueue(
                    /* action    */ Action.StopDaemon,
                    /* onFailure */ emitter::onError,
                    /* onSuccess */ success -> emitter.onComplete()
            );
        }).subscribeOn(Schedulers.io());
    }

    private Completable restartDaemon() {
        // Handle runtime lifecycle with RxJava
        return Completable.create(emitter -> {
            mTorRuntime.enqueue(
                    /* action    */ Action.RestartDaemon,
                    /* onFailure */ emitter::onError,
                    /* onSuccess */ success -> emitter.onComplete()
            );
        }).subscribeOn(Schedulers.io());
    }

    /**
     * @noinspection ResultOfMethodCallIgnored
     */
    @SuppressLint("CheckResult")
    public void startTor() {
        BBLog.d(LOG_TAG, "Start Tor called.");
        if (!isConnecting) {
            isConnecting = true;
            // Start the Tor daemon
            startDaemon().subscribe(
                    () -> BBLog.i(LOG_TAG, "Tor started successfully."),
                    throwable -> {
                        BBLog.e(LOG_TAG, "Starting tor failed: " + throwable.getMessage());
                        //noinspection CallToPrintStackTrace
                        throwable.printStackTrace(); // Optional: Print the stack trace
                    }
            );
        }
    }

    /**
     * @noinspection ResultOfMethodCallIgnored
     */
    @SuppressLint("CheckResult")
    public void stopTor() {
        BBLog.d(LOG_TAG, "Stop Tor called.");
        isConnecting = false;
        stopDaemon().subscribe(
                () -> {
                    setIsProxyRunning(false);
                    BBLog.i(LOG_TAG, "Tor stopped successfully.");},
                throwable -> {
                    BBLog.e(LOG_TAG, "Stopping tor failed: " + throwable.getMessage());
                    //noinspection CallToPrintStackTrace
                    throwable.printStackTrace(); // Optional: Print the stack trace
                }
        );
    }

    /**
     * @noinspection ResultOfMethodCallIgnored
     */
    @SuppressLint("CheckResult")
    public void restartTor() {
        BBLog.d(LOG_TAG, "Restart Tor called.");
        restartDaemon().subscribe(
                () -> BBLog.i(LOG_TAG, "Tor restarted successfully."),
                throwable -> {
                    BBLog.e(LOG_TAG, "SRestarting tor failed: " + throwable.getMessage());
                    //noinspection CallToPrintStackTrace
                    throwable.printStackTrace(); // Optional: Print the stack trace
                }
        );
    }

    public void switchTorPrefState(boolean newActive) {
        if (newActive) {
            if (!isProxyRunning()) {
                startTor();
                // HTTP Client gets restarted automatically once tor connection is established.
            } else {
                // restart HTTP Client
                HttpClient.getInstance().restartHttpClient();
            }
        } else {
            if (!isCurrentNodeConnectionTor() && (isConnecting || isProxyRunning)) {
                // Stop tor service if not used by current node.
                stopTor();
            }
            // restart HTTP Client
            HttpClient.getInstance().restartHttpClient();
        }
    }

    public boolean isCurrentNodeConnectionTor() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            return BackendConfigsManager.getInstance().getCurrentBackendConfig().getUseTor();
        } else {
            return false;
        }
    }

    public int getTorTimeoutMultiplier() {
        if (PrefsUtil.isTorEnabled() || isCurrentNodeConnectionTor()) {
            return RefConstants.TOR_TIMEOUT_MULTIPLIER;
        } else {
            return 1;
        }
    }

    public void broadcastTorError() {
        for (TorErrorListener listener : mTorErrorListeners) {
            listener.onTorBootstrappingFailed();
        }
    }

    public void registerTorErrorListener(TorErrorListener listener) {
        mTorErrorListeners.add(listener);
    }

    public void unregisterTorErrorListener(TorErrorListener listener) {
        mTorErrorListeners.remove(listener);
    }

    public interface TorErrorListener {
        void onTorBootstrappingFailed();
    }
}
