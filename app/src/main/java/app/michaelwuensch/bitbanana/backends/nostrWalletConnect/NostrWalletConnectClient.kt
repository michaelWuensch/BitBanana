package app.michaelwuensch.bitbanana.backends.nostrWalletConnect

import app.michaelwuensch.bitbanana.backendConfigs.nostrWalletConnect.NostrWalletConnectUrlParser
import app.michaelwuensch.bitbanana.backends.BackendManager
import app.michaelwuensch.bitbanana.util.ApiUtil
import app.michaelwuensch.bitbanana.util.BBLog
import rust.nostr.sdk.NostrSdkException
import rust.nostr.sdk.NostrWalletConnectOptions
import rust.nostr.sdk.NostrWalletConnectUri
import rust.nostr.sdk.NostrWalletConnectUri.Companion.parse
import rust.nostr.sdk.Nwc
import rust.nostr.sdk.Nwc.Companion.withOpts
import java.time.Duration


/**
 * Singleton to handle the LndHub http client
 */
class NostrWalletConnectClient private constructor() {
    private var mNwc: Nwc? = null

    fun getNwc(): Nwc? {
        return mNwc
    }

    fun openConnection() {
        val parser =
            NostrWalletConnectUrlParser(BackendManager.getCurrentBackendConfig().fullConnectString).parse()
        if (parser.hasError()) {
            BBLog.e(LOG_TAG, "The backend config has an invalid NostrWalletConnectString")
            BackendManager.setError(BackendManager.ERROR_NWC_CONNECTION_FAILED)
            return
        }

        val nwcUri: NostrWalletConnectUri
        try {
            nwcUri = parse(BackendManager.getCurrentBackendConfig().fullConnectString)
        } catch (e: NostrSdkException) {
            BBLog.e(LOG_TAG, "Nostr Uri parsing failed. (Nostr Rust SDK). Reason: " + e.message)
            BackendManager.setError(BackendManager.ERROR_NWC_CONNECTION_FAILED)
            return
        }

        var nostrWalletConnectOptions: NostrWalletConnectOptions? = null
        try {
            nostrWalletConnectOptions = NostrWalletConnectOptions()
                .timeout(Duration.ofSeconds(ApiUtil.getBackendTimeout()))
        } catch (e: NostrSdkException) {
            BBLog.e(LOG_TAG, "Error creating NostrWalletConnectOptions. Reason: " + e.message)
            BackendManager.setError(BackendManager.ERROR_NWC_CONNECTION_FAILED)
            return
        }
        mNwc = withOpts(nwcUri, nostrWalletConnectOptions)
    }

    companion object {
        private var mNostrWalletConnectClientInstance: NostrWalletConnectClient? = null
        private val LOG_TAG: String = NostrWalletConnectClient::class.java.simpleName

        @JvmStatic
        @get:Synchronized
        val instance: NostrWalletConnectClient
            get() {
                if (mNostrWalletConnectClientInstance == null) {
                    mNostrWalletConnectClientInstance =
                        NostrWalletConnectClient()
                }
                return mNostrWalletConnectClientInstance!!
            }
    }
}
