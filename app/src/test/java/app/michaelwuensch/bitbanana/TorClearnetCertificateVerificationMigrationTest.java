package app.michaelwuensch.bitbanana;

import org.junit.Test;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class TorClearnetCertificateVerificationMigrationTest {

    private static String config(String id, String type, String host, boolean useTor, boolean verify) {
        return "{\"id\":\"" + id + "\",\"alias\":\"" + id + "\",\"backendType\":\"" + type + "\",\"host\":\"" + host + "\",\"port\":10009,"
                + "\"UseTor\":" + useTor + ",\"VerifyCertificate\":" + verify + "}";
    }

    private static BackendConfigsManager manager(String... configs) {
        return new BackendConfigsManager("{\"connections\":[" + String.join(",", configs) + "],\"version\":1}");
    }

    @Test
    public void torToClearnet_verificationGetsEnabled() {
        BackendConfigsManager manager = manager(
                config("lnd", "LND_GRPC", "node.example.com", true, false),
                config("cln", "CORE_LIGHTNING_GRPC", "203.0.113.5", true, false));

        assertTrue(manager.enableCertificateVerificationForTorClearnetConfigs());
        assertTrue(manager.getBackendConfigById("lnd").getVerifyCertificate());
        assertTrue(manager.getBackendConfigById("cln").getVerifyCertificate());
    }

    @Test
    public void otherConfigs_stayUntouched() {
        BackendConfigsManager manager = manager(
                // Hidden services are authenticated by Tor
                config("onion", "LND_GRPC", "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion", true, false),
                config("onionUpperCase", "LND_GRPC", "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567ABCDEFGHIJKLMNOPQRSTUV.ONION", true, false),
                // Without Tor the user deliberately disabled the verification
                config("clearnet", "LND_GRPC", "node.example.com", false, false),
                // LndHub and NWC do not use this setting
                config("lndhub", "LND_HUB", "https://lndhub.example.com/", true, false),
                config("nwc", "NOSTR_WALLET_CONNECT", "", true, false));

        assertFalse(manager.enableCertificateVerificationForTorClearnetConfigs());
        assertFalse(manager.getBackendConfigById("onion").getVerifyCertificate());
        assertFalse(manager.getBackendConfigById("onionUpperCase").getVerifyCertificate());
        assertFalse(manager.getBackendConfigById("clearnet").getVerifyCertificate());
        assertFalse(manager.getBackendConfigById("lndhub").getVerifyCertificate());
        assertFalse(manager.getBackendConfigById("nwc").getVerifyCertificate());
    }

    @Test
    public void alreadyVerified_nothingChanged() {
        BackendConfigsManager manager = manager(config("lnd", "LND_GRPC", "node.example.com", true, true));

        assertFalse(manager.enableCertificateVerificationForTorClearnetConfigs());
        assertTrue(manager.getBackendConfigById("lnd").getVerifyCertificate());
    }
}
