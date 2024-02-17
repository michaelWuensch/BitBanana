package app.michaelwuensch.bitbanana.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.btcPay.BTCPayConfig;
import app.michaelwuensch.bitbanana.backendConfigs.btcPay.BTCPayConfigParser;
import app.michaelwuensch.bitbanana.backendConfigs.lndConnect.LndConnectConfig;
import app.michaelwuensch.bitbanana.backendConfigs.lndConnect.LndConnectStringParser;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class RemoteConnectUtil {

    private static final String LOG_TAG = RemoteConnectUtil.class.getSimpleName();

    public static void decodeConnectionString(Context ctx, String data, OnRemoteConnectDecodedListener listener) {
        if (data == null) {
            listener.onNoConnectData();
            return;
        }
        if (UriUtil.isLNDConnectUri(data)) {
            decodeLndConnectString(ctx, data, listener);
        } else if (data.startsWith("config=")) {
            // URL to BTCPayConfigJson
            String configUrl = data.replace("config=", "");

            Request btcPayConfigRequest = new Request.Builder()
                    .url(configUrl)
                    .build();

            HttpClient.getInstance().getClient().newCall(btcPayConfigRequest).enqueue(new Callback() {
                Handler threadHandler = new Handler(Looper.getMainLooper());

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    threadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(ctx.getResources().getString(R.string.error_unableToFetchBTCPayConfig), RefConstants.ERROR_DURATION_SHORT);
                        }
                    });
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    threadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                decodeBtcPay(ctx, response.body().string(), listener);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        } else if (BTCPayConfigParser.isValidJson(data)) {
            // Valid BTCPay JSON
            decodeBtcPay(ctx, data, listener);
        } else {
            listener.onNoConnectData();
        }
    }

    private static void decodeLndConnectString(Context ctx, String data, OnRemoteConnectDecodedListener listener) {
        LndConnectStringParser parser = new LndConnectStringParser(data).parse();

        if (parser.hasError()) {
            switch (parser.getError()) {
                case LndConnectStringParser.ERROR_INVALID_CONNECT_STRING:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_invalidLndConnectString), RefConstants.ERROR_DURATION_LONG);
                    break;
                case LndConnectStringParser.ERROR_NO_MACAROON:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_no_macaroon), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case LndConnectStringParser.ERROR_INVALID_CERTIFICATE:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_invalid_certificate), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case LndConnectStringParser.ERROR_INVALID_MACAROON:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_invalid_macaroon), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case LndConnectStringParser.ERROR_INVALID_HOST_OR_PORT:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_invalid_host_or_port), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
            }
        } else {
            listener.onValidLndConnectString(parser.getConnectionConfig());
        }
    }

    private static void decodeBtcPay(Context ctx, @NonNull String btcPayConfigurationJson, OnRemoteConnectDecodedListener listener) {
        BTCPayConfigParser btcPayConfigParser = new BTCPayConfigParser(btcPayConfigurationJson).parse();

        if (btcPayConfigParser.hasError()) {
            switch (btcPayConfigParser.getError()) {
                case BTCPayConfigParser.ERROR_INVALID_JSON:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_btcpay_invalid_json), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case BTCPayConfigParser.ERROR_MISSING_BTC_GRPC_CONFIG:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_btcpay_invalid_config), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case BTCPayConfigParser.ERROR_NO_MACAROON:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_no_macaroon), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
            }
        } else {
            // Parsing was successful
            listener.onValidBTCPayConnectData(btcPayConfigParser.getConnectionConfig());
        }
    }


    public static void saveRemoteConfiguration(Context ctx, BaseBackendConfig config, @Nullable String walletUUID, OnSaveRemoteConfigurationListener listener) {
        int port = config.getPort();
        if (port == 8080) {
            // BitBanana does not support REST. If the REST port was supplied, we ask the user if he wants to change it to 10009 (gRPC port).
            new AlertDialog.Builder(ctx)
                    .setMessage(R.string.rest_port)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            executeSaveRemoteConfiguration(config, walletUUID, 10009, listener);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            executeSaveRemoteConfiguration(config, walletUUID, port, listener);
                        }
                    }).show();
        } else {
            executeSaveRemoteConfiguration(config, walletUUID, port, listener);
        }
    }

    private static void executeSaveRemoteConfiguration(BaseBackendConfig config, @Nullable String walletUUID, int port, OnSaveRemoteConfigurationListener listener) {
        BackendConfigsManager backendConfigsManager = BackendConfigsManager.getInstance();

        try {
            if (config instanceof BackendConfig) {
                BackendConfig backendConfig = (BackendConfig) config;

                String id;
                if (walletUUID == null) {
                    backendConfig.setPort(port);
                    backendConfig.setLocation(BaseBackendConfig.LOCATION_REMOTE);
                    id = backendConfigsManager.addBackendConfig(backendConfig).getId();
                } else {
                    id = walletUUID;
                    BackendConfig backendConfig2 = backendConfigsManager.getBackendConfigById(id);
                    // All data that cannot be set on manual setup page has to be fetched from existing wallet.
                    backendConfig.setId(backendConfig2.getId());
                    backendConfig.setLocation(backendConfig2.getLocation());
                    backendConfig.setNetwork(backendConfig2.getNetwork());
                    backendConfig.setBackendType(backendConfig2.getBackendType());
                    backendConfigsManager.updateBackendConfig(backendConfig);
                }

                backendConfigsManager.apply();

                listener.onSaved(id);

            } else if (config instanceof LndConnectConfig) {
                LndConnectConfig lndConnectConfig = (LndConnectConfig) config;

                String id;
                if (walletUUID == null) {

                    BackendConfig configToAdd = new BackendConfig();
                    configToAdd.setHost(lndConnectConfig.getHost());
                    configToAdd.setPort(port);
                    configToAdd.setLocation(BaseBackendConfig.LOCATION_REMOTE);
                    configToAdd.setBackendType(BaseBackendConfig.BACKEND_TYPE_LND_GRPC);
                    configToAdd.setNetwork(lndConnectConfig.getNetwork());
                    configToAdd.setCert(lndConnectConfig.getCert());
                    configToAdd.setMacaroon(lndConnectConfig.getMacaroon());
                    configToAdd.setUseTor(lndConnectConfig.getUseTor());
                    configToAdd.setVerifyCertificate(lndConnectConfig.getVerifyCertificate());

                    id = backendConfigsManager.addBackendConfig(configToAdd).getId();
                } else {
                    id = walletUUID;
                    BackendConfig backendConfig = backendConfigsManager.getBackendConfigById(id);
                    backendConfig.setBackendType(BaseBackendConfig.BACKEND_TYPE_LND_GRPC);
                    backendConfig.setHost(lndConnectConfig.getHost());
                    backendConfig.setPort(port);
                    backendConfig.setCert(lndConnectConfig.getCert());
                    backendConfig.setMacaroon(lndConnectConfig.getMacaroon());
                    backendConfig.setUseTor(lndConnectConfig.getUseTor());
                    backendConfig.setVerifyCertificate(lndConnectConfig.getVerifyCertificate());
                    // id, alias, location, network and VPNConfig stay the same as this info is not available
                    backendConfigsManager.updateBackendConfig(backendConfig);
                }

                backendConfigsManager.apply();

                listener.onSaved(id);

            } else if (config instanceof BTCPayConfig) {
                BTCPayConfig btcPayConfig = (BTCPayConfig) config;

                String id;
                if (walletUUID == null) {

                    BackendConfig configToAdd = new BackendConfig();
                    configToAdd.setHost(btcPayConfig.getHost());
                    configToAdd.setPort(port);
                    configToAdd.setLocation(BaseBackendConfig.LOCATION_REMOTE);
                    configToAdd.setBackendType(BaseBackendConfig.BACKEND_TYPE_LND_GRPC);
                    configToAdd.setNetwork(btcPayConfig.getNetwork());
                    configToAdd.setCert(null);
                    configToAdd.setMacaroon(btcPayConfig.getMacaroon());
                    configToAdd.setUseTor(btcPayConfig.getUseTor());
                    configToAdd.setVerifyCertificate(btcPayConfig.getVerifyCertificate());
                    configToAdd.setVpnConfig(new VPNConfig());

                    id = backendConfigsManager.addBackendConfig(configToAdd).getId();
                } else {
                    id = walletUUID;
                    BackendConfig backendConfig = backendConfigsManager.getBackendConfigById(id);
                    backendConfig.setBackendType(BaseBackendConfig.BACKEND_TYPE_LND_GRPC);
                    backendConfig.setNetwork(btcPayConfig.getNetwork());
                    backendConfig.setHost(btcPayConfig.getHost());
                    backendConfig.setPort(port);
                    backendConfig.setCert(null);
                    backendConfig.setMacaroon(btcPayConfig.getMacaroon());
                    backendConfig.setUseTor(btcPayConfig.getUseTor());
                    backendConfig.setVerifyCertificate(btcPayConfig.getVerifyCertificate());
                    // id, alias, location and VPNConfig stay the same as this info is not available
                    backendConfigsManager.updateBackendConfig(backendConfig);
                }

                backendConfigsManager.apply();

                listener.onSaved(id);

            }
        } catch (Exception e) {
            e.printStackTrace();
            listener.onError(e.getMessage(), RefConstants.ERROR_DURATION_SHORT);
        }
    }

    public static boolean isTorHostAddress(String Host) {
        if (Host == null) {
            return false;
        }
        return Host.toLowerCase().endsWith(".onion");
    }


    public interface OnRemoteConnectDecodedListener {

        void onValidLndConnectString(BaseBackendConfig baseBackendConfig);

        void onValidBTCPayConnectData(BaseBackendConfig baseBackendConfig);

        void onNoConnectData();

        void onError(String error, int duration);
    }

    public interface OnSaveRemoteConfigurationListener {

        void onSaved(String walletId);

        void onError(String error, int duration);
    }
}
