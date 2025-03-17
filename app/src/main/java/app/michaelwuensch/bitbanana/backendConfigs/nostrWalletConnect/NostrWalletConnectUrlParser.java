package app.michaelwuensch.bitbanana.backendConfigs.nostrWalletConnect;

import java.net.URI;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BaseConnectionParser;
import app.michaelwuensch.bitbanana.models.LnAddress;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.HexUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;
import rust.nostr.sdk.NostrWalletConnectUri;

/**
 * This class parses a NostrWalletConnect string: (https://github.com/nostr-protocol/nips/blob/master/47.md#nostr-wallet-connect-uri)
 * <p>
 * A NostrWalletConnect uri consists of the following parts:
 * nostr+walletconnect://<PUBKEY>?relay=<RELAY>&secret=<SECRET>
 * The parser returns an object containing the desired data or an descriptive error.
 */
public class NostrWalletConnectUrlParser extends BaseConnectionParser {

    public static final int ERROR_INVALID_CONNECT_STRING = 0;
    public static final int ERROR_INVALID_PUBKEY = 1;
    public static final int ERROR_NO_RELAY = 2;
    public static final int ERROR_NO_SECRET = 3;

    private String mPubKey;

    public String getPubKey() {
        return mPubKey;
    }

    private String mRelay;

    public String getRelay() {
        return mRelay;
    }

    private String mSecret;

    public String getSecret() {
        return mSecret;
    }

    private String mLud16;

    public String getLud16() {
        return mLud16;
    }

    private static final String LOG_TAG = NostrWalletConnectUrlParser.class.getSimpleName();

    public NostrWalletConnectUrlParser(String connectString) {
        super(connectString);
    }

    public NostrWalletConnectUrlParser parse() {

        // validate not null
        if (mConnectionString == null) {
            BBLog.e(LOG_TAG, "NostrWalletConnect string is null");
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        // validate scheme
        if (!UriUtil.isNostrWalletConnectUri(mConnectionString)) {
            BBLog.e(LOG_TAG, "NostrWalletConnect string does not start with nostr+walletconnect://");
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }

        URI connectURI = null;
        try {
            connectURI = new URI(mConnectionString);

            // validate PubKey
            if (!HexUtil.isHex(connectURI.getHost()) || connectURI.getHost().length() != 64) {
                BBLog.e(LOG_TAG, "NostrWalletConnect string does not include a valid PubKey");
                mError = ERROR_INVALID_PUBKEY;
                return this;
            }

            String relay = null;
            String secret = null;
            String lud16 = null;

            // fetch params
            if (connectURI.getQuery() != null) {
                String[] valuePairs = connectURI.getQuery().split("&");

                for (String pair : valuePairs) {
                    String[] param = pair.split("=");
                    if (param.length > 1) {
                        if (param[0].equalsIgnoreCase("relay")) {
                            relay = param[1];
                        }
                        if (param[0].equalsIgnoreCase("secret")) {
                            secret = param[1];
                        }
                        if (param[0].equalsIgnoreCase("lud16")) {
                            lud16 = param[1];
                        }
                    }
                }

                // validate relay
                if (relay == null) {
                    BBLog.e(LOG_TAG, "NostrWalletConnect string does not include a relay");
                    mError = ERROR_NO_RELAY;
                    return this;
                } else {
                    // ToDo: relay validation
                }

                // validate secret
                if (secret == null) {
                    BBLog.e(LOG_TAG, "NostrWalletConnect string does not include a secret");
                    mError = ERROR_NO_SECRET;
                    return this;
                } else {
                    // validate PubKey
                    if (!HexUtil.isHex(secret) || secret.length() != 64) {
                        BBLog.e(LOG_TAG, "NostrWalletConnect secret format is invalid");
                        mError = ERROR_INVALID_PUBKEY;
                        return this;
                    }
                }

                // validate lud16 (lud16 is optional)
                if (lud16 != null) {
                    if (!new LnAddress(lud16).isValidLnurlAddress()) {
                        BBLog.w(LOG_TAG, "NostrWalletConnect lud16 address is invalid.");
                    } else {
                        mLud16 = lud16;
                    }
                }

                // Use the parser form rust.nostr to validate the rest. It does not give very helpful error messages, therefore we validated lots of things first before running this parser.
                try {
                    NostrWalletConnectUri.Companion.parse(mConnectionString);
                } catch (Exception e) {
                    BBLog.e(LOG_TAG, "Rust.nostr nwc uri parsing failed. Exception message: " + e.getMessage());
                    mError = ERROR_INVALID_CONNECT_STRING;
                    return this;
                }

                // everything is ok
                BackendConfig backendConfig = new BackendConfig();
                backendConfig.setSource(BackendConfig.Source.NOSTR_WALLET_CONNECT);
                backendConfig.setBackendType(BackendConfig.BackendType.NOSTR_WALLET_CONNECT);
                backendConfig.setLocation(BackendConfig.Location.REMOTE);
                backendConfig.setNetwork(BackendConfig.Network.UNKNOWN);
                backendConfig.setFullConnectString(mConnectionString);
                if(mLud16 != null) {
                    backendConfig.setQuickReceiveType(BackendConfig.QuickReceiveType.SIMPLE_STRING);
                    backendConfig.setQuickReceiveString(mLud16);
                }

                setBackendConfig(backendConfig);

                mPubKey = connectURI.getHost();
                mRelay = relay;
                mSecret = secret;

                return this;

            } else {
                BBLog.e(LOG_TAG, "NostrWalletConnect URI has no parameters");
                mError = ERROR_INVALID_CONNECT_STRING;
                return this;
            }

        } catch (Exception e) {
            BBLog.e(LOG_TAG, "URI could not be parsed. Exception message: " + e.getMessage());
            mError = ERROR_INVALID_CONNECT_STRING;
            return this;
        }
    }
}
