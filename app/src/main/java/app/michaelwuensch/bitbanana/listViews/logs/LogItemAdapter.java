package app.michaelwuensch.bitbanana.listViews.logs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.SortedListAdapterCallback;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.logs.items.LogItemViewHolder;
import app.michaelwuensch.bitbanana.listViews.logs.items.LogListItem;


public class LogItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final SortedList<LogListItem> mSortedList = new SortedList<>(LogListItem.class, new SortedListAdapterCallback<LogListItem>(this) {
        @Override
        public int compare(LogListItem p1, LogListItem p2) {
            return p1.compareTo(p2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(LogListItem p1, LogListItem p2) {
            return p1.equalsWithSameContent(p2);
        }

        @Override
        public boolean areItemsTheSame(LogListItem p1, LogListItem p2) {
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
    private LogSelectListener mLogSelectListener;

    // Construct the adapter with a data list
    public LogItemAdapter(LogSelectListener logSelectListener) {
        mLogSelectListener = logSelectListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View logItemView = inflater.inflate(R.layout.list_log_item, parent, false);
        return new LogItemViewHolder(logItemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        LogItemViewHolder logItemViewHolder = (LogItemViewHolder) holder;
        LogListItem logListItem = mSortedList.get(position);
        logItemViewHolder.bindLogListItem(logListItem, position);
        logItemViewHolder.addOnLogSelectListener(mLogSelectListener);
    }

    public int add(LogListItem logListItem) {
        return mSortedList.add(logListItem);
    }

    public void remove(LogListItem logListItem) {
        mSortedList.remove(logListItem);
    }

    public void add(List<LogListItem> logListItems) {
        mSortedList.addAll(logListItems);
    }

    public void remove(List<LogListItem> logListItems) {
        mSortedList.beginBatchedUpdates();
        for (LogListItem logListItem : logListItems) {
            mSortedList.remove(logListItem);
        }
        mSortedList.endBatchedUpdates();
    }

    public void replaceAll(List<LogListItem> logListItems) {
        mSortedList.replaceAll(logListItems);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mSortedList.size();
    }
}
