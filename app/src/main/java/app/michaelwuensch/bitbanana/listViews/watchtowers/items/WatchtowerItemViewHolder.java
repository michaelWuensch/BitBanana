package app.michaelwuensch.bitbanana.listViews.watchtowers.items;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.watchtowers.WatchtowerSelectListener;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class WatchtowerItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = WatchtowerItemViewHolder.class.getSimpleName();

    private TextView mWatchtowerName;
    private View mRootView;
    private WatchtowerSelectListener mWatchtowerSelectListener;


    public WatchtowerItemViewHolder(View v) {
        super(v);

        mWatchtowerName = v.findViewById(R.id.watchtowerName);
        mRootView = v.findViewById(R.id.watchtowerRootView);
    }

    public void bindWatchtowerListItem(WatchtowerListItem watchtowerListItem) {

        // Set watchtower name
        mWatchtowerName.setText(watchtowerListItem.getAlias());

        // Set on click listener
        setOnRootViewClickListener(watchtowerListItem);
    }

    public void addOnWatchtowerSelectListener(WatchtowerSelectListener watchtowerSelectListener) {
        mWatchtowerSelectListener = watchtowerSelectListener;
    }

    void setOnRootViewClickListener(@NonNull WatchtowerListItem item) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mWatchtowerSelectListener != null) {
                    mWatchtowerSelectListener.onWatchtowerSelect(item.getWatchtower());
                }
            }
        });
    }
}
