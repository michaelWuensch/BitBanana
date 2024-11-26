package app.michaelwuensch.bitbanana.backup;

import java.util.Map;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.contacts.Contact;

public class DataBackup {
    private BackendConfig[] connections;
    private Contact[] contacts;
    private Map<String, ?> settings;

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
}
