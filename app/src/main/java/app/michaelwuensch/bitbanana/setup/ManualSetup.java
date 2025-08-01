package app.michaelwuensch.bitbanana.setup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backendConfigs.nostrWalletConnect.NostrWalletConnectUrlParser;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.vpn.VPNUtil;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.customView.BBPasswordInputFieldView;
import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.listViews.backendConfigs.ManageBackendConfigsActivity;
import app.michaelwuensch.bitbanana.util.AppLockUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.HexUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.inputFilters.InputFilterPortRange;
import app.michaelwuensch.bitbanana.wallet.QuickReceiveConfig;

public class ManualSetup extends BaseAppCompatActivity {

    private static final String LOG_TAG = ManualSetup.class.getSimpleName();

    private BBInputFieldView mEtName;
    private BBInputFieldView mEtFullConnectString;
    private BBInputFieldView mEtHost;
    private BBInputFieldView mEtPort;
    private BBInputFieldView mEtAuthenticationToken;
    private BBInputFieldView mEtServerCertificate;
    private BBInputFieldView mEtClientCertificate;
    private BBInputFieldView mEtClientKey;
    private BBInputFieldView mEtUser;
    private View mVpnAutomationLayout;
    private BBPasswordInputFieldView mEtPassword;
    private SwitchCompat mSwTor;
    private SwitchCompat mSwVerify;
    private VPNConfigView mVpnConfigView;
    private BBButton mBtnSave;
    private ImageButton mVpnHelpButton;
    private String mWalletUUID;
    private BackendConfig mOriginalBackendConfig;
    private View mSpTypeLayout;
    private Spinner mSpType;
    private View mVerifyCertVisibilityLayout;

    private int mPreviousSpinnerPosition = 0;

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

        mEtName = findViewById(R.id.inputName);
        mEtFullConnectString = findViewById(R.id.inputFullConnectString);
        mEtHost = findViewById(R.id.inputHost);
        mEtPort = findViewById(R.id.inputPort);
        mEtAuthenticationToken = findViewById(R.id.inputAuthenticationToken);
        mEtServerCertificate = findViewById(R.id.inputServerCertificate);
        mEtClientCertificate = findViewById(R.id.inputClientCertificate);
        mEtClientKey = findViewById(R.id.inputClientKey);
        mEtUser = findViewById(R.id.inputUser);
        mEtPassword = findViewById(R.id.inputPassword);
        mSwTor = findViewById(R.id.torSwitch);
        mSwVerify = findViewById(R.id.verifyCertSwitch);
        mVpnAutomationLayout = findViewById(R.id.vpnAutomationLayout);
        mVpnConfigView = findViewById(R.id.vpnConfigView);
        mBtnSave = findViewById(R.id.saveButton);
        mVpnHelpButton = findViewById(R.id.vpnHelpButton);
        mSpTypeLayout = findViewById(R.id.typeSpinnerLayout);
        mSpType = findViewById(R.id.typeSpinner);
        mVerifyCertVisibilityLayout = findViewById(R.id.verifyCertVisibilityLayout);

        mEtName.setSingleLine(true);
        mEtHost.setSingleLine(true);
        mEtPort.getEditText().setFilters(new InputFilter[]{new InputFilterPortRange()});
        mEtPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String[] items = new String[4];
        items[0] = BackendConfig.BackendType.LND_GRPC.getDisplayName();
        items[1] = BackendConfig.BackendType.CORE_LIGHTNING_GRPC.getDisplayName();
        items[2] = BackendConfig.BackendType.LND_HUB.getDisplayName();
        items[3] = BackendConfig.BackendType.NOSTR_WALLET_CONNECT.getDisplayName();

        mSpType.setAdapter(new ArrayAdapter<String>(this, R.layout.spinner_item, items));
        mSpType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        // Lnd gRPC
                        mEtFullConnectString.setVisibility(View.GONE);
                        mEtHost.setVisibility(View.VISIBLE);
                        mEtPort.setVisibility(View.VISIBLE);
                        mEtAuthenticationToken.setVisibility(View.VISIBLE);
                        mEtServerCertificate.setVisibility(View.VISIBLE);
                        mEtClientCertificate.setVisibility(View.GONE);
                        mEtClientKey.setVisibility(View.GONE);
                        mEtUser.setVisibility(View.GONE);
                        mEtPassword.setVisibility(View.GONE);
                        mVpnAutomationLayout.setVisibility(View.VISIBLE);
                        mSwTor.setVisibility(View.VISIBLE);
                        mVerifyCertVisibilityLayout.setVisibility(View.VISIBLE);
                        mPreviousSpinnerPosition = 0;
                        break;
                    case 1:
                        // Core Lightning gRPC
                        mEtFullConnectString.setVisibility(View.GONE);
                        mEtHost.setVisibility(View.VISIBLE);
                        mEtPort.setVisibility(View.VISIBLE);
                        mEtAuthenticationToken.setVisibility(View.GONE);
                        mEtServerCertificate.setVisibility(View.VISIBLE);
                        mEtClientCertificate.setVisibility(View.VISIBLE);
                        mEtClientKey.setVisibility(View.VISIBLE);
                        mEtUser.setVisibility(View.GONE);
                        mEtPassword.setVisibility(View.GONE);
                        mVpnAutomationLayout.setVisibility(View.VISIBLE);
                        mSwTor.setVisibility(View.VISIBLE);
                        mVerifyCertVisibilityLayout.setVisibility(View.VISIBLE);
                        mPreviousSpinnerPosition = 1;
                        break;
                    case 2:
                        // Lnd Hub
                        mEtFullConnectString.setVisibility(View.GONE);
                        mEtHost.setVisibility(View.VISIBLE);
                        mEtPort.setVisibility(View.GONE);
                        mEtAuthenticationToken.setVisibility(View.GONE);
                        mEtServerCertificate.setVisibility(View.GONE);
                        mEtClientCertificate.setVisibility(View.GONE);
                        mEtClientKey.setVisibility(View.GONE);
                        mEtUser.setVisibility(View.VISIBLE);
                        mEtPassword.setVisibility(View.VISIBLE);
                        mVpnAutomationLayout.setVisibility(View.VISIBLE);
                        mSwTor.setVisibility(View.VISIBLE);
                        mVerifyCertVisibilityLayout.setVisibility(View.GONE);
                        mPreviousSpinnerPosition = 2;
                        break;
                    case 3:
                        // Nostr Wallet Connect
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            showError(getString(R.string.error_nwc_min_android_version), RefConstants.ERROR_DURATION_MEDIUM);
                            mSpType.setSelection(mPreviousSpinnerPosition);
                        } else {
                            mEtFullConnectString.setVisibility(View.VISIBLE);
                            mEtHost.setVisibility(View.GONE);
                            mEtPort.setVisibility(View.GONE);
                            mEtAuthenticationToken.setVisibility(View.GONE);
                            mEtServerCertificate.setVisibility(View.GONE);
                            mEtClientCertificate.setVisibility(View.GONE);
                            mEtClientKey.setVisibility(View.GONE);
                            mEtUser.setVisibility(View.GONE);
                            mEtPassword.setVisibility(View.GONE);
                            mVpnAutomationLayout.setVisibility(View.VISIBLE);
                            mSwTor.setVisibility(View.GONE);
                            mVerifyCertVisibilityLayout.setVisibility(View.GONE);

                            mEtFullConnectString.setDescription("Nostr Wallet Connect URL");
                            mEtFullConnectString.getEditText().setHint("nostr+walletconnect://");
                            mPreviousSpinnerPosition = 3;
                        }
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Fill in values if existing wallet is edited
        if (mWalletUUID != null) {
            BackendConfig BackendConfig = BackendConfigsManager.getInstance().getBackendConfigById(mWalletUUID);
            mOriginalBackendConfig = BackendConfig;
            switch (BackendConfig.getBackendType()) {
                case LND_GRPC:
                    mSpType.setSelection(0);
                    break;
                case CORE_LIGHTNING_GRPC:
                    mSpType.setSelection(1);
                    break;
                case LND_HUB:
                    mSpType.setSelection(2);
                    break;
                case NOSTR_WALLET_CONNECT:
                    mSpType.setSelection(3);
                    break;
            }
            mEtName.setValue(BackendConfig.getAlias());
            mEtFullConnectString.setValue(BackendConfig.getFullConnectString());
            mEtHost.setValue(BackendConfig.getHost());
            mEtPort.setValue(String.valueOf(BackendConfig.getPort()));
            mEtAuthenticationToken.setValue(BackendConfig.getAuthenticationToken());
            mSwTor.setChecked(BackendConfig.getUseTor());
            mVpnConfigView.setupWithVpnConfig(BackendConfig.getVpnConfig());
            if (BackendConfig.getUseTor()) {
                mSwVerify.setChecked(false);
                mSwVerify.setVisibility(View.GONE);
            } else {
                mSwVerify.setChecked(BackendConfig.getVerifyCertificate());
            }
            if (BackendConfig.getServerCert() != null && !BackendConfig.getServerCert().isEmpty()) {
                mEtServerCertificate.setValue(HexUtil.bytesToHex(BaseEncoding.base64().decode(BackendConfig.getServerCert())));
            }
            if (BackendConfig.getClientCert() != null && !BackendConfig.getClientCert().isEmpty()) {
                mEtClientCertificate.setValue(HexUtil.bytesToHex(BaseEncoding.base64().decode(BackendConfig.getClientCert())));
            }
            if (BackendConfig.getClientKey() != null && !BackendConfig.getClientKey().isEmpty()) {
                mEtClientKey.setValue(HexUtil.bytesToHex(BaseEncoding.base64().decode(BackendConfig.getClientKey())));
            }
            mEtUser.setValue(BackendConfig.getUser());
            mEtPassword.setValue(BackendConfig.getPassword());

            // Delay is necessary to hide the Type Spinner. Without set selection function doesn't work.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSpTypeLayout.setVisibility(View.GONE);
                }
            }, 1);

        } else {
            mVpnConfigView.setupWithVpnConfig(null); // This makes sure start on open and stop on close are set to true;
            mSpType.setSelection(0);
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
                        public void onConfirmed() {
                            mSwVerify.setChecked(false);
                        }

                        @Override
                        public void onCancelled() {

                        }
                    }).securityCertificateVerification();
                }
            }
        });

        mVpnHelpButton.setVisibility(FeatureManager.isHelpButtonsEnabled() ? View.VISIBLE : View.GONE);
        mVpnHelpButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                HelpDialogUtil.showDialog(ManualSetup.this, R.string.help_dialog_vpn_automation);
            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });
    }

    private BackendConfig getBackendConfig() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setSource(BackendConfig.Source.MANUAL_INPUT);
        backendConfig.setAlias(mEtName.getData());
        switch (mSpType.getSelectedItemPosition()) {
            case 0:
                backendConfig.setBackendType(BackendConfig.BackendType.LND_GRPC);
                break;
            case 1:
                backendConfig.setBackendType(BackendConfig.BackendType.CORE_LIGHTNING_GRPC);
                break;
            case 2:
                backendConfig.setBackendType(BackendConfig.BackendType.LND_HUB);
                break;
            case 3:
                backendConfig.setBackendType(BackendConfig.BackendType.NOSTR_WALLET_CONNECT);
                // For a freshly added NWC connection we want to auto add quick connection if lud16 is set.
                if (mOriginalBackendConfig == null) {
                    NostrWalletConnectUrlParser tempParser = new NostrWalletConnectUrlParser(mEtFullConnectString.getData()).parse();
                    if (!tempParser.hasError()) {
                        QuickReceiveConfig tempQuickReceiveConfig = tempParser.getBackendConfig().getQuickReceiveConfig();
                        if (tempQuickReceiveConfig.getQuickReceiveType() != QuickReceiveConfig.QuickReceiveType.OFF) {
                            backendConfig.setQuickReceiveConfig(tempQuickReceiveConfig);
                        }
                    }
                }
                break;
        }
        backendConfig.setHost(mEtHost.getData());
        backendConfig.setFullConnectString(mEtFullConnectString.getData());
        try {
            backendConfig.setPort(Integer.parseInt(mEtPort.getData()));
        } catch (Exception ignored) {
        }
        backendConfig.setAuthenticationToken(mEtAuthenticationToken.getData());
        backendConfig.setUseTor(mSwTor.isChecked());
        backendConfig.setVerifyCertificate(mSwVerify.isChecked());
        backendConfig.setVpnConfig(mVpnConfigView.getVPNConfig());
        backendConfig.setServerCert(mEtServerCertificate.getData());
        backendConfig.setClientCert(mEtClientCertificate.getData());
        backendConfig.setClientKey(mEtClientKey.getData());
        backendConfig.setUser(mEtUser.getData());
        backendConfig.setPassword(mEtPassword.getData());
        if (mOriginalBackendConfig != null) {
            // Add all data that cannot be set manually
            backendConfig.setId(mOriginalBackendConfig.getId());
            backendConfig.setNetwork(mOriginalBackendConfig.getNetwork());
            backendConfig.setLocation(mOriginalBackendConfig.getLocation());
            backendConfig.setTempAccessToken(mOriginalBackendConfig.getTempAccessToken());
            backendConfig.setTempRefreshToken(mOriginalBackendConfig.getTempRefreshToken());
            backendConfig.setWalletID(mOriginalBackendConfig.getWalletID());
            backendConfig.setAvatarMaterial(mOriginalBackendConfig.getAvatarMaterial());
            // If we are editing a NWC connection we always want to keep the quick receive settings. The place to edit these is in the receive dialog.
            if (mOriginalBackendConfig.hasQuickReceiveConfig())
                backendConfig.setQuickReceiveConfig(mOriginalBackendConfig.getQuickReceiveConfig());
            if (mOriginalBackendConfig.hasAddedInEmergencyMode())
                backendConfig.setAddedInEmergencyMode(mOriginalBackendConfig.wasAddedInEmergencyMode());
        } else {
            backendConfig.setAddedInEmergencyMode(AppLockUtil.isEmergencyUnlocked);
        }
        return backendConfig;
    }

    private void save() {
        if (mEtName.getData() == null || mEtName.getData().isEmpty()) {
            showError(getString(R.string.error_input_field_empty, getString(R.string.name)), RefConstants.ERROR_DURATION_SHORT);
            return;
        }

        if (mSpType.getSelectedItemPosition() == 0) {
            // LND grpc

            if (mEtHost.getData() == null || mEtHost.getData().isEmpty()) {
                showError(getString(R.string.error_input_field_empty, getString(R.string.host)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }

            if (mEtPort.getData() == null || mEtPort.getData().isEmpty()) {
                showError(getString(R.string.error_input_field_empty, getString(R.string.port)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }

            if ((mEtAuthenticationToken.getData() == null || mEtAuthenticationToken.getData().isEmpty())) {
                showError(getString(R.string.error_input_field_empty, getString(R.string.macaroon)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }
            if (!HexUtil.isHex(mEtAuthenticationToken.getData())) {
                showError(getString(R.string.error_input_macaroon_hex), RefConstants.ERROR_DURATION_SHORT);
                return;
            }
        }

        if (mSpType.getSelectedItemPosition() == 1) {
            // CoreLightning grpc
            if (mEtHost.getData() == null || mEtHost.getData().isEmpty()) {
                showError(getString(R.string.error_input_field_empty, getString(R.string.host)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }

            if (mEtPort.getData() == null || mEtPort.getData().isEmpty()) {
                showError(getString(R.string.error_input_field_empty, getString(R.string.port)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }

            if ((mEtClientCertificate.getData() == null || mEtClientCertificate.getData().isEmpty())) {
                if (mEtClientCertificate.getEditText().getText().toString().isEmpty())
                    showError(getString(R.string.error_input_field_empty, getString(R.string.client_certificate)), RefConstants.ERROR_DURATION_SHORT);
                else
                    showError(getString(R.string.error_input_invalid_data, getString(R.string.client_certificate)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }
            if ((mEtClientKey.getData() == null || mEtClientKey.getData().isEmpty())) {
                if (mEtClientKey.getEditText().getText().toString().isEmpty())
                    showError(getString(R.string.error_input_field_empty, getString(R.string.client_key)), RefConstants.ERROR_DURATION_SHORT);
                else
                    showError(getString(R.string.error_input_invalid_data, getString(R.string.client_key)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }
        }

        if (mSpType.getSelectedItemPosition() == 2) {
            // LNDHub
            if (mEtHost.getData() == null || mEtHost.getData().isEmpty()) {
                showError(getString(R.string.error_input_field_empty, getString(R.string.host)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }

            if ((mEtUser.getData() == null || mEtUser.getData().isEmpty())) {
                showError(getString(R.string.error_input_field_empty, getString(R.string.username)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }
            if ((mEtPassword.getData() == null || mEtPassword.getData().isEmpty())) {
                showError(getString(R.string.error_input_field_empty, getString(R.string.password)), RefConstants.ERROR_DURATION_SHORT);
                return;
            }
        }

        if (mSpType.getSelectedItemPosition() == 3) {
            if (mEtFullConnectString.getData() == null || mEtFullConnectString.getData().isEmpty()) {
                showError(getString(R.string.error_input_field_empty, "Nostr Wallet Connect String"), RefConstants.ERROR_DURATION_SHORT);
                return;
            }
            if (new NostrWalletConnectUrlParser(mEtFullConnectString.getData()).parse().hasError()) {
                showError(getString(R.string.error_connection_invalidNostrWalletConnectString), RefConstants.ERROR_DURATION_MEDIUM);
                return;
            }
        }


        // everything is ok
        BackendConfig backendConfig = getBackendConfig();

        RemoteConnectUtil.saveRemoteConfiguration(ManualSetup.this, backendConfig, mWalletUUID, new RemoteConnectUtil.OnSaveRemoteConfigurationListener() {

            @Override
            public void onSaved(String id) {

                // The configuration was saved. Now make it the currently active wallet.
                PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_BACKEND_CONFIG, id).commit();

                // Do not ask for pin again...
                TimeOutUtil.getInstance().restartTimer();

                // Show home screen, remove history stack. Going to HomeActivity will initiate the connection to our new remote configuration.
                Intent intent = new Intent(ManualSetup.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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
        } else if (id == android.R.id.home) {
            onBackPressed();
            return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mWalletUUID != null) {
            // we are in edit mode
            String original = new Gson().toJson(mOriginalBackendConfig);
            String actual = new Gson().toJson(getBackendConfig());

            if (!original.equals(actual))
                new AlertDialog.Builder(this)
                        .setMessage(R.string.unsaved_changes)
                        .setCancelable(true)
                        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                save();
                            }
                        })
                        .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ManualSetup.super.onBackPressed();
                            }
                        })
                        .show();
            else
                ManualSetup.super.onBackPressed();
        } else {
            // we are in add manually mode
            if (mEtName.getData() != null ||
                    mEtFullConnectString.getData() != null ||
                    mEtHost.getData() != null ||
                    mEtPort.getData() != null ||
                    mEtAuthenticationToken.getData() != null ||
                    mEtServerCertificate.getData() != null ||
                    mEtClientCertificate.getData() != null ||
                    mEtClientKey.getData() != null ||
                    mEtUser.getData() != null ||
                    mEtPassword.getData() != null) {

                new AlertDialog.Builder(this)
                        .setMessage(R.string.unsaved_changes)
                        .setCancelable(true)
                        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                save();
                            }
                        })
                        .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ManualSetup.super.onBackPressed();
                            }
                        })
                        .show();
            } else {
                ManualSetup.super.onBackPressed();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == VPNUtil.PERMISSION_WIREGUARD_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(VPNUtil.PERMISSION_WIREGUARD)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        mVpnConfigView.updateView(mVpnConfigView.getVPNConfig().getVpnType());
                    }
                }
            }
        }
    }
}