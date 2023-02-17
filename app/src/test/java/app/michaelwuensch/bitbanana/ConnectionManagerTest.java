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

import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.BBNodeConfig;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.BBNodeConfigsJson;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.NodeConfigsManager;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

public class ConnectionManagerTest {

    private static final String WALLET_1_ID = "e4f2fcf7-82c7-46f4-8867-50c3f8a603f4";
    private static final String WALLET_2_ID = "a4f2fcf7-82c7-46f4-8867-50c3f8a603f4";
    private static final String INVALID_ID = "notExistingOrInvalid";

    @Test
    public void givenNoConfigs_whenDoesWalletExist_thenReturnFalse() {
        NodeConfigsManager manager = new NodeConfigsManager(null);
        BBNodeConfig BBNodeConfigToFind = new BBNodeConfig(WALLET_1_ID);
        boolean result = manager.doesNodeConfigExist(BBNodeConfigToFind);

        assertFalse(result);
    }


    @Test
    public void givenExistingId_whenDoesWalletExist_thenReturnTrue() {
        String configJson = readStringFromFile("wallet_configs.json");
        NodeConfigsManager manager = new NodeConfigsManager(configJson);
        BBNodeConfig BBNodeConfigToFind = new BBNodeConfig(WALLET_1_ID);
        boolean result = manager.doesNodeConfigExist(BBNodeConfigToFind);

        assertTrue(result);
    }

    @Test
    public void givenNoConfigs_whenLoadWalletConfig_thenReturnNull() {
        NodeConfigsManager manager = new NodeConfigsManager(null);
        BBNodeConfig result = manager.getNodeConfigById(WALLET_1_ID);

        assertNull(result);
    }

    @Test
    public void givenNonExistingId_whenLoadWalletConfig_thenReturnNull() {
        String configJson = readStringFromFile("wallet_configs.json");
        NodeConfigsManager manager = new NodeConfigsManager(configJson);
        BBNodeConfig result = manager.getNodeConfigById(INVALID_ID);

        assertNull(result);
    }

    @Test
    public void givenExistingId_whenLoadWalletConfig_thenReceiveCorrectWalletConfig() throws UnsupportedEncodingException {
        BBNodeConfig expected = readWalletConfigsJsonFromFile("wallet_configs.json").getConnectionById(WALLET_1_ID);
        String configJson = readStringFromFile("wallet_configs.json");
        NodeConfigsManager manager = new NodeConfigsManager(configJson);
        BBNodeConfig result = manager.getNodeConfigById(WALLET_1_ID);

        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getAlias(), result.getAlias());
        assertEquals(expected.getType(), result.getType());
        assertEquals(expected.getHost(), result.getHost());
        assertEquals(expected.getPort(), result.getPort());
        assertEquals(expected.getCert(), result.getCert());
        assertEquals(expected.getMacaroon(), result.getMacaroon());
    }

    @Test
    public void givenNewId_whenAddWalletConfig_thenReceiveUpdatedWalletConfigs() throws UnsupportedEncodingException {
        BBNodeConfig expected = readWalletConfigsJsonFromFile("wallet_configs.json").getConnectionById(WALLET_1_ID);

        NodeConfigsManager manager = new NodeConfigsManager(null);
        manager.addNodeConfig(expected.getAlias(), expected.getType(), expected.getImplementation(), expected.getHost(), expected.getPort(), expected.getCert(), expected.getMacaroon(), expected.getUseTor(), expected.getVerifyCertificate());
        BBNodeConfig actual = (BBNodeConfig) manager.getNodeConfigsJson().getConnections().toArray()[0];

        assertEquals(expected.getAlias(), actual.getAlias());
        assertEquals(expected.getCert(), actual.getCert());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getHost(), actual.getHost());
        assertEquals(expected.getPort(), actual.getPort());
        assertEquals(expected.getMacaroon(), actual.getMacaroon());
    }

    @Test
    public void givenNonExistingId_whenRemoveWalletConfig_thenReturnFalse() {
        String configJson = readStringFromFile("wallet_configs.json");
        NodeConfigsManager manager = new NodeConfigsManager(configJson);

        String expected = new Gson().toJson(manager.getNodeConfigsJson());
        BBNodeConfig BBNodeConfigToRemove = manager.getNodeConfigById(INVALID_ID);
        boolean removed = manager.removeNodeConfig(BBNodeConfigToRemove);
        String result = new Gson().toJson(manager.getNodeConfigsJson());

        assertFalse(removed);
        assertEquals(expected, result);
    }

    @Test
    public void givenExistingId_whenRemoveWalletConfig_thenReceiveUpdatedWalletConfigs() {
        String configJson = readStringFromFile("wallet_configs.json");

        NodeConfigsManager manager = new NodeConfigsManager(configJson);
        BBNodeConfig BBNodeConfigToRemove = manager.getNodeConfigById(WALLET_2_ID);
        boolean removed = manager.removeNodeConfig(BBNodeConfigToRemove);

        assertTrue(removed);
        assertNull(manager.getNodeConfigById(WALLET_2_ID));
        assertNotNull(manager.getNodeConfigById(WALLET_1_ID));
    }

    @Test
    public void givenNonExistingId_whenRenameWalletConfig_thenReturnFalse() {
        String configJson = readStringFromFile("wallet_configs.json");
        NodeConfigsManager manager = new NodeConfigsManager(configJson);
        String expected = new Gson().toJson(manager.getNodeConfigsJson());
        BBNodeConfig BBNodeConfigToRename = manager.getNodeConfigById(INVALID_ID);
        boolean renamed = manager.renameNodeConfig(BBNodeConfigToRename, "NewWalletName");
        String result = new Gson().toJson(manager.getNodeConfigsJson());

        assertFalse(renamed);
        assertEquals(expected, result);
    }


    @Test
    public void givenExistingId_whenRenameWalletConfig_thenReceiveUpdatedWalletConfigs() throws UnsupportedEncodingException {
        BBNodeConfig expected = readWalletConfigsJsonFromFile("wallet_configs_rename.json").getConnectionById(WALLET_1_ID);
        String configJson = readStringFromFile("wallet_configs_create.json");
        NodeConfigsManager manager = new NodeConfigsManager(configJson);
        BBNodeConfig BBNodeConfigToRename = manager.getNodeConfigById(WALLET_1_ID);
        boolean renamed = manager.renameNodeConfig(BBNodeConfigToRename, "NewWalletName");
        BBNodeConfig actual = manager.getNodeConfigById(WALLET_1_ID);

        assertTrue(renamed);
        assertNotNull(manager.getNodeConfigById(WALLET_1_ID));
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getAlias(), actual.getAlias());
        assertEquals(expected.getCert(), actual.getCert());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getHost(), actual.getHost());
        assertEquals(expected.getPort(), actual.getPort());
        assertEquals(expected.getMacaroon(), actual.getMacaroon());
    }

    private BBNodeConfigsJson readWalletConfigsJsonFromFile(String filename) throws UnsupportedEncodingException {
        InputStream inputstream = this.getClass().getClassLoader().getResourceAsStream(filename);
        Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
        return new Gson().fromJson(reader, BBNodeConfigsJson.class);
    }

    private String readStringFromFile(String filename) {
        InputStream inputstream = this.getClass().getClassLoader().getResourceAsStream(filename);
        return new BufferedReader(new InputStreamReader(inputstream))
                .lines().collect(Collectors.joining("\n"));
    }
}