package app.michaelwuensch.bitbanana.backends.lnd.services;

import io.reactivex.rxjava3.core.Single;

public interface LndWalletKitService {

    Single<com.github.lightningnetwork.lnd.walletrpc.ListUnspentResponse> listUnspent(com.github.lightningnetwork.lnd.walletrpc.ListUnspentRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.LeaseOutputResponse> leaseOutput(com.github.lightningnetwork.lnd.walletrpc.LeaseOutputRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.ReleaseOutputResponse> releaseOutput(com.github.lightningnetwork.lnd.walletrpc.ReleaseOutputRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.ListLeasesResponse> listLeases(com.github.lightningnetwork.lnd.walletrpc.ListLeasesRequest request);

    Single<com.github.lightningnetwork.lnd.signrpc.KeyDescriptor> deriveNextKey(com.github.lightningnetwork.lnd.walletrpc.KeyReq request);

    Single<com.github.lightningnetwork.lnd.signrpc.KeyDescriptor> deriveKey(com.github.lightningnetwork.lnd.signrpc.KeyLocator request);

    Single<com.github.lightningnetwork.lnd.walletrpc.AddrResponse> nextAddr(com.github.lightningnetwork.lnd.walletrpc.AddrRequest request);

    Single<com.github.lightningnetwork.lnd.lnrpc.Transaction> getTransaction(com.github.lightningnetwork.lnd.walletrpc.GetTransactionRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.ListAccountsResponse> listAccounts(com.github.lightningnetwork.lnd.walletrpc.ListAccountsRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.RequiredReserveResponse> requiredReserve(com.github.lightningnetwork.lnd.walletrpc.RequiredReserveRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.ListAddressesResponse> listAddresses(com.github.lightningnetwork.lnd.walletrpc.ListAddressesRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.SignMessageWithAddrResponse> signMessageWithAddr(com.github.lightningnetwork.lnd.walletrpc.SignMessageWithAddrRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.VerifyMessageWithAddrResponse> verifyMessageWithAddr(com.github.lightningnetwork.lnd.walletrpc.VerifyMessageWithAddrRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.ImportAccountResponse> importAccount(com.github.lightningnetwork.lnd.walletrpc.ImportAccountRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.ImportPublicKeyResponse> importPublicKey(com.github.lightningnetwork.lnd.walletrpc.ImportPublicKeyRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.ImportTapscriptResponse> importTapscript(com.github.lightningnetwork.lnd.walletrpc.ImportTapscriptRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.PublishResponse> publishTransaction(com.github.lightningnetwork.lnd.walletrpc.Transaction request);

    Single<com.github.lightningnetwork.lnd.walletrpc.RemoveTransactionResponse> removeTransaction(com.github.lightningnetwork.lnd.walletrpc.GetTransactionRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.SendOutputsResponse> sendOutputs(com.github.lightningnetwork.lnd.walletrpc.SendOutputsRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.EstimateFeeResponse> estimateFee(com.github.lightningnetwork.lnd.walletrpc.EstimateFeeRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.PendingSweepsResponse> pendingSweeps(com.github.lightningnetwork.lnd.walletrpc.PendingSweepsRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.BumpFeeResponse> bumpFee(com.github.lightningnetwork.lnd.walletrpc.BumpFeeRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.ListSweepsResponse> listSweeps(com.github.lightningnetwork.lnd.walletrpc.ListSweepsRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.LabelTransactionResponse> labelTransaction(com.github.lightningnetwork.lnd.walletrpc.LabelTransactionRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.FundPsbtResponse> fundPsbt(com.github.lightningnetwork.lnd.walletrpc.FundPsbtRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.SignPsbtResponse> signPsbt(com.github.lightningnetwork.lnd.walletrpc.SignPsbtRequest request);

    Single<com.github.lightningnetwork.lnd.walletrpc.FinalizePsbtResponse> finalizePsbt(com.github.lightningnetwork.lnd.walletrpc.FinalizePsbtRequest request);
}