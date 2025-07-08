package app.michaelwuensch.bitbanana.listViews.utxos.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.labels.LabelsManager;
import app.michaelwuensch.bitbanana.listViews.utxos.UTXOSelectListener;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class UTXOItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = UTXOItemViewHolder.class.getSimpleName();

    private View mUTXOContentView;
    private TextView mUTXOAddress;
    private AmountView mUTXOAmount;
    private View mRootView;
    private UTXOSelectListener mUTXOSelectListener;
    private ImageView mLeasedIcon;
    private Context mContext;
    private TextView mLabel;
    private UTXOListItem mUTXOListItem;


    public UTXOItemViewHolder(View v) {
        super(v);

        mLeasedIcon = v.findViewById(R.id.leasedIcon);
        mUTXOAddress = v.findViewById(R.id.utxoAddress);
        mUTXOAmount = v.findViewById(R.id.utxoAmount);
        mUTXOContentView = v.findViewById(R.id.utxoContent);
        mRootView = v.findViewById(R.id.utxoRootView);
        mLabel = v.findViewById(R.id.label);
        mContext = v.getContext();
    }

    public void bindUTXOListItem(UTXOListItem utxoListItem) {
        mUTXOListItem = utxoListItem;

        Utxo utxo = utxoListItem.getUtxo();

        // Update locked icon
        mLeasedIcon.setVisibility(utxo.isLeased() ? View.VISIBLE : View.GONE);

        // Set utxo address
        if (utxo.hasAddress())
            mUTXOAddress.setText(utxo.getAddress());
        else if (utxo.isLeased())
            mUTXOAddress.setText(R.string.locked);

        // Set utxo amount
        mUTXOAmount.setAmountMsat(utxo.getAmount());

        // Show unconfirmed as semitransparent
        if (utxo.isLeased())
            mUTXOContentView.setAlpha(1f);
        else
            mUTXOContentView.setAlpha(utxo.getConfirmations() == 0 ? 0.5f : 1f);

        // Label
        if (FeatureManager.isLabelsEnabled()) {
            String label = LabelsManager.getLabel(utxo);
            if (label != null) {
                mLabel.setText(label);
                mLabel.setVisibility(View.VISIBLE);
            } else {
                mLabel.setVisibility(View.GONE);
            }
        } else {
            mLabel.setVisibility(View.GONE);
        }

        // Set on click listener
        setOnRootViewClickListener(utxoListItem);
    }

    public void rebind() {
        bindUTXOListItem(mUTXOListItem);
    }

    public void addOnUTXOSelectListener(UTXOSelectListener utxoSelectListener) {
        mUTXOSelectListener = utxoSelectListener;
    }

    void setOnRootViewClickListener(@NonNull UTXOListItem item) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mUTXOSelectListener != null) {
                    mUTXOSelectListener.onUtxoSelect(item.getUtxo());
                }
            }
        });
    }
}
