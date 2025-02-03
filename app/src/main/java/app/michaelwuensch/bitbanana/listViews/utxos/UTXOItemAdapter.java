package app.michaelwuensch.bitbanana.listViews.utxos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.utxos.items.UTXOItemViewHolder;
import app.michaelwuensch.bitbanana.listViews.utxos.items.UTXOListItem;
import app.michaelwuensch.bitbanana.models.Outpoint;


public class UTXOItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<UTXOListItem> mItems;
    private Set<Integer> selectedPositions = new HashSet<>(); // Tracks selected items
    private UTXOSelectListener mUtxoSelectListener;
    private int mMode;

    // Construct the adapter with a data list
    public UTXOItemAdapter(List<UTXOListItem> dataset, UTXOSelectListener utxoSelectListener, int mode) {
        mItems = dataset;
        mUtxoSelectListener = utxoSelectListener;
        mMode = mode;
    }

    public void setPreselectedItems(List<Outpoint> preselectedItems) {
        if (preselectedItems == null || preselectedItems.isEmpty())
            return;
        selectedPositions.clear();
        for (int i = 0; i < mItems.size(); i++) {
            for (Outpoint outpoint : preselectedItems) {
                if (outpoint.toString().equals(mItems.get(i).getUtxo().getOutpoint().toString())) {
                    selectedPositions.add(i);
                    notifyItemChanged(i);
                }
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
        UTXOListItem utxoListItem = mItems.get(position);
        utxoItemViewHolder.bindUTXOListItem(utxoListItem);
        utxoItemViewHolder.addOnUTXOSelectListener(mUtxoSelectListener);

        if (mMode == UTXOsActivity.MODE_SELECT) {

            // Highlight the selected item
            holder.itemView.setSelected(selectedPositions.contains(position));

            // Handle item click to toggle selection
            holder.itemView.setOnClickListener(v -> {
                if (!mItems.get(position).getUtxo().isLeased()) {
                    if (selectedPositions.contains(position)) {
                        selectedPositions.remove(position); // Unselect if already selected
                    } else {
                        selectedPositions.add(position); // Select if not selected
                    }

                    // Notify the adapter to update the item's appearance
                    notifyItemChanged(position);
                }
                // Make sure it still transfers the click event to UTXOsActivity.
                mUtxoSelectListener.onUtxoSelect(mItems.get(position).getUtxo());
            });
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public List<UTXOListItem> getSelectedItems() {
        List<UTXOListItem> selectedItems = new ArrayList<>();
        for (int position : selectedPositions) {
            selectedItems.add(mItems.get(position));
        }
        return selectedItems;
    }
}
