package app.michaelwuensch.bitbanana.util;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import app.michaelwuensch.bitbanana.models.NodeAliasInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NodeAliasCacheCodecTest {

    private static final String PUBKEY_1 = "02aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String PUBKEY_2 = "03bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    /**
     * Creates data exactly like the old ObjectSerializer did: Java serialization, every byte encoded as two characters 'a' to 'p'.
     */
    private static String legacySerialize(Serializable object) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes.toByteArray()) {
            sb.append((char) (((b >> 4) & 0xF) + 'a'));
            sb.append((char) ((b & 0xF) + 'a'));
        }
        return sb.toString();
    }

    private static HashSet<NodeAliasInfo> twoAliases() {
        HashSet<NodeAliasInfo> aliases = new HashSet<>();
        aliases.add(new NodeAliasInfo(PUBKEY_1, "ACINQ"));
        aliases.add(new NodeAliasInfo(PUBKEY_2, "Node that went offline forever"));
        return aliases;
    }

    private static NodeAliasInfo find(Set<NodeAliasInfo> aliases, String pubkey) {
        for (NodeAliasInfo info : aliases)
            if (info.getPubKey().equals(pubkey))
                return info;
        return null;
    }

    @Test
    public void givenAliases_whenJsonRoundTrip_thenAllDataIsKept() {
        HashSet<NodeAliasInfo> original = twoAliases();

        HashSet<NodeAliasInfo> restored = NodeAliasCacheCodec.fromJson(NodeAliasCacheCodec.toJson(original));

        assertEquals(2, restored.size());
        for (NodeAliasInfo info : original) {
            NodeAliasInfo copy = find(restored, info.getPubKey());
            assertEquals(info.getAlias(), copy.getAlias());
            assertEquals(info.getTimestamp(), copy.getTimestamp());
        }
    }

    @Test
    public void givenJsonWithInvalidEntries_whenFromJson_thenTheyAreDroppedWithoutCrash() {
        String json = "[{\"mPubKey\":\"" + PUBKEY_1 + "\",\"mAlias\":\"ACINQ\",\"mTimestamp\":1}," +
                "{\"mAlias\":\"no pubkey\"}," +
                "{\"mPubKey\":\"" + PUBKEY_2 + "\"}," +
                "null]";

        HashSet<NodeAliasInfo> aliases = NodeAliasCacheCodec.fromJson(json);

        assertEquals(1, aliases.size());
        assertEquals("ACINQ", find(aliases, PUBKEY_1).getAlias());
    }

    @Test
    public void givenEmptyOrMissingJson_whenFromJson_thenEmptySet() {
        assertTrue(NodeAliasCacheCodec.fromJson(null).isEmpty());
        assertTrue(NodeAliasCacheCodec.fromJson("").isEmpty());
        assertTrue(NodeAliasCacheCodec.fromJson("[]").isEmpty());
    }

    @Test
    public void givenLegacyCache_whenFromLegacySerialization_thenAllDataIsMigrated() throws IOException {
        HashSet<NodeAliasInfo> original = twoAliases();

        HashSet<NodeAliasInfo> migrated = NodeAliasCacheCodec.fromLegacySerialization(legacySerialize(original));

        assertEquals(2, migrated.size());
        for (NodeAliasInfo info : original) {
            NodeAliasInfo copy = find(migrated, info.getPubKey());
            assertEquals(info.getAlias(), copy.getAlias());
            assertEquals(info.getTimestamp(), copy.getTimestamp());
        }
    }

    @Test
    public void givenLegacyDataWithOtherClass_whenFromLegacySerialization_thenRejected() throws IOException {
        // Only HashSet and NodeAliasInfo are allowed. Any other class could be a deserialization gadget.
        HashMap<String, String> map = new HashMap<>();
        map.put("a", "b");
        ArrayList<NodeAliasInfo> list = new ArrayList<>(twoAliases());

        assertRejected(legacySerialize(map));
        assertRejected(legacySerialize(list));
    }

    @Test(timeout = 5000)
    public void givenLegacyDataWithNestedSets_whenFromLegacySerialization_thenRejectedQuickly() throws IOException {
        // Known denial of service: nested sets make deserialization take practically forever because of the hashing.
        Set<Object> root = new HashSet<>();
        Set<Object> s1 = root;
        Set<Object> s2 = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Set<Object> t1 = new HashSet<>();
            Set<Object> t2 = new HashSet<>();
            t1.add("foo");
            s1.add(t1);
            s1.add(t2);
            s2.add(t1);
            s2.add(t2);
            s1 = t1;
            s2 = t2;
        }

        assertRejected(legacySerialize((Serializable) root));
    }

    @Test
    public void givenInvalidOrOversizedLegacyData_whenFromLegacySerialization_thenRejected() {
        assertRejected("xyz");
        assertRejected("abc");
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i <= NodeAliasCacheCodec.MAX_LEGACY_LENGTH; i++)
            huge.append('a');
        assertRejected(huge.toString());
    }

    /**
     * Legacy alias cache with two entries (02aa…: "ACINQ", 03bb…: "OldNodeOfflineForever"), created with the old ObjectSerializer
     * and NodeAliasInfo BEFORE its serialVersionUID was declared explicitly. Do not regenerate it with the current class.
     */
    private static final String LEGACY_CACHE_FROM_OLD_CLASS =
            "kmonaaafhdhcaabbgkgbhggbcohfhegjgmcoeigbhdgifdgfhelkeeifjfjglilhdeadaaaahihahhamaaaaaabadpeaaaaaaaaa" +
            "aaachdhcaadbgbhahacogngjgdgigbgfgmhhhfgfgohdgdgicogcgjhegcgbgogbgogbcogngpgegfgmhdcoeogpgegfebgmgjgb" +
            "hdejgogggpidgcaagefdihidgeacaaadekaaakgnfegjgngfhdhegbgnhaemaaaggnebgmgjgbhdheaabcemgkgbhggbcpgmgbgo" +
            "ghcpfdhehcgjgoghdlemaaahgnfahfgcelgfhjhbaahoaaadhihaaaaaabkapinijpcmheaabfepgmgeeogpgegfepgggggmgjgo" +
            "gfeggphcgfhggfhcheaaecdaddgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgc" +
            "gcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgcgchdhbaahoaaacaaaaabkapinijpcmheaaafebedejeofbhe" +
            "aaecdadcgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgb" +
            "gbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbgbhi";

    @Test
    public void givenSerialVersionUid_thenItMatchesTheValueComputedBeforeItWasDeclared() {
        // If this fails, legacy alias caches and old backups can no longer be read.
        assertEquals(-8979614276125228188L, ObjectStreamClass.lookup(NodeAliasInfo.class).getSerialVersionUID());
    }

    @Test
    public void givenLegacyCacheCreatedWithOldClass_whenFromLegacySerialization_thenItIsStillReadable() throws IOException {
        HashSet<NodeAliasInfo> migrated = NodeAliasCacheCodec.fromLegacySerialization(LEGACY_CACHE_FROM_OLD_CLASS);

        assertEquals(2, migrated.size());
        assertEquals("ACINQ", find(migrated, PUBKEY_1).getAlias());
        assertEquals("OldNodeOfflineForever", find(migrated, PUBKEY_2).getAlias());
    }

    private static NodeAliasInfo alias(String pubkey, String alias, long timestamp) {
        String json = "[{\"mPubKey\":\"" + pubkey + "\",\"mAlias\":\"" + alias + "\",\"mTimestamp\":" + timestamp + "}]";
        return NodeAliasCacheCodec.fromJson(json).iterator().next();
    }

    @Test
    public void givenNewPubkey_whenMerge_thenAdded() {
        HashSet<NodeAliasInfo> target = new HashSet<>(Collections.singletonList(alias(PUBKEY_1, "ACINQ", 100)));

        int changed = NodeAliasCacheCodec.merge(target, Collections.singletonList(alias(PUBKEY_2, "Offline node", 50)));

        assertEquals(1, changed);
        assertEquals(2, target.size());
        assertEquals("ACINQ", find(target, PUBKEY_1).getAlias());
        assertEquals("Offline node", find(target, PUBKEY_2).getAlias());
    }

    @Test
    public void givenSamePubkeyWithNewerTimestamp_whenMerge_thenReplaced() {
        HashSet<NodeAliasInfo> target = new HashSet<>(Collections.singletonList(alias(PUBKEY_1, "Old name", 100)));

        int changed = NodeAliasCacheCodec.merge(target, Collections.singletonList(alias(PUBKEY_1, "New name", 200)));

        assertEquals(1, changed);
        assertEquals(1, target.size());
        assertEquals("New name", find(target, PUBKEY_1).getAlias());
    }

    @Test
    public void givenSamePubkeyWithOlderTimestamp_whenMerge_thenKept() {
        HashSet<NodeAliasInfo> target = new HashSet<>(Collections.singletonList(alias(PUBKEY_1, "New name", 200)));

        int changed = NodeAliasCacheCodec.merge(target, Collections.singletonList(alias(PUBKEY_1, "Old name", 100)));

        assertEquals(0, changed);
        assertNotNull(find(target, PUBKEY_1));
        assertEquals("New name", find(target, PUBKEY_1).getAlias());
    }

    @Test
    public void givenMerge_thenNothingIsEverRemoved() {
        HashSet<NodeAliasInfo> target = new HashSet<>(Collections.singletonList(alias(PUBKEY_1, "ACINQ", 100)));

        NodeAliasCacheCodec.merge(target, new ArrayList<>());

        assertEquals(1, target.size());
    }

    private static void assertRejected(String legacy) {
        try {
            NodeAliasCacheCodec.fromLegacySerialization(legacy);
            fail("IOException expected");
        } catch (IOException expected) {
            // expected
        }
    }
}
