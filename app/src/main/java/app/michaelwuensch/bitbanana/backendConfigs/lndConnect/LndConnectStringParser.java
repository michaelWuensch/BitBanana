package app.michaelwuensch.bitbanana.backendConfigs.lndConnect;

import com.google.common.io.BaseEncoding;

import java.net.URI;
import java.net.URISyntaxException;

import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BaseConnectionParser;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.CertificateUtil;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;

/**
 * This class parses a lndconnect which is defined in this project:
 * https://github.com/LN-Zap/lndconnect
 * <p>
 * A lndconnect string consists of the following parts:
 * lndconnect://<HOST>:<PORT>?cert=<certificate_encoded_as_base64url>&macaroon=<macaroon_encoded_as_base64url>
 * <p>
 * Note: For lndconnect a certificate is not mandatory.
 * <p>
 * The parser returns an object containing the desired data or an descriptive error.
 */
public class LndConnectStringParser extends BaseConnectionParser<LndConnectConfig> {

    public static final int ERROR_INVALID_CONNECT_STRING = 0;
    public static final int ERROR_NO_MACAROON = 1;
    public static final int ERROR_INVALID_CERTIFICATE = 2;
    public static final int ERROR_INVALID_MACAROON = 3;
    public static final int ERROR_INVALID_HOST_OR_PORT = 4;
    private static final String LOG_TAG = LndConnectStringParser.class.getSimpleName();

    public LndConnectStringParser(String connectString) {
        super(connectString);
    }

    public LndConnectStringParser parse() {

        // validate not null
        if (mConnectionString == null) {
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        // validate scheme
        if (!UriUtil.isLNDConnectUri(mConnectionString)) {
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        URI connectURI = null;
        try {
            connectURI = new URI(mConnectionString);

            // validate host and port
            if (connectURI.getPort() == -1) {
                mError = ERROR_INVALID_HOST_OR_PORT;
                return this;
            }

            String cert = null;
            String macaroon = null;

            // fetch params
            if (connectURI.getQuery() != null) {
                String[] valuePairs = connectURI.getQuery().split("&");

                for (String pair : valuePairs) {
                    String[] param = pair.split("=");
                    if (param.length > 1) {
                        if (param[0].equals("cert")) {
                            cert = param[1];
                        }
                        if (param[0].equals("macaroon")) {
                            macaroon = param[1];
                        }
                    }
                }

                // validate cert (Certificate is not mandatory)
                if (cert != null) {
                    try {
                        byte[] certificateBytes = BaseEncoding.base64Url().decode(cert);
                        try {
                            CertificateUtil.certificateFromDER(certificateBytes);
                        } catch (Exception e) {
                            BBLog.e(LOG_TAG, "certificate validation failed");
                            mError = ERROR_INVALID_CERTIFICATE;
                            return this;
                        }
                    } catch (IllegalArgumentException e) {
                        BBLog.e(LOG_TAG, "cert decoding failed");
                        mError = ERROR_INVALID_CERTIFICATE;
                        return this;
                    }
                }

                // validate macaroon if everything was valid so far
                if (macaroon == null) {
                    BBLog.e(LOG_TAG, "lnd connect string does not include a macaroon");
                    mError = ERROR_NO_MACAROON;
                    return this;
                } else {
                    try {
                        BaseEncoding.base64Url().decode(macaroon);
                        byte[] macaroonBytes = BaseEncoding.base64Url().decode(macaroon);
                        macaroon = BaseEncoding.base16().encode(macaroonBytes);
                    } catch (IllegalArgumentException e) {
                        BBLog.e(LOG_TAG, "macaroon decoding failed");

                        mError = ERROR_INVALID_MACAROON;
                        return this;
                    }
                }

                // everything is ok
                LndConnectConfig lndConnectConfig = new LndConnectConfig();
                lndConnectConfig.setBackendType(BaseBackendConfig.BackendType.LND_GRPC);
                lndConnectConfig.setHost(connectURI.getHost());
                lndConnectConfig.setPort(connectURI.getPort());
                lndConnectConfig.setLocation(BaseBackendConfig.Location.REMOTE);
                lndConnectConfig.setNetwork(BaseBackendConfig.Network.UNKNOWN);
                if (cert != null)
                    lndConnectConfig.setServerCert(BaseEncoding.base64().encode(BaseEncoding.base64Url().decode(cert)));
                lndConnectConfig.setMacaroon(macaroon);
                lndConnectConfig.setUseTor(RemoteConnectUtil.isTorHostAddress(connectURI.getHost()));
                lndConnectConfig.setVerifyCertificate(!RemoteConnectUtil.isTorHostAddress(connectURI.getHost()));
                setConnectionConfig(lndConnectConfig);
                return this;

            } else {
                BBLog.e(LOG_TAG, "Connect URI has no parameters");
                mError = ERROR_INVALID_CONNECT_STRING;
                return this;
            }

        } catch (URISyntaxException e) {
            BBLog.e(LOG_TAG, "URI could not be parsed");
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }
    }
}
