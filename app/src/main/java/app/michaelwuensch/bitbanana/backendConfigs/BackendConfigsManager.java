package app.michaelwuensch.bitbanana.backendConfigs;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;

/**
 * This SINGLETON class is used to load and save configurations for backends (nodes).
 * Multiple backends can exist simultaneously, but only one can be active at a time.
 * <p>
 * The backend configurations are stored encrypted in the default shared preferences.
 */
public class BackendConfigsManager {

    private static final String LOG_TAG = BackendConfigsManager.class.getSimpleName();
    private static BackendConfigsManager mInstance;
    private BackendConfigsJson mBackendConfigsJson;

    private BackendConfigsManager() {

        String decrypted = null;
        try {
            decrypted = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.BACKEND_CONFIGS, "");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        if (isValidJson(decrypted)) {
            mBackendConfigsJson = new Gson().fromJson(decrypted, BackendConfigsJson.class);
        } else {
            mBackendConfigsJson = createEmptyBackendConfigsJson();
        }

        if (mBackendConfigsJson == null) {
            mBackendConfigsJson = createEmptyBackendConfigsJson();
        }
    }

    // used for unit tests
    public BackendConfigsManager(String BackendConfigsJson) {
        try {
            mBackendConfigsJson = new Gson().fromJson(BackendConfigsJson, BackendConfigsJson.class);
        } catch (JsonSyntaxException e) {
            mBackendConfigsJson = createEmptyBackendConfigsJson();
        }
        if (mBackendConfigsJson == null) {
            mBackendConfigsJson = createEmptyBackendConfigsJson();
        }
    }

    public static BackendConfigsManager getInstance() {
        if (mInstance == null) {
            mInstance = new BackendConfigsManager();
        }
        return mInstance;
    }

    /**
     * Used to determine if the provided String is a valid backendConfigs JSON.
     *
     * @param backendConfigsString parses as JSON
     * @return if the JSON syntax is valid
     */
    private static boolean isValidJson(String backendConfigsString) {
        try {
            BackendConfigsJson backendConfigs = new Gson().fromJson(backendConfigsString, BackendConfigsJson.class);
            return backendConfigs != null;
        } catch (JsonSyntaxException ex) {
            return false;
        }
    }

    public BackendConfigsJson getBackendConfigsJson() {
        return mBackendConfigsJson;
    }

    private BackendConfigsJson createEmptyBackendConfigsJson() {
        BBLog.d(LOG_TAG, "Creating empty BackendConfigsJSON");
        return new Gson().fromJson("{\"connections\":[], \"version\":" + RefConstants.BACKEND_CONFIG_JSON_VERSION + "}", BackendConfigsJson.class);
    }

    /**
     * Checks if a backend configuration already exists.
     *
     * @param BackendConfig
     * @return
     */
    public boolean doesBackendConfigExist(@NonNull BackendConfig BackendConfig) {
        return mBackendConfigsJson.doesBackendConfigExist(BackendConfig);
    }

    /**
     * Checks if a backend configuration already exists that points to the same destination (host + port).
     *
     * @param host
     * @param port
     * @return
     */
    public boolean doesDestinationExist(@NonNull String host, @NonNull int port) {
        List<BackendConfig> configList = getAllBackendConfigs(false);
        for (BackendConfig tempConfig : configList) {
            if (tempConfig.getHost().equals(host) && tempConfig.getPort() == port) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a backend configuration to our current setup.
     * Do not forget to call apply() afterwards to make this change permanent.
     *
     * @param config The config to add. If it has an ID already set it will be overridden.
     */
    public BackendConfig addBackendConfig(@NonNull BackendConfig config) {

        // Create and assign a new UUID for the config
        String id = UUID.randomUUID().toString();
        config.setId(id);

        // Add the config to our configurations array
        boolean backendAdded = mBackendConfigsJson.addBackendConfig(config);

        if (backendAdded) {
            BBLog.d(LOG_TAG, "The ID of the created BackendConfig is: " + id);
            return config;
        } else {
            return null;
        }
    }

    /**
     * Updates a backend configuration in our current setup.
     * Do not forget to call apply() afterwards to make this change permanent.
     *
     * @param config The UUID of the supplied backend config must already exist, otherwise the operation will fail.
     */
    public BackendConfig updateBackendConfig(@NonNull BackendConfig config) {

        // Update the config in our configurations array
        boolean backendUpdated = mBackendConfigsJson.updateBackendConfig(config);

        if (backendUpdated) {
            BBLog.d(LOG_TAG, "BackendConfig updated! (id = " + config.getId() + ")");
            return config;
        } else {
            return null;
        }
    }


    /**
     * Returns the BackendConfig of the currently active backend.
     *
     * @return
     */
    public BackendConfig getCurrentBackendConfig() {
        BackendConfig config = getBackendConfigById(PrefsUtil.getCurrentBackendConfig());
        if (config == null && hasAnyBackendConfigs()) {
            PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_BACKEND_CONFIG, ((BackendConfig) mBackendConfigsJson.mBackendConfigs.toArray()[0]).getId()).commit();
            return (BackendConfig) mBackendConfigsJson.mBackendConfigs.toArray()[0];
        }
        return config;
    }


    /**
     * Load a backend configuration by its UUID.
     *
     * @param id The UUID of the backend
     * @return Returns null if no configuration is found for the given uuid
     */
    public BackendConfig getBackendConfigById(@NonNull String id) {
        return mBackendConfigsJson.getConnectionById(id);
    }

    /**
     * Returns a List of all BackendConfigs sorted alphabetically.
     *
     * @param activeOnTop if true the currently active backend is on top, ignoring alphabetical order.
     * @return
     */
    public List<BackendConfig> getAllBackendConfigs(boolean activeOnTop) {
        List<BackendConfig> sortedList = new ArrayList<>();
        sortedList.addAll(mBackendConfigsJson.getConnections());

        if (sortedList.size() > 1) {
            // Sort the list alphabetically
            Collections.sort(sortedList);

            // Move the current config to top
            if (activeOnTop) {
                int index = -1;
                for (BackendConfig tempConfig : sortedList) {
                    if (tempConfig.getId().equals(PrefsUtil.getCurrentBackendConfig())) {
                        index = sortedList.indexOf(tempConfig);
                        break;
                    }
                }
                if (index != -1) {
                    BackendConfig currentConfig = sortedList.get(index);
                    sortedList.remove(index);
                    sortedList.add(0, currentConfig);
                }
            }
        }
        return sortedList;
    }


    /**
     * Renames the desired BackendConfig.
     * Do not forget to call apply() afterwards to make this change permanent.
     *
     * @param BackendConfig The BackendConfig that should be renamed.
     * @param newAlias      The new alias
     * @return false if the old alias did not exist.
     */
    public boolean renameBackendConfig(@NonNull BackendConfig BackendConfig, @NonNull String newAlias) {
        return mBackendConfigsJson.renameBackendConfig(BackendConfig, newAlias);
    }

    /**
     * Removes the desired BackendConfig.
     * Do not forget to call apply() afterwards to make this change permanent.
     *
     * @param BackendConfig
     */
    public boolean removeBackendConfig(@NonNull BackendConfig BackendConfig) {
        return mBackendConfigsJson.removeBackendConfig(BackendConfig);
    }

    public boolean hasLocalBackendConfig() {
        if (hasAnyBackendConfigs()) {
            boolean hasLocal = false;
            for (BackendConfig BackendConfig : mBackendConfigsJson.getConnections()) {
                if (BackendConfig.isLocal()) {
                    hasLocal = true;
                    break;
                }
            }
            return hasLocal;
        } else {
            return false;
        }
    }

    public boolean hasAnyBackendConfigs() {
        return !mBackendConfigsJson.getConnections().isEmpty();
    }

    /**
     * Removes all BackendConfigs.
     * Do not forget to call apply() afterwards to make this change permanent.
     */
    public void removeAllBackendConfigs() {
        mBackendConfigsJson = createEmptyBackendConfigsJson();
    }

    /**
     * Saves the current state of BackendConfigs encrypted to default shared preferences.
     * Always use this after you have changed anything on the configurations.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public void apply() throws GeneralSecurityException, IOException {
        // Convert JSON object to string
        String jsonString = new Gson().toJson(mBackendConfigsJson);

        // Save the new backend configurations in encrypted prefs
        PrefsUtil.editEncryptedPrefs().putString(PrefsUtil.BACKEND_CONFIGS, jsonString).commit();
    }
}
