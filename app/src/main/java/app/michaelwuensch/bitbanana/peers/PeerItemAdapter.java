package app.michaelwuensch.bitbanana.peers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.List;

import app.michaelwuensch.bitbanana.R;


public class PeerItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final SortedList<PeerListItem> mSortedList = new SortedList<>(PeerListItem.class, new SortedList.Callback<PeerListItem>() {
        @Override
        public int compare(PeerListItem p1, PeerListItem p2) {
            return p1.compareTo(p2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(PeerListItem p1, PeerListItem p2) {
            return p1.equalsWithSameContent(p2);
        }

        @Override
        public boolean areItemsTheSame(PeerListItem p1, PeerListItem p2) {
            return p1.equals(p2);
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    });
    private PeerSelectListener mPeerSelectListener;

    // Construct the adapter with a data list
    public PeerItemAdapter(PeerSelectListener peerSelectListener) {
        mPeerSelectListener = peerSelectListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View peerItemView = inflater.inflate(R.layout.list_peer_item, parent, false);
        return new PeerItemViewHolder(peerItemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        PeerItemViewHolder peerItemViewHolder = (PeerItemViewHolder) holder;
        PeerListItem peerListItem = mSortedList.get(position);
        peerItemViewHolder.bindPeerListItem(peerListItem);
        peerItemViewHolder.addOnPeerSelectListener(mPeerSelectListener);
    }

    public void add(PeerListItem peerListItem) {
        mSortedList.add(peerListItem);
    }

    public void remove(PeerListItem peerListItem) {
        mSortedList.remove(peerListItem);
    }

    public void add(List<PeerListItem> peerListItems) {
        mSortedList.addAll(peerListItems);
    }

    public void remove(List<PeerListItem> peerListItems) {
        mSortedList.beginBatchedUpdates();
        for (PeerListItem peerListItem : peerListItems) {
            mSortedList.remove(peerListItem);
        }
        mSortedList.endBatchedUpdates();
    }

    public void replaceAll(List<PeerListItem> peerListItems) {
        mSortedList.replaceAll(peerListItems);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mSortedList.size();
    }
}
