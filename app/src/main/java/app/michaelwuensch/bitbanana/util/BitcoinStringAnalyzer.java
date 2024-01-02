package app.michaelwuensch.bitbanana.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.lightningnetwork.lnd.lnrpc.PayReq;

import java.net.MalformedURLException;
import java.net.URL;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.lnurl.LnUrlReader;
import app.michaelwuensch.bitbanana.lnurl.LnurlDecoder;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.staticInternetIdentifier.StaticInternetIdentifier;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.models.LNAddress;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
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
            LNAddress lnAddress = new LNAddress(UriUtil.removeURI(result));
            if (lnAddress.isValid()) {
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
                listener.onValidLnUrlChannel(channelResponse);
            }

            @Override
            public void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse) {
                listener.onValidLnUrlHostedChannel(hostedChannelResponse);
            }

            @Override
            public void onValidLnUrlAuth(URL url) {
                listener.onValidLnUrlAuth(url);
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
            public void onValidLndConnectString(BaseBackendConfig baseBackendConfig) {
                listener.onValidLndConnectString(baseBackendConfig);
            }

            @Override
            public void onValidBTCPayConnectData(BaseBackendConfig baseBackendConfig) {
                listener.onValidBTCPayConnectData(baseBackendConfig);
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
        LightningNodeUri nodeUri = LightningNodeUirParser.parseNodeUri(inputString);

        if (nodeUri != null) {
            listener.onValidNodeUri(nodeUri);

        } else {
            if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                checkIfLnOrBitcoinInvoice(ctx, compositeDisposable, inputString, listener);
            } else {
                listener.onError(ctx.getString(R.string.demo_setupNodeFirst), RefConstants.ERROR_DURATION_SHORT);
            }
        }
    }

    private static void checkIfLnOrBitcoinInvoice(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        InvoiceUtil.readInvoice(ctx, compositeDisposable, inputString, new InvoiceUtil.OnReadInvoiceCompletedListener() {
            @Override
            public void onValidLightningInvoice(PayReq paymentRequest, String invoice) {
                listener.onValidLightningInvoice(paymentRequest, invoice);
            }

            @Override
            public void onValidBitcoinInvoice(String address, long amount, String message, String lightningInvoice) {
                listener.onValidBitcoinInvoice(address, amount, message, lightningInvoice);
            }

            @Override
            public void onError(String error, int duration) {
                listener.onError(error, duration);
            }

            @Override
            public void onNoInvoiceData() {
                checkIfStaticInternetIdentifier(ctx, compositeDisposable, inputString, listener);
            }
        });
    }

    private static void checkIfStaticInternetIdentifier(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        StaticInternetIdentifier.checkIfValidStaticInternetIdentifier(ctx, inputString, new StaticInternetIdentifier.OnStaticIdentifierChecked() {

            @Override
            public void onValidInternetIdentifier(LnUrlPayResponse lnUrlPayResponse) {
                listener.onValidInternetIdentifier(lnUrlPayResponse);
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
        void onValidLightningInvoice(PayReq paymentRequest, String invoice);

        void onValidBitcoinInvoice(String address, long amount, String message, String lightningInvoice);

        void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse);

        void onValidLnUrlChannel(LnUrlChannelResponse channelResponse);

        void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse);

        void onValidLnUrlPay(LnUrlPayResponse payResponse);

        void onValidLnUrlAuth(URL url);

        void onValidInternetIdentifier(LnUrlPayResponse payResponse);

        void onValidLndConnectString(BaseBackendConfig baseBackendConfig);

        void onValidBTCPayConnectData(BaseBackendConfig baseBackendConfig);

        void onValidNodeUri(LightningNodeUri nodeUri);

        void onValidURL(String url);

        void onError(String error, int duration);

        void onNoReadableData();
    }
}
