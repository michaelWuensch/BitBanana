package app.michaelwuensch.bitbanana.backendConfigs.coreLightning;

import com.google.common.io.BaseEncoding;

import java.net.URI;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BaseConnectionParser;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.CertificateUtil;
import app.michaelwuensch.bitbanana.util.RemoteConnectUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;

/**
 * This class parses a core lightning grpc connect string
 * <p>
 * A connect string consists of the following parts:
 * cln-grpc://<HOST>:<PORT>?clientkey=<clientkeyvalue>&clientCert=<clientcertvalue>&caCert=<cacertvalue>
 * <p>
 * The parser returns an object containing the desired data or an descriptive error.
 */
public class CoreLightningConnectStringParser extends BaseConnectionParser {

    public static final int ERROR_INVALID_CONNECT_STRING = 0;
    public static final int ERROR_INVALID_HOST_OR_PORT = 1;
    public static final int ERROR_NO_CLIENT_CERTIFICATE = 2;
    public static final int ERROR_NO_CLIENT_KEY = 3;
    public static final int ERROR_PARAMETER_DECODING_FAILED = 4;
    public static final int ERROR_INVALID_CERTIFICATE_OR_KEY = 5;

    private static final String LOG_TAG = CoreLightningConnectStringParser.class.getSimpleName();

    public CoreLightningConnectStringParser(String connectString) {
        super(connectString);
    }

    public CoreLightningConnectStringParser parse() {

        // validate not null
        if (mConnectionString == null) {
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        // validate scheme
        if (!UriUtil.isCoreLightningGRPCUri(mConnectionString)) {
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

            String caCert = null;
            String clientCert = null;
            String clientKey = null;


            // fetch params
            if (connectURI.getQuery() != null) {
                String[] valuePairs = connectURI.getQuery().split("&");

                for (String pair : valuePairs) {
                    String[] param = pair.split("=");
                    if (param.length > 1) {
                        if (param[0].equalsIgnoreCase("caCert")) {
                            caCert = param[1];
                        }
                        if (param[0].equalsIgnoreCase("clientCert")) {
                            clientCert = param[1];
                        }
                        if (param[0].equalsIgnoreCase("clientKey")) {
                            clientKey = param[1];
                        }
                    }
                }

                // validate caCert (caCert is not mandatory)
                if (caCert != null) {
                    try {
                        byte[] certificateBytes = BaseEncoding.base64Url().decode(caCert);
                        try {
                            CertificateUtil.certificateFromDER(certificateBytes);
                        } catch (Exception e) {
                            BBLog.e(LOG_TAG, "caCert validation failed");
                            mError = ERROR_INVALID_CERTIFICATE_OR_KEY;
                            return this;
                        }
                    } catch (Exception e) {
                        BBLog.e(LOG_TAG, "caCert decoding failed");
                        mError = ERROR_PARAMETER_DECODING_FAILED;
                        return this;
                    }
                }

                // validate client cert
                if (clientCert == null) {
                    BBLog.e(LOG_TAG, "cln-grpc connect string does not include a client cert");
                    mError = ERROR_NO_CLIENT_CERTIFICATE;
                    return this;
                } else {
                    try {
                        byte[] certificateBytes = BaseEncoding.base64Url().decode(clientCert);
                        try {
                            CertificateUtil.certificateFromDER(certificateBytes);
                        } catch (Exception e) {
                            BBLog.e(LOG_TAG, "clientCert validation failed");
                            mError = ERROR_INVALID_CERTIFICATE_OR_KEY;
                            return this;
                        }
                    } catch (Exception e) {
                        BBLog.e(LOG_TAG, "clientCert decoding failed");
                        mError = ERROR_PARAMETER_DECODING_FAILED;
                        return this;
                    }
                }

                // validate client key
                if (clientKey == null) {
                    BBLog.e(LOG_TAG, "cln-grpc connect string does not include a client key");
                    mError = ERROR_NO_CLIENT_KEY;
                    return this;
                } else {
                    try {
                        byte[] keyBytes = BaseEncoding.base64Url().decode(clientKey);
                        // ToDo: more sophisticated key validation
                    } catch (Exception e) {
                        BBLog.e(LOG_TAG, "clientKey decoding failed");
                        mError = ERROR_PARAMETER_DECODING_FAILED;
                        return this;
                    }
                }

                // everything is ok
                BackendConfig backendConfig = new BackendConfig();
                backendConfig.setSource(BackendConfig.Source.CLN_GRPC);
                backendConfig.setBackendType(BackendConfig.BackendType.CORE_LIGHTNING_GRPC);
                backendConfig.setHost(connectURI.getHost());
                backendConfig.setPort(connectURI.getPort());
                backendConfig.setLocation(BackendConfig.Location.REMOTE);
                backendConfig.setNetwork(BackendConfig.Network.UNKNOWN);
                if (caCert != null)
                    backendConfig.setServerCert(BaseEncoding.base64().encode(BaseEncoding.base64Url().decode(caCert)));
                backendConfig.setClientCert(BaseEncoding.base64().encode(BaseEncoding.base64Url().decode(clientCert)));
                backendConfig.setClientKey(BaseEncoding.base64().encode(BaseEncoding.base64Url().decode(clientKey)));
                backendConfig.setUseTor(RemoteConnectUtil.isTorHostAddress(connectURI.getHost()));
                backendConfig.setVerifyCertificate(!RemoteConnectUtil.isTorHostAddress(connectURI.getHost()));
                setBackendConfig(backendConfig);
                return this;

            } else {
                BBLog.e(LOG_TAG, "Connect URI has no parameters");
                mError = ERROR_INVALID_CONNECT_STRING;
                return this;
            }

        } catch (Exception e) {
            BBLog.e(LOG_TAG, "URI could not be parsed. Exception message: " + e.getMessage());
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }
    }
}
