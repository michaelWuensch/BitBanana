package app.michaelwuensch.bitbanana.backendConfigs.manageNodeConfigs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.BBLog;

/**
 * This SINGLETON class is used to load and save configurations for nodes.
 * Multiple nodes can exist simultaneously.
 * <p>
 * The node configurations are stored encrypted in the default shared preferences.
 */
public class NodeConfigsManager {

    private static final String LOG_TAG = NodeConfigsManager.class.getSimpleName();
    private static NodeConfigsManager mInstance;
    private BBNodeConfigsJson mBBNodeConfigsJson;

    private NodeConfigsManager() {

        String decrypted = null;
        try {
            decrypted = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.NODE_CONFIGS, "");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        if (isValidJson(decrypted)) {
            mBBNodeConfigsJson = new Gson().fromJson(decrypted, BBNodeConfigsJson.class);
        } else {
            mBBNodeConfigsJson = createEmptyNodeConfigsJson();
        }

        if (mBBNodeConfigsJson == null) {
            mBBNodeConfigsJson = createEmptyNodeConfigsJson();
        }
    }

    // used for unit tests
    public NodeConfigsManager(String NodeConfigsJson) {
        try {
            mBBNodeConfigsJson = new Gson().fromJson(NodeConfigsJson, BBNodeConfigsJson.class);
        } catch (JsonSyntaxException e) {
            mBBNodeConfigsJson = createEmptyNodeConfigsJson();
        }
        if (mBBNodeConfigsJson == null) {
            mBBNodeConfigsJson = createEmptyNodeConfigsJson();
        }
    }

    public static NodeConfigsManager getInstance() {
        if (mInstance == null) {
            mInstance = new NodeConfigsManager();
        }
        return mInstance;
    }

    /**
     * Used to determine if the provided String is a valid nodeConfigs JSON.
     *
     * @param nodeConfigsString parses as JSON
     * @return if the JSON syntax is valid
     */
    private static boolean isValidJson(String nodeConfigsString) {
        try {
            BBNodeConfigsJson nodeConfigs = new Gson().fromJson(nodeConfigsString, BBNodeConfigsJson.class);
            return nodeConfigs != null;
        } catch (JsonSyntaxException ex) {
            return false;
        }
    }

    public BBNodeConfigsJson getNodeConfigsJson() {
        return mBBNodeConfigsJson;
    }

    private BBNodeConfigsJson createEmptyNodeConfigsJson() {
        return new Gson().fromJson("{\"connections\":[], \"version\":" + RefConstants.NODE_CONFIG_JSON_VERSION + "}", BBNodeConfigsJson.class);
    }

    /**
     * Checks if a node configuration already exists.
     *
     * @param BBNodeConfig
     * @return
     */
    public boolean doesNodeConfigExist(@NonNull BBNodeConfig BBNodeConfig) {
        return mBBNodeConfigsJson.doesNodeConfigExist(BBNodeConfig);
    }

    /**
     * Checks if a node configuration already exists that points to the same destination.
     *
     * @param host
     * @param port
     * @return
     */
    public boolean doesDestinationExist(@NonNull String host, @NonNull int port) {
        List<BBNodeConfig> configList = getAllNodeConfigs(false);
        for (BBNodeConfig tempConfig : configList) {
            if (tempConfig.getHost().equals(host) && tempConfig.getPort() == port) {
                return true;
            }
        }
        return false;
    }


    /**
     * Adds a node configuration to our current setup.
     * Do not forget to call apply() afterwards to make this change permanent.
     *
     * @param alias    Name of the node/configuration
     * @param type     One of the following types: remote, local
     * @param host     The host
     * @param port     The port
     * @param cert     The certificate. This is optional and can be null
     * @param macaroon The Macaroon. Encoded as base16 (hex)
     */
    public BBNodeConfig addNodeConfig(@NonNull String alias, @NonNull String type, @NonNull String implementation, String host,
                                      int port, @Nullable String cert, String macaroon, boolean useTor, boolean verifyHost) {

        // Create the UUID for the new config
        String id = UUID.randomUUID().toString();

        // Create the config
        BBNodeConfig config = new BBNodeConfig(id);
        config.setAlias(alias);
        config.setType(type);
        config.setImplementation(implementation);
        config.setHost(host);
        config.setPort(port);
        config.setCert(cert);
        config.setMacaroon(macaroon);
        config.setUseTor(useTor);
        config.setVerifyCertificate(verifyHost);

        // Add the config to our configurations array
        boolean nodeAdded = mBBNodeConfigsJson.addNode(config);

        if (nodeAdded) {
            BBLog.d(LOG_TAG, "The ID of the created NodeConfig is:" + id);
            return config;
        } else {
            return null;
        }
    }

    /**
     * Updates a node configuration in our current setup.
     * Do not forget to call apply() afterwards to make this change permanent.
     *
     * @param id       UUID of the node/configuration that will be updated
     * @param alias    Name of the node/configuration
     * @param type     One of the following types: remote, local
     * @param host     The host
     * @param port     The port
     * @param cert     The certificate. This is optional and can be null
     * @param macaroon The Macaroon. Encoded as base16 (hex)
     */
    public BBNodeConfig updateNodeConfig(@NonNull String id, @NonNull String alias, @NonNull String implementation, @NonNull String type, String host,
                                         int port, @Nullable String cert, String macaroon, boolean useTor, boolean verifyHost) {

        // Create the config
        BBNodeConfig config = new BBNodeConfig(id);
        config.setAlias(alias);
        config.setType(type);
        config.setImplementation(implementation);
        config.setHost(host);
        config.setPort(port);
        config.setCert(cert);
        config.setMacaroon(macaroon);
        config.setUseTor(useTor);
        config.setVerifyCertificate(verifyHost);

        // Update the config in our configurations array
        boolean nodeUpdated = mBBNodeConfigsJson.updateNodeConfig(config);

        if (nodeUpdated) {
            BBLog.d(LOG_TAG, "NodeConfig updated! (id =" + id + ")");
            return config;
        } else {
            return null;
        }
    }


    /**
     * Returns the node config of the currently active node.
     *
     * @return
     */
    public BBNodeConfig getCurrentNodeConfig() {
        BBNodeConfig config = getNodeConfigById(PrefsUtil.getCurrentNodeConfig());
        if (config == null && hasAnyConfigs()) {
            PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_NODE_CONFIG, ((BBNodeConfig) mBBNodeConfigsJson.mConnections.toArray()[0]).getId()).commit();
            return (BBNodeConfig) mBBNodeConfigsJson.mConnections.toArray()[0];
        }
        return config;
    }


    /**
     * Load a node configuration by its UUID.
     *
     * @param id The UUID of the node
     * @return Returns null if no configuration is found for the given uuid
     */
    public BBNodeConfig getNodeConfigById(@NonNull String id) {
        return mBBNodeConfigsJson.getConnectionById(id);
    }

    /**
     * Returns a List of all node configs sorted alphabetically.
     *
     * @param activeOnTop if true the currently active node is on top, ignoring alphabetical order.
     * @return
     */
    public List<BBNodeConfig> getAllNodeConfigs(boolean activeOnTop) {
        List<BBNodeConfig> sortedList = new ArrayList<>();
        sortedList.addAll(mBBNodeConfigsJson.getConnections());

        if (sortedList.size() > 1) {
            // Sort the list alphabetically
            Collections.sort(sortedList);

            // Move the current config to top
            if (activeOnTop) {
                int index = -1;
                for (BBNodeConfig tempConfig : sortedList) {
                    if (tempConfig.getId().equals(PrefsUtil.getCurrentNodeConfig())) {
                        index = sortedList.indexOf(tempConfig);
                        break;
                    }
                }
                BBNodeConfig currentConfig = sortedList.get(index);
                sortedList.remove(index);
                sortedList.add(0, currentConfig);
            }
        }
        return sortedList;
    }


    /**
     * Renames the desired node config.
     * Do not forget to call apply() afterwards to make this change permanent.
     *
     * @param BBNodeConfig The node config that should be renamed.
     * @param newAlias   The new alias
     * @return false if the old alias did not exist.
     */
    public boolean renameNodeConfig(@NonNull BBNodeConfig BBNodeConfig, @NonNull String newAlias) {
        return mBBNodeConfigsJson.renameNodeConfig(BBNodeConfig, newAlias);
    }

    /**
     * Removes the desired node config.
     * Do not forget to call apply() afterwards to make this change permanent.
     *
     * @param BBNodeConfig
     */
    public boolean removeNodeConfig(@NonNull BBNodeConfig BBNodeConfig) {
        return mBBNodeConfigsJson.removeNodeConfig(BBNodeConfig);
    }

    public boolean hasLocalConfig() {
        if (hasAnyConfigs()) {
            boolean hasLocal = false;
            for (BBNodeConfig BBNodeConfig : mBBNodeConfigsJson.getConnections()) {
                if (BBNodeConfig.isLocal()) {
                    hasLocal = true;
                    break;
                }
            }
            return hasLocal;
        } else {
            return false;
        }
    }

    public boolean hasAnyConfigs() {
        return !mBBNodeConfigsJson.getConnections().isEmpty();
    }

    /**
     * Removes all node configs.
     * Do not forget to call apply() afterwards to make this change permanent.
     */
    public void removeAllNodeConfigs() {
        mBBNodeConfigsJson = createEmptyNodeConfigsJson();
    }

    /**
     * Saves the current state of node configs encrypted to default shared preferences.
     * Always use this after you have changed anything on the configurations.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public void apply() throws GeneralSecurityException, IOException {
        // Convert JSON object to string
        String jsonString = new Gson().toJson(mBBNodeConfigsJson);

        // Save the new node configurations in encrypted prefs
        PrefsUtil.editEncryptedPrefs().putString(PrefsUtil.NODE_CONFIGS, jsonString).commit();
    }
}
