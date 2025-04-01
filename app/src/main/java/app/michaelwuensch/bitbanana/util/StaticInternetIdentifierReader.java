package app.michaelwuensch.bitbanana.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.minidns.constants.DnssecConstants;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.hla.DnssecResolverApi;
import org.minidns.hla.ResolverResult;
import org.minidns.record.Data;
import org.minidns.record.RRSIG;
import org.minidns.record.Record;
import org.minidns.record.TXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.models.LnAddress;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class StaticInternetIdentifierReader {

    private static final String LOG_TAG = StaticInternetIdentifierReader.class.getSimpleName();

    public static void checkIfValidStaticInternetIdentifier(Context ctx, String address, OnStaticIdentifierChecked listener) {
        LnAddress lnAddress = new LnAddress(address);

        if (lnAddress.isValidBip353DnsRecordAddress()) {
            Bip353DNSLookup(lnAddress, ctx, listener);
        } else {
            checkLnurl(lnAddress, ctx, listener);
        }
    }

    private static void Bip353DNSLookup(LnAddress lnAddress, Context ctx, OnStaticIdentifierChecked listener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {

            try {
                ResolverResult<TXT> result = DnssecResolverApi.INSTANCE.resolve(lnAddress.getUsername() + ".user._bitcoin-payment." + lnAddress.getDomain(), TXT.class);
                if (!result.wasSuccessful()) {
                    DnsMessage.RESPONSE_CODE responseCode = result.getResponseCode();
                    BBLog.w(LOG_TAG, "Bip353DNSLookup result not successful. Response Code: " + responseCode);
                    mainHandler.post(() -> {
                        checkLnurl(lnAddress, ctx, listener);
                    });
                    return;
                }
                if (!result.isAuthenticData()) {
                    // Response was not secured with DNSSEC.
                    BBLog.w(LOG_TAG, "Bip353DNSLookup result is not authentic. DNSSEC failed.");
                    mainHandler.post(() -> {
                        checkLnurl(lnAddress, ctx, listener);
                    });
                    return;
                }

                // Perform additional DNSSEC checks as noted in BIP 353

                // Ensure SHA-1 is not used
                for (Record<? extends Data> record : result.getRawAnswer().answerSection) {
                    if (record.getPayload() instanceof RRSIG) {
                        RRSIG rrsig = (RRSIG) record.getPayload();

                        if (rrsig.algorithm == DnssecConstants.SignatureAlgorithm.RSASHA1
                                || rrsig.algorithm == DnssecConstants.SignatureAlgorithm.RSASHA1_NSEC3_SHA1
                                || rrsig.algorithm == DnssecConstants.SignatureAlgorithm.DSA_NSEC3_SHA1) {
                            BBLog.w(LOG_TAG, "Bip353DNSLookup denied: DNSSEC signature uses SHA-1.");
                            mainHandler.post(() -> {
                                checkLnurl(lnAddress, ctx, listener);
                            });
                            return;
                        }
                    }
                }

                // ToDo: validate min Keylength of 1024


                // Extract and validate the content of the TXT records.
                Set<TXT> answers = result.getAnswers();
                List<TXT> validTXTs = new ArrayList<>();

                for (TXT txt : answers) {
                    if (txt.getText().toLowerCase().startsWith("bitcoin:"))
                        validTXTs.add(txt);
                }

                if (validTXTs.isEmpty()) {
                    BBLog.w(LOG_TAG, "Bip353DNSLookup: No valid txt entry.");
                    mainHandler.post(() -> {
                        checkLnurl(lnAddress, ctx, listener);
                    });
                    return;
                }
                if (validTXTs.size() > 1) {
                    BBLog.w(LOG_TAG, "Bip353DNSLookup: More than one valid entry. This is not allowed.");
                    mainHandler.post(() -> {
                        checkLnurl(lnAddress, ctx, listener);
                    });
                    return;
                }

                // Everything valid
                mainHandler.post(() -> {
                    listener.onValidBip353DnsRecord(validTXTs.get(0).getText().replaceAll(" / ", ""));
                });


            } catch (Exception e) {
                BBLog.w(LOG_TAG, "DNS lookup error: " + e.getMessage());
                mainHandler.post(() -> {
                    checkLnurl(lnAddress, ctx, listener);
                });
            }

        }).start();
    }

    private static void checkLnurl(LnAddress lnAddress, Context ctx, OnStaticIdentifierChecked listener) {
        // Check for Lnurl
        if (lnAddress.isValidLnurlAddress()) {
            String requestUrl = IdentifierToLnurlRequest(lnAddress);

            Request lightningAddressRequest = new Request.Builder()
                    .url(requestUrl)
                    .build();

            HttpClient.getInstance().getClient().newCall(lightningAddressRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    BBLog.e(LOG_TAG, e.getMessage());
                    listener.onError(e.getMessage(), RefConstants.ERROR_DURATION_SHORT);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    boolean isBlockedByCloudflare = false;
                    try {
                        String responseAsString = response.body().string();
                        isBlockedByCloudflare = responseAsString.toLowerCase().contains("cloudflare") && responseAsString.toLowerCase().contains("captcha-bypass");
                        BBLog.d(LOG_TAG, responseAsString);
                        LnUrlPayResponse lnUrlPayResponse = new Gson().fromJson(responseAsString, LnUrlPayResponse.class);
                        if (lnUrlPayResponse.hasError()) {
                            listener.onError(lnUrlPayResponse.getReason(), RefConstants.ERROR_DURATION_MEDIUM);
                            return;
                        }
                        listener.onValidLnurlPay(lnUrlPayResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (isBlockedByCloudflare) {
                            listener.onError(ctx.getResources().getString(R.string.error_tor_blocked_lnurl, lnAddress.getDomain()), RefConstants.ERROR_DURATION_VERY_LONG);
                        } else {
                            listener.onError(ctx.getResources().getString(R.string.error_static_identifier_response_unknown, lnAddress.getDomain()), RefConstants.ERROR_DURATION_MEDIUM);
                        }
                    }
                }
            });
        } else {
            listener.onNoStaticInternetIdentifierData();
        }
    }

    /**
     * Please refer to the following specification
     * https://github.com/fiatjaf/lnurl-rfc/blob/luds/16.md
     */
    private static String IdentifierToLnurlRequest(LnAddress lnAddress) {
        if (lnAddress.isTor()) {
            return "http://" + lnAddress.getDomain() + "/.well-known/lnurlp/" + lnAddress.getUsername();
        } else {
            return "https://" + lnAddress.getDomain() + "/.well-known/lnurlp/" + lnAddress.getUsername();
        }
    }


    public interface OnStaticIdentifierChecked {

        void onValidLnurlPay(LnUrlPayResponse lnUrlPayResponse);

        void onValidBip353DnsRecord(String bip21InvoiceString);

        void onError(String error, int duration);

        void onNoStaticInternetIdentifierData();
    }
}
