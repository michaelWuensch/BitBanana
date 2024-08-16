package app.michaelwuensch.bitbanana.listViews.bolt12offers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.bolt12offers.items.Bolt12OfferItemViewHolder;
import app.michaelwuensch.bitbanana.listViews.bolt12offers.items.Bolt12OfferListItem;


public class Bolt12OfferItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final SortedList<Bolt12OfferListItem> mSortedList = new SortedList<>(Bolt12OfferListItem.class, new SortedList.Callback<Bolt12OfferListItem>() {
        @Override
        public int compare(Bolt12OfferListItem p1, Bolt12OfferListItem p2) {
            return p1.compareTo(p2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Bolt12OfferListItem p1, Bolt12OfferListItem p2) {
            return p1.equalsWithSameContent(p2);
        }

        @Override
        public boolean areItemsTheSame(Bolt12OfferListItem p1, Bolt12OfferListItem p2) {
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
    private Bolt12OfferSelectListener mBolt12OfferSelectListener;

    // Construct the adapter with a data list
    public Bolt12OfferItemAdapter(Bolt12OfferSelectListener bolt12OfferSelectListener) {
        mBolt12OfferSelectListener = bolt12OfferSelectListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View bolt12OfferItemView = inflater.inflate(R.layout.list_bolt12_offer_item, parent, false);
        return new Bolt12OfferItemViewHolder(bolt12OfferItemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        Bolt12OfferItemViewHolder bolt12OfferItemViewHolder = (Bolt12OfferItemViewHolder) holder;
        Bolt12OfferListItem bolt12offerListItem = mSortedList.get(position);
        bolt12OfferItemViewHolder.bindBolt12OfferListItem(bolt12offerListItem);
        bolt12OfferItemViewHolder.addOnBolt12OfferSelectListener(mBolt12OfferSelectListener);
    }

    public void add(Bolt12OfferListItem bolt12offerListItem) {
        mSortedList.add(bolt12offerListItem);
    }

    public void remove(Bolt12OfferListItem bolt12offerListItem) {
        mSortedList.remove(bolt12offerListItem);
    }

    public void add(List<Bolt12OfferListItem> bolt12offerListItems) {
        mSortedList.addAll(bolt12offerListItems);
    }

    public void remove(List<Bolt12OfferListItem> bolt12offerListItems) {
        mSortedList.beginBatchedUpdates();
        for (Bolt12OfferListItem bolt12offerListItem : bolt12offerListItems) {
            mSortedList.remove(bolt12offerListItem);
        }
        mSortedList.endBatchedUpdates();
    }

    public void replaceAll(List<Bolt12OfferListItem> bolt12offerListItems) {
        mSortedList.replaceAll(bolt12offerListItems);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mSortedList.size();
    }
}
