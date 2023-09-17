package app.michaelwuensch.bitbanana.coinControl;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class UTXOItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = UTXOItemViewHolder.class.getSimpleName();

    private TextView mUTXOAddress;
    private AmountView mUTXOAmount;
    private View mRootView;
    private UTXOSelectListener mUTXOSelectListener;
    private Context mContext;


    public UTXOItemViewHolder(View v) {
        super(v);

        mUTXOAddress = v.findViewById(R.id.utxoAddress);
        mUTXOAmount = v.findViewById(R.id.utxoAmount);
        mRootView = v.findViewById(R.id.utxoRootView);
        mContext = v.getContext();
    }

    public void bindUTXOListItem(UTXOListItem utxoListItem) {

        // Set utxo address
        mUTXOAddress.setText(utxoListItem.getUtxo().getAddress());

        // Set utxo amount

        mUTXOAmount.setAmount(utxoListItem.getUtxo().getAmountSat());

        // Set on click listener
        setOnRootViewClickListener(utxoListItem);
    }

    public void addOnUTXOSelectListener(UTXOSelectListener utxoSelectListener) {
        mUTXOSelectListener = utxoSelectListener;
    }

    void setOnRootViewClickListener(@NonNull UTXOListItem item) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mUTXOSelectListener != null) {
                    mUTXOSelectListener.onUtxoSelect(item.getUtxo().toByteString());
                }
            }
        });
    }
}
