package app.michaelwuensch.bitbanana.backends.nostrWalletConnect

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig
import app.michaelwuensch.bitbanana.backends.Api
import app.michaelwuensch.bitbanana.backends.BackendManager
import app.michaelwuensch.bitbanana.models.Balances
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest
import app.michaelwuensch.bitbanana.models.CreateInvoiceResponse
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo
import app.michaelwuensch.bitbanana.models.LnInvoice
import app.michaelwuensch.bitbanana.models.LnPayment
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse
import app.michaelwuensch.bitbanana.util.BBLog
import app.michaelwuensch.bitbanana.util.Version
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import rust.nostr.sdk.KeysendTlvRecord
import rust.nostr.sdk.ListTransactionsRequest
import rust.nostr.sdk.LookupInvoiceRequest
import rust.nostr.sdk.MakeInvoiceRequest
import rust.nostr.sdk.Nwc
import rust.nostr.sdk.PayInvoiceRequest
import rust.nostr.sdk.PayKeysendRequest
import rust.nostr.sdk.TransactionType

/**
 * Class that translates BitBanana backend interactions into LndHub API specific interactions.
 *
 *
 * You can find the LndHub documentation here:
 * https://github.com/BlueWallet/LndHub/blob/master/doc/Send-requirements.md
 */
class NostrWalletConnectApi : Api() {
    private val client: Nwc?
        get() = NostrWalletConnectClient.instance.getNwc()

    override fun getCurrentNodeInfo(): Single<CurrentNodeInfo> {
        BBLog.d(LOG_TAG, "getInfo called.")

        return RxNwcWrapper.makeRxCall(
            call = { client!!.getInfo() }, mapper = { response ->
                var network = BackendConfig.Network.MAINNET
                when (response.network?.lowercase()) {
                    "mainnet" -> network = BackendConfig.Network.MAINNET
                    "testnet" -> network = BackendConfig.Network.TESTNET
                    "regtest" -> network = BackendConfig.Network.REGTEST
                    "signet" -> network = BackendConfig.Network.SIGNET
                }

                // Update Backend Features based on returned methods list
                BackendManager.getCurrentBackend()
                    .updateFeatureKeySend(response.methods.contains("pay_keysend"))

                CurrentNodeInfo.newBuilder()
                    .setNetwork(network)
                    .setVersion(Version("1.0"))
                    .setFullVersionString("")
                    .setSynced(true)
                    .setAvatarMaterial(
                        "NWC" + response.pubkey
                    )
                    .build()
            })
            .doOnSuccess { response: CurrentNodeInfo ->
                BBLog.d(LOG_TAG, "getInfo success.")
            }
            .doOnError { throwable: Throwable ->
                BBLog.w(LOG_TAG, "getInfo failed: " + throwable.message)
            }
    }

    override fun getBalances(): Single<Balances> {
        BBLog.d(LOG_TAG, "getBalances called.")

        return RxNwcWrapper.makeRxCall(
            call = { client!!.getBalance() }, mapper = { response ->
                Balances.newBuilder()
                    .setChannelBalance(response.toLong())
                    .build()
            })
            .doOnSuccess { response: Balances ->
                BBLog.d(LOG_TAG, "getBalances success.")
            }
            .doOnError { throwable: Throwable ->
                BBLog.w(LOG_TAG, "getBalances failed: " + throwable.message)
            }
    }

    override fun createInvoice(createInvoiceRequest: CreateInvoiceRequest): Single<CreateInvoiceResponse> {
        BBLog.d(LOG_TAG, "createInvoice called.")

        val request = MakeInvoiceRequest(
            amount = createInvoiceRequest.amount.toULong(),
            description = createInvoiceRequest.description,
            descriptionHash = null,
            expiry = createInvoiceRequest.expiry.toULong()
        )

        return RxNwcWrapper.makeRxCall(
            call = { client!!.makeInvoice(request) }, mapper = { response ->
                CreateInvoiceResponse.newBuilder()
                    .setBolt11(response.invoice)
                    .build()
            })
            .doOnSuccess { response: CreateInvoiceResponse ->
                BBLog.d(LOG_TAG, "createInvoice success.")
            }
            .doOnError { throwable: Throwable ->
                BBLog.w(LOG_TAG, "createInvoice failed: " + throwable.message)
            }
    }

    override fun getInvoice(paymentHash: String?): Single<LnInvoice> {
        BBLog.d(LOG_TAG, "getInvoice called.")

        val request = LookupInvoiceRequest(
            paymentHash = paymentHash,
            invoice = null
        )

        return RxNwcWrapper.makeRxCall(
            call = { client!!.lookupInvoice(request) }, mapper = { response ->
                val builder = LnInvoice.newBuilder()
                    .setType(LnInvoice.InvoiceType.BOLT11_INVOICE)
                    .setCreatedAt(response.createdAt.asSecs().toLong())
                    .setMemo(response.description)
                    .setBolt11(response.invoice)
                    .setAmountRequested(response.amount.toLong())
                    .setPaymentHash(response.paymentHash)
                if (response.settledAt != null && response.settledAt!!.asSecs() != 0L.toULong()) {
                    builder.setPaidAt(response.settledAt!!.asSecs().toLong())
                    builder.setAmountPaid(response.amount.toLong())
                }
                if (response.expiresAt != null && response.expiresAt!!.asSecs() != 0L.toULong()) {
                    builder.setExpiresAt(response.expiresAt!!.asSecs().toLong())
                }
                builder.build()
            })
            .doOnSuccess { response: LnInvoice ->
                BBLog.d(LOG_TAG, "getInvoice success.")
            }
            .doOnError { throwable: Throwable ->
                BBLog.w(LOG_TAG, "getInvoice failed: " + throwable.message)
            }
    }

    override fun listInvoices(
        firstIndexOffset: Long,
        pageSize: Int
    ): Single<MutableList<LnInvoice>> {
        BBLog.d(LOG_TAG, "listInvoices called.")

        val request = ListTransactionsRequest(
            transactionType = TransactionType.INCOMING,
            unpaid = true,
            from = null,
            until = null,
            limit = null,
            offset = null
        )

        return RxNwcWrapper.makeRxCall(
            call = { client!!.listTransactions(request) }, mapper = { response ->
                val invoiceList: MutableList<LnInvoice> = ArrayList()
                for (invoice in response) {
                    val builder = LnInvoice.newBuilder()
                        .setType(LnInvoice.InvoiceType.BOLT11_INVOICE)
                        .setCreatedAt(invoice.createdAt.asSecs().toLong())
                        .setMemo(invoice.description)
                        .setBolt11(invoice.invoice)
                        .setAmountRequested(invoice.amount.toLong())
                        .setPaymentHash(invoice.paymentHash)
                    if (invoice.settledAt != null && invoice.settledAt!!.asSecs() != 0L.toULong()) {
                        builder.setPaidAt(invoice.settledAt!!.asSecs().toLong())
                        builder.setAmountPaid(invoice.amount.toLong())
                    }
                    if (invoice.expiresAt != null && invoice.expiresAt!!.asSecs() != 0L.toULong()) {
                        builder.setExpiresAt(invoice.expiresAt!!.asSecs().toLong())
                    }

                    invoiceList.add(builder.build())
                }
                invoiceList
            })
            .doOnSuccess { response: MutableList<LnInvoice> ->
                BBLog.d(LOG_TAG, "listInvoices success.")
            }
            .doOnError { throwable: Throwable ->
                BBLog.w(LOG_TAG, "listInvoices failed: " + throwable.message)
            }
    }

    override fun listLnPayments(
        firstIndexOffset: Long,
        pageSize: Int
    ): Single<MutableList<LnPayment>> {
        BBLog.d(LOG_TAG, "listPayments called.")

        val request = ListTransactionsRequest(
            transactionType = TransactionType.OUTGOING,
            unpaid = null,
            from = null,
            until = null,
            limit = null,
            offset = null
        )

        return RxNwcWrapper.makeRxCall(
            call = { client!!.listTransactions(request) }, mapper = { response ->
                val paymentsList: MutableList<LnPayment> = ArrayList()
                for (payment in response) {
                    val builder = LnPayment.newBuilder()
                        .setPaymentPreimage(payment.preimage)
                        .setFee(payment.feesPaid.toLong())
                        .setDescription(payment.description)
                        .setCreatedAt(payment.createdAt.asSecs().toLong())
                        .setAmountPaid(payment.amount.toLong())
                        .setBolt11(payment.invoice)
                        .setPaymentHash(payment.paymentHash)
                        .setStatus(LnPayment.Status.SUCCEEDED)

                    paymentsList.add(builder.build())
                }
                paymentsList
            })
            .doOnSuccess { response: MutableList<LnPayment> ->
                BBLog.d(LOG_TAG, "listPayments success.")
            }
            .doOnError { throwable: Throwable ->
                BBLog.w(LOG_TAG, "listPayments failed: " + throwable.message)
            }
    }

    override fun addWatchtower(pubKey: String?, address: String?): Completable {
        return super.addWatchtower(pubKey, address)
    }

    override fun sendLnPayment(sendLnPaymentRequest: SendLnPaymentRequest): Single<SendLnPaymentResponse> {
        BBLog.d(LOG_TAG, "sendLnPayment called.")

        if (sendLnPaymentRequest.paymentType == SendLnPaymentRequest.PaymentType.BOLT11_INVOICE) {
            val request = PayInvoiceRequest(
                id = null,
                invoice = sendLnPaymentRequest.bolt11.bolt11String,
                amount = null
            )

            return RxNwcWrapper.makeRxCall(
                call = { client!!.payInvoice(request) }, mapper = { response ->
                    val builder = SendLnPaymentResponse.newBuilder()
                        .setPaymentPreimage(response.preimage)
                    builder.build()
                })
                .doOnSuccess { response: SendLnPaymentResponse ->
                    BBLog.d(LOG_TAG, "sendLnPayment success.")
                }
                .doOnError { throwable: Throwable ->
                    BBLog.w(LOG_TAG, "sendLnPayment failed: " + throwable.message)
                }
        }

        if (sendLnPaymentRequest.paymentType == SendLnPaymentRequest.PaymentType.KEYSEND) {

            val tlvRecords: MutableList<KeysendTlvRecord> = ArrayList()
            for (record in sendLnPaymentRequest.customRecords) {
                tlvRecords.add(
                    KeysendTlvRecord(
                        record.fieldNumber.toULong(),
                        record.value
                    )
                )
            }
            val request = PayKeysendRequest(
                id = null,
                amount = sendLnPaymentRequest.amount.toULong(),
                pubkey = sendLnPaymentRequest.destinationPubKey,
                preimage = null,
                tlvRecords = tlvRecords
            )
            return RxNwcWrapper.makeRxCall(
                call = { client!!.payKeysend(request) }, mapper = { response ->
                    val builder = SendLnPaymentResponse.newBuilder()
                        .setPaymentPreimage(response.preimage)
                    builder.build()
                })
                .doOnSuccess { response: SendLnPaymentResponse ->
                    BBLog.d(LOG_TAG, "sendLnPayment success.")
                }
                .doOnError { throwable: Throwable ->
                    BBLog.w(LOG_TAG, "sendLnPayment failed: " + throwable.message)
                }
        }
        // Bolt 12 payment is not implemented
        return super.sendLnPayment(sendLnPaymentRequest)
    }

    companion object {
        private val LOG_TAG: String = NostrWalletConnectApi::class.java.simpleName
    }
}