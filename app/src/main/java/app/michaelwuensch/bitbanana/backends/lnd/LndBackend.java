package app.michaelwuensch.bitbanana.backends.lnd;

import com.github.nitram509.jmacaroons.CaveatPacket;
import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonsSerializer;
import com.google.common.io.BaseEncoding;

import java.util.List;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Backend;
import app.michaelwuensch.bitbanana.backends.BackendFeature;
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
        mMinRequiredVersion = new Version("0.18.0");
        mMinRequiredVersionName = "v0.18.0-beta";

        // Features
        FeatureBolt11Receive = new BackendFeature(true);
        FeatureBolt11Sending = new BackendFeature(true);
        FeatureOnChainReceive = new BackendFeature(true);
        FeatureOnChainSending = new BackendFeature(true);
        FeatureChannelManagement = new BackendFeature(true);
        FeatureOpenChannel = new BackendFeature(true);
        FeatureCloseChannel = new BackendFeature(true);
        FeaturePeerManagement = new BackendFeature(true);
        FeaturePeerModification = new BackendFeature(true);
        FeatureRouting = new BackendFeature(true);
        FeatureRoutingPolicyManagement = new BackendFeature(true);
        FeatureCoinControl = new BackendFeature(true);
        FeatureBalanceDetails = new BackendFeature(true);
        FeatureMessageSigningByNodePrivateKey = new BackendFeature(true);
        FeatureLnurlAuth = new BackendFeature(true);
        FeatureKeysend = new BackendFeature(true);
        FeatureOnChainFeeEstimation = new BackendFeature(true);
        FeatureAbsoluteOnChainFeeEstimation = new BackendFeature(true);
        FeatureRoutingFeeEstimation = new BackendFeature(true);
        FeatureIdentityScreen = new BackendFeature(true);
        FeatureBolt11WithoutAmount = new BackendFeature(true);
        FeatureEventSubscriptions = new BackendFeature(true);
        FeatureWatchtowers = new BackendFeature(true);
        FeatureDisplayPaymentRoute = new BackendFeature(true);
        FeatureManuallyLeaseUTXOs = new BackendFeature(true);

        // Based on the macaroon we now deactivate some of the features again if the permission is missing
        try {
            if (backendConfig.getAuthenticationToken() != null) {
                byte[] macaroonBytes = HexUtil.hexToBytes(backendConfig.getAuthenticationToken());
                String base64 = BaseEncoding.base64Url().encode(macaroonBytes);
                Macaroon macaroon = Macaroon.deserialize(base64, MacaroonsSerializer.V2);

                // Print macaroon details
                List<LndMacaroonPermission> permissions = LndMacaroonPermissionParser.parsePermissions(macaroon.identifier);

                if (!(hasReadPermission(permissions, "message") || hasWritePermission(permissions, "message")))
                    FeatureMessageSigningByNodePrivateKey = new BackendFeature(false);

                if (!hasWritePermission(permissions, "address"))
                    FeatureOnChainReceive = new BackendFeature(false);

                if (!hasWritePermission(permissions, "invoices"))
                    FeatureBolt11Receive = new BackendFeature(false);

                if (!hasWritePermission(permissions, "offchain")) {
                    FeatureBolt11Sending = new BackendFeature(false);
                    FeatureKeysend = new BackendFeature(false);
                }

                if (!hasWritePermission(permissions, "onchain")) {
                    FeatureOnChainSending = new BackendFeature(false);
                    FeatureOpenChannel = new BackendFeature(false);
                    FeatureCloseChannel = new BackendFeature(false);
                }

                if (!hasReadPermission(permissions, "peers"))
                    FeaturePeerManagement = new BackendFeature(false);

                if (!hasWritePermission(permissions, "peers"))
                    FeaturePeerModification = new BackendFeature(false);


                // Check for account restriction
                CaveatPacket[] caveats = macaroon.caveatPackets;
                if (caveats != null) {
                    for (CaveatPacket caveat : caveats) {
                        if (caveat.getValueAsText().contains("lnd-custom account")) {
                            // Save account string
                            account = caveat.getValueAsText().split("lnd-custom account")[1].trim();

                            // Adapt features
                            isAccountRestricted = true;
                            FeatureChannelManagement = new BackendFeature(false);
                            FeaturePeerManagement = new BackendFeature(false);
                            FeatureWatchtowers = new BackendFeature(false);
                            FeatureCoinControl = new BackendFeature(false);
                            FeatureRouting = new BackendFeature(false);
                            FeatureRoutingFeeEstimation = new BackendFeature(false);
                            FeatureEventSubscriptions = new BackendFeature(false);
                            FeatureIdentityScreen = new BackendFeature(false);
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