package app.michaelwuensch.bitbanana.backends;

import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import app.michaelwuensch.bitbanana.util.BBLog;

public class CertificateInfoStore {
    private static final String LOG_TAG = CertificateInfoStore.class.getSimpleName();
    private static volatile X509Certificate serverCertificate;

    public static void clear() {
        serverCertificate = null;
    }

    public static boolean hasCertificate() {
        return serverCertificate != null;
    }

    public static void setServerCertificate(X509Certificate cert) {
        serverCertificate = cert;
        BBLog.i(LOG_TAG, "Server certificate issuer: " + getCertificateIssuer());
    }

    public static String getCertificateIssuer() {
        return serverCertificate != null ? serverCertificate.getIssuerX500Principal().getName() : null;
    }

    public static String getCertificateIssuerOrganization(boolean fallbackToIssuerIfNull) {
        if (serverCertificate == null)
            return null;
        X500Principal principal = serverCertificate.getIssuerX500Principal();
        String dn = principal.getName();  // e.g. "CN=WE1, O=Google Trust Services, C=US"

        Pattern pattern = Pattern.compile("O=([^,]+)");
        Matcher matcher = pattern.matcher(dn);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        if (fallbackToIssuerIfNull)
            return getCertificateIssuer();

        return null;
    }

    public static boolean isSelfSigned() {
        if (serverCertificate == null)
            return false;
        try {
            return serverCertificate.getSubjectX500Principal().equals(serverCertificate.getIssuerX500Principal())
                    && verifySignature(serverCertificate, serverCertificate.getPublicKey());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean verifySignature(X509Certificate cert, java.security.PublicKey publicKey) {
        try {
            cert.verify(publicKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

