package app.michaelwuensch.bitbanana.backendConfigs.coreLightning;

import com.google.common.io.BaseEncoding;

import java.net.URI;
import java.nio.charset.StandardCharsets;

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
 * clngrpc://<HOST>:<PORT>?pubkey=<pubkey>&protoPath=<protopath>&certs=<Base64-encoded concatenation of client key, client cert, and CA cert>
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
            String pubkey = null;
            String certsString = null;


            // fetch params
            if (connectURI.getQuery() != null) {
                String[] valuePairs = connectURI.getQuery().split("&");

                for (String pair : valuePairs) {
                    String[] param = pair.split("=");
                    if (param.length > 1) {
                        if (param[0].equalsIgnoreCase("pubkey")) {
                            pubkey = param[1];
                        }
                        if (param[0].equalsIgnoreCase("certs")) {
                            certsString = param[1];
                        }
                    }
                }

                if (certsString == null) {
                    mError = ERROR_PARAMETER_DECODING_FAILED;
                    return this;
                }

                String decodedCertsBundle = null;
                try {
                    decodedCertsBundle = new String(BaseEncoding.base64().decode(certsString), StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    BBLog.e(LOG_TAG, "certs parameter decoding failed");
                    mError = ERROR_PARAMETER_DECODING_FAILED;
                    return this;
                }

                int currentIndex = 0;

                // Extract Client Key
                int keyStart = decodedCertsBundle.indexOf("-----BEGIN PRIVATE KEY-----", currentIndex);
                if (keyStart != -1) {
                    int keyEnd = decodedCertsBundle.indexOf("-----END PRIVATE KEY-----", keyStart);
                    if (keyEnd != -1) {
                        keyEnd += "-----END PRIVATE KEY-----".length();
                        clientKey = decodedCertsBundle.substring(keyStart, keyEnd).trim();
                        currentIndex = keyEnd;
                    }
                }

                // Extract Client Cert
                int cert1Start = decodedCertsBundle.indexOf("-----BEGIN CERTIFICATE-----", currentIndex);
                if (cert1Start != -1) {
                    int cert1End = decodedCertsBundle.indexOf("-----END CERTIFICATE-----", cert1Start);
                    if (cert1End != -1) {
                        cert1End += "-----END CERTIFICATE-----".length();
                        clientCert = decodedCertsBundle.substring(cert1Start, cert1End).trim();
                        currentIndex = cert1End;
                    }
                }

                // Extract CA Cert
                int cert2Start = decodedCertsBundle.indexOf("-----BEGIN CERTIFICATE-----", currentIndex);
                if (cert2Start != -1) {
                    int cert2End = decodedCertsBundle.indexOf("-----END CERTIFICATE-----", cert2Start);
                    if (cert2End != -1) {
                        cert2End += "-----END CERTIFICATE-----".length();
                        caCert = decodedCertsBundle.substring(cert2Start, cert2End).trim();
                    }
                }

                // validate caCert (caCert is not mandatory)
                if (caCert != null) {
                    byte[] certificateBytes = caCert.getBytes(StandardCharsets.UTF_8);
                    try {
                        CertificateUtil.certificateFromDER(certificateBytes);
                    } catch (Exception e) {
                        BBLog.e(LOG_TAG, "caCert validation failed");
                        mError = ERROR_INVALID_CERTIFICATE_OR_KEY;
                        return this;
                    }
                }

                // validate client cert
                if (clientCert == null) {
                    BBLog.e(LOG_TAG, "clngrpc connect string does not include a client cert");
                    mError = ERROR_NO_CLIENT_CERTIFICATE;
                    return this;
                } else {
                    byte[] certificateBytes = clientCert.getBytes(StandardCharsets.UTF_8);
                    try {
                        CertificateUtil.certificateFromDER(certificateBytes);
                    } catch (Exception e) {
                        BBLog.e(LOG_TAG, "clientCert validation failed");
                        mError = ERROR_INVALID_CERTIFICATE_OR_KEY;
                        return this;
                    }
                }

                // validate client key
                if (clientKey == null) {
                    BBLog.e(LOG_TAG, "clngrpc connect string does not include a client key");
                    mError = ERROR_NO_CLIENT_KEY;
                    return this;
                } else {
                    try {
                        byte[] keyBytes = clientKey.getBytes(StandardCharsets.UTF_8);
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
                    backendConfig.setServerCert(BaseEncoding.base64().encode(CertificateUtil.cert_PEM_To_DER(caCert)));
                backendConfig.setClientCert(BaseEncoding.base64().encode(CertificateUtil.cert_PEM_To_DER(clientCert)));
                backendConfig.setClientKey(BaseEncoding.base64().encode(CertificateUtil.privateKey_PEM_To_DER(clientKey)));
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
