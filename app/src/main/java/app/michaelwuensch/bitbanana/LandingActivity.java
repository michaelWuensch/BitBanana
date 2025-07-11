package app.michaelwuensch.bitbanana;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.util.AppLockUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;

public class LandingActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = LandingActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep in app language picker in sync with system per app language setting.
        updateLanguageSetting();

        // support for clearing shared preferences, on breaking changes
        if (PrefsUtil.getPrefs().contains(PrefsUtil.SETTINGS_VERSION)) {
            int ver = PrefsUtil.getPrefs().getInt(PrefsUtil.SETTINGS_VERSION, RefConstants.CURRENT_SETTINGS_VERSION);
            if (ver < RefConstants.CURRENT_SETTINGS_VERSION) {
                if (ver == 21) {
                    migrateLanguageSetting();
                    migrateCurrencySettings();
                    migrateHideBalanceOptions();
                    migrateBackendConfigs();
                    migrateCertificateEncodingAndMacaroon();
                    enterWallet();
                } else if (ver == 22) {
                    migrateCurrencySettings();
                    migrateHideBalanceOptions();
                    migrateBackendConfigs();
                    migrateCertificateEncodingAndMacaroon();
                    enterWallet();
                } else if (ver == 23) {
                    migrateHideBalanceOptions();
                    migrateBackendConfigs();
                    migrateCertificateEncodingAndMacaroon();
                    enterWallet();
                } else if (ver == 24) {
                    migrateBackendConfigs();
                    migrateCertificateEncodingAndMacaroon();
                    enterWallet();
                } else { // ver == 25
                    migrateCertificateEncodingAndMacaroon();
                    enterWallet();
                }
            } else {
                enterWallet();
            }
        } else {
            // Make sure settings get reset for versions that don't expose settings version
            resetApp();
        }
    }

    private void resetApp() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            // Reset settings
            PrefsUtil.editPrefs().clear().commit();
            try {
                PrefsUtil.editEncryptedPrefs().clear().commit();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }

            new AlertDialog.Builder(LandingActivity.this)
                    .setTitle(R.string.note)
                    .setMessage(R.string.app_reset_message)
                    .setCancelable(true)
                    .setOnCancelListener(dialogInterface -> enterWallet())
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> enterWallet())
                    .show();
        } else {
            enterWallet();
        }
    }

    private void enterWallet() {

        // Set new settings version
        PrefsUtil.editPrefs().putInt(PrefsUtil.SETTINGS_VERSION, RefConstants.CURRENT_SETTINGS_VERSION).commit();

        AppLockUtil.askForAccess(this, true, () -> {
            Intent homeIntent = new Intent(this, HomeActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            // FinishAffinity is needed here as this forces the on destroy events from previous activities to be executed before continuing.
            finishAffinity();

            startActivity(homeIntent);
        });
    }

    private void migrateCertificateEncodingAndMacaroon() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            for (BackendConfig config : BackendConfigsManager.getInstance().getAllBackendConfigs(false)) {
                if (config.getMacaroon() != null) {
                    config.setAuthenticationToken(config.getMacaroon().toLowerCase());
                    config.setMacaroon(null);
                }
                if (config.getServerCert() != null)
                    config.setServerCert(BaseEncoding.base64().encode(BaseEncoding.base64Url().decode(config.getServerCert())));
                BackendConfigsManager.getInstance().updateBackendConfig(config);
            }
            try {
                BackendConfigsManager.getInstance().apply();
            } catch (GeneralSecurityException | IOException e) {
                BBLog.w(LOG_TAG, "Certificate encoding migration failed");
                throw new RuntimeException(e);
            }
        }
    }

    private void migrateLanguageSetting() {
        String currentPrefsValue = PrefsUtil.getPrefs().getString(PrefsUtil.LANGUAGE, "system");
        if (currentPrefsValue.equals("system")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(currentPrefsValue));
        }
    }

    private void migrateBackendConfigs() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            for (BackendConfig config : BackendConfigsManager.getInstance().getAllBackendConfigs(false)) {
                config.setBackendType(BackendConfig.BackendType.LND_GRPC);
                config.setNetwork(BackendConfig.Network.UNKNOWN);
                config.setLocation(BackendConfig.Location.REMOTE);
                config.setVpnConfig(new VPNConfig());
                BackendConfigsManager.getInstance().updateBackendConfig(config);
            }
            try {
                BackendConfigsManager.getInstance().apply();
            } catch (GeneralSecurityException | IOException e) {
                BBLog.w(LOG_TAG, "Saving BackendConfigs migration failed");
                throw new RuntimeException(e);
            }
        }
    }

    private void migrateCurrencySettings() {
        String oldFirstCurrencyCode = PrefsUtil.getPrefs().getString(PrefsUtil.FIRST_CURRENCY, "");
        String oldSecondCurrencyCode = PrefsUtil.getPrefs().getString(PrefsUtil.SECOND_CURRENCY, "");

        String newFirstCurrencyCode;
        switch (oldFirstCurrencyCode) {
            case "BTC":
                newFirstCurrencyCode = "ccBTC";
                break;
            case "mBTC":
                newFirstCurrencyCode = "ccMBTC";
                break;
            case "bit":
                newFirstCurrencyCode = "ccBIT";
                break;
            default:
                newFirstCurrencyCode = "ccSAT";
        }

        String newSecondCurrencyCode;
        switch (oldSecondCurrencyCode) {
            case "BTC":
                newSecondCurrencyCode = "ccBTC";
                break;
            case "mBTC":
                newSecondCurrencyCode = "ccMBTC";
                break;
            case "bit":
                newSecondCurrencyCode = "ccBIT";
                break;
            case "sat":
                newSecondCurrencyCode = "ccSAT";
                break;
            default:
                newSecondCurrencyCode = oldSecondCurrencyCode;
        }

        PrefsUtil.editPrefs()
                .putString(PrefsUtil.FIRST_CURRENCY, newFirstCurrencyCode)
                .putString(PrefsUtil.SECOND_CURRENCY, newSecondCurrencyCode)
                .remove(PrefsUtil.AVAILABLE_FIAT_CURRENCIES)
                .commit();
    }

    private void migrateHideBalanceOptions() {
        if (PrefsUtil.getPrefs().getBoolean("hideTotalBalance", false))
            PrefsUtil.editPrefs().putString(PrefsUtil.BALANCE_HIDE_TYPE, "total").commit();
    }

    private void updateLanguageSetting() {
        if (AppCompatDelegate.getApplicationLocales().get(0) != null) {
            // This will make sure that the Language setting is in sync with the per app language settings.
            SharedPreferences.Editor editor = PrefsUtil.editPrefs();
            String languageTag = AppCompatDelegate.getApplicationLocales().get(0).toLanguageTag();
            String[] languageValues = getResources().getStringArray(R.array.languageValues);
            boolean valid = Arrays.asList(languageValues).contains(languageTag);

            if (languageTag.contains("-") && !valid) {
                // The system picker set the locale to a country specific locale that is not supported. We remove the country information.
                String[] parts = languageTag.split("-");
                languageTag = parts[0];
            }

            editor.putString(PrefsUtil.LANGUAGE, languageTag);
            editor.commit();
        }
    }
}
