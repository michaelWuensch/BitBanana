package app.michaelwuensch.bitbanana.util;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The certificates and keys used here were generated only for these tests and are not used anywhere else.
 */
public class CertificateUtilTest {

    @Test
    public void givenMatchingEcKey_whenIsPrivateKeyMatchingCertificate_thenTrue() throws Exception {
        assertTrue(CertificateUtil.isPrivateKeyMatchingCertificate(loadKey("test_only_ec_client_key.pem"), loadCertificate("test_only_ec_client_cert.pem")));
    }

    @Test
    public void givenMatchingRsaKey_whenIsPrivateKeyMatchingCertificate_thenTrue() throws Exception {
        assertTrue(CertificateUtil.isPrivateKeyMatchingCertificate(loadKey("test_only_rsa_client_key.pem"), loadCertificate("test_only_rsa_client_cert.pem")));
    }

    @Test
    public void givenKeyOfAnotherCertificate_whenIsPrivateKeyMatchingCertificate_thenFalse() throws Exception {
        assertFalse(CertificateUtil.isPrivateKeyMatchingCertificate(loadKey("test_only_ec_other_key.pem"), loadCertificate("test_only_ec_client_cert.pem")));
    }

    @Test
    public void givenKeyOfDifferentAlgorithm_whenIsPrivateKeyMatchingCertificate_thenFalse() throws Exception {
        assertFalse(CertificateUtil.isPrivateKeyMatchingCertificate(loadKey("test_only_rsa_client_key.pem"), loadCertificate("test_only_ec_client_cert.pem")));
    }

    @Test
    public void givenInvalidKeyData_whenIsPrivateKeyMatchingCertificate_thenFalse() throws Exception {
        byte[] invalidKey = "this is not a key".getBytes(StandardCharsets.UTF_8);
        assertFalse(CertificateUtil.isPrivateKeyMatchingCertificate(invalidKey, loadCertificate("test_only_ec_client_cert.pem")));
        assertFalse(CertificateUtil.isPrivateKeyMatchingCertificate(new byte[0], loadCertificate("test_only_ec_client_cert.pem")));
    }

    private byte[] loadKey(String filename) {
        return CertificateUtil.privateKey_PEM_To_DER(readStringFromFile(filename));
    }

    private Certificate loadCertificate(String filename) throws Exception {
        return CertificateUtil.certificateFromDER(readStringFromFile(filename).getBytes(StandardCharsets.UTF_8));
    }

    private String readStringFromFile(String filename) {
        InputStream inputstream = this.getClass().getClassLoader().getResourceAsStream(filename);
        return new BufferedReader(new InputStreamReader(inputstream))
                .lines().collect(Collectors.joining("\n"));
    }
}
