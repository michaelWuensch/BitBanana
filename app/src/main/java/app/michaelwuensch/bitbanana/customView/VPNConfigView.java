package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.connection.vpn.VPNConfig;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class VPNConfigView extends ConstraintLayout {

    private TextView mTvVpnName;
    private ImageView mArrowImage;
    private ClickableConstraintLayoutGroup mTopGroup;
    private Group mExpandedContentGroup;
    private Spinner mSpType;
    private BBInputFieldView mInputViewTunnel;
    private SwitchCompat mSwStart;
    private SwitchCompat mSwStop;

    public VPNConfigView(Context context) {
        super(context);
        init();
    }

    public VPNConfigView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VPNConfigView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_vpn_config, this);

        mTvVpnName = view.findViewById(R.id.vpnName);
        mTopGroup = view.findViewById(R.id.topGroup);
        mArrowImage = view.findViewById(R.id.arrowImage);
        mExpandedContentGroup = view.findViewById(R.id.expandedContentGroup);
        mSpType = view.findViewById(R.id.typeSpinner);
        mInputViewTunnel = view.findViewById(R.id.tunnelInput);
        mSwStart = view.findViewById(R.id.startSwitch);
        mSwStop = view.findViewById(R.id.stopSwitch);

        // Toggle content on click
        mTopGroup.setOnAllClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                boolean isContentExpanded = mExpandedContentGroup.getVisibility() == View.VISIBLE;
                setContentExpandedState(!isContentExpanded);
            }
        });

        String[] items = new String[VPNConfig.VPNType.values().length];
        for (int i = 0; i < VPNConfig.VPNType.values().length; i++) {
            items[i] = VPNConfig.VPNType.values()[i].getDisplayName();
        }
        mSpType.setAdapter(new ArrayAdapter<>(getContext(), R.layout.spinner_item, items));
        mSpType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                VPNConfig.VPNType selectedType = VPNConfig.VPNType.values()[position];
                mTvVpnName.setText(selectedType.getDisplayName());
                switch (selectedType) {
                    case NONE:
                    case TAILSCALE:
                        mInputViewTunnel.setVisibility(GONE);
                        mInputViewTunnel.setValue(null);
                        break;
                    case WIREGUARD:
                        mInputViewTunnel.setVisibility(VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void setupWithVpnConfig(VPNConfig vpnConfig) {
        if (vpnConfig == null)
            vpnConfig = new VPNConfig();

        mSwStart.setChecked(vpnConfig.getStartVPNOnOpen());
        mSwStop.setChecked(vpnConfig.getStopVPNOnClose());
        mTvVpnName.setText(vpnConfig.getVpnType().getDisplayName());
        mInputViewTunnel.setValue(vpnConfig.getTunnelName());

        // Fake spinner selection which triggers additional code.
        int index = 0;
        for (int i = 0; i < VPNConfig.VPNType.values().length; i++) {
            if (VPNConfig.VPNType.values()[i] == vpnConfig.getVpnType()) {
                index = i;
                break;
            }
        }
        mSpType.setSelection(index);
    }

    private void setContentExpandedState(boolean expanded) {
        TransitionManager.beginDelayedTransition((ViewGroup) getRootView());
        mArrowImage.setImageResource(expanded ? R.drawable.ic_arrow_up_24dp : R.drawable.ic_arrow_down_24dp);
        mExpandedContentGroup.setVisibility(expanded ? View.VISIBLE : View.GONE);
    }

    public VPNConfig getVPNConfig() {
        VPNConfig config = new VPNConfig();
        config.setVpnType(VPNConfig.VPNType.values()[mSpType.getSelectedItemPosition()]);
        if (mInputViewTunnel.getData() != null)
            config.setTunnelName(mInputViewTunnel.getData());
        config.setStartVPNOnOpen(mSwStart.isChecked());
        config.setStopVPNOnClose(mSwStop.isChecked());
        return config;
    }
}
