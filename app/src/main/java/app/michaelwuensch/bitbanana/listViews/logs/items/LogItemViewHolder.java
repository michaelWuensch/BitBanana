package app.michaelwuensch.bitbanana.listViews.logs.items;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.logs.LogSelectListener;
import app.michaelwuensch.bitbanana.models.BBLogItem;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class LogItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = LogItemViewHolder.class.getSimpleName();

    private TextView mLogMessage;
    private View mContent;
    private View mRootView;
    private LogSelectListener mLogSelectListener;


    public LogItemViewHolder(View v) {
        super(v);
        mLogMessage = v.findViewById(R.id.logMessage);
        mContent = v.findViewById(R.id.logContent);
        mRootView = v.findViewById(R.id.logRootView);
    }

    public void bindLogListItem(LogListItem logListItem, int position) {
        /*
        if (position % 2 == 0) {
            mContent.setBackground(mLogMessage.getResources().getDrawable(R.drawable.bg_clickable_item));
        } else {
            mContent.setBackground(mLogMessage.getResources().getDrawable(R.drawable.bg_clickable_item_dark));
        }
         */

        BBLogItem logItem = logListItem.getLogItem();
        mLogMessage.setText(logItem.getTag() + " | " + logItem.getMessage());
        switch (logItem.getVerbosity()) {
            case VERBOSE:
                mLogMessage.setTextColor(mLogMessage.getResources().getColor(R.color.gray));
                break;
            case DEBUG:
                mLogMessage.setTextColor(mLogMessage.getResources().getColor(R.color.white));
                break;
            case INFO:
                mLogMessage.setTextColor(mLogMessage.getResources().getColor(R.color.blue_gradient));
                break;
            case WARNING:
                mLogMessage.setTextColor(mLogMessage.getResources().getColor(R.color.banana_yellow));
                break;
            case ERROR:
                mLogMessage.setTextColor(mLogMessage.getResources().getColor(R.color.red));
                break;
        }

        // Set on click listener
        setOnRootViewClickListener(logListItem);
    }

    public void addOnLogSelectListener(LogSelectListener logSelectListener) {
        mLogSelectListener = logSelectListener;
    }

    void setOnRootViewClickListener(@NonNull LogListItem item) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mLogSelectListener != null) {
                    mLogSelectListener.onLogSelect(item.getLogItem());
                }
            }
        });
    }
}
