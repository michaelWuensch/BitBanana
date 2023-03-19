package app.michaelwuensch.bitbanana.models;

import androidx.annotation.Nullable;

import java.io.Serializable;

public class NodeAliasInfo implements Serializable {
    private String mPubKey;
    private String mAlias;
    private long mTimestamp;

    public NodeAliasInfo(String pubkey, String alias) {
        mPubKey = pubkey;
        mAlias = alias;
        mTimestamp = System.currentTimeMillis();
    }

    public boolean AliasEqualsPubkey() {
        // Some nodes use a shortened version of the pubkey. Therefore we check if the first 10 are equal.
        if (mAlias.length() < 10)
            return false;
        return mAlias.startsWith(mPubKey.substring(0, 9));
    }

    public String getAlias() {
        return mAlias;
    }

    public String getPubKey() {
        return mPubKey;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        return this.getPubKey().equals(((NodeAliasInfo) obj).getPubKey());
    }

    @Override
    public int hashCode() {
        return this.getPubKey().hashCode();
    }
}
