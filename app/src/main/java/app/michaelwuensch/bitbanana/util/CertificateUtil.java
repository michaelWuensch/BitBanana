package app.michaelwuensch.bitbanana.util;

import com.google.common.io.BaseEncoding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;

/**
 * Utility for working with Certificates and private keys.
 * This is not a fully-fledged library, it only offers what is needed in the context of BitBanana.
 */
public class CertificateUtil {
    private static final String LOG_TAG = CertificateUtil.class.getSimpleName();

    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private final static String LINE_SEPARATOR = System.getProperty("line.separator");


    /**
     * Converts a x509 certificates or a PKCS8 encoded private key from DER to PEM
     *
     * @param DER Cert or private key in DER format
     * @return PEM encoded result. Null if it was neither a x509 certificate nor a PKCS8 encoded private key.
     */
    public static String DER_To_PEM(byte[] DER) {
        String result;
        result = certToPEM(DER);
        if (result != null)
            return result;

        result = privateKeyToPEM(DER);
        if (result != null)
            return result;
        else
            return null;
    }

    /**
     * Converts a x509 certificate from DER encoding to PEM
     *
     * @param DER
     * @return
     */
    private static String certToPEM(final byte[] DER) {
        if (DER == null)
            return null;

        InputStream inputStream = new ByteArrayInputStream(DER);
        try {
            CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
        } catch (CertificateException e) {
            return null;
        }
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
        final String encodedCertText = new String(encoder.encode(DER));
        return BEGIN_CERT + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_CERT;
    }

    /**
     * Converts a PKCS8 encoded private key from DER encoding to PEM
     *
     * @param DER
     * @return
     */
    private static String privateKeyToPEM(final byte[] DER) {
        if (DER == null)
            return null;

        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
        final String encodedCertText = new String(encoder.encode(DER));
        if (encodedCertText == null || encodedCertText.isEmpty())
            return null;
        return BEGIN_PRIVATE_KEY + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_PRIVATE_KEY;
    }

    /**
     * Converts a x509 certificates or a PKCS8 encoded private key from PEM to DER
     *
     * @param PEM Cert or private key in PEM format
     * @return DER encoded result. Null if it was neither a x509 certificate nor a PKCS8 encoded private key.
     */
    public static byte[] PEM_To_DER(String PEM) {
        if (PEM.startsWith("-----BEGIN CERTIFICATE-----"))
            return cert_PEM_To_DER(PEM);
        if (PEM.startsWith("-----BEGIN PRIVATE KEY-----"))
            return privateKey_PEM_To_DER(PEM);
        return null;
    }

    /**
     * Converts a PKCS8 encoded private key from PEM format to DER format.
     *
     * @param privateKeyPEM The private key in PEM format as string
     * @return The private key in DER format
     */
    public static byte[] privateKey_PEM_To_DER(String privateKeyPEM) {
        privateKeyPEM = privateKeyPEM.replace("\r\n", "");
        privateKeyPEM = privateKeyPEM.replace("\n", "");
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "");
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
        byte[] DER = BaseEncoding.base64().decode(privateKeyPEM);
        return DER;
    }

    /**
     * Converts a certificate from PEM format to DER format.
     *
     * @param certificatePEM The certificate in PEM format as string
     * @return The private key in DER format
     */
    public static byte[] cert_PEM_To_DER(String certificatePEM) {
        certificatePEM = certificatePEM.replace("\r\n", "");
        certificatePEM = certificatePEM.replace("\n", "");
        certificatePEM = certificatePEM.replace("-----BEGIN CERTIFICATE-----", "");
        certificatePEM = certificatePEM.replace("-----END CERTIFICATE-----", "");
        byte[] DER = BaseEncoding.base64().decode(certificatePEM);
        return DER;
    }

    /**
     * Creates a x.509 certificate from DER data.
     *
     * @param DER
     * @return Certificate
     * @throws Exception
     */
    public static Certificate certificateFromDER(byte[] DER) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(DER);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(inputStream);
    }
}