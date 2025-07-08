package app.michaelwuensch.bitbanana.backup;

import java.util.Map;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.contacts.Contact;

public class DataBackup {
    private BackendConfig[] connections;
    private Contact[] contacts;

    /**
     * deprecated!
     */
    private Map<String, ?> settings;

    private String settingsJson;
    private String labelsJson;

    public BackendConfig[] getBackendConfigs() {
        return connections;
    }

    public Contact[] getContacts() {
        return contacts;
    }

    public Map<String, ?> getSettings() {
        return settings;
    }

    public void setWalletConfigs(BackendConfig[] mBackendConfigs) {
        this.connections = mBackendConfigs;
    }

    public void setContacts(Contact[] mContacts) {
        this.contacts = mContacts;
    }

    public void setSettings(Map<String, ?> settings) {
        this.settings = settings;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }

    public String getLabelsJson() {
        return labelsJson;
    }

    public void setLabelsJson(String labelsJson) {
        this.labelsJson = labelsJson;
    }
}
