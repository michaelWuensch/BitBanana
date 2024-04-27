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
import app.michaelwuensch.bitbanana.backendConfigs.btcPay.BTCPayConfigParser;
import app.michaelwuensch.bitbanana.backendConfigs.coreLightning.CoreLightningConnectStringParser;
import app.michaelwuensch.bitbanana.backendConfigs.lndConnect.LndConnectStringParser;
import app.michaelwuensch.bitbanana.backendConfigs.lndHub.LndHubConnectStringParser;
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
        } else if (UriUtil.isCoreLightningGRPCUri(data)) {
            decodeCoreLightningConnectString(ctx, data, listener);
        } else if (UriUtil.isCLightningRestUri(data)) {
            listener.onError(ctx.getResources().getString(R.string.error_connection_no_c_lightning_rest_support), RefConstants.ERROR_DURATION_LONG);
        } else if (UriUtil.isLNDHUBUri(data)) {
            decodeLndHubConnectString(ctx, data, listener);
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
            listener.onValidConnectData(parser.getBackendConfig());
        }
    }

    private static void decodeCoreLightningConnectString(Context ctx, String data, OnRemoteConnectDecodedListener listener) {
        CoreLightningConnectStringParser parser = new CoreLightningConnectStringParser(data).parse();
        // ToDo: nice error messages
        if (parser.hasError()) {
            switch (parser.getError()) {
                case CoreLightningConnectStringParser.ERROR_INVALID_CONNECT_STRING:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_clngrpc_invalidConnectString), RefConstants.ERROR_DURATION_LONG);
                    break;
                case CoreLightningConnectStringParser.ERROR_INVALID_HOST_OR_PORT:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_invalid_host_or_port), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case CoreLightningConnectStringParser.ERROR_NO_CLIENT_CERTIFICATE:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_clngrpc_missingClientCert), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case CoreLightningConnectStringParser.ERROR_NO_CLIENT_KEY:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_clngrpc_missingClientKey), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case CoreLightningConnectStringParser.ERROR_INVALID_CERTIFICATE_OR_KEY:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_clngrpc_invalidCertificateOrKey), RefConstants.ERROR_DURATION_MEDIUM);
                    break;
                case CoreLightningConnectStringParser.ERROR_PARAMETER_DECODING_FAILED:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_clngrpc_parameterDecodingFailed), RefConstants.ERROR_DURATION_LONG);
                    break;
            }
        } else {
            listener.onValidConnectData(parser.getBackendConfig());
        }
    }

    private static void decodeLndHubConnectString(Context ctx, String data, OnRemoteConnectDecodedListener listener) {
        LndHubConnectStringParser parser = new LndHubConnectStringParser(data).parse();

        if (parser.hasError()) {
            switch (parser.getError()) {
                case LndHubConnectStringParser.ERROR_INVALID_CONNECT_STRING:
                    listener.onError(ctx.getResources().getString(R.string.error_connection_invalidLndHubConnectString), RefConstants.ERROR_DURATION_LONG);
                    break;
            }
        } else {
            listener.onValidConnectData(parser.getBackendConfig());
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
            listener.onValidConnectData(btcPayConfigParser.getBackendConfig());
        }
    }


    public static void saveRemoteConfiguration(Context ctx, BackendConfig config, @Nullable String walletUUID, OnSaveRemoteConfigurationListener listener) {
        int port = config.getPort();
        if (config.getBackendType() == BackendConfig.BackendType.LND_GRPC && port == 8080) {
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

    private static void executeSaveRemoteConfiguration(BackendConfig config, @Nullable String walletUUID, int port, OnSaveRemoteConfigurationListener listener) {
        BackendConfigsManager backendConfigsManager = BackendConfigsManager.getInstance();

        try {
            String id;
            if (walletUUID == null) {
                config.setPort(port);
                if (config.getAlias() == null)
                    config.setAlias(config.getHost());
                id = backendConfigsManager.addBackendConfig(config).getId();
            } else {
                id = walletUUID;
                BackendConfig backendConfig = backendConfigsManager.getBackendConfigById(id);

                // We update an existing backend config. Therefore we only want to override the information
                // that is actually provided within our new data.
                if (config.getSource() != null)
                    switch (config.getSource()) {
                        case MANUAL_INPUT:
                            backendConfig.setAlias(config.getAlias());
                            backendConfig.setBackendType(config.getBackendType());
                            backendConfig.setHost(config.getHost());
                            backendConfig.setPort(port);
                            backendConfig.setServerCert(config.getServerCert());
                            backendConfig.setClientCert(config.getClientCert());
                            backendConfig.setClientKey(config.getClientKey());
                            backendConfig.setAuthenticationToken(config.getAuthenticationToken());
                            backendConfig.setUser(config.getUser());
                            backendConfig.setPassword(config.getPassword());
                            backendConfig.setVpnConfig(config.getVpnConfig());
                            backendConfig.setUseTor(config.getUseTor());
                            backendConfig.setVerifyCertificate(config.getVerifyCertificate());
                            break;
                        case LND_CONNECT:
                            backendConfig.setBackendType(config.getBackendType());
                            backendConfig.setHost(config.getHost());
                            backendConfig.setPort(port);
                            backendConfig.setServerCert(config.getServerCert());
                            backendConfig.setAuthenticationToken(config.getAuthenticationToken());
                            break;
                        case CLN_GRPC:
                            backendConfig.setBackendType(config.getBackendType());
                            backendConfig.setHost(config.getHost());
                            backendConfig.setPort(port);
                            backendConfig.setServerCert(config.getServerCert());
                            backendConfig.setClientCert(config.getClientCert());
                            backendConfig.setClientKey(config.getClientKey());
                            break;
                        case LND_HUB_CONNECT:
                            backendConfig.setBackendType(config.getBackendType());
                            backendConfig.setHost(config.getHost());
                            backendConfig.setUser(config.getUser());
                            backendConfig.setPassword(config.getPassword());
                            break;
                        case BTC_PAY_DATA:
                            backendConfig.setBackendType(config.getBackendType());
                            backendConfig.setHost(config.getHost());
                            backendConfig.setPort(port);
                            backendConfig.setAuthenticationToken(config.getAuthenticationToken());
                            break;
                        default:
                            // Override everything and use the new one.
                            backendConfig = config;
                            backendConfig.setId(id);
                    }
                if (backendConfig.getVpnConfig() == null)
                    backendConfig.setVpnConfig(new VPNConfig());
                backendConfigsManager.updateBackendConfig(backendConfig);
            }
            backendConfigsManager.apply();
            listener.onSaved(id);
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

        void onValidConnectData(BackendConfig backendConfig);

        void onNoConnectData();

        void onError(String error, int duration);
    }

    public interface OnSaveRemoteConfigurationListener {

        void onSaved(String walletId);

        void onError(String error, int duration);
    }
}
