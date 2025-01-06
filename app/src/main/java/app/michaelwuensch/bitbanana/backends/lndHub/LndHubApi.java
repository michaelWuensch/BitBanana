package app.michaelwuensch.bitbanana.backends.lndHub;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.RxRestWrapper;
import app.michaelwuensch.bitbanana.backends.lndHub.models.LndHubAddInvoiceResponse;
import app.michaelwuensch.bitbanana.backends.lndHub.models.LndHubBalanceResponse;
import app.michaelwuensch.bitbanana.backends.lndHub.models.LndHubGetOnChainAddress;
import app.michaelwuensch.bitbanana.backends.lndHub.models.LndHubTx;
import app.michaelwuensch.bitbanana.backends.lndHub.models.LndHubUserInvoice;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceResponse;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.util.Version;
import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Class that translates BitBanana backend interactions into LndHub API specific interactions.
 * <p>
 * You can find the LndHub documentation here:
 * https://github.com/BlueWallet/LndHub/blob/master/doc/Send-requirements.md
 */
public class LndHubApi extends Api {
    private static final String LOG_TAG = LndHubApi.class.getSimpleName();

    public LndHubApi() {

    }

    private OkHttpClient getClient() {
        return LndHubHttpClient.getInstance().getClient();
    }

    private String getBaseUrl() {
        return BackendManager.getCurrentBackendConfig().getHostWithOverride();
    }

    @Override
    public Single<CurrentNodeInfo> getCurrentNodeInfo() {
        okhttp3.Request request = new Request.Builder()
                .url(getBaseUrl() + "getinfo")
                .build();

        return RxRestWrapper.makeRxCall(getClient(), request, null, response -> {
            // The response does not return any useful data, therefore we fake it.
            return CurrentNodeInfo.newBuilder()
                    .setNetwork(BackendConfig.Network.MAINNET)
                    .setVersion(new Version("1.0"))
                    .setFullVersionString("")
                    .setSynced(true)
                    .setAvatarMaterial(BackendManager.getCurrentBackendConfig().getUser() + BackendManager.getCurrentBackendConfig().getPassword())
                    .build();
        });
    }

    @Override
    public Single<Balances> getBalances() {
        okhttp3.Request request = new Request.Builder()
                .url(getBaseUrl() + "balance")
                .build();

        return RxRestWrapper.makeRxCall(getClient(), request, LndHubBalanceResponse.class, response -> {
            return Balances.newBuilder()
                    .setChannelBalance(response.getBTC().getAvailableBalance() * 1000L)
                    .build();
        });
    }

    @Override
    public Single<CreateInvoiceResponse> createInvoice(CreateInvoiceRequest createInvoiceRequest) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json.put("amt", String.valueOf(createInvoiceRequest.getAmount() / 1000L));
            json.put("memo", createInvoiceRequest.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);

        okhttp3.Request request = new Request.Builder()
                .url(getBaseUrl() + "addinvoice")
                .post(body)
                .build();

        return RxRestWrapper.makeRxCall(getClient(), request, LndHubAddInvoiceResponse.class, response -> {
            return CreateInvoiceResponse.newBuilder()
                    .setBolt11(response.getPaymentRequest())
                    .build();
        });
    }

    @Override
    public Single<List<LnInvoice>> listInvoices(int page, int pageSize) {
        okhttp3.Request request = new Request.Builder()
                .url(getBaseUrl() + "getuserinvoices")
                .build();
        return RxRestWrapper.makeRxCall(getClient(), request, LndHubUserInvoice[].class, response -> {
            List<LnInvoice> invoiceList = new ArrayList<>();

            for (LndHubUserInvoice invoice : response) {
                LnInvoice.Builder builder = LnInvoice.newBuilder()
                        .setType(LnInvoice.InvoiceType.BOLT11_INVOICE)
                        .setCreatedAt(invoice.getTimestamp())
                        .setPaidAt(invoice.getTimestamp())
                        .setExpiresAt(invoice.getTimestamp() + invoice.getExpireTime())
                        .setMemo(invoice.getDescription())
                        .setBolt11(invoice.getPaymentRequest())
                        .setAmountRequested(invoice.getAmt() * 1000L)
                        .setPaymentHash(invoice.getPaymentHash());
                if (invoice.isPaid()) {
                    builder.setAmountPaid(invoice.getAmt() * 1000L);
                }

                invoiceList.add(builder.build());
            }
            return invoiceList;
        });
    }

    @Override
    public Single<LnInvoice> getInvoice(String paymentHash) {
        okhttp3.Request request = new Request.Builder()
                .url(getBaseUrl() + "getuserinvoices")
                .build();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LndHubUserInvoice.class, new GenericFallbackDeserializer<>(LndHubUserInvoice.class))
                .create();

        return RxRestWrapper.makeRxCall(getClient(), gson, request, LndHubUserInvoice[].class, response -> {
            for (LndHubUserInvoice invoice : response) {
                if (invoice.getPaymentHash().equals(paymentHash)) {
                    LnInvoice.Builder builder = LnInvoice.newBuilder()
                            .setType(LnInvoice.InvoiceType.BOLT11_INVOICE)
                            .setCreatedAt(invoice.getTimestamp())
                            .setPaidAt(invoice.getTimestamp())
                            .setExpiresAt(invoice.getTimestamp() + invoice.getExpireTime())
                            .setMemo(invoice.getDescription())
                            .setBolt11(invoice.getPaymentRequest())
                            .setAmountRequested(invoice.getAmt() * 1000L)
                            .setPaymentHash(invoice.getPaymentHash());

                    if (invoice.isPaid()) {
                        builder.setAmountPaid(invoice.getAmt() * 1000L);
                    }

                    return builder.build();
                }
            }
            return null;
        });
    }

    @Override
    public Single<List<LnPayment>> listLnPayments(int page, int pageSize) {
        okhttp3.Request request = new Request.Builder()
                .url(getBaseUrl() + "gettxs")
                .build();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LndHubTx.class, new GenericFallbackDeserializer<>(LndHubTx.class))
                .create();

        return RxRestWrapper.makeRxCall(getClient(), gson, request, LndHubTx[].class, response -> {
            List<LnPayment> paymentsList = new ArrayList<>();

            for (LndHubTx tx : response)
                paymentsList.add(LnPayment.newBuilder()
                        .setPaymentPreimage(tx.getPaymentPreimage())
                        .setFee(Math.abs(tx.getFee() * 1000L))
                        .setDescription(tx.getMemo())
                        .setCreatedAt(tx.getTimestamp())
                        .setAmountPaid(Math.abs(tx.getValue() * 1000L))
                        .setBolt11(tx.getPaymentRequest())
                        .setPaymentHash(tx.getPaymentHash())
                        .setStatus(LnPayment.Status.SUCCEEDED)
                        .build());

            return paymentsList;
        });
    }

    @Override
    public Single<SendLnPaymentResponse> sendLnPayment(SendLnPaymentRequest sendLnPaymentRequest) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json.put("invoice", sendLnPaymentRequest.getBolt11().getBolt11String());
            //json.put("amount", String.valueOf(sendLnPaymentRequest.getAmount()))   Does not work with alby to pay 0 sat invoices
        } catch (Exception e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);

        okhttp3.Request request = new Request.Builder()
                .url(getBaseUrl() + "payinvoice")
                .post(body)
                .build();

        return RxRestWrapper.makeRxCall(getClient(), request, null, response -> {
            return SendLnPaymentResponse.newBuilder()
                    .setAmount(sendLnPaymentRequest.getAmount())
                    .build();
        });
    }

    @Override
    public Single<String> getNewOnchainAddress(NewOnChainAddressRequest newOnChainAddressRequest) {
        okhttp3.Request request = new Request.Builder()
                .url(getBaseUrl() + "getbtc")
                .build();

        try {
            return RxRestWrapper.makeRxCall(getClient(), request, LndHubGetOnChainAddress[].class, response -> {
                if (response.length > 0)
                    return response[0].getAddress();
                else
                    throw new RuntimeException("Backend does not support creating on-chain addresses");
            });
        } catch (Exception e) {
            return Single.error(new RuntimeException());
        }
    }
}