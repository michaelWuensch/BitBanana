package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;

public class BBSelectedUtxo extends LinearLayout {

    private static final String LOG_TAG = BBSelectedUtxo.class.getSimpleName();

    private TextView mAddress;
    private AmountView mAmount;


    public BBSelectedUtxo(Context context) {
        super(context);
        init(context, null);
    }

    public BBSelectedUtxo(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBSelectedUtxo(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.view_selected_utxo, this);

        mAddress = view.findViewById(R.id.utxoAddress);
        mAmount = view.findViewById(R.id.utxoAmount);

    }

    public void setData(String address, long amount) {
        mAddress.setText(address);
        mAmount.setAmountMsat(amount);
    }
}
