package app.michaelwuensch.bitbanana.listViews.paymentRoute;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.paymentRoute.items.HopListItemViewHolder;
import app.michaelwuensch.bitbanana.listViews.paymentRoute.items.HopListItem;


public class PaymentRouteItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final SortedList<HopListItem> mSortedList = new SortedList<>(HopListItem.class, new SortedList.Callback<HopListItem>() {
        @Override
        public int compare(HopListItem i1, HopListItem i2) {
            return i1.compareTo(i2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(HopListItem oldItem, HopListItem newItem) {
            return oldItem.equalsWithSameContent(newItem);
        }

        @Override
        public boolean areItemsTheSame(HopListItem item1, HopListItem item2) {
            return item1.equals(item2);
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

    // Construct the adapter with a data list
    public PaymentRouteItemAdapter() {
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View hopView = inflater.inflate(R.layout.view_hop, parent, false);
        return new HopListItemViewHolder(hopView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        HopListItemViewHolder hopListItemViewHolder = (HopListItemViewHolder) holder;
        HopListItem hopListItem = (HopListItem) mSortedList.get(position);
        hopListItemViewHolder.bindHopListItem(hopListItem);
    }

    public void replaceAll(List<HopListItem> items) {
        mSortedList.replaceAll(items);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mSortedList.size();
    }
}
