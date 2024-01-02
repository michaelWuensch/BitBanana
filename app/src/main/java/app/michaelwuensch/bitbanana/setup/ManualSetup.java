package app.michaelwuensch.bitbanana.setup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import androidx.appcompat.widget.SwitchCompat;

import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backendConfigs.parseBackendConfig.lndConnect.LndConnectConfig;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.listViews.backendConfigs.ManageBackendConfigsActivity;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.UtilFunctions;
import app.michaelwuensch.bitbanana.util.Wallet;

public class ManualSetup extends BaseAppCompatActivity {

    private static final String LOG_TAG = ManualSetup.class.getSimpleName();

    private BBInputFieldView mEtHost;
    private BBInputFieldView mEtPort;
    private BBInputFieldView mEtMacaroon;
    private BBInputFieldView mEtCertificate;
    private SwitchCompat mSwTor;
    private SwitchCompat mSwVerify;
    private Button mBtnSave;
    private String mWalletUUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(ManageBackendConfigsActivity.NODE_ID)) {
                mWalletUUID = extras.getString(ManageBackendConfigsActivity.NODE_ID);
            }
        }

        setContentView(R.layout.activity_manual_setup);

        mEtHost = findViewById(R.id.inputHost);
        mEtPort = findViewById(R.id.inputPort);
        mEtMacaroon = findViewById(R.id.inputMacaroon);
        mEtCertificate = findViewById(R.id.inputCertificate);
        mSwTor = findViewById(R.id.torSwitch);
        mSwVerify = findViewById(R.id.verifyCertSwitch);
        mBtnSave = findViewById(R.id.saveButton);

        // Fill in vales if existing wallet is edited
        if (mWalletUUID != null) {
            BackendConfig BackendConfig = BackendConfigsManager.getInstance().getBackendConfigById(mWalletUUID);
            mEtHost.setValue(BackendConfig.getHost());
            mEtPort.setValue(String.valueOf(BackendConfig.getPort()));
            mEtMacaroon.setValue(BackendConfig.getMacaroon());
            mSwTor.setChecked(BackendConfig.getUseTor());
            if (BackendConfig.getUseTor()) {
                mSwVerify.setChecked(false);
                mSwVerify.setVisibility(View.GONE);
            } else {
                mSwVerify.setChecked(BackendConfig.getVerifyCertificate());
            }
            if (BackendConfig.getCert() != null && !BackendConfig.getCert().isEmpty()) {
                mEtCertificate.setValue(BackendConfig.getCert());
            }
        }

        mSwTor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mSwVerify.setChecked(false);
                    mSwVerify.setVisibility(View.GONE);
                } else {
                    mSwVerify.setChecked(true);
                    mSwVerify.setVisibility(View.VISIBLE);
                }
            }
        });

        mSwVerify.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (!mSwVerify.isChecked()) {
                    // user wants to disable certificate verification
                    mSwVerify.setChecked(true);
                    new UserGuardian(ManualSetup.this, new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onGuardianConfirmed() {
                            mSwVerify.setChecked(false);
                        }
                    }).securityCertificateVerification();
                }
            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEtHost.getData().isEmpty()) {
                    showError("Host must not be empty!", RefConstants.ERROR_DURATION_SHORT);
                    return;
                }
                if (mEtPort.getData().isEmpty()) {
                    showError("Port must not be empty!", RefConstants.ERROR_DURATION_SHORT);
                    return;
                }
                if (mEtMacaroon.getData().isEmpty()) {
                    showError("Macaroon must not be empty!", RefConstants.ERROR_DURATION_SHORT);
                    return;
                }
                if (!UtilFunctions.isHex(mEtMacaroon.getData())) {
                    showError("Macaroon must be provided in hex format!", RefConstants.ERROR_DURATION_SHORT);
                    return;
                }

                // everything is ok
                LndConnectConfig lndConnectConfig = new LndConnectConfig();
                lndConnectConfig.setHost(mEtHost.getData());
                lndConnectConfig.setPort(Integer.parseInt(mEtPort.getData()));
                lndConnectConfig.setMacaroon(mEtMacaroon.getData());
                lndConnectConfig.setUseTor(mSwTor.isChecked());
                lndConnectConfig.setVerifyCertificate(mSwVerify.isChecked());
                if (!mEtCertificate.getData().isEmpty()) {
                    lndConnectConfig.setCert(mEtCertificate.getData());
                }
                connect(lndConnectConfig);
            }
        });
    }

    private void connect(BaseBackendConfig baseBackendConfig) {
        // Connect using the supplied configuration
        RemoteConnectUtil.saveRemoteConfiguration(ManualSetup.this, baseBackendConfig, mWalletUUID, new RemoteConnectUtil.OnSaveRemoteConfigurationListener() {

            @Override
            public void onSaved(String id) {

                // The configuration was saved. Now make it the currently active wallet.
                PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_BACKEND_CONFIG, id).commit();

                // Do not ask for pin again...
                TimeOutUtil.getInstance().restartTimer();

                // In case another wallet was open before, we want to have all values reset.
                Wallet.getInstance().reset();

                // Show home screen, remove history stack. Going to HomeActivity will initiate the connection to our new remote configuration.
                Intent intent = new Intent(ManualSetup.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onAlreadyExists() {
                new AlertDialog.Builder(ManualSetup.this)
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.scanButton) {
            Intent intent = new Intent(ManualSetup.this, ConnectRemoteNodeActivity.class);
            intent.putExtra(ManageBackendConfigsActivity.NODE_ID, mWalletUUID);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}