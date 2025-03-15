package app.michaelwuensch.bitbanana.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.CustomRecord;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class PaymentUtil {

    private static final String LOG_TAG = PaymentUtil.class.getSimpleName();
    public static final long KEYSEND_MESSAGE_RECORD = 34349334L;
    public static final long KEYSEND_PREIMAGE_RECORD = 5482373484L;
    private static final int PREIMAGE_BYTE_LENGTH = 32;

    /**
     * Used to send a lightning payment for a bolt 11 invoice.
     * Use -1 for custom fee rate if you want to use the default fee rate from the app settings.
     *
     * @param decodedBolt11 The decodedBolt11 invoice that will get paid
     */
    public static SendLnPaymentRequest prepareBolt11InvoicePayment(@NonNull DecodedBolt11 decodedBolt11, long amount, @Nullable ShortChannelId firstHop, @Nullable String lastHop, float customFeeRateInPercent) {
        return SendLnPaymentRequest.newBuilder()
                .setPaymentType(SendLnPaymentRequest.PaymentType.BOLT11_INVOICE)
                .setBolt11(decodedBolt11)
                .setAmount(amount)
                .setMaxFee(calculateAbsoluteFeeLimit(amount, customFeeRateInPercent))
                .setFirstHop(firstHop)
                .setLastHop(lastHop)
                .build();
    }

    /**
     * Used to send a lightning payment for a bolt 12 invoice.
     *
     * @param bolt12Invoice The fetched bolt 12 invoice that will get paid
     */
    public static SendLnPaymentRequest prepareBolt12InvoicePayment(@NonNull String bolt12Invoice, long amount) {
        return SendLnPaymentRequest.newBuilder()
                .setPaymentType(SendLnPaymentRequest.PaymentType.BOLT12_INVOICE)
                .setBolt12InvoiceString(bolt12Invoice)
                .setAmount(amount)
                .setMaxFee(calculateAbsoluteFeeLimit(amount, -1))
                .build();
    }

    public static SendLnPaymentRequest prepareKeysendPayment(String pubkey, long amount, String message, @Nullable ShortChannelId firstHop, @Nullable String lastHop) {

        // Create the preimage upfront
        SecureRandom random = new SecureRandom();
        byte[] preimage = new byte[PREIMAGE_BYTE_LENGTH];
        random.nextBytes(preimage);

        List<CustomRecord> customRecords = new ArrayList<>();
        customRecords.add(CustomRecord.newBuilder()
                .setFieldNumber(KEYSEND_PREIMAGE_RECORD)
                .setValue(HexUtil.bytesToHex(preimage))
                .build());

        if (message != null && !message.isEmpty()) {
            customRecords.add(CustomRecord.newBuilder()
                    .setFieldNumber(KEYSEND_MESSAGE_RECORD)
                    .setValue(HexUtil.bytesToHex(message.getBytes(StandardCharsets.UTF_8)))
                    .build());
        }

        long feeLimit = calculateAbsoluteFeeLimit(amount, -1);

        return SendLnPaymentRequest.newBuilder()
                .setPaymentType(SendLnPaymentRequest.PaymentType.KEYSEND)
                .setDestinationPubKey(pubkey)
                .setPreimage(HexUtil.bytesToHex(preimage))
                .setPaymentHash(HexUtil.bytesToHex(UtilFunctions.sha256HashByte(preimage)))
                .setCustomRecords(customRecords)
                .setAmount(amount)
                .setMaxFee(feeLimit)
                .setFirstHop(firstHop)
                .setLastHop(lastHop)
                .build();
    }

    public static void sendLnPayment(SendLnPaymentRequest sendLnPaymentRequest, CompositeDisposable compositeDisposable, OnPaymentResult result) {
        switch (sendLnPaymentRequest.getPaymentType()) {
            case BOLT11_INVOICE:
                BBLog.d(LOG_TAG, "Trying to pay invoice...");
                break;
            case KEYSEND:
                if (FeatureManager.isKeysendEnabled())
                    BBLog.d(LOG_TAG, "Trying to perform keysend...");
                else {
                    result.onError(App.getAppContext().getString(R.string.error_feature_not_supported_by_backend, BackendManager.getCurrentBackend().getNodeImplementationName(), "KEYSEND"), null, RefConstants.ERROR_DURATION_MEDIUM);
                    return;
                }
                break;
        }

        compositeDisposable.add(BackendManager.api().sendLnPayment(sendLnPaymentRequest)
                .subscribe(response -> {
                    if (response.didSucceed()) {
                        // updated the history, so it is shown the next time the user views it
                        Wallet_TransactionHistory.getInstance().updateLightningPaymentHistory();
                        result.onSuccess(response);
                    } else {
                        result.onError(response.getFailureReason().toString(), response, RefConstants.ERROR_DURATION_MEDIUM);
                    }
                }, throwable -> {
                    BBLog.e(LOG_TAG, "Exception in lightning payment task.");
                    result.onError(throwable.getMessage(), null, RefConstants.ERROR_DURATION_MEDIUM);
                }));
    }


    /**
     * We always allow a fee of at least 3 sats, to ensure also small payments have a chance.
     * For payments of over 100 sat we apply the user settings, for payments lower, we use the square root of the amount to send.
     *
     * @param amountMSatToSend Amount that should be send with the transaction
     * @return maximum number of msats in fee
     */
    public static long calculateAbsoluteFeeLimit(long amountMSatToSend, float customFeeRateInPercent) {
        long absFee;

        // No custom fee rate was set, go with our default procedure to calculate the fee using the settings default
        if (customFeeRateInPercent < 0) {
            if (amountMSatToSend <= RefConstants.LN_PAYMENT_FEE_THRESHOLD * 1000)
                absFee = (long) (Math.sqrt(amountMSatToSend));
            else
                absFee = (long) (getRelativeSettingsFeeLimit() * amountMSatToSend);

            return Math.max(absFee, 3000L);
        }

        // We explicitly set the fee rate to 0. Only consider zero fee routes
        if (customFeeRateInPercent == 0)
            return 0;

        // A custom non zero fee rate was set, apply our default procedure for small amounts, and apply the custom fee rate for bigger values
        if (amountMSatToSend <= RefConstants.LN_PAYMENT_FEE_THRESHOLD * 1000)
            absFee = (long) (Math.sqrt(amountMSatToSend));
        else
            absFee = (long) ((customFeeRateInPercent / 100f) * amountMSatToSend);

        return Math.max(absFee, 3000L);
    }


    public static float getRelativeSettingsFeeLimit() {
        String lightning_feeLimit = PrefsUtil.getPrefs().getString("lightning_feeLimit", "1%");
        String feePercent = lightning_feeLimit.replace("%", "");
        float feeMultiplier = 1f;
        if (!feePercent.equals(App.getAppContext().getResources().getString(R.string.none))) {
            feeMultiplier = Float.parseFloat(feePercent) / 100f;
        }
        return feeMultiplier;
    }

    public interface OnPaymentResult {
        void onSuccess(SendLnPaymentResponse sendLnPaymentResponse);

        void onError(String error, SendLnPaymentResponse sendLnPaymentResponse, int duration);
    }
}
