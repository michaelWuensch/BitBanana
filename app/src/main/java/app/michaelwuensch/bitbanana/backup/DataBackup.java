package app.michaelwuensch.bitbanana.backup;

import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.BBNodeConfig;
import app.michaelwuensch.bitbanana.contacts.Contact;

public class DataBackup {
    private BBNodeConfig[] connections;
    private Contact[] contacts;

    public BBNodeConfig[] getWalletConfigs() {
        return connections;
    }

    public Contact[] getContacts() {
        return contacts;
    }

    public void setWalletConfigs(BBNodeConfig[] mBBNodeConfigs) {
        this.connections = mBBNodeConfigs;
    }

    public void setContacts(Contact[] mContacts) {
        this.contacts = mContacts;
    }
}
