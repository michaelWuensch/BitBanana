package app.michaelwuensch.bitbanana.listViews.backendConfigs.items;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.listViews.backendConfigs.ManageBackendConfigsActivity;
import app.michaelwuensch.bitbanana.listViews.backendConfigs.itemDetails.BackendConfigDetailsActivity;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;

public class BackendConfigItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = BackendConfigItemViewHolder.class.getSimpleName();

    private ImageView mIcon;
    private TextView mNodeTypDescription;
    private TextView mNodeName;
    private TextView mNetworkName;
    private View mRootView;
    private Context mContext;
    private ImageView mCurrentActiveIcon;

    public BackendConfigItemViewHolder(View v) {
        super(v);

        mIcon = v.findViewById(R.id.nodeTypeIcon);
        mNodeTypDescription = v.findViewById(R.id.nodeTypeDescription);
        mNodeName = v.findViewById(R.id.nodeName);
        mNetworkName = v.findViewById(R.id.networkName);
        mRootView = v.findViewById(R.id.transactionRootView);
        mCurrentActiveIcon = v.findViewById(R.id.currentlyActiveIcon);
        mContext = v.getContext();
    }

    public void bindRemoteNodeItem(BackendConfig BackendConfig) {

        // Set Icon
        if (BackendConfig.isLocal()) {
            mIcon.setImageResource(R.drawable.ic_local_black_24dp);
        } else {
            mIcon.setImageResource(R.drawable.ic_remote_black_24dp);
        }
        mIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.banana_yellow)));

        // Set current active icon visibility
        if (BackendConfig.getId().equals(PrefsUtil.getCurrentBackendConfig())) {
            mCurrentActiveIcon.setVisibility(View.VISIBLE);
        } else {
            mCurrentActiveIcon.setVisibility(View.GONE);
        }

        // Set node type description
        mNodeTypDescription.setText(BackendConfig.getLocation().getDisplayName());

        // Set node name
        mNodeName.setText(BackendConfig.getAlias());

        // Set network info
        if (BackendConfig.getNetwork() != null) {
            switch (BackendConfig.getNetwork()) {
                case MAINNET:
                case UNKNOWN:
                    mNetworkName.setVisibility(View.GONE);
                    break;
                default:
                    mNetworkName.setVisibility(View.VISIBLE);
                    mNetworkName.setText(BackendConfig.getNetwork().getDisplayName());
            }
        }

        // Set on click listener
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(mContext, BackendConfigDetailsActivity.class);
                intent.putExtra(ManageBackendConfigsActivity.NODE_ID, BackendConfig.getId());
                mContext.startActivity(intent);
            }
        });
    }
}
