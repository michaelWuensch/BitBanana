package app.michaelwuensch.bitbanana.backendConfigs.lndHub;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BaseConnectionParser;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.UriUtil;

/**
 * This class parses a lndhub connect string:
 * <p>
 * A lndHub connect string consists of the following parts:
 * lndhub://<USERNAME>:<PASSWORD>@<HOST>
 * The parser returns an object containing the desired data or an descriptive error.
 */
public class LndHubConnectStringParser extends BaseConnectionParser<LndHubConnectConfig> {

    public static final int ERROR_INVALID_CONNECT_STRING = 0;

    private static final String LOG_TAG = LndHubConnectStringParser.class.getSimpleName();

    public LndHubConnectStringParser(String connectString) {
        super(connectString);
    }

    public LndHubConnectStringParser parse() {

        // validate not null
        if (mConnectionString == null) {
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        // validate scheme
        if (!UriUtil.isLNDHUBUri(mConnectionString)) {
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        // remove the Uri scheme
        mConnectionString = UriUtil.removeURI(mConnectionString);

        // ToDo: find out what exactly is allowed for username, password & host

        // validate <USERNAME>:<PASSWORD>@<HOST>
        if (!validateFormat(mConnectionString)) {
            BBLog.e(LOG_TAG, "Regex validation of lndhub connect string failed");
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        String username = mConnectionString.split(":")[0];
        String password = mConnectionString.split(":")[1].split("@")[0];
        String host = mConnectionString.split(":")[1].split("@")[1];

        // validate HOST
        URI hostURI = null;
        try {
            hostURI = new URI(host);
        } catch (URISyntaxException e) {
            BBLog.e(LOG_TAG, "Host URI could not be parsed");
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        // everything is ok
        LndHubConnectConfig lndHubConnectConfig = new LndHubConnectConfig();
        lndHubConnectConfig.setBackendType(BaseBackendConfig.BackendType.LND_HUB);
        lndHubConnectConfig.setHost(hostURI.getHost());
        lndHubConnectConfig.setUser(username);
        lndHubConnectConfig.setPassword(password);
        lndHubConnectConfig.setLocation(BaseBackendConfig.Location.REMOTE);
        lndHubConnectConfig.setNetwork(BaseBackendConfig.Network.UNKNOWN);
        setConnectionConfig(lndHubConnectConfig);
        return this;

    }

    private static boolean validateFormat(String connectString) {
        /* Simplified regex checking the following:
            - the string has no white spaces
            - username is an alphanumeric string
            - username is followed by exactly one ":"
            - password is an alphanumeric string
            - password is followed by exactly one "@"
            - host only consists of following characters: alphanumeric, ":", ".", "-", "/"
        */

        String regexPattern = "^\\w+:\\w+@[\\w:.-/]+$";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(connectString);

        return matcher.matches();
    }
}
