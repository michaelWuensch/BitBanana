package app.michaelwuensch.bitbanana.backup;

import android.content.SharedPreferences;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import app.michaelwuensch.bitbanana.contacts.Contact;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.labels.LabelsManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.EncryptionUtil;
import app.michaelwuensch.bitbanana.util.GsonUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UtilFunctions;

public class DataBackupUtil {

    private static final String LOG_TAG = DataBackupUtil.class.getSimpleName();
    public static final String BACKUP_FILE_IDENTIFIER = "BB_Backup:";
    // To allow importing zap backups
    public static final String ZAP_BACKUP_FILE_IDENTIFIER = "ZapBackup:";


    public static byte[] createBackup(String password, int backupVersion) {
        DataBackup backupObject = new DataBackup();

        // Contacts
        if (ContactsManager.getInstance().hasAnyContacts()) {
            List<Contact> contactsList = ContactsManager.getInstance().getAllContacts();
            Contact[] contacts = new Contact[contactsList.size()];
            for (int i = 0; i < contactsList.size(); i++) {
                contacts[i] = contactsList.get(i);
            }
            backupObject.setContacts(contacts);
        }

        // Wallets
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            List<BackendConfig> backendConfigList = BackendConfigsManager.getInstance().getAllBackendConfigs(false);
            BackendConfig[] configs = new BackendConfig[backendConfigList.size()];
            for (int i = 0; i < backendConfigList.size(); i++) {
                configs[i] = backendConfigList.get(i);
            }
            backupObject.setWalletConfigs(configs);
        }

        // Settings
        Map<String, ?> allEntries = PrefsUtil.getPrefs().getAll();
        Map<String, Object> filteredEntries = new HashMap<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("fiat_")) // don't include fiat exchange rates in backup
                continue;
            filteredEntries.put(entry.getKey(), entry.getValue());
        }

        // Labels
        backupObject.setLabelsJson(LabelsManager.getInstance().getWalletLabelsCollectionJson());

        String settingsJsonString = new Gson().toJson(filteredEntries);
        backupObject.setSettingsJson(settingsJsonString);

        // Serialize backupObject as JSON
        String backupJson = new Gson().toJson(backupObject);

        // Convert json backup to bytes
        byte[] backupBytes = backupJson.getBytes(StandardCharsets.UTF_8);

        // Encrypt backup
        byte[] encryptedBackupBytes = EncryptionUtil.PasswordEncryptData(backupBytes, password, RefConstants.DATA_BACKUP_NUM_HASH_ITERATIONS);

        // Construct final backup. (10 bytes file identifier + 4 bytes backupVersion + encrypted backup)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(BACKUP_FILE_IDENTIFIER.getBytes(StandardCharsets.UTF_8));
            outputStream.write(UtilFunctions.intToByteArray(backupVersion));
            outputStream.write(encryptedBackupBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Return final backup as UTF-8 string
        return outputStream.toByteArray();
    }

    public static boolean isThereAnythingToBackup() {
        return BackendConfigsManager.getInstance().hasAnyBackendConfigs() || ContactsManager.getInstance().hasAnyContacts();
    }

    public static boolean restoreBackup(String backup, int backupVersion) {
        if (backupVersion < 6) {
            DataBackup dataBackup = new Gson().fromJson(backup, DataBackup.class);

            // restore backend configs
            if (dataBackup.getBackendConfigs() != null && dataBackup.getBackendConfigs().length > 0) {
                BBLog.d(LOG_TAG, "Restoring connections ...");
                BackendConfigsManager.getInstance().removeAllBackendConfigs();
                for (BackendConfig backendConfig : dataBackup.getBackendConfigs()) {
                    BackendConfigsManager.getInstance().addBackendConfig(backendConfig);
                }
                try {
                    BackendConfigsManager.getInstance().apply();
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            if (backupVersion < 5) {
                // AvatarMaterial -> WalletID
                BBLog.d(LOG_TAG, "Updating connections from old backup version (<5) ...");
                List<BackendConfig> backendConfigs = BackendConfigsManager.getInstance().getAllBackendConfigs(false);
                for (BackendConfig backendConfig : backendConfigs) {
                    backendConfig.setWalletID(backendConfig.getAvatarMaterial());
                    BackendConfigsManager.getInstance().updateBackendConfig(backendConfig);
                }
                try {
                    BackendConfigsManager.getInstance().apply();
                    BBLog.d(LOG_TAG, "Connections updated.");
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }

            if (backupVersion > 4) {
                // restore settings with the new method
                if (dataBackup.getSettingsJson() != null) {
                    BBLog.d(LOG_TAG, "Restoring settings ...");
                    Type type = new TypeToken<Map<String, Object>>() {
                    }.getType();
                    Map<String, Object> restoredMap = GsonUtil.getTypeSafeGson().fromJson(dataBackup.getSettingsJson(), type);

                    // Save to SharedPreferences
                    SharedPreferences.Editor editor = PrefsUtil.editPrefs();

                    for (Map.Entry<String, ?> entry : restoredMap.entrySet()) {
                        Object value = entry.getValue();

                        if (entry.getKey().equals(PrefsUtil.SETTINGS_VERSION))  // this should not be loaded from backup
                            continue;
                        if (entry.getKey().equals("stealthModeActive")) // stealth mode changes require additional code to execute. Ignore for backups.
                            continue;
                        if (entry.getKey().equals("language")) // language change causes problems during restore...
                            continue;
                        if (entry.getKey().startsWith("fiat_")) // we don't want outdated fiat exchange rates...
                            continue;
                        if (entry.getKey().equals(PrefsUtil.PIN_LENGTH)) // As we don't save the PIN which is in the encryptedPrefs, we also don't want to have the PIN length.
                            continue;

                        if (value instanceof Boolean) {
                            editor.putBoolean(entry.getKey(), (Boolean) value);
                        } else if (value instanceof Float) {
                            editor.putFloat(entry.getKey(), (Float) value);
                        } else if (value instanceof Integer) {
                            editor.putInt(entry.getKey(), (Integer) value);
                        } else if (value instanceof Long) {
                            editor.putLong(entry.getKey(), (Long) value);
                        } else if (value instanceof String) {
                            editor.putString(entry.getKey(), (String) value);
                        }
                    }
                    editor.apply();
                    BBLog.d(LOG_TAG, "Settings restored.");
                }

                // restore labels
                BBLog.d(LOG_TAG, "Restoring labels ...");
                LabelsManager.getInstance().restoreWalletLabelsCollectionJson(dataBackup.getLabelsJson());
                BBLog.d(LOG_TAG, "Labels restored.");
            }

            if (backupVersion == 4) {
                // restore settings with the old method
                if (dataBackup.getSettings() != null) {
                    BBLog.d(LOG_TAG, "Restoring settings ...");
                    Map<String, ?> restoredMap = dataBackup.getSettings();

                    // Save to SharedPreferences
                    SharedPreferences.Editor editor = PrefsUtil.editPrefs();

                    for (Map.Entry<String, ?> entry : restoredMap.entrySet()) {
                        Object value = entry.getValue();

                        if (entry.getKey().equals(PrefsUtil.SETTINGS_VERSION))  // this should not be loaded from backup
                            continue;
                        if (entry.getKey().equals("stealthModeActive")) // stealth mode changes require additional code to execute. Ignore for backups.
                            continue;
                        if (entry.getKey().equals("language")) // language change causes problems during restore...
                            continue;
                        if (entry.getKey().startsWith("fiat_")) // we don't want outdated fiat exchange rates...
                            continue;
                        if (entry.getKey().equals(PrefsUtil.PIN_LENGTH)) continue;

                        if (value instanceof Boolean) {
                            editor.putBoolean(entry.getKey(), (Boolean) value);
                        } else if (value instanceof Float) {
                            editor.putFloat(entry.getKey(), (Float) value);
                        } else if (value instanceof Integer) {
                            editor.putInt(entry.getKey(), (Integer) value);
                        } else if (value instanceof Long) {
                            editor.putLong(entry.getKey(), (Long) value);
                        } else if (value instanceof String) {
                            editor.putString(entry.getKey(), (String) value);
                        }
                    }
                    editor.apply();
                    BBLog.d(LOG_TAG, "Settings restored.");
                }
            }

            if (backupVersion == 2) {
                // This is an old backup that did not contain info for network. Apply defaults. Moreover backend values changed therefore, set it to default.
                BBLog.d(LOG_TAG, "Updating connections from old backup version (2) ...");
                List<BackendConfig> backendConfigs = BackendConfigsManager.getInstance().getAllBackendConfigs(false);
                for (BackendConfig backendConfig : backendConfigs) {
                    if (backendConfig.getMacaroon() != null) {
                        backendConfig.setAuthenticationToken(backendConfig.getMacaroon().toLowerCase());
                        backendConfig.setMacaroon(null);
                    }
                    if (backendConfig.getServerCert() != null)
                        backendConfig.setServerCert(BaseEncoding.base64().encode(BaseEncoding.base64Url().decode(backendConfig.getServerCert())));
                    BackendConfigsManager.getInstance().updateBackendConfig(backendConfig);
                }
                try {
                    BackendConfigsManager.getInstance().apply();
                    BBLog.d(LOG_TAG, "Connections restored.");
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }

            if (backupVersion == 1) {
                // This is an old backup that did not contain info for network. Apply defaults. Moreover backend values changed therefore, set it to default.
                BBLog.d(LOG_TAG, "Updating connections from old backup version (1) ...");
                List<BackendConfig> backendConfigs = BackendConfigsManager.getInstance().getAllBackendConfigs(false);
                for (BackendConfig backendConfig : backendConfigs) {
                    // Adds the defaults to the newly introduced or renamed config properties.
                    backendConfig.setLocation(BackendConfig.Location.REMOTE);
                    backendConfig.setNetwork(BackendConfig.Network.UNKNOWN);
                    backendConfig.setBackendType(BackendConfig.BackendType.LND_GRPC);
                    backendConfig.setVpnConfig(new VPNConfig());
                    if (backendConfig.getMacaroon() != null) {
                        backendConfig.setAuthenticationToken(backendConfig.getMacaroon().toLowerCase());
                        backendConfig.setMacaroon(null);
                    }
                    if (backendConfig.getServerCert() != null)
                        backendConfig.setServerCert(BaseEncoding.base64().encode(BaseEncoding.base64Url().decode(backendConfig.getServerCert())));
                    BackendConfigsManager.getInstance().updateBackendConfig(backendConfig);
                }
                try {
                    BackendConfigsManager.getInstance().apply();
                    BBLog.d(LOG_TAG, "Connections restored.");
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }

            if (backupVersion == 0) {
                // This is an old backup that did not contain info for backend, network, useTor & verify certificate. Apply defaults.
                BBLog.d(LOG_TAG, "Updating connections from old backup version (0) ...");
                List<BackendConfig> backendConfigs = BackendConfigsManager.getInstance().getAllBackendConfigs(false);
                for (BackendConfig backendConfig : backendConfigs) {
                    // Adds the defaults to the newly introduced or renamed config properties.
                    backendConfig.setLocation(BackendConfig.Location.REMOTE);
                    backendConfig.setNetwork(BackendConfig.Network.UNKNOWN);
                    backendConfig.setBackendType(BackendConfig.BackendType.LND_GRPC);
                    backendConfig.setUseTor(backendConfig.isTorHostAddress());
                    backendConfig.setVerifyCertificate(!backendConfig.isTorHostAddress());
                    backendConfig.setVpnConfig(new VPNConfig());
                    if (backendConfig.getMacaroon() != null) {
                        backendConfig.setAuthenticationToken(backendConfig.getMacaroon().toLowerCase());
                        backendConfig.setMacaroon(null);
                    }
                    if (backendConfig.getServerCert() != null)
                        backendConfig.setServerCert(BaseEncoding.base64().encode(BaseEncoding.base64Url().decode(backendConfig.getServerCert())));
                    BackendConfigsManager.getInstance().updateBackendConfig(backendConfig);
                }
                try {
                    BackendConfigsManager.getInstance().apply();
                    BBLog.d(LOG_TAG, "Connections restored.");
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }

            // restore contacts
            if (dataBackup.getContacts() != null && dataBackup.getContacts().length > 0) {
                BBLog.d(LOG_TAG, "Restoring contacts ...");
                ContactsManager.getInstance().removeAllContacts();
                for (Contact contact : dataBackup.getContacts()) {
                    ContactsManager.getInstance().addContact(contact.getContactType(), contact.getContactData(), contact.getAlias(), contact.wasAddedInEmergencyMode());
                }
                try {
                    ContactsManager.getInstance().apply();
                    BBLog.d(LOG_TAG, "Contacts restored.");
                } catch (IOException | CertificateException | NoSuchAlgorithmException |
                         InvalidKeyException | UnrecoverableEntryException |
                         InvalidAlgorithmParameterException | NoSuchPaddingException |
                         NoSuchProviderException | BadPaddingException | KeyStoreException |
                         IllegalBlockSizeException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
