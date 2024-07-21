package app.michaelwuensch.bitbanana.listViews.watchtowers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.watchtowers.items.WatchtowerItemViewHolder;
import app.michaelwuensch.bitbanana.listViews.watchtowers.items.WatchtowerListItem;


public class WatchtowerItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final SortedList<WatchtowerListItem> mSortedList = new SortedList<>(WatchtowerListItem.class, new SortedList.Callback<WatchtowerListItem>() {
        @Override
        public int compare(WatchtowerListItem i1, WatchtowerListItem i2) {
            return i1.compareTo(i2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(WatchtowerListItem i1, WatchtowerListItem i2) {
            return i1.equalsWithSameContent(i2);
        }

        @Override
        public boolean areItemsTheSame(WatchtowerListItem i1, WatchtowerListItem i2) {
            return i1.equals(i2);
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
    private WatchtowerSelectListener mWatchtowerSelectListener;

    // Construct the adapter with a data list
    public WatchtowerItemAdapter(WatchtowerSelectListener watchtowerSelectListener) {
        mWatchtowerSelectListener = watchtowerSelectListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View watchtowerItemView = inflater.inflate(R.layout.list_watchtower_item, parent, false);
        return new WatchtowerItemViewHolder(watchtowerItemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        WatchtowerItemViewHolder watchtowerItemViewHolder = (WatchtowerItemViewHolder) holder;
        WatchtowerListItem watchtowerListItem = mSortedList.get(position);
        watchtowerItemViewHolder.bindWatchtowerListItem(watchtowerListItem);
        watchtowerItemViewHolder.addOnWatchtowerSelectListener(mWatchtowerSelectListener);
    }

    public void add(WatchtowerListItem watchtowerListItem) {
        mSortedList.add(watchtowerListItem);
    }

    public void remove(WatchtowerListItem watchtowerListItem) {
        mSortedList.remove(watchtowerListItem);
    }

    public void add(List<WatchtowerListItem> watchtowerListItems) {
        mSortedList.addAll(watchtowerListItems);
    }

    public void remove(List<WatchtowerListItem> watchtowerListItems) {
        mSortedList.beginBatchedUpdates();
        for (WatchtowerListItem watchtowerListItem : watchtowerListItems) {
            mSortedList.remove(watchtowerListItem);
        }
        mSortedList.endBatchedUpdates();
    }

    public void replaceAll(List<WatchtowerListItem> watchtowerListItems) {
        mSortedList.replaceAll(watchtowerListItems);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mSortedList.size();
    }
}
