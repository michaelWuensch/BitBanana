package app.michaelwuensch.bitbanana.util;

import androidx.annotation.NonNull;

import app.michaelwuensch.bitbanana.models.LightningNodeUri;

public class LightningNodeUriParser {

    private static int NODE_URI_MIN_LENGTH = 66;

    public static LightningNodeUri parseNodeUri(@NonNull String uri) {
        if (uri.isEmpty() || uri.length() < NODE_URI_MIN_LENGTH) {
            return null;
        }

        if (uri.length() == NODE_URI_MIN_LENGTH) {
            // PubKey only
            if (HexUtil.isHex(uri)) {
                return new LightningNodeUri.Builder().setPubKey(uri).build();
            } else {
                return null;
            }
        }

        if (!(uri.charAt(NODE_URI_MIN_LENGTH) == '@')) {
            // longer and no @ after PubKey. Something is wrong.
            return null;
        }

        String[] parts = uri.split("@");

        if (parts.length != 2) {
            return null;
        }

        String pubkey = parts[0];

        String[] parts2 = parts[1].split(":");

        if (parts2.length != 2) {
            if (HexUtil.isHex(pubkey)) {
                return new LightningNodeUri.Builder().setPubKey(pubkey).setHost(parts[1]).build();
            } else {
                return null;
            }
        } else {
            if (HexUtil.isHex(pubkey)) {
                int port;
                try {
                    port = Integer.parseInt(parts2[1]);
                } catch (Exception e) {
                    return null;
                }
                return new LightningNodeUri.Builder().setPubKey(pubkey).setHost(parts2[0]).setPort(port).build();
            } else {
                return null;
            }
        }
    }
}
