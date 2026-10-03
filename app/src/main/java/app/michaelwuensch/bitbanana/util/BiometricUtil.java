package app.michaelwuensch.bitbanana.util;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;

/**
 * Biometric unlock of the app lock.
 * <p>
 * Only strong biometrics (class 3) are allowed. Additionally, every biometric unlock is bound to a key in the Android keystore,
 * that can only be used after a successful biometric authentication and that gets invalidated as soon as the biometrics of the device change
 * (e.g. a new fingerprint is enrolled). This way someone who knows the device PIN can not enroll his own fingerprint to open the app.
 * <p>
 * The key is only created after the PIN/password was entered or when the user enables biometrics in the settings, never by a biometric authentication itself.
 */
public class BiometricUtil {
    private static final String LOG_TAG = BiometricUtil.class.getSimpleName();
    private static final String ANDROID_KEY_STORE_NAME = "AndroidKeyStore";
    private static final String KEY_BIOMETRIC_UNLOCK = "BiometricUnlockKey";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG;

    public static boolean hardwareAvailable() {
        int result = BiometricManager.from(App.getAppContext()).canAuthenticate(AUTHENTICATORS);
        return result == BiometricManager.BIOMETRIC_SUCCESS || result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
    }

    /**
     * Returns true if the device supports strong biometrics, but no biometrics (e.g. fingerprints) are enrolled in the Android system settings.
     */
    public static boolean noBiometricsEnrolledOnDevice() {
        return BiometricManager.from(App.getAppContext()).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
    }

    /**
     * Returns true if the biometrics button should be offered on the lock screen.
     * Without a key (e.g. right after updating the app) the PIN/password has to be entered once first.
     */
    public static boolean isBiometricUnlockOffered() {
        return PrefsUtil.isBiometricEnabled() && hardwareAvailable() && hasKey();
    }

    public static BiometricPrompt.PromptInfo createPromptInfo(@NonNull String title, @NonNull String negativeButtonText) {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build();
    }

    /**
     * Has to be called after the PIN/password was entered correctly (not the emergency PIN/password) or a new one was set.
     * It creates the key for biometric unlock if biometrics are enabled and the key does not exist yet.
     */
    public static void onAppLockCredentialVerified() {
        if (PrefsUtil.isBiometricEnabled() && hardwareAvailable() && !noBiometricsEnrolledOnDevice() && !hasKey())
            createKey();
    }

    /**
     * Creates the key for biometric unlock. Call this when the user enables biometric unlock.
     */
    public static void createKey() {
        try {
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEY_BIOMETRIC_UNLOCK,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Every single use of the key requires a strong biometric authentication.
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
            }
            // Below Android 11 the default (no validity duration) already requires a biometric authentication for every use.
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE_NAME);
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
            BBLog.d(LOG_TAG, "Biometric unlock key created.");
        } catch (Exception e) {
            // For example if no biometrics are enrolled.
            BBLog.w(LOG_TAG, "Creating biometric unlock key failed: " + e.getMessage());
        }
    }

    public static void deleteKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_NAME);
            keyStore.load(null);
            keyStore.deleteEntry(KEY_BIOMETRIC_UNLOCK);
        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Deleting biometric unlock key failed: " + e.getMessage());
        }
    }

    private static boolean hasKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_NAME);
            keyStore.load(null);
            return keyStore.containsAlias(KEY_BIOMETRIC_UNLOCK);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates the CryptoObject that has to be passed to the BiometricPrompt.
     *
     * @return null if the key is missing or was invalidated because the biometrics of the device changed.
     * In that case biometric unlock gets disabled and the user has to unlock with the PIN/password and enable it again in the settings.
     */
    @Nullable
    public static BiometricPrompt.CryptoObject createCryptoObject() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_NAME);
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_BIOMETRIC_UNLOCK, null);
            if (key == null)
                throw new IllegalStateException("Biometric unlock key does not exist.");
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return new BiometricPrompt.CryptoObject(cipher);
        } catch (KeyPermanentlyInvalidatedException e) {
            BBLog.w(LOG_TAG, "Biometrics of the device changed. Biometric unlock gets disabled.");
            disableBiometricUnlock();
        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Biometric unlock not possible: " + e.getMessage());
            disableBiometricUnlock();
        }
        return null;
    }

    /**
     * Verifies that the successful biometric authentication actually unlocked our key.
     */
    public static boolean isAuthenticationValid(@NonNull BiometricPrompt.AuthenticationResult result) {
        try {
            BiometricPrompt.CryptoObject cryptoObject = result.getCryptoObject();
            if (cryptoObject == null || cryptoObject.getCipher() == null)
                return false;
            // This only works if the key was unlocked by the biometric authentication.
            cryptoObject.getCipher().doFinal(new byte[16]);
            return true;
        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Biometric authentication could not be verified: " + e.getMessage());
            return false;
        }
    }

    private static void disableBiometricUnlock() {
        deleteKey();
        PrefsUtil.editPrefs()
                .putBoolean(PrefsUtil.BIOMETRICS_ENABLED, false)
                .putBoolean(PrefsUtil.BIOMETRICS_PREFERRED, false)
                .putBoolean(PrefsUtil.BIOMETRICS_DISABLED_NOTICE_PENDING, true)
                .commit();
    }

    /**
     * Informs the user that biometric unlock was disabled because the biometrics of the device changed.
     * The notice stays pending until the user dismissed it. This way it is also shown if the lock screen gets recreated.
     */
    public static void showBiometricUnlockDisabledDialogIfPending(@NonNull Activity activity) {
        if (!PrefsUtil.getPrefs().getBoolean(PrefsUtil.BIOMETRICS_DISABLED_NOTICE_PENDING, false))
            return;
        AlertDialog.Builder adb = new AlertDialog.Builder(activity)
                .setTitle(R.string.biometricPrompt_title)
                .setMessage(R.string.biometric_unlock_disabled_biometrics_changed)
                .setCancelable(true)
                .setOnCancelListener(dialog -> clearBiometricUnlockDisabledNotice())
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> clearBiometricUnlockDisabledNotice());
        Dialog dlg = adb.create();
        // Apply FLAG_SECURE to dialog to prevent screen recording
        if (PrefsUtil.isScreenRecordingPrevented()) {
            dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        dlg.show();
    }

    private static void clearBiometricUnlockDisabledNotice() {
        PrefsUtil.editPrefs().remove(PrefsUtil.BIOMETRICS_DISABLED_NOTICE_PENDING).apply();
    }
}
