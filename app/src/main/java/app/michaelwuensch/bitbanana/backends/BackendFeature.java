package app.michaelwuensch.bitbanana.backends;

import app.michaelwuensch.bitbanana.util.Version;
import app.michaelwuensch.bitbanana.wallet.Wallet;

public class BackendFeature {
    private boolean isEnabled;
    private Version minimumVersion;

    public BackendFeature(boolean isEnabled, String minimumVersion) {
        this.isEnabled = isEnabled;
        this.minimumVersion = new Version(minimumVersion);
    }

    public BackendFeature(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Version getMinimumVersion() {
        return minimumVersion;
    }

    public boolean isAvailable() {
        if (!isEnabled) {
            return false;
        }

        if (minimumVersion == null) {
            return true;
        }

        if (Wallet.getInstance().getCurrentNodeInfo() == null) {
            return false;
        }

        Version currentVersion = Wallet.getInstance().getCurrentNodeInfo().getVersion();
        return currentVersion.compareTo(minimumVersion) >= 0;
    }
}
