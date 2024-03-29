package app.michaelwuensch.bitbanana.util;


import androidx.biometric.BiometricManager;

import app.michaelwuensch.bitbanana.baseClasses.App;

public class BiometricUtil {
    public static boolean hardwareAvailable() {
        return BiometricManager.from(App.getAppContext()).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
                || BiometricManager.from(App.getAppContext()).canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
    }

    public static boolean notSetup() {
        return BiometricManager.from(App.getAppContext()).canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
    }
}
