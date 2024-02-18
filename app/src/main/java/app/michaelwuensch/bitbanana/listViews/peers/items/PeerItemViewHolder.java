package app.michaelwuensch.bitbanana.listViews.peers.items;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.peers.PeerSelectListener;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class PeerItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = PeerItemViewHolder.class.getSimpleName();

    private TextView mPeerName;
    private View mRootView;
    private PeerSelectListener mPeerSelectListener;
    private Context mContext;


    public PeerItemViewHolder(View v) {
        super(v);

        mPeerName = v.findViewById(R.id.peerName);
        mRootView = v.findViewById(R.id.peerRootView);
        mContext = v.getContext();
    }

    public void bindPeerListItem(PeerListItem peerListItem) {

        // Set peer name
        mPeerName.setText(peerListItem.getAlias());

        // Set on click listener
        setOnRootViewClickListener(peerListItem);
    }

    public void addOnPeerSelectListener(PeerSelectListener peerSelectListener) {
        mPeerSelectListener = peerSelectListener;
    }

    void setOnRootViewClickListener(@NonNull PeerListItem item) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mPeerSelectListener != null) {
                    mPeerSelectListener.onPeerSelect(item.getPeer().toByteString());
                }
            }
        });
    }
}
