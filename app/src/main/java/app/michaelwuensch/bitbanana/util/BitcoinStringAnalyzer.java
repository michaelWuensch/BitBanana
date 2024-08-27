package app.michaelwuensch.bitbanana.util;

import android.content.Context;

import androidx.annotation.NonNull;

import java.net.MalformedURLException;
import java.net.URL;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.lnurl.LnUrlReader;
import app.michaelwuensch.bitbanana.lnurl.LnurlDecoder;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnAddress;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class BitcoinStringAnalyzer {

    public static boolean isLnUrl(String inputString) {
        if (UriUtil.isLNURLUri(inputString)) {
            return true;
        }
        try {
            URL url = new URL(inputString);
            String query = url.getQuery();
            if (query != null && query.toLowerCase().contains("lightning=lnurl1")) {
                return true;
            }
        } catch (Exception ignored) {
        }
        try {
            LnurlDecoder.decode(inputString);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String prepareData(String input) {
        String result = input.trim();
        // Before handling normal lnurls, check if there is a lnurl of form lnurlp://lightningAddress
        if (UriUtil.isLNURLPUri(result)) {
            LnAddress lnAddress = new LnAddress(UriUtil.removeURI(result));
            if (lnAddress.isValidLnurlAddress()) {
                return UriUtil.removeURI(result);
            }
        }
        return result;
    }

    public static void analyze(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        if (inputString == null) {
            listener.onNoReadableData();
            return;
        }
        String data = prepareData(inputString);
        checkIfLnUrl(ctx, compositeDisposable, data, listener);
    }

    private static void checkIfLnUrl(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        LnUrlReader.readLnUrl(ctx, inputString, new LnUrlReader.OnLnUrlReadListener() {
            @Override
            public void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse) {
                listener.onValidLnUrlWithdraw(withdrawResponse);
            }

            @Override
            public void onValidLnUrlPay(LnUrlPayResponse payResponse) {
                listener.onValidLnUrlPay(payResponse);
            }

            @Override
            public void onValidLnUrlChannel(LnUrlChannelResponse channelResponse) {
                if (FeatureManager.isOpenChannelEnabled())
                    listener.onValidLnUrlChannel(channelResponse);
                else
                    listener.onError(ctx.getString(R.string.error_feature_not_supported_by_backend, BackendManager.getCurrentBackend().getNodeImplementationName(), "OPEN_CHANNEL"), RefConstants.ERROR_DURATION_MEDIUM);
            }

            @Override
            public void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse) {
                if (FeatureManager.isOpenChannelEnabled())
                    listener.onValidLnUrlHostedChannel(hostedChannelResponse);
                else
                    listener.onError(ctx.getString(R.string.error_feature_not_supported_by_backend, BackendManager.getCurrentBackend().getNodeImplementationName(), "OPEN_CHANNEL"), RefConstants.ERROR_DURATION_MEDIUM);
            }

            @Override
            public void onValidLnUrlAuth(URL url) {
                if (FeatureManager.isLnurlAuthEnabled())
                    listener.onValidLnUrlAuth(url);
                else
                    listener.onError(ctx.getString(R.string.error_feature_not_supported_by_backend, BackendManager.getCurrentBackend().getNodeImplementationName(), "LNURL_AUTH"), RefConstants.ERROR_DURATION_MEDIUM);
            }

            @Override
            public void onError(String error, int duration) {
                listener.onError(error, duration);
            }

            @Override
            public void onNoLnUrlData() {
                checkIfRemoteConnection(ctx, compositeDisposable, inputString, listener);
            }
        });
    }

    private static void checkIfRemoteConnection(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        RemoteConnectUtil.decodeConnectionString(ctx, inputString, new RemoteConnectUtil.OnRemoteConnectDecodedListener() {
            @Override
            public void onValidConnectData(BackendConfig backendConfig) {
                listener.onValidConnectData(backendConfig);
            }

            @Override
            public void onError(String error, int duration) {
                listener.onError(error, duration);
            }

            @Override
            public void onNoConnectData() {
                checkIfNodeUri(ctx, compositeDisposable, inputString, listener);
            }
        });
    }

    private static void checkIfNodeUri(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        LightningNodeUri nodeUri = LightningNodeUriParser.parseNodeUri(inputString);

        if (nodeUri != null)
            listener.onValidNodeUri(nodeUri);
        else {
            if (BackendManager.hasBackendConfigs())
                checkIfLnOrBitcoinInvoice(ctx, compositeDisposable, inputString, listener);
            else
                listener.onError(ctx.getString(R.string.demo_setupNodeFirst), RefConstants.ERROR_DURATION_SHORT);
        }
    }

    private static void checkIfLnOrBitcoinInvoice(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        InvoiceUtil.readInvoice(ctx, inputString, null, new InvoiceUtil.OnReadInvoiceCompletedListener() {
            @Override
            public void onValidLightningInvoice(DecodedBolt11 decodedBolt11, Bip21Invoice fallbackOnChainInvoice) {
                listener.onValidLightningInvoice(decodedBolt11, fallbackOnChainInvoice);
            }

            @Override
            public void onValidBolt12Offer(DecodedBolt12 decodedBolt12, Bip21Invoice fallbackOnChainInvoice) {
                listener.onValidBolt12Offer(decodedBolt12, fallbackOnChainInvoice);
            }

            @Override
            public void onValidBitcoinInvoice(Bip21Invoice onChainInvoice) {
                listener.onValidBitcoinInvoice(onChainInvoice);
            }

            @Override
            public void onError(String error, int duration, int errorCode) {
                listener.onError(error, duration);
            }

            @Override
            public void onNoInvoiceData() {
                checkIfStaticInternetIdentifier(ctx, compositeDisposable, inputString, listener);
            }
        });
    }

    private static void checkIfStaticInternetIdentifier(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        StaticInternetIdentifierReader.checkIfValidStaticInternetIdentifier(ctx, inputString, new StaticInternetIdentifierReader.OnStaticIdentifierChecked() {

            @Override
            public void onValidLnurlPay(LnUrlPayResponse lnUrlPayResponse) {
                listener.onValidLnUrlPay(lnUrlPayResponse);
            }

            @Override
            public void onValidBip353DnsRecord(String bip21InvoiceString) {
                // We have retrieved the actual invoice information that was stored in a dns record for a human readable address like "user@domain.com"
                // Therefore we now do a second round and test that result for validity.
                checkIfLnOrBitcoinInvoice(ctx, compositeDisposable, bip21InvoiceString, listener);
            }

            @Override
            public void onError(String error, int duration) {
                listener.onError(error, duration);
            }

            @Override
            public void onNoStaticInternetIdentifierData() {
                checkIfValidUrl(ctx, compositeDisposable, inputString, listener);
            }
        });
    }

    private static void checkIfValidUrl(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        try {
            URL url = new URL(inputString);
            listener.onValidURL(inputString);
        } catch (MalformedURLException e) {
            // No URL either, we have unrecognizable data
            listener.onNoReadableData();
        }
    }


    public interface OnDataDecodedListener {
        void onValidLightningInvoice(DecodedBolt11 decodedBolt11, Bip21Invoice fallbackOnChainInvoice);

        void onValidBitcoinInvoice(Bip21Invoice onChainInvoice);

        void onValidBolt12Offer(DecodedBolt12 decodedBolt12, Bip21Invoice fallbackOnChainInvoice);

        void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse);

        void onValidLnUrlChannel(LnUrlChannelResponse channelResponse);

        void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse);

        void onValidLnUrlPay(LnUrlPayResponse payResponse);

        void onValidLnUrlAuth(URL url);

        void onValidConnectData(BackendConfig backendConfig);

        void onValidNodeUri(LightningNodeUri nodeUri);

        void onValidURL(String url);

        void onError(String error, int duration);

        void onNoReadableData();
    }
}
