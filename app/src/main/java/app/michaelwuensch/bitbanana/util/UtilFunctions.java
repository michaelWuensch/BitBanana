package app.michaelwuensch.bitbanana.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class UtilFunctions {
    private static final String LOG_TAG = UtilFunctions.class.getSimpleName();
    private static final String CURRENT_APP_LOCK_VERIFIER_PREFIX = "app-lock-v2";
    private static final String CURRENT_APP_LOCK_PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int CURRENT_APP_LOCK_VERIFIER_ITERATIONS = 100000;
    private static final int CURRENT_APP_LOCK_VERIFIER_SALT_BYTES = 16;
    private static final int CURRENT_APP_LOCK_VERIFIER_HASH_BYTES = 32;

    public static String sha256Hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexUtil.bytesToHex(hash);
        } catch (Exception ex) {
            BBLog.printStackTrace(ex);
        }
        return null;
    }

    public static byte[] sha256HashByte(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (Exception ex) {
            BBLog.printStackTrace(ex);
        }
        return null;
    }

    public static String sha256HashAppSalt(String data) {
        return sha256Hash(data + getAppSalt());
    }

    // Used only to verify password and PIN values stored before the versioned verifier format.
    @Deprecated
    public static String appLockDataHash(String data) {
        byte[] hash = new byte[0];
        try {
            hash = encodeLegacyAppLockPbkdf2(data.toCharArray(), getAppSalt().getBytes(), RefConstants.NUM_HASH_ITERATIONS, 32);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Couldn't encode legacy app lock data with PBKDF2", e);
        }

        return HexUtil.bytesToHex(hash);
    }

    public static String getAppSalt() {

        try {
            if (!PrefsUtil.getEncryptedPrefs().contains(PrefsUtil.RANDOM_SOURCE)) {
                createRandomSource();
            }
        } catch (GeneralSecurityException | IOException e) {
            BBLog.printStackTrace(e);
        }
        String salt = "";
        try {
            String decrypted = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.RANDOM_SOURCE, "");
            salt = "BitBanana" + decrypted;
        } catch (Exception e) {
            BBLog.printStackTrace(e);
        }

        return salt;
    }

    public static void createRandomSource() {
        try {
            SecureRandom random = new SecureRandom();
            int randomNumber = random.nextInt();
            PrefsUtil.editEncryptedPrefs().putString(PrefsUtil.RANDOM_SOURCE, String.valueOf(randomNumber)).commit();
        } catch (Exception e) {
            BBLog.printStackTrace(e);
        }
    }

    /**
     * Creates a marked verifier for a newly configured app-lock credential.
     */
    public static String createAppLockVerifier(String enteredCredentials) {
        if (enteredCredentials == null) {
            throw new IllegalArgumentException("App-lock credentials must not be null");
        }

        byte[] salt = new byte[CURRENT_APP_LOCK_VERIFIER_SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        try {
            byte[] hash = encodeCurrentAppLockPbkdf2(enteredCredentials.toCharArray(), salt);
            return CURRENT_APP_LOCK_VERIFIER_PREFIX
                    + "$" + CURRENT_APP_LOCK_PBKDF2_ALGORITHM
                    + "$" + CURRENT_APP_LOCK_VERIFIER_ITERATIONS
                    + "$" + HexUtil.bytesToHex(salt)
                    + "$" + HexUtil.bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Couldn't encode current app lock data with PBKDF2", e);
        }
    }

    /**
     * Verifies either an unmarked legacy verifier or a marked current verifier. A successful
     * legacy verification includes the current-format replacement that callers may persist.
     */
    public static AppLockVerificationResult verifyAppLockVerifier(String enteredCredentials, String storedVerifier) {
        if (enteredCredentials == null || storedVerifier == null) {
            return AppLockVerificationResult.notVerified();
        }

        if (storedVerifier.startsWith(CURRENT_APP_LOCK_VERIFIER_PREFIX + "$")) {
            CurrentAppLockVerifier currentVerifier = parseCurrentAppLockVerifier(storedVerifier);
            if (currentVerifier == null) {
                return AppLockVerificationResult.notVerified();
            }

            try {
                byte[] actualHash = encodeCurrentAppLockPbkdf2(enteredCredentials.toCharArray(), currentVerifier.salt);
                return MessageDigest.isEqual(actualHash, currentVerifier.hash)
                        ? AppLockVerificationResult.verified(null)
                        : AppLockVerificationResult.notVerified();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException("Couldn't verify current app lock data with PBKDF2", e);
            }
        }

        String legacyHash = appLockDataHash(enteredCredentials);
        boolean legacyMatches = MessageDigest.isEqual(
                legacyHash.getBytes(StandardCharsets.US_ASCII),
                storedVerifier.getBytes(StandardCharsets.US_ASCII));
        return legacyMatches
                ? AppLockVerificationResult.verified(createAppLockVerifier(enteredCredentials))
                : AppLockVerificationResult.notVerified();
    }

    private static CurrentAppLockVerifier parseCurrentAppLockVerifier(String storedVerifier) {
        String[] parts = storedVerifier.split("\\$", -1);
        if (parts.length != 5
                || !CURRENT_APP_LOCK_VERIFIER_PREFIX.equals(parts[0])
                || !CURRENT_APP_LOCK_PBKDF2_ALGORITHM.equals(parts[1])) {
            return null;
        }

        try {
            if (Integer.parseInt(parts[2]) != CURRENT_APP_LOCK_VERIFIER_ITERATIONS) {
                return null;
            }
            byte[] salt = hexToBytes(parts[3]);
            byte[] hash = hexToBytes(parts[4]);
            if (salt == null || hash == null
                    || salt.length != CURRENT_APP_LOCK_VERIFIER_SALT_BYTES
                    || hash.length != CURRENT_APP_LOCK_VERIFIER_HASH_BYTES) {
                return null;
            }
            return new CurrentAppLockVerifier(salt, hash);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static byte[] hexToBytes(String value) {
        if (value.length() % 2 != 0) {
            return null;
        }

        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            int high = Character.digit(value.charAt(i), 16);
            int low = Character.digit(value.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    private static byte[] encodeLegacyAppLockPbkdf2(char[] password, byte[] salt, int iterations, int bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] encodeCurrentAppLockPbkdf2(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, CURRENT_APP_LOCK_VERIFIER_ITERATIONS,
                CURRENT_APP_LOCK_VERIFIER_HASH_BYTES * 8);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(CURRENT_APP_LOCK_PBKDF2_ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static final class AppLockVerificationResult {
        private final boolean verified;
        private final String replacementVerifier;

        private AppLockVerificationResult(boolean verified, String replacementVerifier) {
            this.verified = verified;
            this.replacementVerifier = replacementVerifier;
        }

        private static AppLockVerificationResult verified(String replacementVerifier) {
            return new AppLockVerificationResult(true, replacementVerifier);
        }

        private static AppLockVerificationResult notVerified() {
            return new AppLockVerificationResult(false, null);
        }

        public boolean isVerified() {
            return verified;
        }

        public String getReplacementVerifier() {
            return replacementVerifier;
        }
    }

    private static final class CurrentAppLockVerifier {
        private final byte[] salt;
        private final byte[] hash;

        private CurrentAppLockVerifier(byte[] salt, byte[] hash) {
            this.salt = salt;
            this.hash = hash;
        }
    }

    public static String getQueryParam(URL url, String parameter) {
        if (url != null && url.getQuery() != null) {
            String[] params = url.getQuery().split("&");
            for (String param : params) {
                String name = param.split("=")[0];
                if (parameter.equals(name)) {
                    return param.split("=")[1];
                }
            }
        }
        return null;
    }

    public static double roundDouble(double value, int places) {
        if (places < 0)
            places = 0;

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static long bytesToLong(final byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    public static int intFromByteArray(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                ((bytes[3] & 0xFF) << 0);
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
    }

    public static int getBlockHeightFromChanID(long chanID) {
        byte[] chanIDBytes = longToBytes(chanID);
        // first 3 bytes are the block height
        byte[] blockheightBytes = {0x00, chanIDBytes[0], chanIDBytes[1], chanIDBytes[2]};
        return intFromByteArray(blockheightBytes);
    }

    public static String hmacSHA256(byte[] data, byte[] key) {
        return hashHmac("HmacSHA256", data, key);
    }

    public static long satPerVByteToSatPerKw(double satPerVByte) {
        return (long) (satPerVByte * 250.0);
    }

    public static double satPerKwToSatPerVByte(long satPerKw) {
        return satPerKw / 250.0;
    }

    public static long satPerVByteToSatPerKb(double satPerVByte) {
        return (long) (satPerVByte * 1000.0);
    }

    public static double satPerKbToSatPerVByte(long satPerKw) {
        return satPerKw / 1000.0;
    }

    public static long satPerKwToSatPerKb(long satPerKw) {
        return satPerKw * 4;
    }

    public static long satPerKbToSatPerKw(long satPerKb) {
        return satPerKb / 4;
    }

    private static String hashHmac(String algorithm, byte[] data, byte[] key) {
        String result = "";
        final SecretKeySpec secretKey = new SecretKeySpec(key,
                algorithm);
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKey);
            byte[] macData = mac.doFinal(data);
            result = HexUtil.bytesToHex(macData);
        } catch (NoSuchAlgorithmException e) {
            BBLog.printStackTrace(e);
        } catch (InvalidKeyException e) {
            BBLog.printStackTrace(e);
        }
        return result;
    }
}