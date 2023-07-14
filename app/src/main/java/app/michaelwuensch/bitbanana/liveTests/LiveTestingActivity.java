package app.michaelwuensch.bitbanana.liveTests;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;


public class LiveTestingActivity extends BaseAppCompatActivity {
    private static final String LOG_TAG = LiveTestingActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_tests);

        Button btnStringAnalyzer = findViewById(R.id.btnStringAnalyzer);
        btnStringAnalyzer.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                StringAnalyzerTest();
            }
        });
    }

    private void StringAnalyzerTest() {
        BitcoinStringAnalyzerTest.TestResult resultListener = new BitcoinStringAnalyzerTest.TestResult() {
            @Override
            public void onSuccess(String input, int result, String errorMessage) {
                if (result == BitcoinStringAnalyzerTest.RESULT_UNKNOWN) {
                    BBLog.i(LOG_TAG, "Success. Unknown Data: " + input);
                    return;
                }
                if (errorMessage == null)
                    BBLog.i(LOG_TAG, "Success reading: " + input);
                else
                    BBLog.w(LOG_TAG, "Success, error was expected: " + input + ", Error: " + errorMessage);
            }

            @Override
            public void onFailed(String input, int expected, int result, String errorMessage) {
                if (errorMessage == null)
                    BBLog.e(LOG_TAG, "Expected: " + expected + ", Result: " + result + ", Input: " + input);
                else
                    BBLog.e(LOG_TAG, "Expected: " + expected + ", Result: " + result + ", Input: " + input + ", Error Message: " + errorMessage);
            }
        };
        BitcoinStringAnalyzerTest test = new BitcoinStringAnalyzerTest(resultListener);

        // General
        test.execute(null, BitcoinStringAnalyzerTest.RESULT_UNKNOWN);
        test.execute("", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);
        test.execute("randominput_poaijlk:Aiö23!§$%&", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);

        // Lightning Invoice
        // the next 3 tests need to output the error message "Expired" to be successful. On LND invoices can be valid for a maximum of 1 year. So we go with this instead of having to exchange the invoice here every year.
        test.execute("lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w", BitcoinStringAnalyzerTest.RESULT_ERROR);
        test.execute("lightning:lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w", BitcoinStringAnalyzerTest.RESULT_ERROR);
        test.execute("LIGHTNING:lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w", BitcoinStringAnalyzerTest.RESULT_ERROR);
        // lightning:// is not supported
        test.execute("lightning://lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);
        test.execute("LIGHTNING://lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);

        // Bitcoin Invoice
        test.execute("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE); //bech32, mainnet, lower case
        test.execute("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4".toUpperCase(), BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE); //bech32, mainnet, upper case
        test.execute("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE); //bech32m, mainnet (example from BIP 350)
        test.execute("tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx", BitcoinStringAnalyzerTest.RESULT_ERROR); //bech32, testnet, lower case
        test.execute("bcrt1q6rhpng9evdsfnn833a4f4vej0asu6dk5srld6x", BitcoinStringAnalyzerTest.RESULT_ERROR); //bech32, regtest, lower case
        test.execute("17VZNX1SN5NtKa8UQFxwQbFeFc3iqRYhem", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE); //base58, mainnet, P2PKH
        test.execute("mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn", BitcoinStringAnalyzerTest.RESULT_ERROR); //base58, testnet, P2PKH
        test.execute("3EktnHQD7RiAE6uzMj2ZifT9YgRrkSgzQX", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE); //base58, mainnet, P2SH
        test.execute("2MzQwSSnBHWHqSAqtTVQ6v47XtaisrJa1Vc", BitcoinStringAnalyzerTest.RESULT_ERROR); //base58, testnet, P2SH
        test.execute("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE);
        test.execute("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4?amount=0.00005&message=testing%20stuff", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE);
        test.execute("bitcoin://bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE);
        test.execute("BITCOIN://bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE);
        test.execute("BITCOIN://bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE);

        // LNURL (https://lnurl.fiatjaf.com/ was used to generate the lnurls)
        test.execute("lightning:LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("LIGHTNING:LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("https://service.com/giftcard/redeem?id=123&lightning=LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("https://service.com/giftcard/redeem?id=123&LIGHTNING=LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHW6T5DPJ8YCTH8AEK2UMND9HKU0FHVV6NXEFNX4NRYET9VSUNQERPVFSN2CNP8YURVD35XSCNQVNZVCCXVVE5X43KGDT9X33KXDPN8YENWCENV3SKVWPKXYUXVD3CXCURW2S4MTN", BitcoinStringAnalyzerTest.RESULT_LNURL_WITHDRAW);
        test.execute("LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKKX6RPDEHX2MPLWDJHXUMFDAHR6DMRX5EK2VE4VCEX2ETY8YCXGCTZVY6KYCFE8QMRVDP5XYCRYCNXXPNRXDP4VDJR2EF5VD3NGVEEXVMKXVMYV9NRSD338PNRVWPK8QMSJTMXDN", BitcoinStringAnalyzerTest.RESULT_LNURL_CHANNEL);
        test.execute("LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKKCMM8D9HR7ARPVU7KCMM8D9HZV6E385MKXDFNV5EN2E3JV4JKGWFSV3SKYCF4VFSNJWPKXC6RGVFSXF3XVVRXXV6R2CMYX4JNGCMRXSENJVEHVVEKGCTX8QMRZWRXXCURVWPHWN2ETM", BitcoinStringAnalyzerTest.RESULT_LNURL_AUTH);
        test.execute("lnurlp://lnurl.fiatjaf.com/lnurl-pay?session=82d6122cbe3c1a9806aab55bd57d10c74e68eb405fa0c022791dba67fcd8ee7b", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("LNURLP://lnurl.fiatjaf.com/lnurl-pay?session=82d6122cbe3c1a9806aab55bd57d10c74e68eb405fa0c022791dba67fcd8ee7b", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("lnurlc://lnurl.fiatjaf.com/lnurl-channel?session=7c53e35f2eed90daba5ba986644102bf0f345cd5e4cc43937c3daf8618f68687", BitcoinStringAnalyzerTest.RESULT_LNURL_CHANNEL);
        test.execute("lnurlw://lnurl.fiatjaf.com/lnurl-withdraw?session=7c53e35f2eed90daba5ba986644102bf0f345cd5e4cc43937c3daf8618f68687", BitcoinStringAnalyzerTest.RESULT_LNURL_WITHDRAW);
        // lud-17 schemes without // are not supported
        test.execute("lnurlp:lnurl.fiatjaf.com/lnurl-pay?session=82d6122cbe3c1a9806aab55bd57d10c74e68eb405fa0c022791dba67fcd8ee7b", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);
        test.execute("LNURLP:lnurl.fiatjaf.com/lnurl-pay?session=82d6122cbe3c1a9806aab55bd57d10c74e68eb405fa0c022791dba67fcd8ee7b", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);

        // LN Address:
        test.execute("michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_LNADDRESS);
        test.execute("lightning:michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_LNADDRESS);
        test.execute("lnurlp://michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_LNADDRESS);
        test.execute("Michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_UNKNOWN); // mixed case name
        test.execute("MICHAEL90@getalby.com", BitcoinStringAnalyzerTest.RESULT_UNKNOWN); // capitalized name
        test.execute("michael-90@getalby.com", BitcoinStringAnalyzerTest.RESULT_UNKNOWN); // unsupported character in name
        test.execute("michael90@non-existing-domain-1i1kja.com", BitcoinStringAnalyzerTest.RESULT_ERROR);
        test.execute("nonexistinguser123jask@getalby.com", BitcoinStringAnalyzerTest.RESULT_ERROR);

        // ToDo: LNAddress as fallback?
        // test.execute("https://service.com/giftcard/redeem?id=123&lightning=michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_LNADDRESS);

        // LND connect
        test.execute("lndconnect://127.0.0.1:10006?cert=MIICKDCCAc2gAwIBAgIRAL1ZXmdacBLVT0biXWmkzqQwCgYIKoZIzj0EAwIwMTEfMB0GA1UEChMWbG5kIGF1dG9nZW5lcmF0ZWQgY2VydDEOMAwGA1UEAxMFZnJhbmswHhcNMjMwMzE3MTkzNjAyWhcNMjQwNTExMTkzNjAyWjAxMR8wHQYDVQQKExZsbmQgYXV0b2dlbmVyYXRlZCBjZXJ0MQ4wDAYDVQQDEwVmcmFuazBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABBWUo4xSh41140xFWL-0a8jj4i8mXVO9jNuTLnu_O-MMf2at9CB_EQ_ILtCNydqvFKFNPAgdLuqE6KNXBc21QQ-jgcUwgcIwDgYDVR0PAQH_BAQDAgKkMBMGA1UdJQQMMAoGCCsGAQUFBwMBMA8GA1UdEwEB_wQFMAMBAf8wHQYDVR0OBBYEFIDa6oM-iTWdwrrHgDWcANwOc8O-MGsGA1UdEQRkMGKCBWZyYW5rgglsb2NhbGhvc3SCBWZyYW5rgg5wb2xhci1uMy1mcmFua4IEdW5peIIKdW5peHBhY2tldIIHYnVmY29ubocEfwAAAYcQAAAAAAAAAAAAAAAAAAAAAYcErBIACDAKBggqhkjOPQQDAgNJADBGAiEAhDWgOU2apqMeMwnpoVpLHxJzl75f7qaGRF8-HyHDIK8CIQD6-vtKiMuhdWQssEx3_dbPOjM8OO2J8ah6rGU8jVQlfA&macaroon=AgEDbG5kAvgBAwoQs4KpkfOz3WDHzaWUQxKP-RIBMBoWCgdhZGRyZXNzEgRyZWFkEgV3cml0ZRoTCgRpbmZvEgRyZWFkEgV3cml0ZRoXCghpbnZvaWNlcxIEcmVhZBIFd3JpdGUaIQoIbWFjYXJvb24SCGdlbmVyYXRlEgRyZWFkEgV3cml0ZRoWCgdtZXNzYWdlEgRyZWFkEgV3cml0ZRoXCghvZmZjaGFpbhIEcmVhZBIFd3JpdGUaFgoHb25jaGFpbhIEcmVhZBIFd3JpdGUaFAoFcGVlcnMSBHJlYWQSBXdyaXRlGhgKBnNpZ25lchIIZ2VuZXJhdGUSBHJlYWQAAAYgqjpXcQ3SQnKG3GSBPwnDlchWGwoxq5ru8858M78ZNqs", BitcoinStringAnalyzerTest.RESULT_LND_CONNECT);
        test.execute("lndconnect://127.0.0.1:10006?macaroon=AgEDbG5kAvgBAwoQs4KpkfOz3WDHzaWUQxKP-RIBMBoWCgdhZGRyZXNzEgRyZWFkEgV3cml0ZRoTCgRpbmZvEgRyZWFkEgV3cml0ZRoXCghpbnZvaWNlcxIEcmVhZBIFd3JpdGUaIQoIbWFjYXJvb24SCGdlbmVyYXRlEgRyZWFkEgV3cml0ZRoWCgdtZXNzYWdlEgRyZWFkEgV3cml0ZRoXCghvZmZjaGFpbhIEcmVhZBIFd3JpdGUaFgoHb25jaGFpbhIEcmVhZBIFd3JpdGUaFAoFcGVlcnMSBHJlYWQSBXdyaXRlGhgKBnNpZ25lchIIZ2VuZXJhdGUSBHJlYWQAAAYgqjpXcQ3SQnKG3GSBPwnDlchWGwoxq5ru8858M78ZNqs", BitcoinStringAnalyzerTest.RESULT_LND_CONNECT);

        // ToDo: BTC Pay connect

        // NodeUris
        test.execute("030c3f19d742ca294a55c00376b3b355c3c90d61c6b6b39554dbc7ac19b141c14f", BitcoinStringAnalyzerTest.RESULT_NODE_URI);
        test.execute("030c3f19d742ca294a55c00376b3b355c3c90d61c6b6b39554dbc7ac19b141c14f".toUpperCase(), BitcoinStringAnalyzerTest.RESULT_NODE_URI);
        test.execute("030c3f19d742ca294a55c00376b3b355c3c90d61c6b6b39554dbc7ac19b141c14f@52.50.244.44", BitcoinStringAnalyzerTest.RESULT_NODE_URI);
        test.execute("030c3f19d742ca294a55c00376b3b355c3c90d61c6b6b39554dbc7ac19b141c14f@52.50.244.44:9735", BitcoinStringAnalyzerTest.RESULT_NODE_URI);
        test.execute("02d5e7a13e928e5eb35b78c715ce5839191d73ad735fe9239884b48ade61f80e44@2t3dhv3d5nhnkgypzxk2kxbtn3dd2ubpxapqrq5thaknny4zgfxfifyd.onion", BitcoinStringAnalyzerTest.RESULT_NODE_URI);
        test.execute("02D5E7A13E928E5EB35B78C715CE5839191D73AD735FE9239884B48ADE61F80E44@2t3dhv3d5nhnkgypzxk2kxbtn3dd2ubpxapqrq5thaknny4zgfxfifyd.onion", BitcoinStringAnalyzerTest.RESULT_NODE_URI);
        test.execute("02d5e7a13e928e5eb35b78c715ce5839191d73ad735fe9239884b48ade61f80e44@2t3dhv3d5nhnkgypzxk2kxbtn3dd2ubpxapqrq5thaknny4zgfxfifyd.onion:9735", BitcoinStringAnalyzerTest.RESULT_NODE_URI);

        // URLs
        test.execute("https://service.com", BitcoinStringAnalyzerTest.RESULT_URL);
        test.execute("HTTPS://service.com", BitcoinStringAnalyzerTest.RESULT_URL);
        test.execute("https://www.service.com", BitcoinStringAnalyzerTest.RESULT_URL);
        test.execute("http://service.com", BitcoinStringAnalyzerTest.RESULT_URL);
        test.execute("HTTP://service.com", BitcoinStringAnalyzerTest.RESULT_URL);
        test.execute("http://www.service.com", BitcoinStringAnalyzerTest.RESULT_URL);
        test.execute("http://service.onion", BitcoinStringAnalyzerTest.RESULT_URL);
        test.execute("http://www.service.onion", BitcoinStringAnalyzerTest.RESULT_URL);
        test.execute("www.service.com", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);
        test.execute("service.com", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);
    }
}
