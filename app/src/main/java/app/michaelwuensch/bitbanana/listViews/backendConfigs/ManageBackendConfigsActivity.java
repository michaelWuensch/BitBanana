package app.michaelwuensch.bitbanana.listViews.backendConfigs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.setup.SetupActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.backendConfigs.manageNodeConfigs.BBNodeConfig;
import app.michaelwuensch.bitbanana.backendConfigs.manageNodeConfigs.NodeConfigsManager;

public class ManageBackendConfigsActivity extends BaseAppCompatActivity {

    public static final String NODE_ID = "nodeUUID";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private List<BBNodeConfig> mNodeItems;
    private TextView mEmptyListText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_nodes);

        mRecyclerView = findViewById(R.id.nodesList);
        mEmptyListText = findViewById(R.id.listEmpty);

        mNodeItems = new ArrayList<>();

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(ManageBackendConfigsActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new BackendConfigItemAdapter(mNodeItems);
        mRecyclerView.setAdapter(mAdapter);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Add a new node
                Intent intent = new Intent(ManageBackendConfigsActivity.this, SetupActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNodeDisplayList();
    }

    private void updateNodeDisplayList() {

        mNodeItems.clear();
        NodeConfigsManager nodeConfigsManager = NodeConfigsManager.getInstance();
        mNodeItems.addAll(nodeConfigsManager.getAllNodeConfigs(false));

        // Show "No nodes" if the list is empty
        if (mNodeItems.size() == 0) {
            mEmptyListText.setVisibility(View.VISIBLE);
        } else {
            mEmptyListText.setVisibility(View.GONE);
        }

        // Update the list view
        mAdapter.notifyDataSetChanged();
    }

}
