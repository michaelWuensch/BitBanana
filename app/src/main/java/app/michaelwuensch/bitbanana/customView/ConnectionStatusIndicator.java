package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;

public class ConnectionStatusIndicator extends ConstraintLayout {

    private ImageView mStatusIcon;
    private TextView mWalletNameWidthDummy;
    private ConnectionStatusIndicatorListener mOnConnectionStatusIndicatorListener;

    public ConnectionStatusIndicator(Context context) {
        super(context);
        init(context);
    }

    public ConnectionStatusIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConnectionStatusIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_connection_status_indicator, this, true);
        mStatusIcon = findViewById(R.id.status_icon);
        mWalletNameWidthDummy = findViewById(R.id.walletNameWidthDummy);
        mStatusIcon.setOnClickListener(v -> {
            if (mOnConnectionStatusIndicatorListener != null)
                mOnConnectionStatusIndicatorListener.onConnectionStatusIndicatorClicked();
        });
    }

    public void setError() {
        int paddingInDp = 11;
        int topMarginInDp = 3;
        int bottomMarginInDp = 0;
        mStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.red)));
        mStatusIcon.setImageResource(R.drawable.ic_status_dot_black_24dp);
        adjustIconPadding(paddingInDp);
        adjustIconMarginTop(topMarginInDp, bottomMarginInDp);
    }

    public void setLoading() {
        int paddingInDp = 11;
        int topMarginInDp = 3;
        int bottomMarginInDp = 0;
        mStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.banana_yellow)));
        mStatusIcon.setImageResource(R.drawable.ic_status_dot_black_24dp);
        adjustIconPadding(paddingInDp);
        adjustIconMarginTop(topMarginInDp, bottomMarginInDp);
    }

    public void setConnected(BackendConfig backendConfig) {
        int paddingInDp = 11;
        int topMarginInDp = 3;
        int bottomMarginInDp = 0;
        mStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.green)));
        if (backendConfig.getUseTor()) {
            paddingInDp = 5;
            topMarginInDp = 0;
            bottomMarginInDp = 3;
            mStatusIcon.setImageResource(R.drawable.tor_icon);
        } else if (backendConfig.getVerifyCertificate()) {
            paddingInDp = 6;
            topMarginInDp = 0;
            bottomMarginInDp = 1;
            mStatusIcon.setImageResource(R.drawable.ic_baseline_lock_24);
        } else {
            mStatusIcon.setImageResource(R.drawable.ic_status_dot_black_24dp);
        }

        adjustIconPadding(paddingInDp);
        adjustIconMarginTop(topMarginInDp, bottomMarginInDp);
    }

    private void adjustIconPadding(int paddingInDp) {
        int paddingInPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                paddingInDp,
                getResources().getDisplayMetrics()
        );
        mStatusIcon.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
    }

    private void adjustIconMarginTop(int topMarginInDp, int bottomMarginInDp) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mStatusIcon.getLayoutParams();

        // Keep existing left, right margins
        int left = params.leftMargin;
        int right = params.rightMargin;

        // New top margin (example: 24dp converted to px)
        int top = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                topMarginInDp,
                mStatusIcon.getResources().getDisplayMetrics()
        );

        // New bottom margin (example: 24dp converted to px)
        int bottom = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                bottomMarginInDp,
                mStatusIcon.getResources().getDisplayMetrics()
        );

        // Apply new margins
        params.setMargins(left, top, right, bottom);
        mStatusIcon.setLayoutParams(params);
    }

    public void updatePosition(BackendConfig backendConfig) {
        // Update position based on wallet name
        String walletAlias;
        if (backendConfig.getNetwork() == BackendConfig.Network.MAINNET || backendConfig.getNetwork() == BackendConfig.Network.UNKNOWN || backendConfig.getNetwork() == null)
            walletAlias = backendConfig.getAlias();
        else
            walletAlias = backendConfig.getAlias() + " (" + backendConfig.getNetwork().getDisplayName() + ")";
        mWalletNameWidthDummy.setText(walletAlias);
    }

    public interface ConnectionStatusIndicatorListener {
        void onConnectionStatusIndicatorClicked();
    }

    // Set the listener
    public void setConnectionStatusIndicatorListener(ConnectionStatusIndicatorListener listener) {
        this.mOnConnectionStatusIndicatorListener = listener;
    }
}
