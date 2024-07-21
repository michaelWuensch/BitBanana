package app.michaelwuensch.bitbanana.listViews.watchtowerSessions.items;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.listViews.watchtowerSessions.WatchtowerSessionSelectListener;
import app.michaelwuensch.bitbanana.models.WatchtowerSession;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class WatchtowerSessionItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = WatchtowerSessionItemViewHolder.class.getSimpleName();

    private View mRootView;
    private View mContentView;
    private TextView mTvSessionId;
    private TextView mTvState;
    private TextView mTvBackupsLabel;
    private TextView mTvSweepFeeLabel;
    private TextView mTvBackups;
    private TextView mTvSweepFee;
    private WatchtowerSessionSelectListener mWatchtowerSessionSelectListener;


    public WatchtowerSessionItemViewHolder(View v) {
        super(v);

        mRootView = v.findViewById(R.id.sessionRootView);
        mContentView = v.findViewById(R.id.sessionContent);
        mTvSessionId = v.findViewById(R.id.sessionId);
        mTvState = v.findViewById(R.id.sessionState);
        mTvBackupsLabel = v.findViewById(R.id.backupsLabel);
        mTvSweepFeeLabel = v.findViewById(R.id.sessionSweepFeeLabel);
        mTvBackups = v.findViewById(R.id.backups);
        mTvSweepFee = v.findViewById(R.id.sessionSweepFee);
    }

    public void bindWatchtowerSessionListItem(WatchtowerSessionListItem watchtowerSessionListItem) {

        WatchtowerSession session = watchtowerSessionListItem.getWatchtowerSession();

        mTvSessionId.setText(session.getId());

        // State
        if (session.getIsTerminated()) {
            mTvState.setText(App.getAppContext().getString(R.string.terminated));
            mTvState.setTextColor(App.getAppContext().getResources().getColor(R.color.red));
            mTvState.setVisibility(View.VISIBLE);
        } else if (session.getIsExhausted()) {
            mTvState.setText(App.getAppContext().getString(R.string.exhausted));
            mTvState.setTextColor(App.getAppContext().getResources().getColor(R.color.red));
            mTvState.setVisibility(View.VISIBLE);
        } else {
            mTvState.setVisibility(View.GONE);
        }

        String backupsLabel = App.getAppContext().getString(R.string.watchtower_session_backups) + ":";
        mTvBackupsLabel.setText(backupsLabel);
        String sweepFeeLabel = App.getAppContext().getString(R.string.watchtower_session_sweep_fee) + ":";
        mTvSweepFeeLabel.setText(sweepFeeLabel);

        mTvBackups.setText(session.getNumBackups() + "/" + session.getNumMaxBackups());
        mTvSweepFee.setText(String.valueOf(session.getSweepSatPerVByte()));

        // Set on click listener
        setOnRootViewClickListener(watchtowerSessionListItem);
    }

    public void addOnWatchtowerSessionSelectListener(WatchtowerSessionSelectListener watchtowerSessionSelectListener) {
        mWatchtowerSessionSelectListener = watchtowerSessionSelectListener;
    }

    void setOnRootViewClickListener(@NonNull WatchtowerSessionListItem item) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mWatchtowerSessionSelectListener != null) {
                    mWatchtowerSessionSelectListener.onWatchtowerSessionSelect(item.getWatchtowerSession());
                }
            }
        });
    }
}
