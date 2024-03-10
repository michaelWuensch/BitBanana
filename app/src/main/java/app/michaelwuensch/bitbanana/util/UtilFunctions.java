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

    public static String sha256Hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexUtil.bytesToHex(hash);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] sha256HashByte(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String sha256HashAppSalt(String data) {
        return sha256Hash(data + getAppSalt());
    }

    public static String pinHash(String data) {
        //HmacSHA1 with PBKDF2 and AppSalt
        byte[] hash = new byte[0];
        try {
            hash = encodePbkdf2(data.toCharArray(), getAppSalt().getBytes(), RefConstants.NUM_HASH_ITERATIONS, 32);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Couldn't encode pin with PBKDF2", e);
        }

        return HexUtil.bytesToHex(hash);
    }

    public static String getAppSalt() {

        try {
            if (!PrefsUtil.getEncryptedPrefs().contains(PrefsUtil.RANDOM_SOURCE)) {
                createRandomSource();
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        String salt = "";
        try {
            String decrypted = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.RANDOM_SOURCE, "");
            salt = "BitBanana" + decrypted;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return salt;
    }

    public static void createRandomSource() {
        try {
            SecureRandom random = new SecureRandom();
            int randomNumber = random.nextInt();
            PrefsUtil.editEncryptedPrefs().putString(PrefsUtil.RANDOM_SOURCE, String.valueOf(randomNumber)).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] encodePbkdf2(char[] password, byte[] salt, int iterations, int bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(spec).getEncoded();
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
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return result;
    }
}