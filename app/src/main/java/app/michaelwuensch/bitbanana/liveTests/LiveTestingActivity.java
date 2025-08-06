package app.michaelwuensch.bitbanana.liveTests;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
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

    private int expectedResultByNetwork(int mainnet, int testnet, int regtest) {
        switch (BackendManager.getCurrentBackendConfig().getNetwork()) {
            case MAINNET:
                return mainnet;
            case TESTNET:
                return testnet;
            case REGTEST:
                return regtest;
            default:
                return mainnet;
        }
    }

    private int expectedResultByBolt12SendingCapability(int enabled, int disabled) {
        if (BackendManager.getCurrentBackend().supportsBolt12Sending())
            return enabled;
        else
            return disabled;
    }

    private void PerformanceTest() {
        int iterations = 1000;

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {

        }
        long stop = System.currentTimeMillis();
        BBLog.i(LOG_TAG, "Method 1 ms: " + (stop - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {

        }
        stop = System.currentTimeMillis();
        BBLog.i(LOG_TAG, "Method 2 ms: " + (stop - start));
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

        // Bolt12 Lightning offer
        test.execute("lno1qgsqvgnwgcg35z6ee2h3yczraddm72xrfua9uve2rlrm9deu7xyfzrc2pexhjgzyv4ekxunfwp6xjmmwzcss9g069rpwu3yvah7ryysy5yxx9amsfq7707cgd8pesxudttdawsnj", expectedResultByBolt12SendingCapability(BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER, BitcoinStringAnalyzerTest.RESULT_ERROR));
        test.execute("lightning:lno1qgsqvgnwgcg35z6ee2h3yczraddm72xrfua9uve2rlrm9deu7xyfzrc2pexhjgzyv4ekxunfwp6xjmmwzcss9g069rpwu3yvah7ryysy5yxx9amsfq7707cgd8pesxudttdawsnj", expectedResultByBolt12SendingCapability(BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER, BitcoinStringAnalyzerTest.RESULT_ERROR));
        test.execute("LIGHTNING:lno1qgsqvgnwgcg35z6ee2h3yczraddm72xrfua9uve2rlrm9deu7xyfzrc2pexhjgzyv4ekxunfwp6xjmmwzcss9g069rpwu3yvah7ryysy5yxx9amsfq7707cgd8pesxudttdawsnj", expectedResultByBolt12SendingCapability(BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER, BitcoinStringAnalyzerTest.RESULT_ERROR));

        // Bitcoin Addresses
        test.execute("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR)); //bech32, mainnet, lower case
        test.execute("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4".toUpperCase(), expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR)); //bech32, mainnet, upper case
        test.execute("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR)); //bech32m, mainnet (example from BIP 350)
        test.execute("tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR)); //bech32, testnet, lower case
        test.execute("bcrt1q6rhpng9evdsfnn833a4f4vej0asu6dk5srld6x", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE)); //bech32, regtest, lower case
        test.execute("17VZNX1SN5NtKa8UQFxwQbFeFc3iqRYhem", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR)); //base58, mainnet, P2PKH
        test.execute("mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE)); //base58, testnet, P2PKH
        test.execute("3EktnHQD7RiAE6uzMj2ZifT9YgRrkSgzQX", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR)); //base58, mainnet, P2SH
        test.execute("2MzQwSSnBHWHqSAqtTVQ6v47XtaisrJa1Vc", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE)); //base58, testnet, P2SH

        // Bip 21 invoices
        test.execute("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR));
        test.execute("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4?amount=0.00005&message=testing%20stuff", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR));
        test.execute("bitcoin://bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR));
        test.execute("BITCOIN://bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR));
        test.execute("BITCOIN://bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR));
        // Bip 21 invoices with additional payment data (lightning invoice, bolt12 offer, ...)
        test.execute("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4?lightning=lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w", expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR)); // when on mainnet, lightning invoice is ignored as it is expired
        test.execute("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4?lno=lno1pgg8getnw3q8gam9d3mx2tnrv9eks93pqw7dv89vg89dtreg63cwn4mtg7js438yk3alw3a43zshdgsm0p08q", expectedResultByBolt12SendingCapability(expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER_WITH_ON_CHAIN_FALLBACK, BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER, BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER), expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR)));
        // Bip 21 formatted invoices without actual btc address (used in bip 353 dns resolving)
        test.execute("bitcoin:?lightning=lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w", BitcoinStringAnalyzerTest.RESULT_ERROR); // must fail because it is expired
        test.execute("bitcoin:?lno=lno1pgg8getnw3q8gam9d3mx2tnrv9eks93pqw7dv89vg89dtreg63cwn4mtg7js438yk3alw3a43zshdgsm0p08q", expectedResultByBolt12SendingCapability(BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER, BitcoinStringAnalyzerTest.RESULT_ERROR));

        // DNS resolving Bip21 invoices (Bip 353)
        test.execute("stephen@twelve.cash", expectedResultByBolt12SendingCapability(BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER, BitcoinStringAnalyzerTest.RESULT_ERROR));
        test.execute("₿stephen@twelve.cash", expectedResultByBolt12SendingCapability(BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER, BitcoinStringAnalyzerTest.RESULT_ERROR));
        test.execute("test@twelve.cash", expectedResultByBolt12SendingCapability(BitcoinStringAnalyzerTest.RESULT_BOLT12_OFFER, expectedResultByNetwork(BitcoinStringAnalyzerTest.RESULT_BITCOIN_INVOICE, BitcoinStringAnalyzerTest.RESULT_ERROR, BitcoinStringAnalyzerTest.RESULT_ERROR)));


        // LNURL (https://lnurl.fiatjaf.com/ was used to generate the lnurls)
        test.execute("lightning:LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("LIGHTNING:LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("https://service.com/giftcard/redeem?id=123&lightning=LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("https://service.com/giftcard/redeem?id=123&LIGHTNING=LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        // ToDo: LNAddress as fallback? test.execute("https://service.com/giftcard/redeem?id=123&lightning=michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_LNADDRESS);
        test.execute("LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHQCTE8AEK2UMND9HKU0FCXFJRVVFJXF3KYEFNVVCKZWFCXQMXZCTZX56KYEP4XAJRZVRRXU6X2D3CV43RGVP4VESNQCESXGERWWF3V33XZD3HVE3KGWR9V5MKYU2GLGT", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKHW6T5DPJ8YCTH8AEK2UMND9HKU0FHVV6NXEFNX4NRYET9VSUNQERPVFSN2CNP8YURVD35XSCNQVNZVCCXVVE5X43KGDT9X33KXDPN8YENWCENV3SKVWPKXYUXVD3CXCURW2S4MTN", BitcoinStringAnalyzerTest.RESULT_LNURL_WITHDRAW);
        test.execute("LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKKX6RPDEHX2MPLWDJHXUMFDAHR6DMRX5EK2VE4VCEX2ETY8YCXGCTZVY6KYCFE8QMRVDP5XYCRYCNXXPNRXDP4VDJR2EF5VD3NGVEEXVMKXVMYV9NRSD338PNRVWPK8QMSJTMXDN", BitcoinStringAnalyzerTest.RESULT_LNURL_CHANNEL);
        test.execute("LNURL1DP68GURN8GHJ7MRWW4EXCTNXD9SHG6NPVCHXXMMD9AKXUATJDSKKCMM8D9HR7ARPVU7KCMM8D9HZV6E385MKXDFNV5EN2E3JV4JKGWFSV3SKYCF4VFSNJWPKXC6RGVFSXF3XVVRXXV6R2CMYX4JNGCMRXSENJVEHVVEKGCTX8QMRZWRXXCURVWPHWN2ETM", BitcoinStringAnalyzerTest.RESULT_LNURL_AUTH);
        // lud-17 schemes
        test.execute("lnurlp://lnurl.fiatjaf.com/lnurl-pay?session=82d6122cbe3c1a9806aab55bd57d10c74e68eb405fa0c022791dba67fcd8ee7b", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("LNURLP://lnurl.fiatjaf.com/lnurl-pay?session=82d6122cbe3c1a9806aab55bd57d10c74e68eb405fa0c022791dba67fcd8ee7b", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("lnurlc://lnurl.fiatjaf.com/lnurl-channel?session=7c53e35f2eed90daba5ba986644102bf0f345cd5e4cc43937c3daf8618f68687", BitcoinStringAnalyzerTest.RESULT_LNURL_CHANNEL);
        test.execute("lnurlw://lnurl.fiatjaf.com/lnurl-withdraw?session=7c53e35f2eed90daba5ba986644102bf0f345cd5e4cc43937c3daf8618f68687", BitcoinStringAnalyzerTest.RESULT_LNURL_WITHDRAW);
        test.execute("KEYAUTH://lightninglogin.live/login?k1=98434f6c23b9423de261e4bed4f0217ba9c1dae80eb0a67e26b1466cb5cb1cb2&tag=login", BitcoinStringAnalyzerTest.RESULT_LNURL_AUTH);
        test.execute("keyauth://lightninglogin.live/login?k1=98434f6c23b9423de261e4bed4f0217ba9c1dae80eb0a67e26b1466cb5cb1cb2&tag=login", BitcoinStringAnalyzerTest.RESULT_LNURL_AUTH);
        test.execute("keyauth://lightninglogin.live/login?k1=98434f6c23b9423de261e4bed4f0217ba9c1dae80eb0a67e26b1466cb5cb1cb2", BitcoinStringAnalyzerTest.RESULT_ERROR);
        test.execute("keyauth://lightninglogin.live/login?k1=94de261e4bed4f0267e26b1466cb5cb1cb2&tag=login", BitcoinStringAnalyzerTest.RESULT_ERROR);
        // lud-17 schemes without // are not supported
        test.execute("lnurlp:lnurl.fiatjaf.com/lnurl-pay?session=82d6122cbe3c1a9806aab55bd57d10c74e68eb405fa0c022791dba67fcd8ee7b", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);
        test.execute("LNURLP:lnurl.fiatjaf.com/lnurl-pay?session=82d6122cbe3c1a9806aab55bd57d10c74e68eb405fa0c022791dba67fcd8ee7b", BitcoinStringAnalyzerTest.RESULT_UNKNOWN);

        // LN Address:
        test.execute("michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("lightning:michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("lnurlp://michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_LNURL_PAY);
        test.execute("Michael90@getalby.com", BitcoinStringAnalyzerTest.RESULT_UNKNOWN); // mixed case name
        test.execute("MICHAEL90@getalby.com", BitcoinStringAnalyzerTest.RESULT_UNKNOWN); // capitalized name
        test.execute("michael-90@getalby.com", BitcoinStringAnalyzerTest.RESULT_UNKNOWN); // unsupported character in name
        test.execute("michael90@non-existing-domain-1i1kja.com", BitcoinStringAnalyzerTest.RESULT_ERROR);
        test.execute("nonexistinguser123jask@getalby.com", BitcoinStringAnalyzerTest.RESULT_ERROR);

        // LND connect
        test.execute("lndconnect://127.0.0.1:10006?cert=MIICKDCCAc2gAwIBAgIRAL1ZXmdacBLVT0biXWmkzqQwCgYIKoZIzj0EAwIwMTEfMB0GA1UEChMWbG5kIGF1dG9nZW5lcmF0ZWQgY2VydDEOMAwGA1UEAxMFZnJhbmswHhcNMjMwMzE3MTkzNjAyWhcNMjQwNTExMTkzNjAyWjAxMR8wHQYDVQQKExZsbmQgYXV0b2dlbmVyYXRlZCBjZXJ0MQ4wDAYDVQQDEwVmcmFuazBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABBWUo4xSh41140xFWL-0a8jj4i8mXVO9jNuTLnu_O-MMf2at9CB_EQ_ILtCNydqvFKFNPAgdLuqE6KNXBc21QQ-jgcUwgcIwDgYDVR0PAQH_BAQDAgKkMBMGA1UdJQQMMAoGCCsGAQUFBwMBMA8GA1UdEwEB_wQFMAMBAf8wHQYDVR0OBBYEFIDa6oM-iTWdwrrHgDWcANwOc8O-MGsGA1UdEQRkMGKCBWZyYW5rgglsb2NhbGhvc3SCBWZyYW5rgg5wb2xhci1uMy1mcmFua4IEdW5peIIKdW5peHBhY2tldIIHYnVmY29ubocEfwAAAYcQAAAAAAAAAAAAAAAAAAAAAYcErBIACDAKBggqhkjOPQQDAgNJADBGAiEAhDWgOU2apqMeMwnpoVpLHxJzl75f7qaGRF8-HyHDIK8CIQD6-vtKiMuhdWQssEx3_dbPOjM8OO2J8ah6rGU8jVQlfA&macaroon=AgEDbG5kAvgBAwoQs4KpkfOz3WDHzaWUQxKP-RIBMBoWCgdhZGRyZXNzEgRyZWFkEgV3cml0ZRoTCgRpbmZvEgRyZWFkEgV3cml0ZRoXCghpbnZvaWNlcxIEcmVhZBIFd3JpdGUaIQoIbWFjYXJvb24SCGdlbmVyYXRlEgRyZWFkEgV3cml0ZRoWCgdtZXNzYWdlEgRyZWFkEgV3cml0ZRoXCghvZmZjaGFpbhIEcmVhZBIFd3JpdGUaFgoHb25jaGFpbhIEcmVhZBIFd3JpdGUaFAoFcGVlcnMSBHJlYWQSBXdyaXRlGhgKBnNpZ25lchIIZ2VuZXJhdGUSBHJlYWQAAAYgqjpXcQ3SQnKG3GSBPwnDlchWGwoxq5ru8858M78ZNqs", BitcoinStringAnalyzerTest.RESULT_CONNECT_DATA);
        test.execute("lndconnect://127.0.0.1:10006?macaroon=AgEDbG5kAvgBAwoQs4KpkfOz3WDHzaWUQxKP-RIBMBoWCgdhZGRyZXNzEgRyZWFkEgV3cml0ZRoTCgRpbmZvEgRyZWFkEgV3cml0ZRoXCghpbnZvaWNlcxIEcmVhZBIFd3JpdGUaIQoIbWFjYXJvb24SCGdlbmVyYXRlEgRyZWFkEgV3cml0ZRoWCgdtZXNzYWdlEgRyZWFkEgV3cml0ZRoXCghvZmZjaGFpbhIEcmVhZBIFd3JpdGUaFgoHb25jaGFpbhIEcmVhZBIFd3JpdGUaFAoFcGVlcnMSBHJlYWQSBXdyaXRlGhgKBnNpZ25lchIIZ2VuZXJhdGUSBHJlYWQAAAYgqjpXcQ3SQnKG3GSBPwnDlchWGwoxq5ru8858M78ZNqs", BitcoinStringAnalyzerTest.RESULT_CONNECT_DATA);

        // CoreLightning connect
        test.execute("clngrpc://192.168.0.21:9736?pubkey=023456789abcdef&protoPath=/path/to/cln.proto&certs=LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JR0hBZ0VBTUJNR0J5cUdTTTQ5QWdFR0NDcUdTTTQ5QXdFSEJHMHdhd0lCQVFRZ2xQdE0vRnpUNE0wQTBuNW4KelZWVmNFcnRwbmtmNi9PcXNnN3NKb2h0R2oyaFJBTkNBQVNObVFMdXBlWnhOUWVhZ0FORFQvd1FSR01rY3I3TApRQWFrQks0dndtdUJkdGF4MHh3TjFQei9KUHhpeTdKc0lwTHpkdVpkUkhQRjhvL1Y3S0JpTjYyMgotLS0tLUVORCBQUklWQVRFIEtFWS0tLS0tLS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJVRENCOTZBREFnRUNBaFFlYnVkWVkwdDF1dmp0OGJNTkNVR0VDTE5QQ0RBS0JnZ3Foa2pPUFFRREFqQVcKTVJRd0VnWURWUVFEREF0amJHNGdVbTl2ZENCRFFUQWdGdzAzTlRBeE1ERXdNREF3TURCYUdBODBNRGsyTURFdwpNVEF3TURBd01Gb3dHakVZTUJZR0ExVUVBd3dQWTJ4dUlHZHljR01nUTJ4cFpXNTBNRmt3RXdZSEtvWkl6ajBDCkFRWUlLb1pJemowREFRY0RRZ0FFalprQzdxWG1jVFVIbW9BRFEwLzhFRVJqSkhLK3kwQUdwQVN1TDhKcmdYYlcKc2RNY0RkVDgveVQ4WXN1eWJDS1M4M2JtWFVSenhmS1AxZXlnWWpldHRxTWRNQnN3R1FZRFZSMFJCQkl3RUlJRApZMnh1Z2dsc2IyTmhiR2h2YzNRd0NnWUlLb1pJemowRUF3SURTQUF3UlFJZ0lUaldZeUxwa1V3U1F4ZFpNOS9oCk9kOGljTmpYaUsrb0Jyb1NmT01lNlJNQ0lRREFTallKamlMUzhDeXdqQmhIWEhnQitXTUVSbktyb3dNdDB5ZkwKUms3SXBRPT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLS0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLQpNSUlCZkRDQ0FTT2dBd0lCQWdJVU1Pb0ROcTg1WjJ3c0JCZ2FJM1lxL0N2SmloMHdDZ1lJS29aSXpqMEVBd0l3CkZqRVVNQklHQTFVRUF3d0xZMnh1SUZKdmIzUWdRMEV3SUJjTk56VXdNVEF4TURBd01EQXdXaGdQTkRBNU5qQXgKTURFd01EQXdNREJhTUJZeEZEQVNCZ05WQkFNTUMyTnNiaUJTYjI5MElFTkJNRmt3RXdZSEtvWkl6ajBDQVFZSQpLb1pJemowREFRY0RRZ0FFWHN2Tmd3OEp1eTRZdmRVYjgrcnE0QjQ4R050SmtJUUZLaGNiQjdDYlJOV3ZIOUNFCkhrUktBNERTOElmRDA4SS9BaDhDVUhhdnNJcUhsa2VCVGY0eURhTk5NRXN3R1FZRFZSMFJCQkl3RUlJRFkyeHUKZ2dsc2IyTmhiR2h2YzNRd0hRWURWUjBPQkJZRUZFQkhDd2RjZndFenpRUDhhTFNabVJLWldzUytNQThHQTFVZApFd0VCL3dRRk1BTUJBZjh3Q2dZSUtvWkl6ajBFQXdJRFJ3QXdSQUlnR25pMFE2VXBuUXJMcC9hSEZxMzN5T0RvCkdJMzY4S29BUFNubTlMMUdlaFVDSUh2NTJoZkVUTG5tUi9vQjNvWGdGYjFUa0pQSlpmcnRpOEQ0cFY5aGo0K2UKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ==", BitcoinStringAnalyzerTest.RESULT_CONNECT_DATA);

        // LNDHub connect
        test.execute("lndhub://username:password@https://getalby.com/lndhub/", BitcoinStringAnalyzerTest.RESULT_CONNECT_DATA);

        // Nostr Wallet Connect
        test.execute("nostr+walletconnect://b889ff5b1513b641e2a139f661a661364979c5beee91842f8f0ef42ab558e9d4?relay=wss%3A%2F%2Frelay.getalby.com/v1&secret=71a8c14c1407c113601079c4302dab36460f0ccd0ad506f1f2dc73b5100e4f3c", BitcoinStringAnalyzerTest.RESULT_CONNECT_DATA);

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
