package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.nitram509.jmacaroons.CaveatPacket;
import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonsSerializer;
import com.google.common.io.BaseEncoding;

import java.util.List;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.HexUtil;
import app.michaelwuensch.bitbanana.util.Version;

public class LndBackend extends Backend {

    private boolean isAccountRestricted = false;
    private String account = "";

    public boolean getIsAccountRestricted() {
        return isAccountRestricted;
    }

    public String getAccount() {
        return account;
    }

    public LndBackend(BackendConfig backendConfig) {

        // General
        mApi = new LndApi();
        mNodeImplementationName = "LND";
        mMinRequiredVersion = new Version("0.17.0");
        mMinRequiredVersionName = "v0.17.0-beta";

        // Features
        bSupportsBolt11Receive = true;
        bSupportsBolt11Sending = true;
        bSupportsOnChainReceive = true;
        bSupportsOnChainSending = true;
        bSupportsChannelManagement = true;
        bSupportsOpenChannel = true;
        bSupportsCloseChannel = true;
        bSupportsPeerManagement = true;
        bSupportsPeerModification = true;
        bSupportsRouting = true;
        bSupportsRoutingPolicyManagement = true;
        bSupportsCoinControl = true;
        bSupportsBalanceDetails = true;
        bSupportsMessageSigningByNodePrivateKey = true;
        bSupportsLnurlAuth = true;
        bSupportsKeysend = true;
        bSupportsOnChainFeeEstimation = true;
        bSupportsAbsoluteOnChainFeeEstimation = true;
        bSupportsRoutingFeeEstimation = true;
        bSupportsIdentityScreen = true;
        bSupportsBolt11WithoutAmount = true;
        bSupportsEventSubscription = true;
        bSupportsWatchtowers = true;

        // Based on the macaroon we now deactivate some of the features again if the permission is missing
        try {
            if (backendConfig.getAuthenticationToken() != null) {
                byte[] macaroonBytes = HexUtil.hexToBytes(backendConfig.getAuthenticationToken());
                String base64 = BaseEncoding.base64Url().encode(macaroonBytes);
                Macaroon macaroon = Macaroon.deserialize(base64, MacaroonsSerializer.V2);

                // Print macaroon details
                List<LndMacaroonPermission> permissions = LndMacaroonPermissionParser.parsePermissions(macaroon.identifier);

                if (!(hasReadPermission(permissions, "message") || hasWritePermission(permissions, "message")))
                    bSupportsMessageSigningByNodePrivateKey = false;

                if (!hasWritePermission(permissions, "address"))
                    bSupportsOnChainReceive = false;

                if (!hasWritePermission(permissions, "invoices"))
                    bSupportsBolt11Receive = false;

                if (!hasWritePermission(permissions, "offchain")) {
                    bSupportsBolt11Sending = false;
                    bSupportsKeysend = false;
                }

                if (!hasWritePermission(permissions, "onchain")) {
                    bSupportsOnChainSending = false;
                    bSupportsOpenChannel = false;
                    bSupportsCloseChannel = false;
                }

                if (!hasReadPermission(permissions, "peers"))
                    bSupportsPeerManagement = false;

                if (!hasWritePermission(permissions, "peers"))
                    bSupportsPeerModification = false;


                // Check for account restriction
                CaveatPacket[] caveats = macaroon.caveatPackets;
                if (caveats != null) {
                    for (CaveatPacket caveat : caveats) {
                        if (caveat.getValueAsText().contains("lnd-custom account")) {
                            // Save account string
                            account = caveat.getValueAsText().split("lnd-custom account")[1].trim();

                            // Adapt features
                            isAccountRestricted = true;
                            bSupportsChannelManagement = false;
                            bSupportsPeerManagement = false;
                            bSupportsWatchtowers = false;
                            bSupportsCoinControl = false;
                            bSupportsRouting = false;
                            bSupportsRoutingFeeEstimation = false;
                            bSupportsEventSubscription = false;
                            bSupportsIdentityScreen = false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            BBLog.w("Backend", "Failed to parse macaroon and adapt features accordingly.");
        }
    }

    public static boolean hasReadPermission(List<LndMacaroonPermission> permissionList, String key) {
        return hasPermission(permissionList, key, false);
    }

    public static boolean hasWritePermission(List<LndMacaroonPermission> permissionList, String key) {
        return hasPermission(permissionList, key, true);
    }

    private static boolean hasPermission(List<LndMacaroonPermission> permissionList, String key, boolean write) {
        for (LndMacaroonPermission permission : permissionList) {
            if (permission.identifier.equals(key)) {
                if (write)
                    return permission.canWrite;
                else
                    return permission.canRead;
            }
        }
        return false;
    }
}