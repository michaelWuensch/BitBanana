package app.michaelwuensch.bitbanana.util;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.github.michaelwuensch.avathorlibrary.AvathorFactory;

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
    public static final String PIN_LENGTH = "pin_length";
    public static final String SETTINGS_VERSION = "settings_ver";
    public static final String ON_CHAIN_FEE_TIER = "on_chain_fee_tier";
    public static final String BIOMETRICS_PREFERRED = "biometrics_preferred";
    public static final String CURRENT_BACKEND_CONFIG = "current_wallet_config";
    public static final String AVAILABLE_FIAT_CURRENCIES = "fiat_available";
    public static final String LANGUAGE = "language";
    public static final String EXCHANGE_RATE_PROVIDER = "exchangeRateProvider";
    public static final String IS_DEFAULT_CURRENCY_SET = "isDefaultCurrencySet";
    public static final String FIRST_CURRENCY = "firstCurrency";
    public static final String SECOND_CURRENCY = "secondCurrency";
    public static final String THIRD_CURRENCY = "thirdCurrency";
    public static final String FORTH_CURRENCY = "forthCurrency";
    public static final String FIFTH_CURRENCY = "fifthCurrency";
    public static final String LAST_CLIPBOARD_SCAN = "lastClipboardScan";
    public static final String SCAN_CLIPBOARD = "scanClipboard";
    public static final String SHOW_IDENTITY_TAP_HINT = "identityTapHint";
    public static final String NODE_ALIAS_CACHE = "nodeAliasCache";
    public static final String FEE_PRESET_FAST = "feePresetFast";
    public static final String FEE_PRESET_MEDIUM = "feePresetMedium";
    public static final String FEE_PRESET_SLOW = "feePresetSlow";
    public static final String BALANCE_HIDE_TYPE = "hideBalanceType";
    public static final String BLOCK_EXPLORER = "blockExplorer";
    public static final String CUSTOM_BLOCK_EXPLORER_HOST = "customBlockExplorerHost";
    public static final String CUSTOM_EXCHANGE_RATE_PROVIDER_HOST = "customExchangeRateProviderHost";
    public static final String CUSTOM_FEE_ESTIMATION_PROVIDER_HOST = "customFeeEstimationProviderHost";
    public static final String FEE_ESTIMATION_PROVIDER = "feeEstimationProvider";
    public static final String AVATAR_STYLE = "avatarStyle";
    public static final String FEE_ESTIMATE_NEXT_BLOCK = "feeEstimateNextBlock";
    public static final String FEE_ESTIMATE_HOUR = "feeEstimateHour";
    public static final String FEE_ESTIMATE_DAY = "feeEstimateDay";
    public static final String FEE_ESTIMATE_MINIMUM = "feeEstimateMinimum";
    public static final String FEE_ESTIMATE_TIMESTAMP = "feeEstimateTimestamp";
    public static final String CURRENT_CURRENCY_INDEX = "currentCurrencyIndex";
    public static final String CHANNEL_SORT_CRITERIA = "channelSortCriteria";
    public static final String UTXO_SORT_CRITERIA = "utxoSortCriteria";
    public static final String LOG_SORT_CRITERIA = "logSortCriteria";
    public static final String LOG_AUTO_SCROLL = "logAutoScroll";
    public static final String REBALANCE_FEE_LIMIT_PERCENT = "rebalanceFeeLimitPercent";
    public static final String BACKEND_TIMEOUT = "backendTimeout";
    public static final String PAYMENT_TIMEOUT = "paymentTimeout";
    public static final String APP_NUM_UNLOCK_FAILS = "numAppUnlockFails";
    public static final String NOTIFICATIONS_DECLINED = "notificationPermissionDeclined";


    // default values
    public static final String DEFAULT_FIAT_CURRENCIES = "[]";
    public static final String DEFAULT_FEE_PRESET_VALUE_FAST = "1";
    public static final String DEFAULT_FEE_PRESET_VALUE_MEDIUM = "18";
    public static final String DEFAULT_FEE_PRESET_VALUE_SLOW = "144";

    // encrypted preferences references
    public static final String PIN_HASH = "pin_hash";
    public static final String PASSWORD_HASH = "password_hash";
    public static final String EMERGENCY_PIN_HASH = "emergency_pin_hash";
    public static final String EMERGENCY_PASSWORD_HASH = "emergency_password_hash";
    public static final String BACKEND_CONFIGS = "wallet_configs";
    public static final String CONTACTS = "contacts";
    public static final String LABELS = "labels";
    public static final String RANDOM_SOURCE = "random_source";

    private static SharedPreferences encryptedPrefs;


    // Access to default shared prefs
    public static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(App.getAppContext());
    }

    public static SharedPreferences.Editor editPrefs() {
        return getPrefs().edit();
    }

    // Access encrypted preferences. This is a slow operation, therefore we save the result and do a lazy initialization from there on.
    public static synchronized SharedPreferences getEncryptedPrefs() throws GeneralSecurityException, IOException {
        if (encryptedPrefs == null) {
            MasterKey masterKey = new MasterKey.Builder(App.getAppContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            String sharedPrefsFile = "bb_secure_preferences";
            encryptedPrefs = EncryptedSharedPreferences.create(
                    App.getAppContext(),
                    sharedPrefsFile,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        }
        return encryptedPrefs;
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

    public static String getOnChainFeeTier() {
        return getPrefs().getString(ON_CHAIN_FEE_TIER, OnChainFeeView.OnChainFeeTier.FAST.name());
    }

    public static boolean isBiometricPreferred() {
        return getPrefs().getBoolean(BIOMETRICS_PREFERRED, false);
    }

    public static boolean isBiometricEnabled() {
        return getPrefs().getBoolean("biometricsEnabled", true);
    }

    public static String getCurrentBackendConfig() {
        return getPrefs().getString(CURRENT_BACKEND_CONFIG, "");
    }

    public static boolean isPinEnabled() {
        try {
            return getEncryptedPrefs().contains(PIN_HASH);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static boolean isPasswordEnabled() {
        try {
            return getEncryptedPrefs().contains(PASSWORD_HASH);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static boolean isEmergencyPinEnabled() {
        try {
            return getEncryptedPrefs().contains(EMERGENCY_PIN_HASH);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isEmergencyPasswordEnabled() {
        try {
            return getEncryptedPrefs().contains(EMERGENCY_PASSWORD_HASH);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isLoggingEnabled() {
        return getPrefs().getBoolean("featureLogs", false);
    }

    public static String getFirstCurrencyCode() {
        return getPrefs().getString(FIRST_CURRENCY, BBCurrency.CURRENCY_CODE_SATOSHI);
    }

    public static String getSecondCurrencyCode() {
        return getPrefs().getString(SECOND_CURRENCY, "USD");
    }

    public static String getThirdCurrencyCode() {
        return getPrefs().getString(THIRD_CURRENCY, "none");
    }

    public static String getForthCurrencyCode() {
        return getPrefs().getString(FORTH_CURRENCY, "none");
    }

    public static String getFifthCurrencyCode() {
        return getPrefs().getString(FIFTH_CURRENCY, "none");
    }

    public static boolean isTorEnabled() {
        return getPrefs().getBoolean("isTorEnabled", false);
    }

    public static String getBlockExplorer() {
        return getPrefs().getString(BLOCK_EXPLORER, "Mempool.space");
    }

    public static String getExchangeRateProvider() {
        return getPrefs().getString(EXCHANGE_RATE_PROVIDER, "Blockchain.info");
    }

    public static String getFeeEstimationProvider() {
        return getPrefs().getString(FEE_ESTIMATION_PROVIDER, "Internal");
    }

    public static String getCustomBlockExplorerHost() {
        String host = getPrefs().getString(CUSTOM_BLOCK_EXPLORER_HOST, "https://mempool.space");

        String source = PrefsUtil.getPrefs().getString("overrideHostSource", "");
        if (source.isEmpty())
            return host;
        String target = PrefsUtil.getPrefs().getString("overrideHostTarget", "");
        return host.replace(source, target);
    }

    public static String getCustomExchangeRateProviderHost() {
        return getPrefs().getString(CUSTOM_EXCHANGE_RATE_PROVIDER_HOST, "https://mempool.space");
    }

    public static String getCustomFeeEstimationProviderHost() {
        return getPrefs().getString(CUSTOM_FEE_ESTIMATION_PROVIDER_HOST, "https://mempool.space");
    }

    public static String getCustomBlockExplorerAddressSuffix() {
        return getPrefs().getString("customBlockExplorerAddressSuffix", "/address/");
    }

    public static String getCustomBlockExplorerTransactionSuffix() {
        return getPrefs().getString("customBlockExplorerTransactionSuffix", "/tx/");
    }

    public static boolean getAreInvoicesWithoutSpecifiedAmountAllowed() {
        return getPrefs().getBoolean("unspecifiedAmountInvoices", false);
    }

    public static AvathorFactory.AvatarSet getAvatarSet() {
        return AvathorFactory.AvatarSet.valueOf(getPrefs().getString(AVATAR_STYLE, "MIXED"));
    }

    public static int getFeeEstimate_NextBlock() {
        return getPrefs().getInt(FEE_ESTIMATE_NEXT_BLOCK, 50);
    }

    public static int getFeeEstimate_Hour() {
        return getPrefs().getInt(FEE_ESTIMATE_HOUR, 25);
    }

    public static int getFeeEstimate_Day() {
        return getPrefs().getInt(FEE_ESTIMATE_DAY, 10);
    }

    public static int getFeeEstimate_Minimum() {
        return getPrefs().getInt(FEE_ESTIMATE_MINIMUM, 1);
    }

    public static long getFeeEstimate_Timestamp() {
        return getPrefs().getLong(FEE_ESTIMATE_TIMESTAMP, 0);
    }

    public static long getLockScreenTimeout() {
        return Long.parseLong(getPrefs().getString("lockScreenTimeoutPref", "30"));
    }

    public static int getBackendTimeout() {
        int timeout = 0;
        try {
            timeout = Integer.parseInt(getPrefs().getString(BACKEND_TIMEOUT, "20"));
        } catch (NumberFormatException e) {
            return 20;
        }
        if (timeout < 10)
            return 10;
        if (timeout > 300)
            return 300;
        return timeout;
    }

    public static int getPaymentTimeout() {
        int timeout = 0;
        try {
            timeout = Integer.parseInt(getPrefs().getString(PAYMENT_TIMEOUT, "60"));
        } catch (NumberFormatException e) {
            return 60;
        }
        if (timeout < 20)
            return 20;
        if (timeout > 300)
            return 300;
        return timeout;
    }

    public static String getEmergencyUnlockMode() {
        return getPrefs().getString("appLockEmergencyModePref", "erase");
    }
}
