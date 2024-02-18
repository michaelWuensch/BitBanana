package app.michaelwuensch.bitbanana.connection;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * This TrustManager trust ALL certificates. No validation takes place.
 * In our context we use it only for tor connections or if the user explicitly deactivates it.
 */
public class BlindTrustManager implements X509TrustManager {

    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {

    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {

    }
}
