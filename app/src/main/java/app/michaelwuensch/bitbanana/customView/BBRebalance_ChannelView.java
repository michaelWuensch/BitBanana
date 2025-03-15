package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.util.AliasManager;

public class BBRebalance_ChannelView extends ConstraintLayout {

    private View mContentView;
    private TextView mTvRemoteName;
    private TextView mTvStatus;
    private ImageView mIvStatusDot;
    private AmountView mAvLocalBalance;
    private AmountView mAvRemoteBalance;
    private ProgressBar mPbLocalBalance;
    private ProgressBar mPbRemoteBalance;
    private OpenChannel mChannel;

    public BBRebalance_ChannelView(Context context) {
        super(context);
        init(context, null);
    }

    public BBRebalance_ChannelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBRebalance_ChannelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(getContext(), R.layout.channel_list_element_channel, this);

        mContentView = view.findViewById(R.id.channelContent);
        mTvRemoteName = view.findViewById(R.id.remoteName);
        mTvStatus = view.findViewById(R.id.state);
        mIvStatusDot = view.findViewById(R.id.statusDot);
        mAvLocalBalance = view.findViewById(R.id.localBalance);
        mAvRemoteBalance = view.findViewById(R.id.remoteBalance);
        mPbLocalBalance = view.findViewById(R.id.localBar);
        mPbRemoteBalance = view.findViewById(R.id.remoteBar);
    }

    public void setChannel(OpenChannel channel) {
        mChannel = channel;
        mTvRemoteName.setText(AliasManager.getInstance().getAlias(mChannel.getRemotePubKey()));
        setState(channel.isActive());
        setBalances(channel.getLocalBalance(), channel.getRemoteBalance());
        mTvStatus.setVisibility(View.GONE);
        mIvStatusDot.setVisibility(View.GONE);
    }

    public void setBalances(long local, long remote) {
        long availableCapacity = mChannel.getCapacity() - mChannel.getCommitFee();
        float localBarValue = (float) ((double) local / (double) availableCapacity);
        float remoteBarValue = (float) ((double) remote / (double) availableCapacity);

        mPbLocalBalance.setProgress((int) (localBarValue * 100f));
        mPbRemoteBalance.setProgress((int) (remoteBarValue * 100f));

        mAvLocalBalance.setAmountMsat(local);
        mAvRemoteBalance.setAmountMsat(remote);
    }

    void setState(boolean isActive) {
        if (isActive) {
            mTvStatus.setText(R.string.channel_state_open);
            mIvStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.green)));
            mContentView.setAlpha(1f);
        } else {
            mTvStatus.setText(R.string.channel_state_offline);
            mIvStatusDot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.gray)));
            mContentView.setAlpha(0.65f);
        }
    }
}
