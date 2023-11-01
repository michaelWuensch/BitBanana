package app.michaelwuensch.bitbanana;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.NodeConfigsManager;
import app.michaelwuensch.bitbanana.setup.ConnectRemoteNodeActivity;
import app.michaelwuensch.bitbanana.util.PinScreenUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UriUtil;

public class LandingActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = LandingActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep in app language picker in sync with system per app language setting.
        updateLanguageSetting();

        // BitBanana was started from an URI link.
        if (App.getAppContext().getUriSchemeData() != null) {
            if (!NodeConfigsManager.getInstance().hasAnyConfigs() && UriUtil.isLNDConnectUri(App.getAppContext().getUriSchemeData())) {
                setupWalletFromUri();
                return;
            }
        }

        // support for clearing shared preferences, on breaking changes
        if (PrefsUtil.getPrefs().contains(PrefsUtil.SETTINGS_VERSION)) {
            int ver = PrefsUtil.getPrefs().getInt(PrefsUtil.SETTINGS_VERSION, RefConstants.CURRENT_SETTINGS_VERSION);
            if (ver < RefConstants.CURRENT_SETTINGS_VERSION) {
                if (ver < 22) {
                    migrateLanguageSetting();
                    migrateCurrencySettings();
                    migrateHideBalanceOptions();
                    enterWallet();
                } else if (ver < 23) {
                    migrateCurrencySettings();
                    migrateHideBalanceOptions();
                    enterWallet();
                } else {
                    migrateHideBalanceOptions();
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
        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
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

        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {
            PinScreenUtil.askForAccess(this, () -> {
                Intent homeIntent = new Intent(this, HomeActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                // FinishAffinity is needed here as this forces the on destroy events from previous activities to be executed before continuing.
                finishAffinity();

                startActivity(homeIntent);
            });

        } else {
            // Clear connection data if something is there
            PrefsUtil.editPrefs().remove(PrefsUtil.NODE_CONFIGS).commit();

            Intent homeIntent = new Intent(this, HomeActivity.class);

            // Makes sure exiting via back button from Home Screen works later (instead of going to empty activity)
            finishAffinity();

            startActivity(homeIntent);
        }
    }

    private void setupWalletFromUri() {
        Intent connectIntent = new Intent(this, ConnectRemoteNodeActivity.class);
        connectIntent.putExtra(ConnectRemoteNodeActivity.EXTRA_STARTED_FROM_URI, true);
        connectIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(connectIntent);
    }

    private void migrateLanguageSetting() {
        String currentPrefsValue = PrefsUtil.getPrefs().getString(PrefsUtil.LANGUAGE, "system");
        if (currentPrefsValue.equals("system")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(currentPrefsValue));
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
