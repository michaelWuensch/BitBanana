package app.michaelwuensch.bitbanana.backup;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.contacts.Contact;

public class DataBackup {
    private BackendConfig[] connections;
    private Contact[] contacts;

    public BackendConfig[] getBackendConfigs() {
        return connections;
    }

    public Contact[] getContacts() {
        return contacts;
    }

    public void setWalletConfigs(BackendConfig[] mBackendConfigs) {
        this.connections = mBackendConfigs;
    }

    public void setContacts(Contact[] mContacts) {
        this.contacts = mContacts;
    }
}
