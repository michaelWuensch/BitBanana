package app.michaelwuensch.bitbanana.backends.coreLightning.connection;

import com.google.common.io.BaseEncoding;

import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.connection.BlindTrustManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.CertificateUtil;

/**
 * Creates an SSLSocketFactory instance for use with CoreLightning providing mutual TLS authentication.
 * This can be fed into HttpsURLConnection, as well as networking libraries such as OkHttp's OkHttpClient.
 */
public class CoreLightningSSLSocketFactory {

    private static final String LOG_TAG = CoreLightningSSLSocketFactory.class.getSimpleName();

    private CoreLightningSSLSocketFactory() {
        throw new AssertionError();
    }

    public static SSLSocketFactory create(BackendConfig backendConfig) {
        BBLog.d(LOG_TAG, "Creating CoreLightningSSLSocketFactory.");
        SSLContext sslCtx = null;

        try {
            sslCtx = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            BBLog.e(LOG_TAG, "SSLSocketFactory creation failed.");
            return null;
        }

        KeyManager[] km = null;
        try {
            // Initialize KeyManager for client authentication
            KeyStore clientKeyStore = createClientKeyStore(backendConfig);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientKeyStore, null);
            km = kmf.getKeyManagers();
        } catch (Exception e) {
            BBLog.e(LOG_TAG, "Error initializing key manager for client authentication.");
            return null;
        }

        try {
            // Create a TrustManager used to define whether the server certificate should be trusted or not.
            TrustManager[] tm;
            if (backendConfig.isTorHostAddress() || !backendConfig.getVerifyCertificate()) {
                // Always trust the server certificate on Tor connection or when certificate validation is turned off.
                tm = new TrustManager[]{new BlindTrustManager()};
            } else {
                // Generate the CA Certificate from the supplied data
                Certificate ca = createServerCertificate(backendConfig);

                // Load the key store using the CA
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", ca);

                // Initialize the TrustManager with this CA
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);
                tm = tmf.getTrustManagers();
            }

            // Create an SSL context that uses the created KeyManager and TrustManager
            sslCtx.init(km, tm, new SecureRandom());
            return sslCtx.getSocketFactory();

        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Error creating TrustManager for server authentication.");
            e.printStackTrace();
        }

        // If the above failed, use the default TrustManager which is used when set to null
        // This will be the case for btc pay for example as no self signed certificates are used
        try {
            sslCtx.init(km, null, new SecureRandom());
            BBLog.w(LOG_TAG, "Default TrustManager is used.");
        } catch (KeyManagementException e) {
            e.printStackTrace();
            BBLog.e(LOG_TAG, "SSLSocketFactory creation failed.");
            return null;
        }
        return sslCtx.getSocketFactory();
    }

    private static KeyStore createClientKeyStore(BackendConfig backendConfig) throws Exception {
        // get client's key
        KeyFactory keyFactory = KeyFactory.getInstance("EC");  // Polar uses EC, ToDo: See if RSA is used by other implementations
        PrivateKey clientPrivateKey = keyFactory.generatePrivate(getClientEncodedKeySpec(backendConfig));

        // get client's certificate
        Certificate clientCert = createClientCertificate(backendConfig);
        Certificate[] certChain = {clientCert};

        // create and init key store
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        // clear key store before use
        keyStore.load(null, null);

        // init key store with client's certificate and key
        keyStore.setKeyEntry("client-key", clientPrivateKey, null, certChain);
        return keyStore;
    }

    private static PKCS8EncodedKeySpec getClientEncodedKeySpec(BackendConfig backendConfig) {
        byte[] encoded = BaseEncoding.base64().decode(backendConfig.getClientKey());
        return new PKCS8EncodedKeySpec(encoded);
    }

    private static Certificate createClientCertificate(BackendConfig backendConfig) throws Exception {
        byte[] clientCertificateBytes = BaseEncoding.base64().decode(backendConfig.getClientCert());
        return CertificateUtil.certificateFromDER(clientCertificateBytes);
    }

    private static Certificate createServerCertificate(BackendConfig backendConfig) throws Exception {
        byte[] serverCertificateBytes = BaseEncoding.base64().decode(backendConfig.getServerCert());
        return CertificateUtil.certificateFromDER(serverCertificateBytes);
    }
}
