package app.michaelwuensch.bitbanana.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.michaelwuensch.bitbanana.backup.DataBackupUtil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EncryptionUtilTest {

    // Low iteration count to keep the tests fast. The format is the same.
    private static final int ITERATIONS = 1000;
    private static final String PASSWORD = "correct horse battery stäple";
    private static final byte[] DATA = "{\"backendConfigs\":[{\"authenticationToken\":\"secret\"}]}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HEADER = fileHeader(6);

    private static byte[] fileHeader(int version) {
        byte[] identifier = DataBackupUtil.BACKUP_FILE_IDENTIFIER.getBytes(StandardCharsets.UTF_8);
        byte[] header = Arrays.copyOf(identifier, identifier.length + 4);
        System.arraycopy(UtilFunctions.intToByteArray(version), 0, header, identifier.length, 4);
        return header;
    }

    private static byte[] encrypt() {
        byte[] encrypted = EncryptionUtil.PasswordEncryptData(DATA, PASSWORD, ITERATIONS, HEADER);
        assertNotNull(encrypted);
        return encrypted;
    }

    @Test
    public void roundTrip() {
        byte[] encrypted = encrypt();

        assertArrayEquals(DATA, EncryptionUtil.PasswordDecryptData(encrypted, PASSWORD, HEADER));
    }

    @Test
    public void format() {
        byte[] encrypted = encrypt();

        // 1 byte kdf + 4 bytes iterations + 16 bytes salt + 12 bytes nonce + ciphertext (same length as plaintext for GCM) + 16 bytes tag
        assertEquals(1 + 4 + 16 + 12 + DATA.length + 16, encrypted.length);
        assertEquals(EncryptionUtil.KDF_PBKDF2_HMAC_SHA256, encrypted[0]);
        assertEquals(ITERATIONS, UtilFunctions.intFromByteArray(Arrays.copyOfRange(encrypted, 1, 5)));
    }

    @Test
    public void randomSaltAndNonce() {
        assertFalse(Arrays.equals(encrypt(), encrypt()));
    }

    @Test
    public void wrongPassword_returnsNull() {
        assertNull(EncryptionUtil.PasswordDecryptData(encrypt(), "wrong password", HEADER));
    }

    @Test
    public void manipulatedCiphertext_returnsNull() {
        byte[] encrypted = encrypt();
        // Flip one bit in every position after the header, each must be detected.
        for (int i = 33; i < encrypted.length; i++) {
            byte[] manipulated = encrypted.clone();
            manipulated[i] ^= 0x01;
            assertNull("Manipulation at position " + i + " not detected", EncryptionUtil.PasswordDecryptData(manipulated, PASSWORD, HEADER));
        }
    }

    @Test
    public void manipulatedParameters_returnsNull() {
        byte[] encrypted = encrypt();
        // kdf id, iterations, salt and nonce
        for (int i = 0; i < 33; i++) {
            byte[] manipulated = encrypted.clone();
            manipulated[i] ^= 0x01;
            assertNull("Manipulation at position " + i + " not detected", EncryptionUtil.PasswordDecryptData(manipulated, PASSWORD, HEADER));
        }
    }

    @Test
    public void manipulatedFileHeader_returnsNull() {
        byte[] encrypted = encrypt();

        // e.g. a downgrade of the backup version
        assertNull(EncryptionUtil.PasswordDecryptData(encrypted, PASSWORD, fileHeader(5)));
        assertNull(EncryptionUtil.PasswordDecryptData(encrypted, PASSWORD, fileHeader(7)));
    }

    @Test
    public void truncatedData_returnsNull() {
        byte[] encrypted = encrypt();

        assertNull(EncryptionUtil.PasswordDecryptData(Arrays.copyOf(encrypted, encrypted.length - 1), PASSWORD, HEADER));
        assertNull(EncryptionUtil.PasswordDecryptData(Arrays.copyOf(encrypted, 48), PASSWORD, HEADER));
        assertNull(EncryptionUtil.PasswordDecryptData(new byte[0], PASSWORD, HEADER));
        assertNull(EncryptionUtil.PasswordDecryptData(null, PASSWORD, HEADER));
    }

    @Test
    public void excessiveIterations_returnsNullWithoutKeyDerivation() {
        byte[] encrypted = encrypt();
        System.arraycopy(UtilFunctions.intToByteArray(Integer.MAX_VALUE), 0, encrypted, 1, 4);

        long start = System.currentTimeMillis();
        assertNull(EncryptionUtil.PasswordDecryptData(encrypted, PASSWORD, HEADER));
        // Deriving the key with Integer.MAX_VALUE iterations would take hours.
        assertFalse(System.currentTimeMillis() - start > 1000);

        System.arraycopy(UtilFunctions.intToByteArray(0), 0, encrypted, 1, 4);
        assertNull(EncryptionUtil.PasswordDecryptData(encrypted, PASSWORD, HEADER));
        System.arraycopy(UtilFunctions.intToByteArray(-1), 0, encrypted, 1, 4);
        assertNull(EncryptionUtil.PasswordDecryptData(encrypted, PASSWORD, HEADER));
    }

    @Test
    public void legacyExcessiveIterations_returnsNullWithoutKeyDerivation() {
        byte[] legacy = new byte[64];
        System.arraycopy(UtilFunctions.intToByteArray(Integer.MAX_VALUE), 0, legacy, 0, 4);

        long start = System.currentTimeMillis();
        assertNull(EncryptionUtil.PasswordDecryptDataLegacy(legacy, PASSWORD));
        assertFalse(System.currentTimeMillis() - start > 1000);
        assertNull(EncryptionUtil.PasswordDecryptDataLegacy(new byte[10], PASSWORD));
        assertNull(EncryptionUtil.PasswordDecryptDataLegacy(null, PASSWORD));
    }

    @Test
    public void decryptBackup_usesAuthenticatedEncryptionForNewVersions() {
        byte[] encrypted = encrypt();

        assertArrayEquals(DATA, DataBackupUtil.decryptBackup(encrypted, 6, PASSWORD));
        // A changed version in the file header is detected
        assertNull(DataBackupUtil.decryptBackup(encrypted, 7, PASSWORD));
        // Legacy versions never accept the new format
        assertNull(DataBackupUtil.decryptBackup(encrypted, 5, PASSWORD));
    }
}
