package app.michaelwuensch.bitbanana.connection.internetConnectionStatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.util.concurrent.RejectedExecutionException;

import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.NodeConfigsManager;
import app.michaelwuensch.bitbanana.util.Wallet;
import app.michaelwuensch.bitbanana.util.BBLog;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = NetworkChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        int status = NetworkUtil.getConnectivityStatus(context);
        BBLog.d("NetworkChangeReceiver: ", "Network status changed to " + NetworkUtil.getConnectivityStatusString(context));
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
            if (status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
                // The following command will find out, if we have a connection to LND
                Wallet.getInstance().fetchInfoFromLND();
            } else {
                // It needs some time to establish the connection to LND.
                // Therefore we check the connection after a delay.
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    try {
                        // The following command will find out, if we have a connection to LND
                        Wallet.getInstance().fetchInfoFromLND();
                    } catch (RejectedExecutionException ex) {
                        BBLog.d(LOG_TAG, "Execute of fetchFromLND() was rejected");
                    }
                }, 5000);
            }
        } else {
            // The wallet is not setup, simulate connection status. We don't want to show errors at this stage.
            Wallet.getInstance().simulateFetchInfoForDemo();
        }
    }
}
