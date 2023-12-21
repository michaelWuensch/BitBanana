package app.michaelwuensch.bitbanana.setup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.baseClasses.BaseScannerActivity;
import app.michaelwuensch.bitbanana.backendConfigs.BaseNodeConfig;
import app.michaelwuensch.bitbanana.listViews.backendConfigs.ManageBackendConfigsActivity;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.Wallet;

public class ConnectRemoteNodeActivity extends BaseScannerActivity {

    public static final String EXTRA_STARTED_FROM_URI = "startedFromURI";

    private static final String LOG_TAG = ConnectRemoteNodeActivity.class.getSimpleName();
    private String mWalletUUID;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(ManageBackendConfigsActivity.NODE_ID)) {
                mWalletUUID = extras.getString(ManageBackendConfigsActivity.NODE_ID);
            }
            if (extras.getBoolean(EXTRA_STARTED_FROM_URI, false)) {
                String connectString = App.getAppContext().getUriSchemeData();
                App.getAppContext().setUriSchemeData(null);
                verifyDesiredConnection(connectString);
            }
        }

        showButtonHelp();
    }

    @Override
    public void onButtonPasteClick() {
        super.onButtonPasteClick();

        String clipboardContent = "";
        boolean isClipboardContentValid = false;
        try {
            clipboardContent = ClipBoardUtil.getPrimaryContent(getApplicationContext(), true);
            isClipboardContentValid = true;
        } catch (NullPointerException e) {
            showError(getResources().getString(R.string.error_emptyClipboardConnect), RefConstants.ERROR_DURATION_SHORT);
        }
        if (isClipboardContentValid) {
            verifyDesiredConnection(clipboardContent);
        }
    }

    @Override
    public void onButtonInstructionsHelpClick() {
        HelpDialogUtil.showDialog(ConnectRemoteNodeActivity.this, R.string.help_dialog_scanConnectionInfo);
    }

    @Override
    public void handleCameraResult(String result) {
        super.handleCameraResult(result);
        verifyDesiredConnection(result);
    }

    @Override
    public void onButtonHelpClick() {
        super.onButtonHelpClick();
        HelpDialogUtil.showDialogWithLink(ConnectRemoteNodeActivity.this, R.string.help_dialog_connect_node_help, getResources().getString(R.string.documentation), RefConstants.URL_HELP_SETUP);
    }

    private void verifyDesiredConnection(String connectString) {

        RemoteConnectUtil.decodeConnectionString(this, connectString, new RemoteConnectUtil.OnRemoteConnectDecodedListener() {
            @Override
            public void onValidLndConnectString(BaseNodeConfig baseNodeConfig) {
                connectIfUserConfirms(baseNodeConfig);
            }

            @Override
            public void onValidBTCPayConnectData(BaseNodeConfig baseNodeConfig) {
                connectIfUserConfirms(baseNodeConfig);
            }

            @Override
            public void onNoConnectData() {
                showError(getResources().getString(R.string.error_connection_unsupported_format), RefConstants.ERROR_DURATION_LONG);
            }

            @Override
            public void onError(String error, int duration) {
                showError(error, duration);
            }
        });
    }


    private void connectIfUserConfirms(BaseNodeConfig baseNodeConfig) {
        // Ask user to confirm the connection to remote host
        new UserGuardian(this, () -> {
            connect(baseNodeConfig);
        }).securityConnectToRemoteServer(baseNodeConfig.getHost());
    }

    private void connect(BaseNodeConfig baseNodeConfig) {
        // Connect using the supplied configuration
        RemoteConnectUtil.saveRemoteConfiguration(ConnectRemoteNodeActivity.this, baseNodeConfig, mWalletUUID, new RemoteConnectUtil.OnSaveRemoteConfigurationListener() {

            @Override
            public void onSaved(String id) {

                // The configuration was saved. Now make it the currently active wallet.
                PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_NODE_CONFIG, id).commit();

                // Do not ask for pin again...
                TimeOutUtil.getInstance().restartTimer();

                // In case another wallet was open before, we want to have all values reset.
                Wallet.getInstance().reset();

                // Show home screen, remove history stack. Going to HomeActivity will initiate the connection to our new remote configuration.
                Intent intent = new Intent(ConnectRemoteNodeActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onAlreadyExists() {
                new AlertDialog.Builder(ConnectRemoteNodeActivity.this)
                        .setMessage(R.string.node_already_exists)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).show();
            }

            @Override
            public void onError(String error, int duration) {
                showError(error, duration);
            }
        });
    }

}
