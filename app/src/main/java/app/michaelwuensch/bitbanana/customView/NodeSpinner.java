package app.michaelwuensch.bitbanana.customView;


import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.AppCompatSpinner;

import app.michaelwuensch.bitbanana.listViews.backendConfigs.ManageBackendConfigsActivity;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;

public class NodeSpinner extends AppCompatSpinner {

    private boolean initFinished;

    public NodeSpinner(Context context) {
        super(context);
        init();
    }

    public NodeSpinner(Context context, int mode) {
        super(context, mode);
        init();
    }

    public NodeSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NodeSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public NodeSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
        init();
    }

    public NodeSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode, Resources.Theme popupTheme) {
        super(context, attrs, defStyleAttr, mode, popupTheme);
        init();
    }

    private OnNodeSpinnerChangedListener mListener;

    private void init() {
        updateList();

        this.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if (initFinished) {
                    int lastPos = adapterView.getCount() - 1;
                    if (position != lastPos) {
                        String selectedNodeId = BackendConfigsManager.getInstance().getAllBackendConfigs(true).get(position).getId();
                        if (!BackendConfigsManager.getInstance().getCurrentBackendConfig().getId().equals(selectedNodeId)) {
                            // Save selected Node ID in prefs making it the current node.
                            PrefsUtil.editPrefs().putString(PrefsUtil.CURRENT_BACKEND_CONFIG, selectedNodeId).commit();

                            // Update the node spinner list, so everything is at it's correct position again.
                            updateList();

                            // Inform the listener. This is where the new node is opened.
                            mListener.onNodeChanged(selectedNodeId, BackendConfigsManager.getInstance().getBackendConfigById(selectedNodeId).getAlias());
                        }
                    } else {
                        // Open node management
                        Intent intent = new Intent(getContext(), ManageBackendConfigsActivity.class);
                        getContext().startActivity(intent);

                        // If going back we don't want to have "Manage.." selected
                        adapterView.setSelection(0);
                    }
                } else {
                    // When filling the list onItem Selected ist called for the first time.
                    // In this case we don't want to select something, but mark it ready for interaction instead.
                    initFinished = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public boolean performClick() {
        updateList();
        return super.performClick();
    }

    public void updateList() {

        initFinished = false;

        String[] items = new String[BackendConfigsManager.getInstance().getAllBackendConfigs(true).size() + 1];
        for (int i = 0; i < BackendConfigsManager.getInstance().getAllBackendConfigs(true).size(); i++) {
            items[i] = BackendConfigsManager.getInstance().getAllBackendConfigs(true).get(i).getAlias();
        }
        items[items.length - 1] = getContext().getResources().getString(R.string.spinner_manage_nodes);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.node_spinner_item, items);

        //set the spinners adapter to the previously created one.
        this.setAdapter(adapter);
    }

    public interface OnNodeSpinnerChangedListener {
        void onNodeChanged(String id, String alias);
    }

    public void setOnNodeSpinnerChangedListener(OnNodeSpinnerChangedListener listener) {
        mListener = listener;
    }
}
