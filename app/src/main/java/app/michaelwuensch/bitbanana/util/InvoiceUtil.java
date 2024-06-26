package app.michaelwuensch.bitbanana.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.net.UrlEscapers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import fr.acinq.lightning.payment.Bolt11Invoice;

public class InvoiceUtil {
    private static final String LOG_TAG = InvoiceUtil.class.getSimpleName();

    public static String INVOICE_PREFIX_LIGHTNING_MAINNET = "lnbc";
    public static String INVOICE_PREFIX_LIGHTNING_TESTNET = "lntb";
    public static String INVOICE_PREFIX_LIGHTNING_REGTEST = "lnbcrt";
    public static String INVOICE_PREFIX_LIGHTNING_SIGNET = "lntbs";
    public static ArrayList<String> ADDRESS_PREFIX_ONCHAIN_MAINNET = new ArrayList<>(Arrays.asList("1", "3", "bc1"));
    public static ArrayList<String> ADDRESS_PREFIX_ONCHAIN_TESTNET = new ArrayList<>(Arrays.asList("m", "n", "2", "tb1"));
    public static ArrayList<String> ADDRESS_PREFIX_ONCHAIN_REGTEST = new ArrayList<>(Arrays.asList("m", "n", "2", "bcrt1"));
    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_NETWORK_MISMATCH = 1;
    public static final int ERROR_INVOICE_EXPIRED = 1;
    private static final int INVOICE_LIGHTNING_MIN_LENGTH = 6;


    public static boolean isLightningInvoice(@NonNull String data) {
        if (data.isEmpty() || data.length() < INVOICE_LIGHTNING_MIN_LENGTH) {
            return false;
        }

        return hasPrefix(INVOICE_PREFIX_LIGHTNING_MAINNET, data) || hasPrefix(INVOICE_PREFIX_LIGHTNING_TESTNET, data) || hasPrefix(INVOICE_PREFIX_LIGHTNING_REGTEST, data);
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

    public static void readInvoice(Context ctx, String data, OnReadInvoiceCompletedListener listener) {

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
        if (InvoiceUtil.isLightningInvoice(lnInvoice)) {

            // Check if the invoice is for the same network the app is connected to
            switch (Wallet.getInstance().getNetwork()) {
                case MAINNET:
                    if (hasPrefix(INVOICE_PREFIX_LIGHTNING_MAINNET, lnInvoice) && !hasPrefix(INVOICE_PREFIX_LIGHTNING_REGTEST, lnInvoice)) {
                        decodeLightningInvoice(ctx, listener, lnInvoice);
                    } else {
                        // Show error. Please use a MAINNET invoice.
                        listener.onError(ctx.getString(R.string.error_useMainnetRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                case TESTNET:
                    if (hasPrefix(INVOICE_PREFIX_LIGHTNING_TESTNET, lnInvoice)) {
                        decodeLightningInvoice(ctx, listener, lnInvoice);
                    } else {
                        // Show error. Please use a TESTNET invoice.
                        listener.onError(ctx.getString(R.string.error_useTestnetRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                case REGTEST:
                    if (hasPrefix(INVOICE_PREFIX_LIGHTNING_REGTEST, lnInvoice)) {
                        decodeLightningInvoice(ctx, listener, lnInvoice);
                    } else {
                        // Show error. Please use a REGTEST invoice.
                        listener.onError(ctx.getString(R.string.error_useRegtestRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                default:
                    listener.onError(ctx.getString(R.string.error_unsupported_network), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
            }
        } else {
            // We do not have a lightning invoice... check if it is a valid bitcoin address / invoice

            // Check if we have a bitcoin invoice with the "bitcoin:" uri scheme
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
                        }
                    }
                    validateOnChainAddress(ctx, listener, onChainAddress, onChainInvoiceAmount, onChainInvoiceMessage, lightningInvoice);

                } catch (URISyntaxException e) {
                    BBLog.w(LOG_TAG, "URI could not be parsed");
                    e.printStackTrace();
                    listener.onError(ctx.getString(R.string.error_invalid_bitcoin_request), RefConstants.ERROR_DURATION_MEDIUM, ERROR_UNKNOWN);
                }

            } else {
                // We also don't have a bitcoin invoice, check if the data is a valid bitcoin address
                validateOnChainAddress(ctx, listener, data, 0L, null, null);
            }

        }

    }

    private static void validateOnChainAddress(Context ctx, OnReadInvoiceCompletedListener listener, String address, long amount, String message, String lightningInvoice) {
        if (address != null && isBitcoinAddress(address)) {
            switch (Wallet.getInstance().getNetwork()) {
                case MAINNET:
                    if (hasPrefix(ADDRESS_PREFIX_ONCHAIN_MAINNET, address)) {
                        listener.onValidBitcoinInvoice(address, amount, message, lightningInvoice);
                    } else {
                        listener.onError(ctx.getString(R.string.error_useMainnetRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                case TESTNET:
                    if (hasPrefix((ArrayList<String>) ADDRESS_PREFIX_ONCHAIN_TESTNET, address)) {
                        listener.onValidBitcoinInvoice(address, amount, message, lightningInvoice);
                    } else {
                        listener.onError(ctx.getString(R.string.error_useTestnetRequest), RefConstants.ERROR_DURATION_MEDIUM, ERROR_NETWORK_MISMATCH);
                    }
                    break;
                case REGTEST:
                    if (hasPrefix((ArrayList<String>) ADDRESS_PREFIX_ONCHAIN_REGTEST, address)) {
                        listener.onValidBitcoinInvoice(address, amount, message, lightningInvoice);
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

    private static void decodeLightningInvoice(Context ctx, OnReadInvoiceCompletedListener listener, String invoice) {
        try {
            DecodedBolt11 decodedBolt11 = decodeBolt11(invoice);
            if (decodedBolt11.isExpired())
                listener.onError(ctx.getString(R.string.error_paymentRequestExpired), RefConstants.ERROR_DURATION_SHORT, ERROR_INVOICE_EXPIRED);
            else
                listener.onValidLightningInvoice(decodedBolt11);
        } catch (Exception e) {
            listener.onError(e.getMessage(), RefConstants.ERROR_DURATION_MEDIUM, ERROR_UNKNOWN);
        }
    }

    private static String appendParameter(String base, String name, String value) {
        if (!base.contains("?"))
            return base + "?" + name + "=" + value;
        else
            return base + "&" + name + "=" + value;
    }

    public static String generateBitcoinInvoice(@NonNull String address, @Nullable String amount, @Nullable String message, @Nullable String lightningInvoice) {
        String bitcoinInvoice = UriUtil.generateBitcoinUri(address);

        if (amount != null)
            if (!(amount.isEmpty() || amount.equals("0")))
                bitcoinInvoice = InvoiceUtil.appendParameter(bitcoinInvoice, "amount", amount);
        if (message != null)
            if (!message.isEmpty()) {
                String escapedMessage = UrlEscapers.urlPathSegmentEscaper().escape(message);
                bitcoinInvoice = appendParameter(bitcoinInvoice, "message", escapedMessage);
            }
        if (lightningInvoice != null)
            if (!lightningInvoice.isEmpty())
                bitcoinInvoice = appendParameter(bitcoinInvoice, "lightning", message);

        return bitcoinInvoice;
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

    public interface OnReadInvoiceCompletedListener {
        void onValidLightningInvoice(DecodedBolt11 decodedBolt11);

        void onValidBitcoinInvoice(String address, long amount, String message, String lightningInvoice);

        void onError(String error, int duration, int errorCode);

        void onNoInvoiceData();
    }
}
