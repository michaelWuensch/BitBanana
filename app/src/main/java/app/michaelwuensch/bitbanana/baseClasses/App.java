package app.michaelwuensch.bitbanana.baseClasses;

import android.app.Application;

import app.michaelwuensch.bitbanana.connection.tor.TorSetup;
import app.michaelwuensch.bitbanana.util.BBLog;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;


// This class is used as Application class for BitBanana.

public class App extends Application {
    private static App mContext;
    public boolean connectionToLNDEstablished = false;
    // keep the data from the URI Scheme in memory, so we can access it from anywhere.
    private String uriSchemeData;

    public App() {
        mContext = this;

        RxJavaPlugins.setErrorHandler(e -> {
            if (e.getMessage() != null && e.getMessage().contains("shutdownNow")) {
                // Is propagated from gRPC when shutting down channel
            } else {
                BBLog.d("RxJava", e.getMessage());
            }
        });
    }

    public static App getAppContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Prepare the Tor Service
        TorSetup.generateTorServiceControllerBuilder(this).build();
    }

    public String getUriSchemeData() {
        return uriSchemeData;
    }

    public void setUriSchemeData(String data) {
        uriSchemeData = data;
    }
}