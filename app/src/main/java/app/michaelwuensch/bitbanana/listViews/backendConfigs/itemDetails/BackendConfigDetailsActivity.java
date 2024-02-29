package app.michaelwuensch.bitbanana.listViews.backendConfigs.itemDetails;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.view.menu.MenuBuilder;

import app.michaelwuensch.bitbanana.LandingActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.listViews.backendConfigs.ManageBackendConfigsActivity;
import app.michaelwuensch.bitbanana.setup.ManualSetup;
import app.michaelwuensch.bitbanana.util.BackendSwitcher;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;


public class BackendConfigDetailsActivity extends BaseAppCompatActivity {


    private String mId;
    private InputMethodManager mInputMethodManager;
    private String mDuplicateName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_details);

        mInputMethodManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mId = extras.getString(ManageBackendConfigsActivity.NODE_ID);
        }

        loadBackendConfigData();

        Button switchBtn = findViewById(R.id.buttonActivate);
        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_BACKEND_CONFIG, mId).commit();

                // Do not ask for pin again...
                TimeOutUtil.getInstance().restartTimer();

                // This will automatically open the current connection previously saved in the shared prefs
                openHome();
            }
        });

        TextView tvWalletName = findViewById(R.id.nodeName);
        tvWalletName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWalletNameInput(false);
            }
        });
    }

    private void loadBackendConfigData() {
        // Wallet name
        TextView tvWalletName = findViewById(R.id.nodeName);
        tvWalletName.setText(getWalletConfig().getAlias());

        // Wallet type
        ImageView ivTypeIcon = findViewById(R.id.nodeTypeIcon);
        if (getWalletConfig().isLocal()) {
            ivTypeIcon.setImageResource(R.drawable.ic_local_black_24dp);
        } else {
            ivTypeIcon.setImageResource(R.drawable.ic_remote_black_24dp);
        }

        // Connection Data
        View vConnectionData = findViewById(R.id.connectionDataBox);
        if (getWalletConfig().isLocal()) {
            vConnectionData.setVisibility(View.GONE);
        } else {
            vConnectionData.setVisibility(View.VISIBLE);
            TextView tvTypeLabel = findViewById(R.id.typeLabel);
            tvTypeLabel.setText(getResources().getString(R.string.type) + ":");
            TextView tvType = findViewById(R.id.type);
            tvType.setText(getWalletConfig().getBackendType().getDisplayName());
            TextView tvHostLabel = findViewById(R.id.hostLabel);
            tvHostLabel.setText(getResources().getString(R.string.host) + ":");
            TextView tvHost = findViewById(R.id.host);
            tvHost.setText(getWalletConfig().getHost());
            TextView tvPortLabel = findViewById(R.id.portLabel);
            tvPortLabel.setText(getResources().getString(R.string.port) + ":");
            TextView tvPort = findViewById(R.id.port);
            tvPort.setText(String.valueOf(getWalletConfig().getPort()));
            TextView tvMacaroonLabel = findViewById(R.id.macaroonLabel);
            tvMacaroonLabel.setText(getResources().getString(R.string.macaroon) + ":");
            TextView tvMacaroon = findViewById(R.id.macaroon);
            tvMacaroon.setText(getWalletConfig().getMacaroon());
            TextView tvCertificateLabel = findViewById(R.id.certLabel);
            if (getWalletConfig().getServerCert() != null) {
                tvCertificateLabel.setText(getResources().getString(R.string.certificate) + ":");
                tvCertificateLabel.setVisibility(View.VISIBLE);
                TextView tvCertificate = findViewById(R.id.cert);
                tvCertificate.setVisibility(View.VISIBLE);
                tvCertificate.setText(getWalletConfig().getServerCert());
            } else {
                tvCertificateLabel.setVisibility(View.GONE);
                TextView tvCertificate = findViewById(R.id.cert);
                tvCertificate.setVisibility(View.GONE);
            }
            TextView tvVpnLabel = findViewById(R.id.vpnLabel);
            tvVpnLabel.setText(getResources().getString(R.string.vpn) + ":");
            TextView tvVpn = findViewById(R.id.vpn);
            tvVpn.setText(getWalletConfig().getVpnConfig().getVpnType().getDisplayName());
            TextView tvTorLabel = findViewById(R.id.torLabel);
            tvTorLabel.setText(getResources().getString(R.string.settings_tor) + ":");
            TextView tvTor = findViewById(R.id.tor);
            tvTor.setText(getWalletConfig().getUseTor() ? R.string.yes : R.string.no);

            Button changeBtn = findViewById(R.id.buttonChangeConnection);
            changeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(BackendConfigDetailsActivity.this, ManualSetup.class);
                    intent.putExtra(ManageBackendConfigsActivity.NODE_ID, mId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
            });
        }
    }

    private void showWalletNameInput(boolean duplicate) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        if (duplicate)
            adb.setTitle(R.string.name_duplicate);
        else
            adb.setTitle(R.string.node_name);
        adb.setCancelable(false);
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_input_text, null, false);

        final EditText input = viewInflated.findViewById(R.id.input);
        input.setShowSoftInputOnFocus(true);
        input.setText(getWalletConfig().getAlias());
        input.requestFocus();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        }, 250);


        adb.setView(viewInflated);

        adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // This gets overridden below.
                // We need to do this to validate the input without closing the dialog.
            }
        });
        adb.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                mInputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                dialog.cancel();
            }
        });

        AlertDialog dialog = adb.create();
        dialog.show();
        Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (input.getText().toString().trim().isEmpty()) {
                    Toast.makeText(BackendConfigDetailsActivity.this, R.string.error_empty_node_name, Toast.LENGTH_LONG).show();
                } else {
                    if (duplicate) {
                        mDuplicateName = input.getText().toString().trim();
                        duplicate();
                    } else {
                        BackendConfigsManager backendConfigsManager = BackendConfigsManager.getInstance();
                        backendConfigsManager.renameBackendConfig(BackendConfigsManager.getInstance().getBackendConfigById(mId), input.getText().toString());
                        try {
                            backendConfigsManager.apply();
                            TextView tvWalletName = findViewById(R.id.nodeName);
                            tvWalletName.setText(input.getText().toString().trim());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    mInputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    dialog.dismiss();
                }
            }
        });
    }

    private BackendConfig getWalletConfig() {
        return BackendConfigsManager.getInstance().getBackendConfigById(mId);
    }

    private void openHome() {
        // Open home and clear history
        Intent intent = new Intent(BackendConfigDetailsActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void duplicate() {
        BackendConfig tempConfig = getWalletConfig().getCopy();
        tempConfig.setAlias(mDuplicateName);
        RemoteConnectUtil.saveRemoteConfiguration(BackendConfigDetailsActivity.this, tempConfig, null, new RemoteConnectUtil.OnSaveRemoteConfigurationListener() {
            @Override
            public void onSaved(String walletId) {
                mId = walletId;
                loadBackendConfigData();
                Toast.makeText(BackendConfigDetailsActivity.this, R.string.duplication_successful, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error, int duration) {
                showError(getString(R.string.error_duplication), RefConstants.ERROR_DURATION_SHORT);
            }
        });
    }

    private void actionDelete() {
        new AlertDialog.Builder(BackendConfigDetailsActivity.this)
                .setMessage(R.string.confirm_node_deletion)
                .setCancelable(true)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        deleteBackendConfig();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    private void deleteBackendConfig() {
        BackendConfigsManager backendConfigsManager = BackendConfigsManager.getInstance();
        backendConfigsManager.removeBackendConfig(BackendConfigsManager.getInstance().getBackendConfigById(mId));
        try {
            backendConfigsManager.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (PrefsUtil.getCurrentBackendConfig().equals(mId)) {
            // Current active backend is deleted...
            BackendSwitcher.deactivateCurrentBackendConfig(this, false, false);
            PrefsUtil.editPrefs().remove(PrefsUtil.CURRENT_BACKEND_CONFIG).commit();
            Intent intent = new Intent(BackendConfigDetailsActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            finish();
        }
    }

    @Override
    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.backend_config_details_menu, menu);

        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;

            //noinspection RestrictedApi
            m.setOptionalIconsVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_rename:
                showWalletNameInput(false);
                break;
            case R.id.action_duplicate:
                showWalletNameInput(true);
                break;
            case R.id.action_delete:
                actionDelete();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
