package app.michaelwuensch.bitbanana.listViews.transactionHistory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.DateItem;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.DateLineViewHolder;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.HistoryListItem;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.LnInvoiceItem;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.LnInvoiceViewHolder;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.LnPaymentItem;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.LnPaymentViewHolder;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.OnChainTransactionItem;
import app.michaelwuensch.bitbanana.listViews.transactionHistory.items.OnChainTransactionViewHolder;
import io.reactivex.rxjava3.disposables.CompositeDisposable;


public class HistoryItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private TransactionSelectListener mTransactionSelectListener;
    private CompositeDisposable mCompositeDisposable;

    private final SortedList<HistoryListItem> mSortedList = new SortedList<>(HistoryListItem.class, new SortedList.Callback<HistoryListItem>() {
        @Override
        public int compare(HistoryListItem i1, HistoryListItem i2) {
            return i1.compareTo(i2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(HistoryListItem oldItem, HistoryListItem newItem) {
            return oldItem.equalsWithSameContent(newItem);
        }

        @Override
        public boolean areItemsTheSame(HistoryListItem item1, HistoryListItem item2) {
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

    public HistoryItemAdapter(TransactionSelectListener transactionSelectListener, CompositeDisposable compositeDisposable) {
        mTransactionSelectListener = transactionSelectListener;
        mCompositeDisposable = compositeDisposable;
    }

    @Override
    public int getItemViewType(int position) {
        return mSortedList.get(position).getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case HistoryListItem.TYPE_DATE:
                View dateView = inflater.inflate(R.layout.list_element_date_line, parent, false);
                return new DateLineViewHolder(dateView);
            case HistoryListItem.TYPE_ON_CHAIN_TRANSACTION:
                View transactionView = inflater.inflate(R.layout.history_list_element_transaction, parent, false);
                return new OnChainTransactionViewHolder(transactionView);
            case HistoryListItem.TYPE_LN_INVOICE:
                View lnTransactionView = inflater.inflate(R.layout.history_list_element_transaction, parent, false);
                return new LnInvoiceViewHolder(lnTransactionView);
            case HistoryListItem.TYPE_LN_PAYMENT:
                View lnPaymentView = inflater.inflate(R.layout.history_list_element_transaction, parent, false);
                return new LnPaymentViewHolder(lnPaymentView);
            default:
                throw new IllegalStateException("Unknown history list item type: " + viewType);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        switch (type) {
            case HistoryListItem.TYPE_DATE:
                DateLineViewHolder dateHolder = (DateLineViewHolder) holder;
                DateItem dateItem = (DateItem) mSortedList.get(position);
                dateHolder.bindDateItem(dateItem);
                break;
            case HistoryListItem.TYPE_ON_CHAIN_TRANSACTION:
                OnChainTransactionViewHolder onChainTransactionHolder = (OnChainTransactionViewHolder) holder;
                OnChainTransactionItem onChainTransactionItem = (OnChainTransactionItem) mSortedList.get(position);
                onChainTransactionHolder.bindOnChainTransactionItem(onChainTransactionItem);
                onChainTransactionHolder.addOnTransactionSelectListener(mTransactionSelectListener);
                break;
            case HistoryListItem.TYPE_LN_INVOICE:
                LnInvoiceViewHolder lnInvoiceHolder = (LnInvoiceViewHolder) holder;
                LnInvoiceItem lnInvoiceItem = (LnInvoiceItem) mSortedList.get(position);
                lnInvoiceHolder.bindLnInvoiceItem(lnInvoiceItem);
                lnInvoiceHolder.addOnTransactionSelectListener(mTransactionSelectListener);
                break;
            case HistoryListItem.TYPE_LN_PAYMENT:
                LnPaymentViewHolder lnPaymentHolder = (LnPaymentViewHolder) holder;
                LnPaymentItem lnPaymentItem = (LnPaymentItem) mSortedList.get(position);
                lnPaymentHolder.bindLnPaymentItem(lnPaymentItem);
                lnPaymentHolder.addOnTransactionSelectListener(mTransactionSelectListener);
                break;
            default:
                throw new IllegalStateException("Unknown history list item type: " + type);
        }
    }

    public void replaceAll(List<HistoryListItem> items) {
        mSortedList.replaceAll(items);
    }

    public void add(HistoryListItem item) {
        mSortedList.add(item);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mSortedList.size();
    }
}
