package app.michaelwuensch.bitbanana.connection.internetConnectionStatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.util.concurrent.RejectedExecutionException;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.wallet.Wallet;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = NetworkChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        int status = NetworkUtil.getConnectivityStatus(context);
        BBLog.d("NetworkChangeReceiver: ", "Network status changed to " + NetworkUtil.getConnectivityStatusString(context));
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            if (status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
                // The following command will find out, if we have a connection to our node
                if (Wallet.getInstance().isConnectedToNode())
                    Wallet.getInstance().connectionTest(false);
            } else {
                // It needs some time to establish the connection to LND.
                // Therefore we check the connection after a delay.
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    try {
                        // The following command will find out, if we have a connection to our node
                        if (Wallet.getInstance().isConnectedToNode())
                            Wallet.getInstance().connectionTest(false);
                    } catch (RejectedExecutionException ex) {
                        BBLog.d(LOG_TAG, "Execute of fetchFromLND() was rejected");
                    }
                }, 5000);
            }
        }
    }
}
