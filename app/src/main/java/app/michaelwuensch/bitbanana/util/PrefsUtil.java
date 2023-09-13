package app.michaelwuensch.bitbanana.util;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;

import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.customView.OnChainFeeView;

/**
 * This class simplifies management of preferences.
 */
public class PrefsUtil {

    private static final String LOG_TAG = PrefsUtil.class.getSimpleName();

    // shared preference references
    public static final String PREVENT_SCREEN_RECORDING = "preventScreenRecording";
    public static final String FIRST_CURRENCY_IS_PRIMARY = "firstCurrencyIsPrimary";
    public static final String PIN_HASH = "pin_hash";
    public static final String PIN_LENGTH = "pin_length";
    public static final String SETTINGS_VERSION = "settings_ver";
    public static final String ON_CHAIN_FEE_TIER = "on_chain_fee_tier";
    public static final String BIOMETRICS_PREFERRED = "biometrics_preferred";
    public static final String CURRENT_NODE_CONFIG = "current_wallet_config";
    public static final String AVAILABLE_FIAT_CURRENCIES = "fiat_available";
    public static final String LANGUAGE = "language";
    public static final String EXCHANGE_RATE_PROVIDER = "exchangeRateProvider";
    public static final String IS_DEFAULT_CURRENCY_SET = "isDefaultCurrencySet";
    public static final String FIRST_CURRENCY = "firstCurrency";
    public static final String SECOND_CURRENCY = "secondCurrency";
    public static final String LAST_CLIPBOARD_SCAN = "lastClipboardScan";
    public static final String SCAN_CLIPBOARD = "scanClipboard";
    public static final String SHOW_IDENTITY_TAP_HINT = "identityTapHint";
    public static final String NODE_ALIAS_CACHE = "nodeAliasCache";
    public static final String FEE_PRESET_FAST = "feePresetFast";
    public static final String FEE_PRESET_MEDIUM = "feePresetMedium";
    public static final String FEE_PRESET_SLOW = "feePresetSlow";
    public static final String ROUTING_SUMMARY_VOLUME = "routingSummaryMode";

    // node config preferences references
    public static final String NODE_CONFIGS = "wallet_configs";
    public static final String CONTACTS = "contacts";
    public static final String RANDOM_SOURCE = "random_source";

    // default values
    public static final String DEFAULT_FIAT_CURRENCIES = "[]";
    public static final String DEFAULT_FEE_PRESET_VALUE_FAST = "1";
    public static final String DEFAULT_FEE_PRESET_VALUE_MEDIUM = "18";
    public static final String DEFAULT_FEE_PRESET_VALUE_SLOW = "144";


    // Access to default shared prefs
    public static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(App.getAppContext());
    }

    public static SharedPreferences.Editor editPrefs() {
        return getPrefs().edit();
    }

    // Access encrypted preferences
    public static SharedPreferences getEncryptedPrefs() throws GeneralSecurityException, IOException {

        MasterKey masterKey = new MasterKey.Builder(App.getAppContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        String sharedPrefsFile = "bb_secure_preferences";
        SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                App.getAppContext(),
                sharedPrefsFile,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        return sharedPreferences;
    }

    public static SharedPreferences.Editor putSerializable(String key, Serializable obj) {
        SharedPreferences.Editor editor = editPrefs();
        try {
            String serialized = ObjectSerializer.serialize(obj);
            editor.putString(key, serialized);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return editor;
    }

    public static Object getSerializable(String key, Object defaultObject) {
        try {
            return ObjectSerializer.deserialize(getPrefs().getString(key, null));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return defaultObject;
    }

    public static SharedPreferences.Editor editEncryptedPrefs() throws GeneralSecurityException, IOException {
        return getEncryptedPrefs().edit();
    }

    // Shortcuts to often used preferences
    public static boolean isScreenRecordingPrevented() {
        return getPrefs().getBoolean(PREVENT_SCREEN_RECORDING, true);
    }

    public static boolean isFirstCurrencyPrimary() {
        return getPrefs().getBoolean(FIRST_CURRENCY_IS_PRIMARY, true);
    }

    public static String getOnChainFeeTier() {
        return getPrefs().getString(ON_CHAIN_FEE_TIER, OnChainFeeView.OnChainFeeTier.FAST.name());
    }

    public static boolean isBiometricPreferred() {
        return getPrefs().getBoolean(BIOMETRICS_PREFERRED, false);
    }

    public static boolean isBiometricEnabled() {
        return getPrefs().getBoolean("biometricsEnabled", true);
    }

    public static String getCurrentNodeConfig() {
        return getPrefs().getString(CURRENT_NODE_CONFIG, "");
    }

    public static boolean isPinEnabled() {
        try {
            return getEncryptedPrefs().contains(PIN_HASH);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static String getFirstCurrency() {
        return getPrefs().getString(FIRST_CURRENCY, MonetaryUtil.SATOSHI_UNIT);
    }

    public static String getSecondCurrency() {
        return getPrefs().getString(SECOND_CURRENCY, "USD");
    }

    public static boolean isTorEnabled() {
        return getPrefs().getBoolean("isTorEnabled", true);
    }
}
