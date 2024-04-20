package app.michaelwuensch.bitbanana.backends.lnd.connection;

import com.google.common.io.BaseEncoding;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.connection.BlindTrustManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.CertificateUtil;

/**
 * Creates an SSLSocketFactory instance for use with a self signed Certificate,
 * which would otherwise be considered "not trustworthy".
 * This can be fed into HttpsURLConnection, as well as networking libraries such as OkHttp's OkHttpClient.
 */
public class LndSSLSocketFactory {

    private static final String LOG_TAG = LndSSLSocketFactory.class.getSimpleName();

    private LndSSLSocketFactory() {
        throw new AssertionError();
    }

    public static SSLSocketFactory create(BackendConfig backendConfig) {
        SSLContext sslCtx = null;

        try {
            sslCtx = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            BBLog.e(LOG_TAG, "SSLSocketFactory creation failed.");
            return null;
        }

        if (backendConfig.isTorHostAddress() || !backendConfig.getVerifyCertificate()) {
            // Always trust the server certificate on Tor connection or when certificate validation is turned off.
            try {
                sslCtx.init(null, new TrustManager[]{new BlindTrustManager()}, null);
            } catch (KeyManagementException e) {
                e.printStackTrace();
                return null;
            }
            return sslCtx.getSocketFactory();

        } else {
            // On clearnet when verify certificate is enabled, we want to validate the certificate.
            if (backendConfig.getServerCert() != null && !backendConfig.getServerCert().isEmpty()) {
                // Try to create a TrustManager used to define whether the server certificate should be trusted or not.
                try {
                    // Generate the CA Certificate from the supplied data
                    Certificate ca = createServerCertificate(backendConfig);

                    // Load the key store using the CA
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(null, null);
                    keyStore.setCertificateEntry("ca", ca);

                    // Initialize the TrustManager with this CA
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(keyStore);

                    // Create an SSL context that uses the created trust manager
                    sslCtx.init(null, tmf.getTrustManagers(), new SecureRandom());
                    return sslCtx.getSocketFactory();

                } catch (Exception e) {
                    BBLog.w(LOG_TAG, "Error creating TrustManager for server authentication.");
                    e.printStackTrace();
                }
            }
        }

        // If the above failed, use the default TrustManager which is used when set to null
        // This will be the case for btc pay for example as no self signed certificates are used
        try {
            sslCtx.init(null, null, new SecureRandom());
            BBLog.w(LOG_TAG, "Default TrustManager is used.");
        } catch (KeyManagementException e) {
            e.printStackTrace();
            BBLog.e(LOG_TAG, "SSLSocketFactory creation failed.");
            return null;
        }
        return sslCtx.getSocketFactory();
    }

    private static Certificate createServerCertificate(BackendConfig backendConfig) throws Exception {
        byte[] serverCertificateBytes = BaseEncoding.base64().decode(backendConfig.getServerCert());
        return CertificateUtil.certificateFromDER(serverCertificateBytes);
    }
}
