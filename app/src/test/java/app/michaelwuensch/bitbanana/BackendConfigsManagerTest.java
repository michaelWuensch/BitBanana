package app.michaelwuensch.bitbanana;

import com.google.gson.Gson;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsJson;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

public class BackendConfigsManagerTest {

    private static final String WALLET_1_ID = "e4f2fcf7-82c7-46f4-8867-50c3f8a603f4";
    private static final String WALLET_2_ID = "a4f2fcf7-82c7-46f4-8867-50c3f8a603f4"; // first letter is different!
    private static final String INVALID_ID = "notExistingOrInvalid";

    @Test
    public void givenNoConfigs_whenDoesWalletExist_thenReturnFalse() {
        BackendConfigsManager manager = new BackendConfigsManager(null);
        BackendConfig BackendConfigToFind = new BackendConfig();
        BackendConfigToFind.setId(WALLET_1_ID);
        boolean result = manager.doesBackendConfigExist(BackendConfigToFind);

        assertFalse(result);
    }


    @Test
    public void givenExistingId_whenDoesWalletExist_thenReturnTrue() {
        String configJson = readStringFromFile("backend_configs.json");
        BackendConfigsManager manager = new BackendConfigsManager(configJson);
        BackendConfig BackendConfigToFind = new BackendConfig();
        BackendConfigToFind.setId(WALLET_1_ID);
        boolean result = manager.doesBackendConfigExist(BackendConfigToFind);

        assertTrue(result);
    }

    @Test
    public void givenNoConfigs_whenLoadWalletConfig_thenReturnNull() {
        BackendConfigsManager manager = new BackendConfigsManager(null);
        BackendConfig result = manager.getBackendConfigById(WALLET_1_ID);

        assertNull(result);
    }

    @Test
    public void givenNonExistingId_whenLoadWalletConfig_thenReturnNull() {
        String configJson = readStringFromFile("backend_configs.json");
        BackendConfigsManager manager = new BackendConfigsManager(configJson);
        BackendConfig result = manager.getBackendConfigById(INVALID_ID);

        assertNull(result);
    }

    @Test
    public void givenExistingId_whenLoadWalletConfig_thenReceiveCorrectWalletConfig() throws UnsupportedEncodingException {
        BackendConfig expected = readBackendConfigsJsonFromFile("backend_configs.json").getConnectionById(WALLET_1_ID);
        String configJson = readStringFromFile("backend_configs.json");
        BackendConfigsManager manager = new BackendConfigsManager(configJson);
        BackendConfig result = manager.getBackendConfigById(WALLET_1_ID);

        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getBackendType(), result.getBackendType());
        assertEquals(expected.getAlias(), result.getAlias());
        assertEquals(expected.getLocation(), result.getLocation());
        assertEquals(expected.getHost(), result.getHost());
        assertEquals(expected.getPort(), result.getPort());
        assertEquals(expected.getServerCert(), result.getServerCert());
        assertEquals(expected.getMacaroon(), result.getMacaroon());
        assertEquals(expected.getUseTor(), result.getUseTor());
        assertEquals(expected.getVerifyCertificate(), result.getVerifyCertificate());
        assertEquals(expected.getVpnConfig(), result.getVpnConfig());
    }

    @Test
    public void givenNewId_whenAddWalletConfig_thenReceiveUpdatedWalletConfigs() throws UnsupportedEncodingException {
        BackendConfig expected = readBackendConfigsJsonFromFile("backend_configs.json").getConnectionById(WALLET_1_ID);

        BackendConfigsManager manager = new BackendConfigsManager(null);

        BackendConfig configToAdd = new BackendConfig();
        configToAdd.setAlias(expected.getAlias());
        configToAdd.setLocation(expected.getLocation());
        configToAdd.setBackendType(expected.getBackendType());
        configToAdd.setNetwork(expected.getNetwork());
        configToAdd.setHost(expected.getHost());
        configToAdd.setPort(expected.getPort());
        configToAdd.setServerCert(expected.getServerCert());
        configToAdd.setMacaroon(expected.getMacaroon());
        configToAdd.setUseTor(expected.getUseTor());
        configToAdd.setVerifyCertificate(expected.getVerifyCertificate());
        configToAdd.setVpnConfig(expected.getVpnConfig());

        manager.addBackendConfig(configToAdd);
        BackendConfig actual = (BackendConfig) manager.getBackendConfigsJson().getConnections().toArray()[0];

        assertEquals(expected.getAlias(), actual.getAlias());
        assertEquals(expected.getBackendType(), actual.getBackendType());
        assertEquals(expected.getServerCert(), actual.getServerCert());
        assertEquals(expected.getLocation(), actual.getLocation());
        assertEquals(expected.getNetwork(), actual.getNetwork());
        assertEquals(expected.getHost(), actual.getHost());
        assertEquals(expected.getPort(), actual.getPort());
        assertEquals(expected.getMacaroon(), actual.getMacaroon());
        assertEquals(expected.getUseTor(), actual.getUseTor());
        assertEquals(expected.getVerifyCertificate(), actual.getVerifyCertificate());
        assertEquals(expected.getVpnConfig(), actual.getVpnConfig());
    }

    @Test
    public void givenNonExistingId_whenRemoveWalletConfig_thenReturnFalse() {
        String configJson = readStringFromFile("backend_configs.json");
        BackendConfigsManager manager = new BackendConfigsManager(configJson);

        String expected = new Gson().toJson(manager.getBackendConfigsJson());
        BackendConfig BackendConfigToRemove = manager.getBackendConfigById(INVALID_ID);
        boolean removed = manager.removeBackendConfig(BackendConfigToRemove);
        String result = new Gson().toJson(manager.getBackendConfigsJson());

        assertFalse(removed);
        assertEquals(expected, result);
    }

    @Test
    public void givenExistingId_whenRemoveWalletConfig_thenReceiveUpdatedWalletConfigs() {
        String configJson = readStringFromFile("backend_configs.json");

        BackendConfigsManager manager = new BackendConfigsManager(configJson);
        BackendConfig BackendConfigToRemove = manager.getBackendConfigById(WALLET_2_ID);
        boolean removed = manager.removeBackendConfig(BackendConfigToRemove);

        assertTrue(removed);
        assertNull(manager.getBackendConfigById(WALLET_2_ID));
        assertNotNull(manager.getBackendConfigById(WALLET_1_ID));
    }

    @Test
    public void givenNonExistingId_whenRenameWalletConfig_thenReturnFalse() {
        String configJson = readStringFromFile("backend_configs.json");
        BackendConfigsManager manager = new BackendConfigsManager(configJson);
        String expected = new Gson().toJson(manager.getBackendConfigsJson());
        BackendConfig BackendConfigToRename = manager.getBackendConfigById(INVALID_ID);
        boolean renamed = manager.renameBackendConfig(BackendConfigToRename, "NewWalletName");
        String result = new Gson().toJson(manager.getBackendConfigsJson());

        assertFalse(renamed);
        assertEquals(expected, result);
    }


    @Test
    public void givenExistingId_whenRenameWalletConfig_thenReceiveUpdatedWalletConfigs() throws UnsupportedEncodingException {
        BackendConfig expected = readBackendConfigsJsonFromFile("backend_configs_rename.json").getConnectionById(WALLET_1_ID);
        String configJson = readStringFromFile("backend_configs_create.json");
        BackendConfigsManager manager = new BackendConfigsManager(configJson);
        BackendConfig BackendConfigToRename = manager.getBackendConfigById(WALLET_1_ID);
        boolean renamed = manager.renameBackendConfig(BackendConfigToRename, "NewWalletName");
        BackendConfig actual = manager.getBackendConfigById(WALLET_1_ID);

        assertTrue(renamed);
        assertNotNull(manager.getBackendConfigById(WALLET_1_ID));
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getBackendType(), actual.getBackendType());
        assertEquals(expected.getAlias(), actual.getAlias());
        assertEquals(expected.getServerCert(), actual.getServerCert());
        assertEquals(expected.getLocation(), actual.getLocation());
        assertEquals(expected.getHost(), actual.getHost());
        assertEquals(expected.getPort(), actual.getPort());
        assertEquals(expected.getMacaroon(), actual.getMacaroon());
        assertEquals(expected.getUseTor(), actual.getUseTor());
        assertEquals(expected.getVerifyCertificate(), actual.getVerifyCertificate());
        assertEquals(expected.getVpnConfig(), actual.getVpnConfig());
    }

    private BackendConfigsJson readBackendConfigsJsonFromFile(String filename) throws UnsupportedEncodingException {
        InputStream inputstream = this.getClass().getClassLoader().getResourceAsStream(filename);
        Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
        return new Gson().fromJson(reader, BackendConfigsJson.class);
    }

    private String readStringFromFile(String filename) {
        InputStream inputstream = this.getClass().getClassLoader().getResourceAsStream(filename);
        return new BufferedReader(new InputStreamReader(inputstream))
                .lines().collect(Collectors.joining("\n"));
    }
}