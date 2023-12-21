package app.michaelwuensch.bitbanana.backendConfigs.manageNodeConfigs;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

public class BBNodeConfigsJson {

    @SerializedName("connections")
    Set<BBNodeConfig> mConnections;

    @SerializedName("version")
    int version;

    public BBNodeConfig getConnectionById(@NonNull String id) {
        for (BBNodeConfig nodeConnectionConfig : mConnections) {
            if (nodeConnectionConfig.getId().equals(id)) {
                return nodeConnectionConfig;
            }
        }
        return null;
    }

    public BBNodeConfig getConnectionByAlias(@NonNull String alias) {
        for (BBNodeConfig nodeConnectionConfig : mConnections) {
            if (nodeConnectionConfig.getAlias().toLowerCase().equals(alias.toLowerCase())) {
                return nodeConnectionConfig;
            }
        }
        return null;
    }

    public Set<BBNodeConfig> getConnections() {
        return mConnections;
    }

    boolean doesNodeConfigExist(@NonNull BBNodeConfig BBNodeConfig) {
        return mConnections.contains(BBNodeConfig);
    }

    boolean addNode(@NonNull BBNodeConfig BBNodeConfig) {
        return mConnections.add(BBNodeConfig);
    }

    boolean removeNodeConfig(BBNodeConfig BBNodeConfig) {
        return mConnections.remove(BBNodeConfig);
    }

    boolean updateNodeConfig(BBNodeConfig BBNodeConfig) {
        if (doesNodeConfigExist(BBNodeConfig)) {
            mConnections.remove(BBNodeConfig);
            mConnections.add(BBNodeConfig);
            return true;
        } else {
            return false;
        }
    }

    boolean renameNodeConfig(BBNodeConfig BBNodeConfig, @NonNull String newAlias) {
        if (doesNodeConfigExist(BBNodeConfig)) {
            BBNodeConfig tempConfig = getConnectionById(BBNodeConfig.getId());
            tempConfig.setAlias(newAlias);
            mConnections.remove(BBNodeConfig);
            mConnections.add(tempConfig);
            return true;
        } else {
            return false;
        }
    }
}
