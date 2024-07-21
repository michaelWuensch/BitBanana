package app.michaelwuensch.bitbanana.listViews.watchtowerSessions.itemDetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.models.WatchtowerSession;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;

public class SessionDetailBSDFragment extends BaseBSDFragment {

    public static final String TAG = SessionDetailBSDFragment.class.getSimpleName();
    public static final String ARGS_SESSION = "SESSION";

    private BSDScrollableMainView mBSDScrollableMainView;
    private TextView mIdLabel;
    private TextView mId;
    private ImageView mIdCopyIcon;
    private TextView mTypeLabel;
    private TextView mType;
    private TextView mStateLabel;
    private TextView mState;
    private TextView mBackupsLabel;
    private TextView mBackups;
    private TextView mPendingBackupsLabel;
    private TextView mPendingBackups;
    private TextView mMaxBackupsLabel;
    private TextView mMaxBackups;
    private TextView mSweepFeeLabel;
    private TextView mSweepFee;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_watchtower_session_detail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mIdLabel = view.findViewById(R.id.idLabel);
        mId = view.findViewById(R.id.id);
        mIdCopyIcon = view.findViewById(R.id.idCopyIcon);
        mTypeLabel = view.findViewById(R.id.typeLabel);
        mType = view.findViewById(R.id.type);
        mStateLabel = view.findViewById(R.id.stateLabel);
        mState = view.findViewById(R.id.state);
        mBackupsLabel = view.findViewById(R.id.backupsLabel);
        mBackups = view.findViewById(R.id.backups);
        mPendingBackupsLabel = view.findViewById(R.id.pendingBackupsLabel);
        mPendingBackups = view.findViewById(R.id.pendingBackups);
        mMaxBackupsLabel = view.findViewById(R.id.maxBackupsLabel);
        mMaxBackups = view.findViewById(R.id.maxBackups);
        mSweepFeeLabel = view.findViewById(R.id.sweepFeeLabel);
        mSweepFee = view.findViewById(R.id.sweepFee);

        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            bindSession((WatchtowerSession) getArguments().getSerializable(ARGS_SESSION));
        }
        return view;
    }

    private void bindSession(WatchtowerSession session) {


        String idLabel = getString(R.string.id) + ":";
        mIdLabel.setText(idLabel);
        String typeLabel = getString(R.string.type) + ":";
        mTypeLabel.setText(typeLabel);
        String stateLabel = getString(R.string.state) + ":";
        mStateLabel.setText(stateLabel);
        String backupsLabel = getString(R.string.watchtower_session_backups) + ":";
        mBackupsLabel.setText(backupsLabel);
        String pendingBackupsLabel = getString(R.string.watchtower_session_pending_backups) + ":";
        mPendingBackupsLabel.setText(pendingBackupsLabel);
        String maxBackupsLabel = getString(R.string.watchtower_session_max_backups) + ":";
        mMaxBackupsLabel.setText(maxBackupsLabel);
        String sweepFeeLabel = getString(R.string.watchtower_session_sweep_fee) + ":";
        mSweepFeeLabel.setText(sweepFeeLabel);

        mBSDScrollableMainView.setTitle(R.string.watchtower_session_details);


        mId.setText(session.getId());
        mIdCopyIcon.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "SessionID", session.getId()));

        mType.setText(session.getType().name());

        // State
        if (session.getIsTerminated()) {
            mState.setText(getString(R.string.terminated));
            mState.setTextColor(getResources().getColor(R.color.red));
            mState.setVisibility(View.VISIBLE);
            mStateLabel.setVisibility(View.VISIBLE);
        } else if (session.getIsExhausted()) {
            mState.setText(getString(R.string.exhausted));
            mState.setTextColor(getResources().getColor(R.color.red));
            mState.setVisibility(View.VISIBLE);
            mStateLabel.setVisibility(View.VISIBLE);
        } else {
            mState.setVisibility(View.GONE);
            mStateLabel.setVisibility(View.GONE);
        }

        mBackups.setText(String.valueOf(session.getNumBackups()));
        mPendingBackups.setText(String.valueOf(session.getNumPendingBackups()));
        mMaxBackups.setText(String.valueOf(session.getNumMaxBackups()));
        mSweepFee.setText(session.getSweepSatPerVByte() + " sat/vByte");
    }
}
