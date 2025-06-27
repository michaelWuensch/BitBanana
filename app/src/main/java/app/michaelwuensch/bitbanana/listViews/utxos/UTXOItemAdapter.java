package app.michaelwuensch.bitbanana.listViews.utxos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.utxos.items.UTXOItemViewHolder;
import app.michaelwuensch.bitbanana.listViews.utxos.items.UTXOListItem;
import app.michaelwuensch.bitbanana.models.Outpoint;


public class UTXOItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final SortedList<UTXOListItem> mSortedList = new SortedList<>(UTXOListItem.class, new SortedList.Callback<UTXOListItem>() {
        @Override
        public int compare(UTXOListItem i1, UTXOListItem i2) {
            return i1.compareTo(i2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(UTXOListItem oldItem, UTXOListItem newItem) {
            return oldItem.equalsWithSameContent(newItem);
        }

        @Override
        public boolean areItemsTheSame(UTXOListItem item1, UTXOListItem item2) {
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
    private Set<String> selectedOutpoints = new HashSet<>(); // Tracks selected items by their outpoints
    private UTXOSelectListener mUtxoSelectListener;
    private int mMode;

    // Construct the adapter with a data list
    public UTXOItemAdapter(UTXOSelectListener utxoSelectListener, int mode) {
        mUtxoSelectListener = utxoSelectListener;
        mMode = mode;
    }

    public void setPreselectedItems(List<Outpoint> preselectedItems) {
        if (preselectedItems == null || preselectedItems.isEmpty())
            return;
        selectedOutpoints.clear();
        for (Outpoint outpoint : preselectedItems) {
            selectedOutpoints.add(outpoint.toString());
        }
        notifyDataSetChanged();
    }

    public void rebindVisibleViewHolders(RecyclerView recyclerView) {
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            if (holder instanceof UTXOItemViewHolder) {
                ((UTXOItemViewHolder) holder).rebind();
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View utxoItemView = inflater.inflate(R.layout.list_utxo_item, parent, false);
        return new UTXOItemViewHolder(utxoItemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        UTXOItemViewHolder utxoItemViewHolder = (UTXOItemViewHolder) holder;
        UTXOListItem utxoListItem = mSortedList.get(position);
        utxoItemViewHolder.bindUTXOListItem(utxoListItem);
        utxoItemViewHolder.addOnUTXOSelectListener(mUtxoSelectListener);

        if (mMode == UTXOsActivity.MODE_SELECT) {

            // Highlight the selected item using outpoint instead of position
            holder.itemView.setSelected(selectedOutpoints.contains(utxoListItem.getUtxo().getOutpoint().toString()));

            // Handle item click to toggle selection
            holder.itemView.setOnClickListener(v -> {
                if (!utxoListItem.getUtxo().isLeased()) {
                    String outpoint = utxoListItem.getUtxo().getOutpoint().toString();
                    if (selectedOutpoints.contains(outpoint)) {
                        selectedOutpoints.remove(outpoint); // Unselect if already selected
                    } else {
                        selectedOutpoints.add(outpoint); // Select if not selected
                    }

                    // Notify the adapter to update the item's appearance
                    notifyItemChanged(mSortedList.indexOf(utxoListItem));
                }
                // Make sure it still transfers the click event to UTXOsActivity.
                mUtxoSelectListener.onUtxoSelect(utxoListItem.getUtxo());
            });
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mSortedList.size();
    }

    public List<UTXOListItem> getSelectedItems() {
        List<UTXOListItem> selectedItems = new ArrayList<>();
        for (int i = 0; i < mSortedList.size(); i++) {
            UTXOListItem item = mSortedList.get(i);
            if (selectedOutpoints.contains(item.getUtxo().getOutpoint().toString())) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    public void replaceAll(List<UTXOListItem> items) {
        mSortedList.replaceAll(items);
    }
}
