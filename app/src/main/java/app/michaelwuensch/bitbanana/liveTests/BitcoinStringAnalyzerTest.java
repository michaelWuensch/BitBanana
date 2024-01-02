package app.michaelwuensch.bitbanana.liveTests;

import com.github.lightningnetwork.lnd.lnrpc.PayReq;

import java.net.URL;

import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.util.BitcoinStringAnalyzer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/*
As the string analyzer involves all kinds of things like http request, and grpc calls to the node,
it is easier to write a test like this that can be executed in the app in development builds.
 */
public class BitcoinStringAnalyzerTest {
    public static final int RESULT_ERROR = -1;
    public static final int RESULT_UNKNOWN = 0;
    public static final int RESULT_LIGHTNING_INVOICE = 1;
    public static final int RESULT_BITCOIN_INVOICE = 2;
    public static final int RESULT_LNURL_WITHDRAW = 3;
    public static final int RESULT_LNURL_CHANNEL = 4;
    public static final int RESULT_LNURL_HOSTED_CHANNEL = 5;
    public static final int RESULT_LNURL_PAY = 6;
    public static final int RESULT_LNURL_AUTH = 7;
    public static final int RESULT_LNADDRESS = 8;
    public static final int RESULT_LND_CONNECT = 9;
    public static final int RESULT_BTCPAY_CONNECT = 10;
    public static final int RESULT_NODE_URI = 11;
    public static final int RESULT_URL = 12;
    private static final String ERROR_LNURL_CHECK_FAILED = "The end result was a lnurl, but BitcoinStringAnalyzer.isLnUrl() did not recognize it.";

    CompositeDisposable mCompositeDisposable;
    TestResult mResultListener;

    public BitcoinStringAnalyzerTest(TestResult testResult) {
        mCompositeDisposable = new CompositeDisposable();
        mResultListener = testResult;
    }

    public void execute(String input, int expected) {
        BitcoinStringAnalyzer.analyze(App.getAppContext(), mCompositeDisposable, input, new BitcoinStringAnalyzer.OnDataDecodedListener() {
            @Override
            public void onValidLightningInvoice(PayReq paymentRequest, String invoice) {
                if (expected == RESULT_LIGHTNING_INVOICE)
                    mResultListener.onSuccess(input, RESULT_LIGHTNING_INVOICE, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_LIGHTNING_INVOICE, null);
            }

            @Override
            public void onValidBitcoinInvoice(String address, long amount, String message, String lightningInvoice) {
                if (expected == RESULT_BITCOIN_INVOICE)
                    mResultListener.onSuccess(input, RESULT_BITCOIN_INVOICE, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_BITCOIN_INVOICE, null);
            }

            @Override
            public void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse) {
                if (!BitcoinStringAnalyzer.isLnUrl(input))
                    mResultListener.onFailed(input, expected, RESULT_LNURL_WITHDRAW, ERROR_LNURL_CHECK_FAILED);
                else if (expected == RESULT_LNURL_WITHDRAW)
                    mResultListener.onSuccess(input, RESULT_LNURL_WITHDRAW, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_LNURL_WITHDRAW, null);
            }

            @Override
            public void onValidLnUrlChannel(LnUrlChannelResponse channelResponse) {
                if (!BitcoinStringAnalyzer.isLnUrl(input))
                    mResultListener.onFailed(input, expected, RESULT_LNURL_CHANNEL, ERROR_LNURL_CHECK_FAILED);
                else if (expected == RESULT_LNURL_CHANNEL)
                    mResultListener.onSuccess(input, RESULT_LNURL_CHANNEL, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_LNURL_CHANNEL, null);
            }

            @Override
            public void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse) {
                if (!BitcoinStringAnalyzer.isLnUrl(input))
                    mResultListener.onFailed(input, expected, RESULT_LNURL_HOSTED_CHANNEL, ERROR_LNURL_CHECK_FAILED);
                else if (expected == RESULT_LNURL_HOSTED_CHANNEL)
                    mResultListener.onSuccess(input, RESULT_LNURL_HOSTED_CHANNEL, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_LNURL_HOSTED_CHANNEL, null);
            }

            @Override
            public void onValidLnUrlPay(LnUrlPayResponse payResponse) {
                if (!BitcoinStringAnalyzer.isLnUrl(input))
                    mResultListener.onFailed(input, expected, RESULT_LNURL_PAY, ERROR_LNURL_CHECK_FAILED);
                else if (expected == RESULT_LNURL_PAY)
                    mResultListener.onSuccess(input, RESULT_LNURL_PAY, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_LNURL_PAY, null);
            }

            @Override
            public void onValidLnUrlAuth(URL url) {
                if (!BitcoinStringAnalyzer.isLnUrl(input))
                    mResultListener.onFailed(input, expected, RESULT_LNURL_AUTH, ERROR_LNURL_CHECK_FAILED);
                else if (expected == RESULT_LNURL_AUTH)
                    mResultListener.onSuccess(input, RESULT_LNURL_AUTH, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_LNURL_AUTH, null);
            }

            @Override
            public void onValidInternetIdentifier(LnUrlPayResponse payResponse) {
                if (expected == RESULT_LNADDRESS)
                    mResultListener.onSuccess(input, RESULT_LNADDRESS, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_LNADDRESS, null);
            }

            @Override
            public void onValidLndConnectString(BaseBackendConfig baseBackendConfig) {
                if (expected == RESULT_LND_CONNECT)
                    mResultListener.onSuccess(input, RESULT_LND_CONNECT, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_LND_CONNECT, null);
            }

            @Override
            public void onValidBTCPayConnectData(BaseBackendConfig baseBackendConfig) {
                if (expected == RESULT_BTCPAY_CONNECT)
                    mResultListener.onSuccess(input, RESULT_BTCPAY_CONNECT, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_BTCPAY_CONNECT, null);
            }

            @Override
            public void onValidNodeUri(LightningNodeUri nodeUri) {
                if (expected == RESULT_NODE_URI)
                    mResultListener.onSuccess(input, RESULT_NODE_URI, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_NODE_URI, null);
            }

            @Override
            public void onValidURL(String url) {
                if (expected == RESULT_URL)
                    mResultListener.onSuccess(input, RESULT_URL, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_URL, null);
            }

            @Override
            public void onError(String error, int duration) {
                if (expected == RESULT_ERROR)
                    mResultListener.onSuccess(input, RESULT_ERROR, error);
                else
                    mResultListener.onFailed(input, expected, RESULT_ERROR, error);
            }

            @Override
            public void onNoReadableData() {
                if (expected == RESULT_UNKNOWN)
                    mResultListener.onSuccess(input, RESULT_UNKNOWN, null);
                else
                    mResultListener.onFailed(input, expected, RESULT_UNKNOWN, null);
            }
        });
    }

    public interface TestResult {
        void onSuccess(String input, int result, String errorMessage);

        void onFailed(String input, int expected, int result, String errorMessage);
    }
}
