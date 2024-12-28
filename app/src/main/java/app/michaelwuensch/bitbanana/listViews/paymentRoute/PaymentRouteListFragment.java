package app.michaelwuensch.bitbanana.listViews.paymentRoute;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.paymentRoute.items.HopListItem;


/**
 * A simple {@link Fragment} subclass.
 */
public class PaymentRouteListFragment extends Fragment {

    private static final String LOG_TAG = PaymentRouteListFragment.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private PaymentRouteItemAdapter mAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recyclerview_list, container, false);

        // Get View elements
        mRecyclerView = view.findViewById(R.id.recyclerList);

        mAdapter = new PaymentRouteItemAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    public void replaceAllItems(List<HopListItem> items) {
        if (mAdapter != null) {
            mAdapter.replaceAll(items);
        }
    }

    public void scrollToPosition(int pos) {
        mRecyclerView.scrollToPosition(pos);
    }
}
