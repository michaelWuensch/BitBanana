package app.michaelwuensch.bitbanana.listViews.forwardings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ForwardingSummaryFragment extends Fragment {

    private static final String LOG_TAG = ForwardingSummaryFragment.class.getSimpleName();

    public static final int TYPE_AMOUNT_EARNED = 0;
    public static final int TYPE_ROUTED_VOLUME = 1;
    public static final int TYPE_AVG_EARNED = 2;
    public static final int TYPE_AVG_ROUTED = 3;
    public static final int TYPE_AVG_EVENTS_PER_DAY = 4;

    private TextView mTvSummaryText;
    private AmountView mAvAmount;
    private View mProgressIndicator;
    private int mType;

    public ForwardingSummaryFragment(int type) {
        mType = type;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_forwarding_summary, container, false);

        // Get View elements
        mProgressIndicator = view.findViewById(R.id.progressIndicator);
        mAvAmount = view.findViewById(R.id.amount);
        mTvSummaryText = view.findViewById(R.id.forwardingSummaryText);

        initView();

        return view;
    }

    private void initView() {
        switch (mType) {
            case TYPE_AMOUNT_EARNED:
                mTvSummaryText.setText(R.string.forwarding_earned_description);
                break;
            case TYPE_ROUTED_VOLUME:
                mTvSummaryText.setText(R.string.forwarding_volume_description);
                break;
            case TYPE_AVG_EARNED:
                mTvSummaryText.setText(R.string.forwarding_avg_earned_description);
                break;
            case TYPE_AVG_ROUTED:
                mTvSummaryText.setText(R.string.forwarding_avg_volume_description);
                break;
            default:
                mTvSummaryText.setText(R.string.forwarding_avg_events_per_day);
        }
    }

    public void setAmountSat(long sats) {
        if (mAvAmount != null)
            mAvAmount.setAmountSat(sats);
    }

    public void setAmountMSat(long msats) {
        if (mAvAmount != null)
            mAvAmount.setAmountMsat(msats);
    }

    public void overrideValue(String value) {
        if (mAvAmount != null) {
            mAvAmount.overrideWithText(value);
            mAvAmount.setSwitchValueOnClick(false);
        }
    }

    public void setInProgress(boolean inProgress) {
        if (inProgress) {
            if (mAvAmount != null)
                mAvAmount.setVisibility(View.GONE);
            if (mProgressIndicator != null)
                mProgressIndicator.setVisibility(View.VISIBLE);
        } else {
            if (mAvAmount != null)
                mAvAmount.setVisibility(View.VISIBLE);
            if (mProgressIndicator != null)
                mProgressIndicator.setVisibility(View.GONE);
        }
    }
}
