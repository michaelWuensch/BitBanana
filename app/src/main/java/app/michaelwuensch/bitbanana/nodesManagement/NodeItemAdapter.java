package app.michaelwuensch.bitbanana.nodesManagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.BBNodeConfig;


public class NodeItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<BBNodeConfig> mItems;

    // Construct the adapter with a data list
    public NodeItemAdapter(List<BBNodeConfig> dataset) {
        mItems = dataset;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View nodeItemView = inflater.inflate(R.layout.node_list_element, parent, false);
        return new NodeItemViewHolder(nodeItemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        NodeItemViewHolder nodeViewHolder = (NodeItemViewHolder) holder;
        BBNodeConfig remoteNodeItem = mItems.get(position);
        nodeViewHolder.bindRemoteNodeItem(remoteNodeItem);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mItems.size();
    }
}
