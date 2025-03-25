package app.michaelwuensch.bitbanana.util;

import com.google.protobuf.ByteString;

import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.Outpoint;

public class ApiUtil {

    private static final String LOG_TAG = ApiUtil.class.getSimpleName();

    public static long getBackendTimeout() {
        return (long) PrefsUtil.getBackendTimeout() * TorManager.getInstance().getTorTimeoutMultiplier();
    }

    public static int getPaymentTimeout() {
        return PrefsUtil.getPaymentTimeout();
    }

    public static String StringFromHexByteString(ByteString hexByteString) {
        return HexUtil.bytesToHex(hexByteString.toByteArray());
    }

    public static ByteString ByteStringFromHexString(String hexString) {
        return ByteString.copyFrom(HexUtil.hexToBytes(hexString));
    }

    public static Outpoint OutpointFromString(String outpoint) {
        return Outpoint.newBuilder()
                .setTransactionID(outpoint.split(":")[0])
                .setOutputIndex(Integer.parseInt(outpoint.split(":")[1]))
                .build();
    }

    public static ShortChannelId ScidFromString(String scid) {
        String[] parts = scid.split("x");
        return ShortChannelId.newBuilder()
                .setBlockHeight(Integer.parseInt(parts[0]))
                .setIndex(Integer.parseInt(parts[1]))
                .setOutputIndex(Integer.parseInt(parts[2]))
                .build();
    }

    public static ShortChannelId ScidFromLong(long scid) {
        byte[] scidBytes = UtilFunctions.longToBytes(scid);
        byte[] blockHeightBytes = {0x00, scidBytes[0], scidBytes[1], scidBytes[2]};
        byte[] indexBytes = {0x00, scidBytes[3], scidBytes[4], scidBytes[5]};
        byte[] outputIndexBytes = {0x00, 0x00, scidBytes[6], scidBytes[7]};
        int blockHeight = UtilFunctions.intFromByteArray(blockHeightBytes);
        int index = UtilFunctions.intFromByteArray(indexBytes);
        int outputIndex = UtilFunctions.intFromByteArray(outputIndexBytes);
        return ShortChannelId.newBuilder()
                .setBlockHeight(blockHeight)
                .setIndex(index)
                .setOutputIndex(outputIndex)
                .build();
    }

    public static long LongFromScid(ShortChannelId scid) {
        byte[] blockHeightBytes = UtilFunctions.intToByteArray(scid.getBlockHeight());
        byte[] indexBytes = UtilFunctions.intToByteArray(scid.getIndex());
        byte[] indexOutputBytes = UtilFunctions.intToByteArray(scid.getOutputIndex());
        byte[] combined = {blockHeightBytes[1], blockHeightBytes[2], blockHeightBytes[3],
                indexBytes[1], indexBytes[2], indexBytes[3],
                indexOutputBytes[2], indexOutputBytes[3]};
        return UtilFunctions.bytesToLong(combined);
    }
}
