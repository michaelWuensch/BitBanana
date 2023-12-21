package app.michaelwuensch.bitbanana.listViews.channels;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.protobuf.ByteString;

import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.channels.items.ChannelListItem;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChannelListFragment extends Fragment implements ChannelSelectListener {

    private static final String LOG_TAG = ChannelListFragment.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private ChannelItemAdapter mAdapter;
    private ChannelSelectListener mChannelSelectListener;

    public void setChannelSelectListener (ChannelSelectListener channelSelectListener) {
        mChannelSelectListener = channelSelectListener;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_channel_list, container, false);

        // Get View elements
        mRecyclerView = view.findViewById(R.id.channelList);

        mAdapter = new ChannelItemAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    public void replaceAllItems(List<ChannelListItem> items) {
        if (mAdapter != null) {
            mAdapter.replaceAll(items);
        }
    }

    public void scrollToPosition(int pos) {
        mRecyclerView.scrollToPosition(pos);
    }

    @Override
    public void onChannelSelect(ByteString channel, int type) {
        if (mChannelSelectListener != null) {
            mChannelSelectListener.onChannelSelect(channel, type);
        }
    }
}
