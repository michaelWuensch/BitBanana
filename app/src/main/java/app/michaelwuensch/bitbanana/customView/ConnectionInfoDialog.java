package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.CertificateInfoStore;

public class ConnectionInfoDialog extends LinearLayout {

    private TextView mConnectionTypeLabel;
    private TextView mConnectionType;
    private TextView mTargetLabel;
    private TextView mTarget;
    private TextView mStatusLabel;
    private TextView mStatusDot;
    private TextView mStatus;
    private TextView mSecurityLabel;
    private TextView mSecurityDot;
    private TextView mSecurity;

    private BackendConfig mBackendConfig;

    public ConnectionInfoDialog(Context context) {
        super(context);
        init(context);
    }

    public ConnectionInfoDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConnectionInfoDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_connection_info, this, true);

        mConnectionTypeLabel = findViewById(R.id.connectionTypeLabel);
        mConnectionType = findViewById(R.id.connectionType);
        mTargetLabel = findViewById(R.id.targetLabel);
        mTarget = findViewById(R.id.target);
        mStatusLabel = findViewById(R.id.statusLabel);
        mStatusDot = findViewById(R.id.statusDot);
        mStatus = findViewById(R.id.status);
        mSecurityLabel = findViewById(R.id.securityLabel);
        mSecurityDot = findViewById(R.id.securityDot);
        mSecurity = findViewById(R.id.security);

        if (!isInEditMode())
            setupLabels(context);
    }

    private void setupLabels(Context context) {
        String connectionTypeLabel = context.getString(R.string.connection_type) + ":";
        mConnectionTypeLabel.setText(connectionTypeLabel);
        String targetLabel = context.getString(R.string.target) + ":";
        mTargetLabel.setText(targetLabel);
        String statusLabel = context.getString(R.string.status) + ":";
        mStatusLabel.setText(statusLabel);
        String securityLabel = context.getString(R.string.security) + ":";
        mSecurityLabel.setText(securityLabel);
    }

    public void setStatus(ConnectionStatus status) {
        switch (status) {
            case DISCONNECTED:
                mStatusDot.setText("🔴");
                mStatus.setText(R.string.disconnected);
                break;
            case CONNECTING:
                mStatusDot.setText("🟡");
                mStatus.setText(R.string.connection_state_connecting);
                break;
            case CONNECTED:
                mStatusDot.setText("🟢");
                mStatus.setText(R.string.connected);
                break;
        }

        if (mBackendConfig != null)
            updateInfo();
    }

    public void setBackendConfig(BackendConfig backendConfig) {
        mBackendConfig = backendConfig;
        if (mBackendConfig != null)
            updateInfo();
    }

    public void updateInfo() {
        // Connection Type
        switch (mBackendConfig.getBackendType()) {
            case LND_GRPC:
            case CORE_LIGHTNING_GRPC:
            case LND_HUB:
                if (mBackendConfig.getUseTor())
                    mConnectionType.setText(R.string.settings_tor);
                else
                    mConnectionType.setText(R.string.clearnet_tls);
                break;
            case NOSTR_WALLET_CONNECT:
                mConnectionType.setText(R.string.nostr_wallet_connect);
                break;

        }

        // Target
        switch (mBackendConfig.getBackendType()) {
            case LND_GRPC:
                mTarget.setText(R.string.target_remote_lightning_node_lnd);
                break;
            case CORE_LIGHTNING_GRPC:
                mTarget.setText(R.string.target_remote_lightning_node_core_lightning);
                break;
            case NOSTR_WALLET_CONNECT:
            case LND_HUB:
                mTarget.setText(R.string.target_remote_wallet_service);
                break;
        }

        // Security
        switch (mBackendConfig.getBackendType()) {
            case LND_GRPC:
            case CORE_LIGHTNING_GRPC:
                if (mBackendConfig.getUseTor())
                    setSecurityTor();
                else if (mBackendConfig.getVerifyCertificate())
                    if (mBackendConfig.getServerCert() != null)
                        setSecurityClearnetSSLVerifiedPinned();
                    else
                        setSecurityClearnetSSLVerified();
                else
                    setSecurityClearnetSSLUnverified();
                break;
            case NOSTR_WALLET_CONNECT:
                setSecurityNWC();
                break;
            case LND_HUB:
                if (mBackendConfig.getUseTor())
                    setSecurityTor();
                else if (mBackendConfig.getHost().contains("http:"))
                    setSecurityClearnetPlainTextHTTP();
                else
                    setSecurityClearnetSSLVerified();
                break;
        }
    }

    private void setSecurityNWC() {
        mSecurity.setText(R.string.connection_security_info_nwc);
        mSecurityDot.setText("🟢");
    }

    private void setSecurityTor() {
        mSecurity.setText(R.string.connection_security_info_tor);
        mSecurityDot.setText("🟢");
    }

    private void setSecurityClearnetPlainTextHTTP() {
        mSecurity.setText(getResources().getString(R.string.connection_security_info_clearnet_http));
        mSecurityDot.setText("🔴");
    }

    private void setSecurityClearnetSSLVerified() {
        String securityString = getResources().getString(R.string.connection_security_info_clearnet_tls) + " " + getResources().getString(R.string.connection_security_info_clearnet_tls_verified);
        securityString = appendCertificateInfo(securityString);
        mSecurity.setText(securityString);
        mSecurityDot.setText("🟢");
    }

    private void setSecurityClearnetSSLVerifiedPinned() {
        String securityString = getResources().getString(R.string.connection_security_info_clearnet_tls) + " " + getResources().getString(R.string.connection_security_info_clearnet_tls_verified_pinned);
        securityString = appendCertificateInfo(securityString);
        mSecurity.setText(securityString);
        mSecurityDot.setText("🟢");
    }

    private void setSecurityClearnetSSLUnverified() {
        String securityString = getResources().getString(R.string.connection_security_info_clearnet_tls) + " " + getResources().getString(R.string.connection_security_info_clearnet_tls_not_verified);
        securityString = appendCertificateInfo(securityString);
        mSecurity.setText(securityString);
        mSecurityDot.setText("🟡");
    }

    private String appendCertificateInfo(String input) {
        if (CertificateInfoStore.hasCertificate()) {
            input = input + "\n\n" + getResources().getString(R.string.connection_security_info_tls_certificate_issuer) + ":\n" + CertificateInfoStore.getCertificateIssuerOrganization(true);
            if (CertificateInfoStore.isSelfSigned())
                input = input + " (" + getResources().getString(R.string.connection_security_Info_tls_self_signed) + ")";
        }
        return input;
    }

    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED;
    }
}