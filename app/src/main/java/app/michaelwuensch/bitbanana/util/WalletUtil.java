package app.michaelwuensch.bitbanana.util;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_Channels;

public class WalletUtil {
    private static final String LOG_TAG = WalletUtil.class.getSimpleName();

    /**
     * Get the remote pubkey from a channel Id.
     * If the id does not match with any open channel, null will be returned.
     *
     * @return remote pub key
     */
    public static String getRemotePubKeyFromChannelId(ShortChannelId shortChannelId) {
        if (Wallet_Channels.getInstance().getOpenChannelsList() != null) {
            for (OpenChannel channel : Wallet_Channels.getInstance().getOpenChannelsList()) {
                if (channel.getShortChannelId().equals(shortChannelId)) {
                    return channel.getRemotePubKey();
                }
            }
        }

        if (Wallet_Channels.getInstance().getPendingChannelsList() != null) {
            for (PendingChannel pendingChannel : Wallet_Channels.getInstance().getPendingChannelsList()) {
                if (pendingChannel.hasShortChannelId())
                    if (pendingChannel.getShortChannelId().equals(shortChannelId))
                        return pendingChannel.getRemotePubKey();
            }
        }

        if (Wallet_Channels.getInstance().getClosedChannelsList() != null) {
            for (ClosedChannel closedChannel : Wallet_Channels.getInstance().getClosedChannelsList()) {
                if (closedChannel.hasShortChannelId())
                    if (closedChannel.getShortChannelId().equals(shortChannelId))
                        return closedChannel.getRemotePubKey();
            }
        }

        return null;
    }

    /**
     * This functions helps us to link on-chain channel transaction with the corresponding channel's public node alias.
     *
     * @return pubKey of the Node the channel is linked to
     */
    public static String getNodePubKeyFromChannelTransaction(OnChainTransaction transaction) {

        // open channels
        if (Wallet_Channels.getInstance().getOpenChannelsList() != null) {
            for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList())
                if (transaction.getTransactionId().equals(c.getFundingOutpoint().getTransactionID()))
                    return c.getRemotePubKey();
        }

        // pending channels
        if (Wallet_Channels.getInstance().getPendingChannelsList() != null) {
            for (PendingChannel c : Wallet_Channels.getInstance().getPendingChannelsList()) {
                if (transaction.getTransactionId().equals(c.getFundingOutpoint().getTransactionID()))
                    return c.getRemotePubKey();
                if (c.hasCloseTransactionId()) {
                    if (transaction.getTransactionId().equals(c.getCloseTransactionId()))
                        return c.getRemotePubKey();
                    if (transaction.getInputs() != null)
                        for (Outpoint op : transaction.getInputs()) {
                            if (op.getTransactionID().equals(c.getCloseTransactionId()))
                                return c.getRemotePubKey();
                        }
                }
            }
        }

        // closed channels
        if (Wallet_Channels.getInstance().getClosedChannelsList() != null) {
            for (ClosedChannel c : Wallet_Channels.getInstance().getClosedChannelsList()) {
                if (transaction.getTransactionId().equals(c.getFundingOutpoint().getTransactionID()) || transaction.getTransactionId().equals(c.getCloseTransactionId()))
                    return c.getRemotePubKey();
                for (String sweepTxId : c.getSweepTransactionIds())
                    if (transaction.getTransactionId().equals(sweepTxId))
                        return c.getRemotePubKey();
            }
        }
        return "";
    }

    /**
     * This function determines if the given transaction is is associated with a channel operation.
     *
     * @param transaction
     * @return
     */
    public static boolean isChannelTransaction(OnChainTransaction transaction) {

        // This is faster especially for nodes with lots of channels
        if (BackendManager.getCurrentBackendType() == BackendConfig.BackendType.LND_GRPC && hasChannelTransactionLabel(transaction)) {
            return true;
        }
        if (BackendManager.getCurrentBackendType() == BackendConfig.BackendType.CORE_LIGHTNING_GRPC)
            return false;

        String pubKey = getNodePubKeyFromChannelTransaction(transaction);
        return !pubKey.equals("");
    }

    /**
     * This function determines if according to the label, that gets applied automatically by lnd, this is a chanel transaction.
     * The labelTypes are derived from: https://github.com/lightningnetwork/lnd/blob/master/labels/labels.go
     *
     * @param transaction
     * @return
     */
    public static boolean hasChannelTransactionLabel(OnChainTransaction transaction) {
        String[] labelType = {":openchannel", ":closechannel", ":justicetx"};
        if (transaction.getLabel() != null && !transaction.getLabel().isEmpty())
            for (String label : labelType)
                if (transaction.getLabel().toLowerCase().contains(label))
                    return true;
        return false;
    }

    /**
     * Returns if the wallet has at least one active channel.
     */
    public static boolean hasOpenActiveChannels() {
        if (FeatureManager.isOpenChannelEnabled()) {
            if (Wallet_Channels.getInstance().getOpenChannelsList() != null) {
                if (Wallet_Channels.getInstance().getOpenChannelsList().size() != 0) {
                    for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList()) {
                        if (c.isActive()) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            // As we cannot open channels, we just assume that we can use some that are already open.
            // We have to assume it as we might not have the permission to examine channels.
            return true;
        }
    }

    /**
     * Get the maximum amount that can be received over Lightning Channels.
     *
     * @return amount in msat
     */
    public static long getMaxLightningReceiveAmount() {
        if (FeatureManager.isOpenChannelEnabled()) {
            long tempMax = 0L;
            if (Wallet_Channels.getInstance().getOpenChannelsList() != null)
                for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList())
                    if (c.isActive())
                        tempMax = tempMax + Math.max(c.getRemoteBalance() - c.getRemoteChannelConstraints().getChannelReserve(), 0);
            return tempMax;
        } else {
            // This is the case for LNDHub connections or accounts on LND for example. We don't know how much can be received. Return a high number (10 BTC).
            return 1000000000L;
        }
    }

    /**
     * Get the maximum amount that can be send over Lightning Channels.
     *
     * @return amount in msat
     */
    public static long getMaxLightningSendAmount() {
        if (FeatureManager.isOpenChannelEnabled()) {
            long tempMax = 0L;
            if (Wallet_Channels.getInstance().getOpenChannelsList() != null)
                for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList())
                    if (c.isActive())
                        tempMax = tempMax + Math.max(c.getLocalBalance() - c.getLocalChannelConstraints().getChannelReserve(), 0);
            return tempMax;
        } else {
            return Wallet_Balance.getInstance().getBalances().channelBalance();
        }
    }

    public static int getBlockHeight() {
        if (Wallet.getInstance().getCurrentNodeInfo() != null)
            return Wallet.getInstance().getCurrentNodeInfo().getBlockHeight();
        else {
            BBLog.w(LOG_TAG, "Block height of 0 was returned as the wallet info is not yet available!");
            return 0;
        }
    }

    public static NewOnChainAddressRequest getNewOnChainAddressRequest() {
        NewOnChainAddressRequest.Type addressType;
        String addressTypeString = PrefsUtil.getPrefs().getString("btcAddressType", "bech32m");
        if (addressTypeString.equals("bech32")) {
            addressType = NewOnChainAddressRequest.Type.SEGWIT;
        } else {
            if (addressTypeString.equals("bech32m")) {
                addressType = NewOnChainAddressRequest.Type.TAPROOT;
            } else {
                addressType = NewOnChainAddressRequest.Type.SEGWIT_COMPATIBILITY;
            }
        }

        return NewOnChainAddressRequest.newBuilder()
                .setType(addressType)
                .setUnused(true)
                .build();
    }
}