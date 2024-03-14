package app.michaelwuensch.bitbanana.util;

import com.github.lightningnetwork.lnd.lnrpc.PreviousOutPoint;
import com.github.lightningnetwork.lnd.lnrpc.Transaction;

import java.util.List;

import app.michaelwuensch.bitbanana.backendConfigs.BaseBackendConfig;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
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
    public static String getNodePubKeyFromChannelTransaction(Transaction transaction) {

        // open channels
        if (Wallet_Channels.getInstance().getOpenChannelsList() != null) {
            for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList())
                if (transaction.getTxHash().equals(c.getFundingOutpoint().getTransactionID()))
                    return c.getRemotePubKey();
        }

        // pending channels
        if (Wallet_Channels.getInstance().getPendingChannelsList() != null) {
            for (PendingChannel c : Wallet_Channels.getInstance().getPendingChannelsList()) {
                if (transaction.getTxHash().equals(c.getFundingOutpoint().getTransactionID()))
                    return c.getRemotePubKey();
                if (c.hasCloseTransactionId())
                    if (transaction.getTxHash().equals(c.getCloseTransactionId()))
                        return c.getRemotePubKey();

                List<PreviousOutPoint> previousOutPoints = transaction.getPreviousOutpointsList();
                for (PreviousOutPoint op : previousOutPoints) {
                    if (c.hasCloseTransactionId())
                        if (op.getOutpoint().split(":")[0].equals(c.getCloseTransactionId()))
                            return c.getRemotePubKey();
                }
            }
        }

        // closed channels
        if (Wallet_Channels.getInstance().getClosedChannelsList() != null) {
            for (ClosedChannel c : Wallet_Channels.getInstance().getClosedChannelsList()) {
                if (transaction.getTxHash().equals(c.getFundingOutpoint().getTransactionID()) || transaction.getTxHash().equals(c.getCloseTransactionId()))
                    return c.getRemotePubKey();
                for (String sweepTxId : c.getSweepTransactionIds())
                    if (transaction.getTxHash().equals(sweepTxId))
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
    public static boolean isChannelTransaction(Transaction transaction) {

        // This is faster especially for nodes with lots of channels
        if (BackendManager.getCurrentBackendType() == BaseBackendConfig.BackendType.LND_GRPC && hasChannelTransactionLabel(transaction)) {
            return true;
        }

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
    public static boolean hasChannelTransactionLabel(Transaction transaction) {
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
    }

    /**
     * Get the maximum amount that can be received over Lightning Channels.
     *
     * @return amount in msat
     */
    public static long getMaxLightningReceiveAmount() {
        long tempMax = 0L;
        if (Wallet_Channels.getInstance().getOpenChannelsList() != null)
            for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList())
                if (c.isActive())
                    tempMax = tempMax + Math.max(c.getRemoteBalance() - c.getRemoteChannelConstraints().getChannelReserve(), 0);
        return tempMax;
    }

    /**
     * Get the maximum amount that can be send over Lightning Channels.
     *
     * @return amount in msat
     */
    public static long getMaxLightningSendAmount() {
        long tempMax = 0L;
        if (Wallet_Channels.getInstance().getOpenChannelsList() != null)
            for (OpenChannel c : Wallet_Channels.getInstance().getOpenChannelsList())
                if (c.isActive())
                    tempMax = tempMax + Math.max(c.getLocalBalance() - c.getLocalChannelConstraints().getChannelReserve(), 0);
        return tempMax;
    }
}