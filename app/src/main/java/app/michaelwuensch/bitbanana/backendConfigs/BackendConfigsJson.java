package app.michaelwuensch.bitbanana.backendConfigs;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

public class BackendConfigsJson {

    @SerializedName("connections")
    Set<BackendConfig> mBackendConfigs;

    @SerializedName("version")
    int version;

    public BackendConfig getConnectionById(@NonNull String id) {
        for (BackendConfig backendConfig : mBackendConfigs) {
            if (backendConfig.getId().equals(id)) {
                return backendConfig;
            }
        }
        return null;
    }

    public BackendConfig getConnectionByAlias(@NonNull String alias) {
        for (BackendConfig backendConfig : mBackendConfigs) {
            if (backendConfig.getAlias().toLowerCase().equals(alias.toLowerCase())) {
                return backendConfig;
            }
        }
        return null;
    }

    public Set<BackendConfig> getConnections() {
        return mBackendConfigs;
    }

    boolean doesBackendConfigExist(@NonNull BackendConfig BackendConfig) {
        return mBackendConfigs.contains(BackendConfig);
    }

    boolean addBackendConfig(@NonNull BackendConfig BackendConfig) {
        return mBackendConfigs.add(BackendConfig);
    }

    boolean removeBackendConfig(BackendConfig BackendConfig) {
        return mBackendConfigs.remove(BackendConfig);
    }

    boolean updateBackendConfig(BackendConfig BackendConfig) {
        if (doesBackendConfigExist(BackendConfig)) {
            mBackendConfigs.remove(BackendConfig);
            mBackendConfigs.add(BackendConfig);
            return true;
        } else {
            return false;
        }
    }

    boolean renameBackendConfig(BackendConfig BackendConfig, @NonNull String newAlias) {
        if (doesBackendConfigExist(BackendConfig)) {
            BackendConfig tempConfig = getConnectionById(BackendConfig.getId());
            tempConfig.setAlias(newAlias);
            mBackendConfigs.remove(BackendConfig);
            mBackendConfigs.add(tempConfig);
            return true;
        } else {
            return false;
        }
    }
}
