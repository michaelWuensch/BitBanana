package app.michaelwuensch.bitbanana.connection;

import org.junit.Test;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CaBlocklistTrustManagerTest {

    private static final String COMPROMISED_INTERMEDIATE = "ca_blocklist_emudhra_em_dv_tls_ca_g3b.pem";
    private static final String UNAFFECTED_ROOT = "ca_blocklist_isrg_root_x1.pem";

    @Test
    public void givenCompromisedEMudhraIntermediate_whenIsBlocked_thenTrue() throws Exception {
        assertTrue(CaBlocklistTrustManager.isBlocked(loadCertificate(COMPROMISED_INTERMEDIATE)));
    }

    @Test
    public void givenUnaffectedCertificate_whenIsBlocked_thenFalse() throws Exception {
        assertFalse(CaBlocklistTrustManager.isBlocked(loadCertificate(UNAFFECTED_ROOT)));
    }

    @Test
    public void givenBlocklist_thenAllEntriesAreLowercaseSha256Hex() {
        for (String hash : CaBlocklistTrustManager.BLOCKED_SPKI_SHA256) {
            assertEquals(64, hash.length());
            assertTrue(hash, hash.matches("[0-9a-f]{64}"));
        }
    }

    @Test
    public void givenChainWithCompromisedCa_whenCheckServerTrusted_thenRejectedEvenIfDelegateAccepts() throws Exception {
        CaBlocklistTrustManager trustManager = new CaBlocklistTrustManager(new AcceptAllTrustManager());
        X509Certificate[] chain = {loadCertificate(UNAFFECTED_ROOT), loadCertificate(COMPROMISED_INTERMEDIATE)};
        try {
            trustManager.checkServerTrusted(chain, "ECDHE_ECDSA");
            fail("Chain containing a compromised CA must be rejected.");
        } catch (CertificateException expected) {
        }
    }

    @Test
    public void givenChainWithoutBlockedCa_whenCheckServerTrusted_thenAccepted() throws Exception {
        CaBlocklistTrustManager trustManager = new CaBlocklistTrustManager(new AcceptAllTrustManager());
        trustManager.checkServerTrusted(new X509Certificate[]{loadCertificate(UNAFFECTED_ROOT)}, "ECDHE_RSA");
    }

    @Test
    public void givenDelegateRejects_whenCheckServerTrusted_thenRejected() throws Exception {
        CaBlocklistTrustManager trustManager = new CaBlocklistTrustManager(new RejectAllTrustManager());
        try {
            trustManager.checkServerTrusted(new X509Certificate[]{loadCertificate(UNAFFECTED_ROOT)}, "ECDHE_RSA");
            fail("The validation of the delegate must still be applied.");
        } catch (CertificateException expected) {
        }
    }

    private X509Certificate loadCertificate(String filename) throws Exception {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(filename)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
        }
    }

    private static class AcceptAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private static class RejectAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("rejected");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("rejected");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
