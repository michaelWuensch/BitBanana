package app.michaelwuensch.bitbanana.util;

import android.content.Context;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import fr.acinq.lightning.payment.Bolt11Invoice;
import fr.acinq.lightning.payment.Bolt12Invoice;
import fr.acinq.lightning.wire.OfferTypes;

public class InvoiceUtil {
    private static final String LOG_TAG = InvoiceUtil.class.getSimpleName();

    public static String INVOICE_PREFIX_LIGHTNING_MAINNET = "lnbc";
    public static String INVOICE_PREFIX_LIGHTNING_TESTNET = "lntb";
    public static String INVOICE_PREFIX_LIGHTNING_REGTEST = "lnbcrt";
    public static String INVOICE_PREFIX_LIGHTNING_SIGNET = "lntbs";
    public static String OFFER_PREFIX = "lno";
    public static ArrayList<String> ADDRESS_PREFIX_ONCHAIN_MAINNET = new ArrayList<>(Arrays.asList("1", "3", "bc1"));
    public static ArrayList<String> ADDRESS_PREFIX_ONCHAIN_TESTNET = new ArrayList<>(Arrays.asList("m", "n", "2", "tb1"));
    public static ArrayList<String> ADDRESS_PREFIX_ONCHAIN_REGTEST = new ArrayList<>(Arrays.asList("m", "n", "2", "bcrt1"));
    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_NETWORK_MISMATCH = 1;
    public static final int ERROR_INVOICE_EXPIRED = 1;
    public static final int ERROR_BOLT12_EXPIRED = 1;
    private static final int INVOICE_LIGHTNING_MIN_LENGTH = 6;


    public static boolean isLightningInvoice(@NonNull String data) {
        if (data.isEmpty() || data.length() < INVOICE_LIGHTNING_MIN_LENGTH) {
            return false;
        }

        return hasPrefix(INVOICE_PREFIX_LIGHTNING_MAINNET, data) || hasPrefix(INVOICE_PREFIX_LIGHTNING_TESTNET, data) || hasPrefix(INVOICE_PREFIX_LIGHTNING_REGTEST, data);
    }

    public static boolean isLightningOffer(@NonNull String data) {
        return hasPrefix(OFFER_PREFIX, data);
    }

    public static boolean isBitcoinAddress(@NonNull String data) {
        if (data.isEmpty()) {
            return false;
        }
        return (isBase58Address(data) || isBech32Address(data));
    }

    public static boolean isBase58Address(String input) {
        // This is not a full validation. It just checks if only valid characters are used and if the length is correct. The checksum is not verified.
        // The full validation is done by LND when interacting with the address.
        return input.matches("^[123mn][a-km-zA-HJ-NP-Z1-9]{25,34}$");
    }

    public static boolean isBech32Address(String input) {
        // This is not a full validation. It just checks if only valid characters are used and if the length is correct. The checksum is not verified.
        // The simplified validation works for both Bech32 (BIP173) and Bech32m (BIP 350)
        // The full validation is done by LND when interacting with the address.
        return input.matches("^((bc1|tb1|bcrt1)[ac-hj-np-z02-9]{11,71}|(BC1|TB1|BCRT1)[AC-HJ-NP-Z02-9]{11,71})$");
    }

    public static void readInvoice(Context ctx, String data, Bip21Invoice fallbackOnChainInvoice, OnReadInvoiceCompletedListener listener) {

        // Avoid index out of bounds. An Request with less than 11 characters isn't valid.
        if (data.length() < 11) {
            listener.onNoInvoiceData();
            return;
        }

        // convert to lower case
        String lnInvoice = data.toLowerCase();

        // Remove the "lightning:" uri scheme if it is present, LND needs it without uri scheme
        lnInvoice = UriUtil.removeURI(lnInvoice);

        // Check if the invoice is a lightning invoice
        if (isLightningInvoice(lnInvoice)) {

            // Check if the invoice is for the same network the app is connected to
            switch (Wallet.getInstance().getNetwork()) {
                case MAINNET:
                    if (hasPrefix(INVOICE_PREFIX_LIGHTNING_MAINNET, lnInvoice) && !hasPrefix(INVOICE_PREFIX_LIGHTNING_REGTEST, lnInvoice)) {
                        decodeLightningInvoice(ctx, listener, lnInvoice, fallbackOnChainInvoice);
                    } else {
                        // Show error. Please use a MAINNET invoice.
                        listener.onError(ctx.getString(R.string.error_useMainnetRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                case TESTNET:
                    if (hasPrefix(INVOICE_PREFIX_LIGHTNING_TESTNET, lnInvoice)) {
                        decodeLightningInvoice(ctx, listener, lnInvoice, fallbackOnChainInvoice);
                    } else {
                        // Show error. Please use a TESTNET invoice.
                        listener.onError(ctx.getString(R.string.error_useTestnetRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                case REGTEST:
                    if (hasPrefix(INVOICE_PREFIX_LIGHTNING_REGTEST, lnInvoice)) {
                        decodeLightningInvoice(ctx, listener, lnInvoice, fallbackOnChainInvoice);
                    } else {
                        // Show error. Please use a REGTEST invoice.
                        listener.onError(ctx.getString(R.string.error_useRegtestRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                default:
                    listener.onError(ctx.getString(R.string.error_unsupported_network), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
            }
        } else if (isLightningOffer(lnInvoice)) {
            decodeLightningOffer(ctx, listener, lnInvoice, fallbackOnChainInvoice);
        } else {
            // We do not have a lightning invoice... check if it is a valid bitcoin address / invoice

            // Check if we have a bip21 bitcoin invoice with the "bitcoin:" uri scheme
            if (UriUtil.isBitcoinUri(data)) {

                // Add "//" to make it parsable for the java URI class if it is not present
                if (!data.substring(0, 10).equalsIgnoreCase(UriUtil.URI_PREFIX_BITCOIN + "//")) {
                    data = UriUtil.URI_PREFIX_BITCOIN + "//" + data.substring(8);
                }

                URI bitcoinURI = null;
                try {
                    bitcoinURI = new URI(data);

                    String onChainAddress = bitcoinURI.getHost();

                    long onChainInvoiceAmount = 0L;
                    String onChainInvoiceMessage = null;
                    String lightningInvoice = null;
                    String bolt12Offer = null;
                    String lnurl = null;

                    // Fetch params
                    if (bitcoinURI.getQuery() != null) {
                        String[] valuePairs = bitcoinURI.getQuery().split("&");
                        for (String pair : valuePairs) {
                            String[] param = pair.split("=");
                            if (param[0].equals("amount")) {
                                onChainInvoiceAmount = (long) (Double.parseDouble(param[1]) * 1e8 * 1000L);
                            }
                            if (param[0].equals("message")) {
                                onChainInvoiceMessage = param[1];
                            }
                            if (param[0].equals("lightning")) {
                                lightningInvoice = param[1];
                            }
                            if (param[0].equals("lno")) {
                                bolt12Offer = param[1];
                            }
                            if (param[0].equals("lnurl")) {
                                lnurl = param[1];
                            }
                        }

                        Bip21Invoice onChainInvoice = Bip21Invoice.newBuilder()
                                .setAddress(onChainAddress)
                                .setAmount(onChainInvoiceAmount)
                                .setMessage(onChainInvoiceMessage)
                                .build();

                        boolean validOnChainAddress = validateOnChainAddress(onChainAddress);


                        // Check for other payment data than standard bitcoin address. We prefer lightning.
                        // If other data is available, we validate it. If it is invalid, we continue with the BTC address.

                        // If more than one payment information is provided, this order in the code defines what we use.
                        if (bolt12Offer != null && BackendManager.getCurrentBackend().supportsBolt12Sending()) {
                            if (validateBolt12LightningOffer(bolt12Offer)) {
                                if (validOnChainAddress)
                                    readInvoice(ctx, bolt12Offer, onChainInvoice, listener);
                                else
                                    readInvoice(ctx, bolt12Offer, null, listener);
                                return;
                            }
                        }
                        if (lightningInvoice != null) {
                            if (validateBolt11Invoice(lightningInvoice)) {
                                if (validOnChainAddress)
                                    readInvoice(ctx, lightningInvoice, onChainInvoice, listener);
                                else
                                    readInvoice(ctx, lightningInvoice, null, listener);
                                return;
                            }
                        }
                        if (lnurl != null) {
                            // ToDo: Support this case
                        }

                        // None of the extra payment data is valid.  Now use the same order a
                        if (!validOnChainAddress) {
                            // onChain address is invalid as well. Now we call the methods in the same order, so that an actual useful error message will be displayed.
                            if (bolt12Offer != null) {
                                decodeLightningOffer(ctx, listener, bolt12Offer, null);
                                return;
                            }
                            if (lightningInvoice != null) {
                                decodeLightningInvoice(ctx, listener, lightningInvoice, null);
                                return;
                            }
                        }
                    }

                    // No extra payment data, go with bitcoin address
                    readOnChainAddress(ctx, listener, Bip21Invoice.newBuilder()
                            .setAddress(onChainAddress)
                            .setAmount(onChainInvoiceAmount)
                            .setMessage(onChainInvoiceMessage)
                            .build());

                } catch (URISyntaxException e) {
                    BBLog.w(LOG_TAG, "URI could not be parsed");
                    e.printStackTrace();
                    listener.onError(ctx.getString(R.string.error_invalid_bitcoin_request), RefConstants.ERROR_DURATION_MEDIUM, ERROR_UNKNOWN);
                }

            } else {
                // We also don't have a bip21 bitcoin invoice, check if the data is a valid bitcoin address
                readOnChainAddress(ctx, listener, Bip21Invoice.newBuilder()
                        .setAddress(data)
                        .build());
            }
        }
    }

    private static void readOnChainAddress(Context ctx, OnReadInvoiceCompletedListener listener, Bip21Invoice onChainInvoice) {
        String address = onChainInvoice.getAddress();
        if (address != null && isBitcoinAddress(address)) {
            switch (Wallet.getInstance().getNetwork()) {
                case MAINNET:
                    if (hasPrefix(ADDRESS_PREFIX_ONCHAIN_MAINNET, address)) {
                        listener.onValidBitcoinInvoice(onChainInvoice);
                    } else {
                        listener.onError(ctx.getString(R.string.error_useMainnetRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                case TESTNET:
                    if (hasPrefix(ADDRESS_PREFIX_ONCHAIN_TESTNET, address)) {
                        listener.onValidBitcoinInvoice(onChainInvoice);
                    } else {
                        listener.onError(ctx.getString(R.string.error_useTestnetRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                case REGTEST:
                    if (hasPrefix(ADDRESS_PREFIX_ONCHAIN_REGTEST, address)) {
                        listener.onValidBitcoinInvoice(onChainInvoice);
                    } else {
                        listener.onError(ctx.getString(R.string.error_useRegtestRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                default:
                    listener.onError(ctx.getString(R.string.error_unsupported_network), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
            }
        } else {
            listener.onNoInvoiceData();
        }
    }

    private static void decodeLightningInvoice(Context ctx, OnReadInvoiceCompletedListener listener, String invoice, Bip21Invoice fallbackOnChainInvoice) {
        try {
            DecodedBolt11 decodedBolt11 = decodeBolt11(invoice);
            if (decodedBolt11.isExpired())
                listener.onError(ctx.getString(R.string.error_paymentRequestExpired), RefConstants.ERROR_DURATION_SHORT, ERROR_INVOICE_EXPIRED);
            else
                listener.onValidLightningInvoice(decodedBolt11, fallbackOnChainInvoice);
        } catch (Exception e) {
            listener.onError(e.getMessage(), RefConstants.ERROR_DURATION_MEDIUM, ERROR_UNKNOWN);
        }
    }

    /**
     * This function just validates if a on chain bitcoin address will be valid in the given context. It does not cause any action.
     */
    private static boolean validateOnChainAddress(String address) {
        if (!BackendManager.getCurrentBackend().supportsOnChainSending())
            return false;

        if (address != null && isBitcoinAddress(address)) {
            switch (Wallet.getInstance().getNetwork()) {
                case MAINNET:
                    if (!hasPrefix(ADDRESS_PREFIX_ONCHAIN_MAINNET, address)) {
                        return false;
                    }
                    break;
                case TESTNET:
                    if (!hasPrefix(ADDRESS_PREFIX_ONCHAIN_TESTNET, address)) {
                        return false;
                    }
                    break;
                case REGTEST:
                    if (!hasPrefix(ADDRESS_PREFIX_ONCHAIN_REGTEST, address)) {
                        return false;
                    }
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * This function just validates if a bolt11 invoice will be valid in the given context. It does not cause any action.
     */
    private static boolean validateBolt11Invoice(String invoice) {
        if (!FeatureManager.isOffchainSendingEnabled())
            return false;

        switch (Wallet.getInstance().getNetwork()) {
            case MAINNET:
                if (!hasPrefix(INVOICE_PREFIX_LIGHTNING_MAINNET, invoice) || hasPrefix(INVOICE_PREFIX_LIGHTNING_REGTEST, invoice)) {
                    return false;
                }
                break;
            case TESTNET:
                if (!hasPrefix(INVOICE_PREFIX_LIGHTNING_TESTNET, invoice)) {
                    return false;
                }
                break;
            case REGTEST:
                if (!hasPrefix(INVOICE_PREFIX_LIGHTNING_REGTEST, invoice)) {
                    return false;
                }
                break;
        }

        try {
            DecodedBolt11 decodedBolt11 = decodeBolt11(invoice);
            if (decodedBolt11.isExpired())
                return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This function just validates if a bolt12 offer will be valid in the given context. It does not cause any action.
     */
    private static boolean validateBolt12LightningOffer(String offer) {
        if (!BackendManager.getCurrentBackend().supportsBolt12Sending())
            return false;

        try {
            DecodedBolt12 decodedBolt12 = decodeBolt12(offer);
            if (decodedBolt12.isExpired())
                return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void decodeLightningOffer(Context ctx, OnReadInvoiceCompletedListener listener, String invoice, Bip21Invoice fallbackOnChainInvoice) {
        if (!BackendManager.getCurrentBackend().supportsBolt12Sending()) {
            listener.onError(ctx.getString(R.string.error_feature_not_supported_by_backend, BackendManager.getCurrentBackend().getNodeImplementationName(), "BOLT12 sending"), RefConstants.ERROR_DURATION_MEDIUM, ERROR_UNKNOWN);
            return;
        }
        try {
            DecodedBolt12 decodedBolt12 = decodeBolt12(invoice);
            if (decodedBolt12.isExpired())
                listener.onError(ctx.getString(R.string.error_bolt12_expired), RefConstants.ERROR_DURATION_SHORT, ERROR_INVOICE_EXPIRED);
            else
                listener.onValidBolt12Offer(decodedBolt12, fallbackOnChainInvoice);
        } catch (Exception e) {
            listener.onError(e.getMessage(), RefConstants.ERROR_DURATION_MEDIUM, ERROR_UNKNOWN);
        }
    }

    private static boolean hasPrefix(@NonNull String prefix, @NonNull String data) {
        if (data.isEmpty() || data.length() < prefix.length()) {
            return false;
        }

        return data.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }

    private static boolean hasPrefix(@NonNull ArrayList<String> prefixes, @NonNull String data) {
        if (data.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (data.length() >= prefix.length()) {
                if (data.substring(0, prefix.length()).equalsIgnoreCase(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static DecodedBolt11 decodeBolt11(String bolt11) throws Exception {
        try {
            Bolt11Invoice decoded = Bolt11Invoice.Companion.read(bolt11).get();
            long amount = decoded.getAmount() == null ? 0 : decoded.getAmount().getMsat();
            long expiry = decoded.getExpirySeconds() == null ? 3600 : decoded.getExpirySeconds(); // 3600 is default if not specified, see bolt11 specs
            String descriptionHash = decoded.getDescriptionHash() == null ? null : decoded.getDescriptionHash().toHex();
            return DecodedBolt11.newBuilder()
                    .setBolt11String(bolt11)
                    .setPaymentHash(decoded.getPaymentHash().toHex())
                    .setPaymentSecret(decoded.getPaymentSecret().toHex())
                    .setDestinationPubKey(decoded.getNodeId().toString())
                    .setAmountRequested(amount)
                    .setTimestamp(decoded.getTimestampSeconds())
                    .setDescription(decoded.getDescription())
                    .setDescriptionHash(descriptionHash)
                    .setExpiry(expiry)
                    .build();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public static DecodedBolt12 decodeBolt12(String bolt12) throws Exception {
        try {
            OfferTypes.Offer decoded = OfferTypes.Offer.Companion.decode(bolt12).get();
            long amount = decoded.getAmount() == null ? 0 : decoded.getAmount().getMsat();
            long expiry = decoded.getExpirySeconds() == null ? 0 : decoded.getExpirySeconds();
            return DecodedBolt12.newBuilder()
                    .setBolt12String(bolt12)
                    .setOfferId(decoded.getOfferId().toHex())
                    .setAmount(amount)
                    .setDescription(decoded.getDescription())
                    .setIssuer(decoded.getIssuer())
                    .setExpiresAt(expiry)
                    .build();
        } catch (Exception e) {
            throw new Exception("Bolt12 decoding failed: " + e.getMessage());
        }
    }

    /**
     * Reading the description field from a bolt11 invoice string.
     */
    public static String getBolt11Description(@NonNull String bolt11) {
        try {
            return InvoiceUtil.decodeBolt11(bolt11).getDescription();
        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Error while trying to read description of the following invoice:  " + bolt11);
            BBLog.w(LOG_TAG, "Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reading the description field from a bolt12 invoice string. (lni1...)
     */
    public static String getBolt12InvoiceDescription(@NonNull String bolt12invoice) {
        try {
            return Bolt12Invoice.Companion.fromString(bolt12invoice).get().getDescription();
        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Error while trying to read description of the following bolt12 invoice:  " + bolt12invoice);
            BBLog.w(LOG_TAG, "Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reading the payer note from a bolt12 invoice string. (lni1...)
     */
    public static String getBolt12InvoicePayerNote(@NonNull String bolt12invoice) {
        try {
            return Bolt12Invoice.Companion.fromString(bolt12invoice).get().getInvoiceRequest().getPayerNote();
        } catch (Exception e) {
            BBLog.w(LOG_TAG, "Error while trying to read payer note of the following bolt12 invoice:  " + bolt12invoice);
            BBLog.w(LOG_TAG, "Error: " + e.getMessage());
            return null;
        }
    }

    public interface OnReadInvoiceCompletedListener {
        void onValidLightningInvoice(DecodedBolt11 decodedBolt11, Bip21Invoice fallbackOnChainInvoice);

        void onValidBolt12Offer(DecodedBolt12 decodedBolt12, Bip21Invoice fallbackOnChainInvoice);

        void onValidBitcoinInvoice(Bip21Invoice onChainInvoice);

        void onError(String error, int duration, int errorCode);

        void onNoInvoiceData();
    }
}
