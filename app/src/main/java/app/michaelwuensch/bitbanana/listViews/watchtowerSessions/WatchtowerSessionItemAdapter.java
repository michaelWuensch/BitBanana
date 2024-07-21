package app.michaelwuensch.bitbanana.listViews.watchtowerSessions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.watchtowerSessions.items.WatchtowerSessionItemViewHolder;
import app.michaelwuensch.bitbanana.listViews.watchtowerSessions.items.WatchtowerSessionListItem;


public class WatchtowerSessionItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final SortedList<WatchtowerSessionListItem> mSortedList = new SortedList<>(WatchtowerSessionListItem.class, new SortedList.Callback<WatchtowerSessionListItem>() {
        @Override
        public int compare(WatchtowerSessionListItem i1, WatchtowerSessionListItem i2) {
            return i1.compareTo(i2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(WatchtowerSessionListItem i1, WatchtowerSessionListItem i2) {
            return i1.equalsWithSameContent(i2);
        }

        @Override
        public boolean areItemsTheSame(WatchtowerSessionListItem i1, WatchtowerSessionListItem i2) {
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
    private WatchtowerSessionSelectListener mWatchtowerSessionSelectListener;

    // Construct the adapter with a data list
    public WatchtowerSessionItemAdapter(WatchtowerSessionSelectListener watchtowerSessionSelectListener) {
        mWatchtowerSessionSelectListener = watchtowerSessionSelectListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View watchtowerSessionItemView = inflater.inflate(R.layout.list_watchtower_session_item, parent, false);
        return new WatchtowerSessionItemViewHolder(watchtowerSessionItemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        WatchtowerSessionItemViewHolder watchtowerSessionItemViewHolder = (WatchtowerSessionItemViewHolder) holder;
        WatchtowerSessionListItem watchtowerSessionListItem = mSortedList.get(position);
        watchtowerSessionItemViewHolder.bindWatchtowerSessionListItem(watchtowerSessionListItem);
        watchtowerSessionItemViewHolder.addOnWatchtowerSessionSelectListener(mWatchtowerSessionSelectListener);
    }

    public void add(WatchtowerSessionListItem watchtowerSessionListItem) {
        mSortedList.add(watchtowerSessionListItem);
    }

    public void remove(WatchtowerSessionListItem watchtowerSessionListItem) {
        mSortedList.remove(watchtowerSessionListItem);
    }

    public void add(List<WatchtowerSessionListItem> watchtowerSessionListItems) {
        mSortedList.addAll(watchtowerSessionListItems);
    }

    public void remove(List<WatchtowerSessionListItem> watchtowerSessionListItems) {
        mSortedList.beginBatchedUpdates();
        for (WatchtowerSessionListItem watchtowerSessionListItem : watchtowerSessionListItems) {
            mSortedList.remove(watchtowerSessionListItem);
        }
        mSortedList.endBatchedUpdates();
    }

    public void replaceAll(List<WatchtowerSessionListItem> watchtowerSessionListItems) {
        mSortedList.replaceAll(watchtowerSessionListItems);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mSortedList.size();
    }
}
