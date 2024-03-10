package app.michaelwuensch.bitbanana.util;

import com.google.protobuf.ByteString;

import app.michaelwuensch.bitbanana.connection.tor.TorManager;

public class ApiUtil {

    public static long timeout_short() {
        return RefConstants.TIMEOUT_SHORT * TorManager.getInstance().getTorTimeoutMultiplier();
    }

    public static long timeout_medium() {
        return RefConstants.TIMEOUT_MEDIUM * TorManager.getInstance().getTorTimeoutMultiplier();
    }

    public static long timeout_long() {
        return RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier();
    }

    public static String StringFromHexByteString(ByteString hexByteString) {
        return HexUtil.bytesToHex(hexByteString.toByteArray());
    }
}
