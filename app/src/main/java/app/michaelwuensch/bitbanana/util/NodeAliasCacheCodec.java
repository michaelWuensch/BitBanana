package app.michaelwuensch.bitbanana.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.michaelwuensch.bitbanana.models.NodeAliasInfo;

/**
 * Converts the node alias cache to and from its stored form.
 * <p>
 * The cache is stored as JSON. Older versions stored it with Java serialization. Deserializing data with a plain ObjectInputStream
 * can instantiate any class on the classpath, which is dangerous as the cache is also part of backups (a manipulated backup could inject arbitrary objects).
 * The old format is therefore only read once to migrate it and with a stream that only allows the classes the cache actually consists of.
 */
public class NodeAliasCacheCodec {

    // The cache is valuable (aliases of nodes that went offline for good cannot be fetched again), but it never gets anywhere near this size.
    // Larger data is not a real cache and is rejected to protect against memory exhaustion.
    static final int MAX_LEGACY_LENGTH = 10 * 1024 * 1024;

    private static final Type LIST_TYPE = new TypeToken<ArrayList<NodeAliasInfo>>() {
    }.getType();

    public static String toJson(@NonNull Set<NodeAliasInfo> aliases) {
        return new Gson().toJson(new ArrayList<>(aliases), LIST_TYPE);
    }

    /**
     * Entries without pubkey or alias are dropped, as the rest of the app relies on both being present.
     *
     * @throws RuntimeException if the JSON is malformed
     */
    @NonNull
    public static HashSet<NodeAliasInfo> fromJson(@Nullable String json) {
        HashSet<NodeAliasInfo> aliases = new HashSet<>();
        if (json == null || json.isEmpty())
            return aliases;
        // Parsed as list first. Adding to the set calls hashCode(), which would fail for entries without pubkey.
        List<NodeAliasInfo> list = new Gson().fromJson(json, LIST_TYPE);
        if (list == null)
            return aliases;
        for (NodeAliasInfo info : list) {
            if (isValid(info))
                aliases.add(info);
        }
        return aliases;
    }

    /**
     * Reads a cache that was stored with Java serialization by older versions of BitBanana. Only used for migration.
     *
     * @throws IOException if the data is invalid or contains anything else than a set of NodeAliasInfo
     */
    @NonNull
    static HashSet<NodeAliasInfo> fromLegacySerialization(@Nullable String serialized) throws IOException {
        HashSet<NodeAliasInfo> aliases = new HashSet<>();
        if (serialized == null || serialized.isEmpty())
            return aliases;
        if (serialized.length() > MAX_LEGACY_LENGTH)
            throw new IOException("Legacy alias cache is too large");

        Object object;
        try (ObjectInputStream in = new RestrictedObjectInputStream(new ByteArrayInputStream(decodeLegacyBytes(serialized)))) {
            object = in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        if (!(object instanceof HashSet))
            throw new InvalidObjectException("Legacy alias cache is not a set");
        for (Object element : (HashSet<?>) object) {
            if (element instanceof NodeAliasInfo && isValid((NodeAliasInfo) element))
                aliases.add((NodeAliasInfo) element);
        }
        return aliases;
    }

    /**
     * Merges incoming aliases into the target. Unknown pubkeys are added. For known pubkeys the entry with the newer timestamp is kept.
     * Nothing is ever removed, as aliases of nodes that went offline for good cannot be fetched again.
     *
     * @return the number of added or updated entries
     */
    public static int merge(@NonNull Set<NodeAliasInfo> target, @NonNull Collection<NodeAliasInfo> incoming) {
        int changed = 0;
        for (NodeAliasInfo info : incoming) {
            if (!isValid(info))
                continue;
            NodeAliasInfo existing = null;
            for (NodeAliasInfo candidate : target) {
                if (candidate.getPubKey().equals(info.getPubKey())) {
                    existing = candidate;
                    break;
                }
            }
            if (existing == null || info.getTimestamp() > existing.getTimestamp()) {
                // HashSet.add() does not replace an equal entry (equality is based on the pubkey), so it has to be removed first.
                if (existing != null)
                    target.remove(existing);
                target.add(info);
                changed++;
            }
        }
        return changed;
    }

    private static boolean isValid(@Nullable NodeAliasInfo info) {
        return info != null && info.getPubKey() != null && info.getAlias() != null;
    }

    /**
     * Counterpart of the encoding the old ObjectSerializer used: every byte was stored as two characters 'a' to 'p'.
     */
    private static byte[] decodeLegacyBytes(String str) throws IOException {
        if (str.length() % 2 != 0)
            throw new IOException("Invalid legacy alias cache encoding");
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length(); i += 2) {
            int high = str.charAt(i) - 'a';
            int low = str.charAt(i + 1) - 'a';
            if (high < 0 || high > 15 || low < 0 || low > 15)
                throw new IOException("Invalid legacy alias cache encoding");
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    /**
     * Only allows the classes a legacy alias cache consists of (a HashSet of NodeAliasInfo, which only contains Strings and a long).
     * This prevents the instantiation of any other class, which could otherwise be abused (deserialization gadgets).
     * It also only allows a single set, as nested sets can be used to make deserialization take practically forever.
     */
    private static class RestrictedObjectInputStream extends ObjectInputStream {
        private static final Set<String> ALLOWED_CLASSES = new HashSet<>(Arrays.asList(
                HashSet.class.getName(),
                NodeAliasInfo.class.getName()
        ));
        private int mSetCount = 0;

        RestrictedObjectInputStream(InputStream in) throws IOException {
            super(in);
            enableResolveObject(true);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            if (!ALLOWED_CLASSES.contains(desc.getName()))
                throw new InvalidClassException(desc.getName(), "Class not allowed in node alias cache");
            return super.resolveClass(desc);
        }

        @Override
        protected Object resolveObject(Object obj) throws IOException {
            if (obj instanceof HashSet && ++mSetCount > 1)
                throw new InvalidObjectException("Nested sets are not allowed in node alias cache");
            return obj;
        }
    }
}
