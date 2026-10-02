package app.michaelwuensch.bitbanana.util;

import java.util.HashSet;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.models.NodeAliasInfo;

/**
 * This SINGLETON class is used to load and save aliases.
 * It includes caching to avoid requesting the information over and over.
 */
public class AliasManager {

    private static final String LOG_TAG = AliasManager.class.getSimpleName();
    private static AliasManager mInstance;
    private HashSet<NodeAliasInfo> mAliases = new HashSet<>();

    private AliasManager() {
    }

    public static AliasManager getInstance() {
        if (mInstance == null) {
            mInstance = new AliasManager();
            mInstance.readAliasesFromCache();
        }
        return mInstance;
    }

    public void saveAlias(String pubkey, String alias) {
        mAliases.remove(new NodeAliasInfo(pubkey, ""));
        mAliases.add(new NodeAliasInfo(pubkey, alias));
    }

    public String getAliasWithoutPubkey(String pubkey) {
        return internalGetAlias(pubkey, false);
    }

    public String getAlias(String pubkey) {
        return internalGetAlias(pubkey, true);
    }

    private String internalGetAlias(String pubkey, boolean addPubkey) {
        if (pubkey == null || pubkey.length() < 6)
            return App.getAppContext().getResources().getString(R.string.channel_no_alias);

        ContactsManager cm = ContactsManager.getInstance();
        if (cm.doesContactDataExist(pubkey))
            return cm.getContactByContactData(pubkey).getAlias();

        String alias = "";
        for (NodeAliasInfo nodeAliasInfo : mAliases) {
            if (nodeAliasInfo.getPubKey().equals(pubkey)) {
                if (nodeAliasInfo.AliasEqualsPubkey()) {
                    return getDisplayNameForPubkey(pubkey, addPubkey);
                } else {
                    alias = nodeAliasInfo.getAlias();
                    if (alias == null || alias.isEmpty())
                        return App.getAppContext().getResources().getString(R.string.channel_no_alias);
                    return alias;
                }
            }
        }
        return getDisplayNameForPubkey(pubkey, addPubkey);
    }

    public void updateTimestampForAlias(String pubkey) {
        NodeAliasInfo nodeAliasInfo = getNodeAliasInfo(pubkey);
        mAliases.remove(nodeAliasInfo);
        mAliases.add(new NodeAliasInfo(nodeAliasInfo.getPubKey(), nodeAliasInfo.getAlias()));
    }

    public boolean hasUpToDateAliasInfo(String pubkey) {
        if (!hasAliasInfo(pubkey))
            return false;
        return System.currentTimeMillis() - getNodeAliasInfo(pubkey).getTimestamp() < RefConstants.ALIAS_CACHE_AGE * 1000L;
    }

    public boolean hasAliasInfo(String pubkey) {
        return mAliases.contains(new NodeAliasInfo(pubkey, ""));
    }

    public NodeAliasInfo getNodeAliasInfo(String pubkey) {
        for (NodeAliasInfo nodeAliasInfo : mAliases) {
            if (nodeAliasInfo.getPubKey().equals(pubkey)) {
                return nodeAliasInfo;
            }
        }
        return null;
    }

    private String getDisplayNameForPubkey(String pubkey, boolean addPubkey) {
        String unnamed = App.getAppContext().getResources().getString(R.string.channel_no_alias);
        if (addPubkey)
            return (unnamed + " (" + pubkey.substring(0, 5) + "...)");
        else
            return unnamed;
    }


    /**
     * Used to save node aliases to the shared preferences.
     */
    public void saveAliasesToCache() {
        PrefsUtil.editPrefs().putString(PrefsUtil.NODE_ALIAS_CACHE_JSON, NodeAliasCacheCodec.toJson(mAliases)).apply();
        BBLog.v(LOG_TAG, "Saved Alias cache.");
    }

    /**
     * Loads the node alias cache from shared preferences.
     */
    private void readAliasesFromCache() {
        try {
            mAliases = NodeAliasCacheCodec.fromJson(PrefsUtil.getPrefs().getString(PrefsUtil.NODE_ALIAS_CACHE_JSON, null));
        } catch (RuntimeException e) {
            BBLog.w(LOG_TAG, "Alias cache could not be read: " + BBLog.redactSensitiveException(e));
            mAliases = new HashSet<>();
        }
        migrateLegacyAliasCache();
        BBLog.d(LOG_TAG, "Loaded Alias cache.");
    }

    /**
     * Older versions stored the cache with Java serialization. It is migrated once to JSON.
     * This also happens after restoring a backup created by an older version.
     */
    private void migrateLegacyAliasCache() {
        String legacy = PrefsUtil.getPrefs().getString(PrefsUtil.NODE_ALIAS_CACHE, null);
        if (legacy == null)
            return;
        // Remove it before reading it. If the data is manipulated and reading it crashes the app, this way it can only happen once.
        PrefsUtil.editPrefs().remove(PrefsUtil.NODE_ALIAS_CACHE).commit();
        try {
            HashSet<NodeAliasInfo> legacyAliases = NodeAliasCacheCodec.fromLegacySerialization(legacy);
            NodeAliasCacheCodec.merge(mAliases, legacyAliases);
            saveAliasesToCache();
            BBLog.d(LOG_TAG, "Migrated legacy alias cache with " + legacyAliases.size() + " entries.");
        } catch (Exception | OutOfMemoryError | StackOverflowError e) {
            BBLog.w(LOG_TAG, "Legacy alias cache could not be migrated: " + BBLog.redactSensitiveException(e));
        }
    }

    /**
     * Merges the alias cache of a restored backup into the current cache instead of replacing it.
     * Aliases of nodes that went offline for good cannot be fetched again, so neither the current nor the restored ones must get lost.
     * Replacing the stored value directly would also not work reliably, as this instance keeps the cache in memory and would overwrite it on the next save.
     *
     * @param key   the preference key from the backup (NODE_ALIAS_CACHE_JSON or the legacy NODE_ALIAS_CACHE)
     * @param value the stored cache from the backup
     */
    public void mergeRestoredAliasCache(String key, String value) {
        try {
            HashSet<NodeAliasInfo> restored = key.equals(PrefsUtil.NODE_ALIAS_CACHE)
                    ? NodeAliasCacheCodec.fromLegacySerialization(value)
                    : NodeAliasCacheCodec.fromJson(value);
            int changed = NodeAliasCacheCodec.merge(mAliases, restored);
            saveAliasesToCache();
            BBLog.d(LOG_TAG, "Merged restored alias cache. Added or updated entries: " + changed);
        } catch (Exception | OutOfMemoryError | StackOverflowError e) {
            BBLog.w(LOG_TAG, "Restored alias cache could not be merged: " + BBLog.redactSensitiveException(e));
        }
    }
}
