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
        return System.currentTimeMillis() - getNodeAliasInfo(pubkey).getTimestamp() < RefConstants.ALIAS_CHACHE_AGE * 1000L;
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
        PrefsUtil.putSerializable(PrefsUtil.NODE_ALIAS_CACHE, mAliases).apply();
        BBLog.d(LOG_TAG, "Saved Alias cache.");
    }

    /**
     * Loads the node alias cache from shared preferences.
     */
    private void readAliasesFromCache() {
        Object o = PrefsUtil.getSerializable(PrefsUtil.NODE_ALIAS_CACHE, null);
        if (o != null)
            mAliases = (HashSet<NodeAliasInfo>) o;
        else
            mAliases = new HashSet<>();
        BBLog.d(LOG_TAG, "Loaded Alias cache.");
    }
}
