package app.michaelwuensch.bitbanana.backendConfigs.btcPay;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BaseConnectionParser;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;

/**
 * This class parses a BTCPayConfig JSON which is defined in this project:
 * https://github.com/btcpayserver/btcpayserver
 * <p>
 * Note: A certificate is not mandatory.
 * <p>
 * The parser returns an object containing the desired data or an descriptive error.
 */
public class BTCPayConfigParser extends BaseConnectionParser {

    public static final int ERROR_INVALID_JSON = 0;
    public static final int ERROR_MISSING_BTC_GRPC_CONFIG = 1;
    public static final int ERROR_NO_MACAROON = 2;
    public static final int ERROR_INVALID_HOST_OR_PORT = 3;

    private static final String LOG_TAG = BTCPayConfigParser.class.getSimpleName();

    public BTCPayConfigParser(String connectString) {
        super(connectString);
    }

    /**
     * Used to determine if the provided String is a valid BTCPayConfiguration JSON.
     *
     * @param connectionString parses as JSON
     * @return if the JSON syntax is valid
     */
    public static boolean isValidJson(String connectionString) {
        try {
            BTCPayConfigJson btcPayConfigJson = new Gson().fromJson(connectionString, BTCPayConfigJson.class);
            return btcPayConfigJson != null;
        } catch (JsonSyntaxException ex) {
            return false;
        }
    }

    public BTCPayConfigParser parse() {

        BTCPayConfigJson btcPayConfigJson;

        try {
            btcPayConfigJson = new Gson().fromJson(mConnectionString, BTCPayConfigJson.class);
        } catch (JsonSyntaxException ex) {
            BBLog.e(LOG_TAG, "BTCPay Configuration JSON syntax is invalid");
            mError = ERROR_INVALID_JSON;
            return this;
        }

        BTCPayConfig configuration = btcPayConfigJson.getConfiguration(BTCPayConfig.TYPE_GRPC, BTCPayConfig.CRYPTO_TYPE_BTC);

        if (configuration == null) {
            BBLog.e(LOG_TAG, "BTCPay Configuration does not contain BTC gRPC config");
            mError = ERROR_MISSING_BTC_GRPC_CONFIG;
            return this;
        }

        // validate host and port
        if (configuration.getPort() == 0 || configuration.getHost().isEmpty()) {
            BBLog.e(LOG_TAG, "BTCPay Configuration does not contain a host or port");
            mError = ERROR_INVALID_HOST_OR_PORT;
            return this;
        }

        // validate macaroon if everything was valid so far
        if (configuration.getMacaroon().isEmpty()) {
            BBLog.e(LOG_TAG, "BTCPay Configuration does not include a default macaroon");
            mError = ERROR_NO_MACAROON;
            return this;
        }

        // Everything is valid.
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setSource(BackendConfig.Source.BTC_PAY_DATA);
        backendConfig.setBackendType(BackendConfig.BackendType.LND_GRPC);
        backendConfig.setHost(configuration.getHost());
        backendConfig.setPort(configuration.getPort());
        backendConfig.setLocation(BackendConfig.Location.REMOTE);
        backendConfig.setNetwork(BackendConfig.Network.UNKNOWN);
        backendConfig.setAuthenticationToken(configuration.getMacaroon());
        backendConfig.setUseTor(RemoteConnectUtil.isTorHostAddress(configuration.getHost()));
        backendConfig.setVerifyCertificate(!RemoteConnectUtil.isTorHostAddress(configuration.getHost()));
        setBackendConfig(backendConfig);
        return this;
    }
}
