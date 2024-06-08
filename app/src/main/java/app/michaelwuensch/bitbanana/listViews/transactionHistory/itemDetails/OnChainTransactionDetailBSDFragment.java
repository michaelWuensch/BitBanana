package app.michaelwuensch.bitbanana.listViews.transactionHistory.itemDetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BlockExplorer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.util.WalletUtil;

public class OnChainTransactionDetailBSDFragment extends BaseBSDFragment {

    public static final String TAG = OnChainTransactionDetailBSDFragment.class.getSimpleName();
    public static final String ARGS_TRANSACTION = "TRANSACTION";

    private BSDScrollableMainView mBSDScrollableMainView;
    private OnChainTransaction mTransaction;
    private TextView mNodeLabel;
    private TextView mNode;
    private TextView mEventLabel;
    private TextView mEvent;
    private TextView mAmountLabel;
    private AmountView mAmount;
    private TextView mFeeLabel;
    private AmountView mFee;
    private TextView mDateLabel;
    private TextView mDate;
    private TextView mTransactionIDLabel;
    private TextView mTransactionID;
    private ImageView mTransactionIDCopyButton;
    private TextView mAddressLabel;
    private TextView mAddress;
    private TextView mConfrimationsLabel;
    private TextView mConfirmations;
    private ImageView mAddressCopyButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_on_chain_transaction_detail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mNodeLabel = view.findViewById(R.id.nodeLabel);
        mNode = view.findViewById(R.id.node);
        mEventLabel = view.findViewById(R.id.eventLabel);
        mEvent = view.findViewById(R.id.event);
        mAmountLabel = view.findViewById(R.id.amountLabel);
        mAmount = view.findViewById(R.id.amount);
        mFeeLabel = view.findViewById(R.id.feeLabel);
        mFee = view.findViewById(R.id.fee);
        mDateLabel = view.findViewById(R.id.dateLabel);
        mDate = view.findViewById(R.id.date);
        mTransactionIDLabel = view.findViewById(R.id.transactionIDLabel);
        mTransactionID = view.findViewById(R.id.transactionID);
        mTransactionIDCopyButton = view.findViewById(R.id.txIDCopyIcon);
        mAddressLabel = view.findViewById(R.id.addressLabel);
        mAddress = view.findViewById(R.id.address);
        mAddressCopyButton = view.findViewById(R.id.addressCopyIcon);
        mConfrimationsLabel = view.findViewById(R.id.confirmationsLabel);
        mConfirmations = view.findViewById(R.id.confirmations);

        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            bindOnChainTransaction((OnChainTransaction) getArguments().getSerializable(ARGS_TRANSACTION));
        }

        return view;
    }


    private void bindOnChainTransaction(OnChainTransaction transaction) {
        mTransaction = transaction;

        String nodeLabel = getString(R.string.node) + ":";
        mNodeLabel.setText(nodeLabel);
        String eventLabel = getString(R.string.event) + ":";
        mEventLabel.setText(eventLabel);
        String amountLabel = getString(R.string.amount) + ":";
        mAmountLabel.setText(amountLabel);
        String feeLabel = getString(R.string.fee) + ":";
        mFeeLabel.setText(feeLabel);
        String dateLabel = getString(R.string.date) + ":";
        mDateLabel.setText(dateLabel);
        String transactionIDLabel = getString(R.string.transactionID) + ":";
        mTransactionIDLabel.setText(transactionIDLabel);
        String addressLabel = getString(R.string.address) + ":";
        mAddressLabel.setText(addressLabel);
        String confirmationsLabel = getString(R.string.confirmations) + ":";
        mConfrimationsLabel.setText(confirmationsLabel);

        mDate.setText(TimeFormatUtil.formatTimeAndDateLong(mTransaction.getTimeStamp(), getActivity()));


        mTransactionID.setText(mTransaction.getTransactionId());

        mTransactionID.setOnClickListener(view -> new BlockExplorer().showTransaction(mTransaction.getTransactionId(), getActivity()));
        //mAddress.setOnClickListener(view -> new BlockExplorer().showAddress(mTransaction.getDestAddresses(0), getActivity()));
        mTransactionIDCopyButton.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "TransactionID", mTransaction.getTransactionId()));
        //mAddressCopyButton.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "Address", mTransaction.getDestAddresses(0)));
        if (mTransaction.getConfirmations() < 7) {
            mConfirmations.setText(String.valueOf(mTransaction.getConfirmations()));
        } else {
            mConfirmations.setText("6+");
        }


        // is internal?
        if (WalletUtil.isChannelTransaction(mTransaction)) {
            bindInternal();
        } else {
            bindNormalTransaction();
        }
    }

    private void bindInternal() {
        mBSDScrollableMainView.setTitle(R.string.channel_event);
        Long amount = mTransaction.getAmount();

        mAddress.setVisibility(View.GONE);
        mAddressLabel.setVisibility(View.GONE);
        mAddressCopyButton.setVisibility(View.GONE);
        mAmount.setVisibility(View.GONE);
        mAmountLabel.setVisibility(View.GONE);

        if (mTransaction.getFee() > 0) {
            mFee.setAmountMsat(mTransaction.getFee());
        } else {
            mFee.setVisibility(View.GONE);
            mFeeLabel.setVisibility(View.GONE);
        }

        String alias = AliasManager.getInstance().getAlias(WalletUtil.getNodePubKeyFromChannelTransaction(mTransaction));
        mNode.setText(alias);

        switch (amount.compareTo(0L)) {
            case 0:
                // amount = 0
                mEvent.setText(R.string.force_closed_channel);
                break;
            case 1:
                // amount > 0 (Channel closed)
                mEvent.setText(R.string.closed_channel);
                break;
            case -1:
                if (mTransaction.hasLabel() && mTransaction.getLabel().toLowerCase().contains("sweep")) {
                    // in some rare cases for sweep transactions the value is actually the fee payed for the sweep.
                    mEvent.setText(R.string.closed_channel);
                } else {
                    // amount < 0 (Channel opened)
                    mEvent.setText(R.string.opened_channel);
                }
                break;
        }
    }

    private void bindNormalTransaction() {
        mBSDScrollableMainView.setTitle(R.string.transaction_detail);
        mNode.setVisibility(View.GONE);
        mNodeLabel.setVisibility(View.GONE);
        mEvent.setVisibility(View.GONE);
        mEventLabel.setVisibility(View.GONE);
        mAmount.setAmountMsat(Math.abs(mTransaction.getAmount()));

        Long amount = mTransaction.getAmount();

        switch (amount.compareTo(0L)) {
            case 0:
                // amount = 0 (should actually not happen)
                mFee.setAmountMsat(mTransaction.getFee());
                break;
            case 1:
                // amount > 0 (received on-chain)
                mAmount.setAmountMsat(Math.abs(mTransaction.getAmount()));
                mFee.setVisibility(View.GONE);
                mFeeLabel.setVisibility(View.GONE);
                break;
            case -1:
                // amount < 0 (sent on-chain)
                if (BackendManager.getCurrentBackendType() == BackendConfig.BackendType.LND_GRPC)  // ToDo: do it the correct way for lnd
                    mAmount.setAmountMsat(Math.abs(mTransaction.getAmount() + mTransaction.getFee()));
                else
                    mAmount.setAmountMsat(Math.abs(mTransaction.getAmount()));
                mFee.setAmountMsat(mTransaction.getFee());
                break;
        }
    }
}
