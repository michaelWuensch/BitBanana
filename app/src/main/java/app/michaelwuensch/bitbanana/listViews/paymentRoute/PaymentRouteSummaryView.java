package app.michaelwuensch.bitbanana.listViews.paymentRoute;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;

public class PaymentRouteSummaryView extends LinearLayout {

    private TextView mTvPath;
    private AmountView mAvAmount;
    private AmountView mAvFee;

    public PaymentRouteSummaryView(Context context) {
        super(context);
        init();
    }

    public PaymentRouteSummaryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PaymentRouteSummaryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_route_summary, this);

        mTvPath = view.findViewById(R.id.path);
        mAvAmount = view.findViewById(R.id.amount);
        mAvFee = view.findViewById(R.id.fee);
    }

    public void updateSummary(int pathNumber, int pathsCount, long amount, long fee) {
        mTvPath.setText(pathNumber + "/" + pathsCount);
        mAvAmount.setAmountMsat(amount);
        mAvFee.setAmountMsat(fee);
    }
}
