package app.michaelwuensch.bitbanana.util;

import java.util.concurrent.TimeUnit;

public class RefConstants {

    /* This value has to be increased if any changes are made, that break the current implementation.
    It should ONLY be UPDATED ON BREAKING CHANGES.
    For example the hashing algorithm of the PIN. It will basically cause the app to reset itself on
    next startup. HANDLE THIS WITH CARE. IT MIGHT CAUSE LOSS OF IMPORTANT DATA FOR USERS.

    History:
    16: Biometrics and new Cryptography using Android Keystore
    17: Removed deviceID usage (0.2.5-alpha)
    18: Wallet name based WalletConfigs -> UUID based WalletConfigs (0.3.0-beta)
    19: Androidx.security implementation (0.4.0-beta)
    20: Added additional data to node connections: Implementation, Tor, Certificate verification (0.5.9-beta)
    21: Rebranding to BitBanana (0.6.0)
    22: Changed language selection (0.6.6)
    23: Changed currency codes (0.6.7)
    24: Changed balanceHide options (0.6.8)
    25: Added additional data to backup config: Network, VPNConfig. Changed values for backend (0.7.3)
    26: Changed certificate encoding from Base64Url to Base64 (0.7.6)
    */
    public static final int CURRENT_SETTINGS_VERSION = 26;

    // If any changes are done here, CURRENT_SETTINGS_VERSION has to be updated.

    // PIN settings
    public static final int NUM_HASH_ITERATIONS = 5000;

    ///////////////////////////////////////////////////////////////////////////////////
    // All settings below here do not require an update of the CURRENT_SETTINGS_VERSION
    ///////////////////////////////////////////////////////////////////////////////////

    // PIN settings
    public static final int PIN_MIN_LENGTH = 4;
    public static final int PIN_MAX_LENGTH = 10;
    public static final int PIN_MAX_FAILS = 3;
    public static final int PIN_DELAY_TIME = 30;

    /* This value has to be increased when something changes that affects the data backup.
    By keeping track of this number backwards compatibility can be ensured
    and new versions can restore backups from older versions.
    History:
    0: Initial release of data backup for phone migration (0.5.3-beta)
    1: Added additional data to node connections: Implementation, Tor, Certificate verification (0.5.9-beta)
    ----------- Change to BitBanana ------------
    2: Added additional data to backup config: Network. Changed values for backend (0.7.3)
    3: Changed certificate encoding from Base64Url to Base64 (0.7.6)
    */
    public static final int DATA_BACKUP_VERSION = 3;
    public static final int DATA_BACKUP_LAST_SUPPORTED_VERSION = 0;
    public static final int DATA_BACKUP_NUM_HASH_ITERATIONS = 250000;


    // Versioning for JSON data structures
    public static final int CONTACTS_JSON_VERSION = 1;
    public static final int BACKEND_CONFIG_JSON_VERSION = 0;

    // API request timeouts (in seconds)
    public static final int TIMEOUT_SHORT = 5;
    public static final int TIMEOUT_MEDIUM = 10;
    public static final int TIMEOUT_LONG = 20;
    public static final int TOR_TIMEOUT_MULTIPLIER = 3;

    // Error message durations (in milliseconds)
    public static final int ERROR_DURATION_SHORT = 3000;
    public static final int ERROR_DURATION_MEDIUM = 5000;
    public static final int ERROR_DURATION_LONG = 8000;
    public static final int ERROR_DURATION_VERY_LONG = 12000;

    // Number of seconds after moving the app to background until the app gets locked.
    public static final int ACCESS_TIMEOUT = 15;

    /* Number of seconds after moving the app to background until all connection is teared down.
    This should be long enough so that if a user accidentally minimizes minimizes the app he has enough time to open it again without having to reconnect.
    On Android 13 this seemed to work even with long delays like 30 seconds. Since Android 14 everything above 5 seconds is unreliable as battery optimization seems to stop the handler.
    If a PIN is activated, the connection will also be stopped when the PIN Screen is shown.
     */
    public static final int DISCONNECT_TIMEOUT = 3;

    // Schedule intervals
    public static final int EXCHANGE_RATE_PERIOD = 90;
    public static final TimeUnit EXCHANGE_RATE_PERIOD_UNIT = TimeUnit.SECONDS;

    // Haptic vibration
    public static final int VIBRATE_SHORT = 50;
    public static final int VIBRATE_LONG = 200;

    /* This value is a threshold used to determine if the user specified fee limit should
    be taken into consideration.
    If the payment amount is below/equal to this threshold, the user setting is not used.
    If the payment amount is above this threshold, the user setting will be considered. */
    public static final int LN_PAYMENT_FEE_THRESHOLD = 100;

    // Max number of paths to use for multi path payments (mpp)
    public static final int LN_MAX_PARTS = 10;

    // URLS
    public static final String URL_HELP = "https://github.com/michaelWuensch/BitBanana/wiki";
    public static final String URL_HELP_SETUP = "https://github.com/michaelWuensch/BitBanana/wiki/Setup";
    public static final String URL_PRIVACY = "https://bitbanana.app/privacy";
    public static final String URL_CONTRIBUTE = "https://bitbanana.app/contribute";
    public static final String URL_DONATE = "https://bitbanana.app/donate";
    public static final String URL_ISSUES = "https://github.com/michaelWuensch/BitBanana/issues";
    public static final String URL_LNPLUS = "https://lightningnetwork.plus";

    // Age in seconds for alias entries to be considered outdated.
    public static final int ALIAS_CHACHE_AGE = 3600;

    // Other
    public static final String SETUP_MODE = "setupMode";

}
