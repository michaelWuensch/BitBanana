package app.michaelwuensch.bitbanana.listViews.backendConfigs.items;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.listViews.backendConfigs.ManageBackendConfigsActivity;
import app.michaelwuensch.bitbanana.listViews.backendConfigs.itemDetails.BackendConfigDetailsActivity;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.manageNodeConfigs.BBNodeConfig;

public class BackendConfigItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = BackendConfigItemViewHolder.class.getSimpleName();

    private ImageView mIcon;
    private TextView mNodeTypDescription;
    private TextView mNodeName;
    private View mRootView;
    private Context mContext;
    private ImageView mCurrentActiveIcon;

    public BackendConfigItemViewHolder(View v) {
        super(v);

        mIcon = v.findViewById(R.id.nodeTypeIcon);
        mNodeTypDescription = v.findViewById(R.id.nodeTypeDescription);
        mNodeName = v.findViewById(R.id.nodeName);
        mRootView = v.findViewById(R.id.transactionRootView);
        mCurrentActiveIcon = v.findViewById(R.id.currentlyActiveIcon);
        mContext = v.getContext();
    }

    public void bindRemoteNodeItem(BBNodeConfig BBNodeConfig) {

        // Set Icon
        if (BBNodeConfig.getType().equals(BBNodeConfig.NODE_TYPE_LOCAL)) {
            mIcon.setImageResource(R.drawable.ic_local_black_24dp);
        } else {
            mIcon.setImageResource(R.drawable.ic_remote_black_24dp);
        }
        mIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.banana_yellow)));

        // Set current active icon visibility
        if (BBNodeConfig.getId().equals(PrefsUtil.getCurrentNodeConfig())) {
            mCurrentActiveIcon.setVisibility(View.VISIBLE);
        } else {
            mCurrentActiveIcon.setVisibility(View.GONE);
        }

        // Set node type description
        mNodeTypDescription.setText(BBNodeConfig.getType());

        // Set node name
        mNodeName.setText(BBNodeConfig.getAlias());

        // Set on click listener
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(mContext, BackendConfigDetailsActivity.class);
                intent.putExtra(ManageBackendConfigsActivity.NODE_ID, BBNodeConfig.getId());
                mContext.startActivity(intent);
            }
        });
    }
}
