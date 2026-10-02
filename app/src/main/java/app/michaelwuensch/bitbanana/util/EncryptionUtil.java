package app.michaelwuensch.bitbanana.util;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {
    public static final String TAG = EncryptionUtil.class.getSimpleName();

    // Current format
    public static final byte KDF_PBKDF2_HMAC_SHA256 = 1;
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH = 16;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int HEADER_LENGTH = 1 + 4 + SALT_LENGTH + NONCE_LENGTH; // kdf id + iterations + salt + nonce
    // Bounds for the iterations read from a file. This prevents manipulated files from freezing the app.
    public static final int MIN_ITERATIONS = 1;
    public static final int MAX_ITERATIONS = 10_000_000;

    // Legacy format
    private static final String LEGACY_ENCRYPTION_ALGORITHM = "PBEWITHSHA256AND256BITAES-CBC-BC";
    // All legacy backups were created with 250 000 iterations.
    private static final int LEGACY_MAX_ITERATIONS = 1_000_000;

    /**
     * This function encrypts a message using a password based authenticated AES-256-GCM encryption.
     * <p>
     * The key is derived from the password using PBKDF2-HMAC-SHA256 with a random salt and the given amount of iterations.
     * The random salt makes sure that even the use of the same password will always result in a different key. This makes pre-generated rainbow tables for brute force attacks impossible.
     * The iterations slow down brute force attacks. Use a value that is as high as possible without annoying the user.
     * <p>
     * The result has the following format:
     * 1 byte kdf identifier | 4 bytes iterations | 16 bytes salt | 12 bytes nonce | encrypted message including 16 bytes authentication tag
     * <p>
     * GCM authenticates the encrypted message as well as all parameters and the given associated data.
     * Any modification will therefore be detected on decryption.
     *
     * @param dataToEncrypt  The data to encrypt.
     * @param password       The password to derive the key from.
     * @param iterations     Number of PBKDF2 iterations.
     * @param associatedData Additional data that is not encrypted, but authenticated (e.g. the file header). It has to be provided again for decryption.
     * @return The encrypted message or null if the encryption failed.
     */
    public static byte[] PasswordEncryptData(byte[] dataToEncrypt, String password, int iterations, byte[] associatedData) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);
            byte[] nonce = new byte[NONCE_LENGTH];
            new SecureRandom().nextBytes(nonce);

            ByteArrayOutputStream header = new ByteArrayOutputStream();
            header.write(KDF_PBKDF2_HMAC_SHA256);
            header.write(UtilFunctions.intToByteArray(iterations));
            header.write(salt);
            header.write(nonce);
            byte[] headerBytes = header.toByteArray();

            SecretKey key = derivePbkdf2Key(password, salt, iterations);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(associatedData);
            cipher.updateAAD(headerBytes);
            byte[] encryptedData = cipher.doFinal(dataToEncrypt);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(headerBytes);
            outputStream.write(encryptedData);
            return outputStream.toByteArray();
        } catch (Exception e) {
            BBLog.e(TAG, "Error encrypting data: " + e.getMessage());
        }
        return null;
    }

    /**
     * This function decrypts a message created with the PasswordEncryptData function.
     *
     * @param dataToDecrypt  The encrypted message.
     * @param password       The password.
     * @param associatedData The same associated data that was used for encryption.
     * @return The decrypted data or null if the password is wrong, the data was manipulated or it has an invalid format.
     */
    public static byte[] PasswordDecryptData(byte[] dataToDecrypt, String password, byte[] associatedData) {
        try {
            if (dataToDecrypt == null || dataToDecrypt.length < HEADER_LENGTH + TAG_LENGTH_BITS / 8) {
                BBLog.e(TAG, "Error decrypting data: Data too short.");
                return null;
            }
            if (dataToDecrypt[0] != KDF_PBKDF2_HMAC_SHA256) {
                BBLog.e(TAG, "Error decrypting data: Unknown key derivation function.");
                return null;
            }
            int iterations = UtilFunctions.intFromByteArray(Arrays.copyOfRange(dataToDecrypt, 1, 5));
            if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
                BBLog.e(TAG, "Error decrypting data: Invalid iterations count.");
                return null;
            }
            byte[] salt = Arrays.copyOfRange(dataToDecrypt, 5, 5 + SALT_LENGTH);
            byte[] nonce = Arrays.copyOfRange(dataToDecrypt, 5 + SALT_LENGTH, HEADER_LENGTH);
            byte[] headerBytes = Arrays.copyOfRange(dataToDecrypt, 0, HEADER_LENGTH);
            byte[] message = Arrays.copyOfRange(dataToDecrypt, HEADER_LENGTH, dataToDecrypt.length);

            SecretKey key = derivePbkdf2Key(password, salt, iterations);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(associatedData);
            cipher.updateAAD(headerBytes);
            // Throws an AEADBadTagException if the password is wrong or the data was manipulated.
            return cipher.doFinal(message);
        } catch (Exception e) {
            BBLog.e(TAG, "Error decrypting data. Wrong password or manipulated data.");
        }
        return null;
    }

    /**
     * This function decrypts a message in the legacy format (backup versions up to 5, including Zap backups).
     * The legacy format is not authenticated, so manipulations cannot be detected reliably. Only use it to read old data.
     * <p>
     * Format: 4 bytes iterations | 12 bytes salt | 16 bytes initialization vector | AES-256-CBC encrypted message
     *
     * @param dataToDecrypt The encrypted message.
     * @param password      The password.
     * @return The decrypted data or null if the decryption failed.
     */
    public static byte[] PasswordDecryptDataLegacy(byte[] dataToDecrypt, String password) {
        try {
            if (dataToDecrypt == null || dataToDecrypt.length < 32) {
                BBLog.e(TAG, "Error decrypting legacy data: Data too short.");
                return null;
            }
            // extract iterations, salt, initialization vector & message from dataToDecrypt
            int iterations = UtilFunctions.intFromByteArray(Arrays.copyOfRange(dataToDecrypt, 0, 4));
            if (iterations < MIN_ITERATIONS || iterations > LEGACY_MAX_ITERATIONS) {
                BBLog.e(TAG, "Error decrypting legacy data: Invalid iterations count.");
                return null;
            }
            byte[] salt = Arrays.copyOfRange(dataToDecrypt, 4, 16);
            byte[] iv = Arrays.copyOfRange(dataToDecrypt, 16, 32);
            byte[] message = Arrays.copyOfRange(dataToDecrypt, 32, dataToDecrypt.length);

            // generate secret key
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, iterations);
            SecretKey key = SecretKeyFactory.getInstance(LEGACY_ENCRYPTION_ALGORITHM).generateSecret(pbeKeySpec);
            Cipher cipher = Cipher.getInstance(LEGACY_ENCRYPTION_ALGORITHM);

            // decrypt the message
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            return cipher.doFinal(message);
        } catch (Exception e) {
            BBLog.e(TAG, "Error decrypting legacy data.");
        }
        return null;
    }

    private static SecretKey derivePbkdf2Key(String password, byte[] salt, int iterations) throws GeneralSecurityException {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
        try {
            byte[] keyBytes = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(pbeKeySpec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            pbeKeySpec.clearPassword();
        }
    }
}
