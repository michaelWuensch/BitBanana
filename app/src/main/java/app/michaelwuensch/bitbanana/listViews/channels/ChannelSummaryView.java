package app.michaelwuensch.bitbanana.listViews.channels;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;

public class ChannelSummaryView extends LinearLayout {

    private AmountView mTVTotalOutbound;
    private AmountView mTVTotalIntbound;
    private AmountView mTVTotalUnavailable;

    public ChannelSummaryView(Context context) {
        super(context);
        init();
    }

    public ChannelSummaryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChannelSummaryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_channel_summary, this);

        mTVTotalIntbound = view.findViewById(R.id.totalInbound);
        mTVTotalOutbound = view.findViewById(R.id.totalOutbound);
        mTVTotalUnavailable = view.findViewById(R.id.totalUnavailable);
    }

    public void updateBalances(long outbound, long inbound, long unavailable) {
        mTVTotalOutbound.setAmountSat(outbound);
        mTVTotalIntbound.setAmountSat(inbound);
        mTVTotalUnavailable.setAmountSat(unavailable);
    }
}
