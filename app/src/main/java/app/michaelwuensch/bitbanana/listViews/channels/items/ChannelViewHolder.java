package app.michaelwuensch.bitbanana.listViews.channels.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.channels.ChannelSelectListener;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class ChannelViewHolder extends RecyclerView.ViewHolder {

    TextView mStatus;
    ImageView mStatusDot;
    View mRootView;
    View mContentView;
    Context mContext;
    private ChannelSelectListener mChannelSelectListener;
    private TextView mRemoteName;
    private AmountView mLocalBalance;
    private AmountView mRemoteBalance;
    private TextView mCapacity;
    private ProgressBar mLocalBar;
    private ProgressBar mRemoteBar;

    ChannelViewHolder(@NonNull View itemView) {
        super(itemView);

        mRemoteName = itemView.findViewById(R.id.remoteName);
        mStatus = itemView.findViewById(R.id.state);
        mStatusDot = itemView.findViewById(R.id.statusDot);
        mLocalBalance = itemView.findViewById(R.id.localBalance);
        mRemoteBalance = itemView.findViewById(R.id.remoteBalance);
        mCapacity = itemView.findViewById(R.id.capacity);
        mLocalBar = itemView.findViewById(R.id.localBar);
        mRemoteBar = itemView.findViewById(R.id.remoteBar);
        mRootView = itemView.findViewById(R.id.channelRootView);
        mContentView = itemView.findViewById(R.id.channelContent);
        mContext = itemView.getContext();
    }

    public void setName(String channelRemotePubKey) {
        mRemoteName.setText(AliasManager.getInstance().getAlias(channelRemotePubKey));
    }

    void setBalances(long local, long remote, long capacity) {
        float localBarValue = (float) ((double) local / (double) capacity);
        float remoteBarValue = (float) ((double) remote / (double) capacity);

        mLocalBar.setProgress((int) (localBarValue * 100f));
        mRemoteBar.setProgress((int) (remoteBarValue * 100f));

        mLocalBalance.setAmountSat(local);
        mRemoteBalance.setAmountSat(remote);

        mCapacity.setText(MonetaryUtil.getInstance().getPrimaryDisplayStringFromSats(capacity));
    }

    public void addOnChannelSelectListener(ChannelSelectListener channelSelectListener) {
        mChannelSelectListener = channelSelectListener;
    }

    void setOnRootViewClickListener(@NonNull ChannelListItem item, int type) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mChannelSelectListener != null) {
                    mChannelSelectListener.onChannelSelect(item.getChannelByteString(), type);
                }
            }
        });
    }
}
