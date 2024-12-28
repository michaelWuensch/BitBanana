package app.michaelwuensch.bitbanana.listViews.paymentRoute.items;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.models.LnHop;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.wallet.Wallet_NodesAndPeers;

public class HopListItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = HopListItemViewHolder.class.getSimpleName();

    private TextView mHopNumber;
    private TextView mHopName;
    private TextView mHopAction;
    private AmountView mAmount;
    private View mFeeLayout;
    private AmountView mFee;
    private View mLine;


    public HopListItemViewHolder(View v) {
        super(v);

        mHopNumber = v.findViewById(R.id.hopNumber);
        mHopName = v.findViewById(R.id.hopName);
        mHopAction = v.findViewById(R.id.hopAction);
        mAmount = v.findViewById(R.id.hopActionAmount);
        mFeeLayout = v.findViewById(R.id.hopFeeLayout);
        mFee = v.findViewById(R.id.hopFeeAmount);
        mLine = v.findViewById(R.id.hopLine);
    }

    public void bindHopListItem(HopListItem hopListItem) {

        LnHop hop = hopListItem.getHop();


        // Set hop number
        mHopNumber.setText(String.valueOf(hop.getIdInRoute() + 1));

        // Set hop name
        if (hop.getIdInRoute() == 0) {
            mHopName.setText(R.string.you);
        } else {
            updateHopName(hop.getPubKey());
            if (!AliasManager.getInstance().hasAliasInfo(hop.getPubKey())) {
                Wallet_NodesAndPeers.getInstance().fetchNodeInfo(hop.getPubKey(), false, true, new Wallet_NodesAndPeers.NodeInfoFetchedListener() {
                    @Override
                    public void onNodeInfoFetched(String pubkey) {
                        updateHopName(pubkey);
                    }
                });
            }
        }

        // Set hop action description
        if (hop.getIdInRoute() == 0) {
            mHopAction.setText(R.string.sent);
        } else {
            if (hop.getIsLastHop()) {
                mHopAction.setText(R.string.received);
            } else {
                mHopAction.setText(R.string.forwarded);
            }
        }

        // Set hop action amount
        mAmount.setAmountMsat(hop.getAmount());

        // Set fee
        if (hop.getIdInRoute() == 0 || hop.getIsLastHop()) {
            mFeeLayout.setVisibility(View.GONE);
        } else {
            mFeeLayout.setVisibility(View.VISIBLE);
            mFee.setAmountMsat(hop.getFee());
        }

        if (hop.getIsLastHop()) {
            mLine.setVisibility(View.GONE);
        } else {
            mLine.setVisibility(View.VISIBLE);
        }

    }

    private void updateHopName(String pubKey) {
        mHopName.setText(AliasManager.getInstance().getAlias(pubKey));
    }
}
