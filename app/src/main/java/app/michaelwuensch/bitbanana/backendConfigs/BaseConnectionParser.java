package app.michaelwuensch.bitbanana.backendConfigs;

/**
 * Base class meant to be extended for parsing various connection strings like
 * - BTCPay Configuration
 * - LNConnect Configuration
 */
public abstract class BaseConnectionParser {

    protected int mError = -1;
    protected String mConnectionString;

    private BackendConfig mBackendConfig;

    public BaseConnectionParser(String connectionString) {
        mConnectionString = connectionString;
    }

    public BackendConfig getBackendConfig() {
        return mBackendConfig;
    }

    protected void setBackendConfig(BackendConfig backendConfig) {
        mBackendConfig = backendConfig;
    }

    public boolean hasError() {
        return mError > -1;
    }

    public int getError() {
        return mError;
    }
}
