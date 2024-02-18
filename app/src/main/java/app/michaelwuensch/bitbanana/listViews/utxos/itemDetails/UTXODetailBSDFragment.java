package app.michaelwuensch.bitbanana.listViews.utxos.itemDetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.lightningnetwork.lnd.lnrpc.Utxo;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.BlockExplorer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;

public class UTXODetailBSDFragment extends BaseBSDFragment {

    public static final String TAG = UTXODetailBSDFragment.class.getSimpleName();
    public static final String ARGS_UTXO = "UTXO";

    private BSDScrollableMainView mBSDScrollableMainView;
    private Utxo mUTXO;
    private TextView mAmountLabel;
    private AmountView mAmount;
    private TextView mTransactionIDLabel;
    private TextView mTransactionID;
    private ImageView mTransactionIDCopyButton;
    private TextView mAddressLabel;
    private TextView mAddress;
    private TextView mConfirmationsLabel;
    private TextView mConfirmations;
    private ImageView mAddressCopyButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_utxo_detail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mAmountLabel = view.findViewById(R.id.amountLabel);
        mAmount = view.findViewById(R.id.amount);
        mTransactionIDLabel = view.findViewById(R.id.transactionIDLabel);
        mTransactionID = view.findViewById(R.id.transactionID);
        mTransactionIDCopyButton = view.findViewById(R.id.txIDCopyIcon);
        mAddressLabel = view.findViewById(R.id.addressLabel);
        mAddress = view.findViewById(R.id.address);
        mAddressCopyButton = view.findViewById(R.id.addressCopyIcon);
        mConfirmationsLabel = view.findViewById(R.id.confirmationsLabel);
        mConfirmations = view.findViewById(R.id.confirmations);

        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            ByteString transactionString = (ByteString) getArguments().getSerializable(ARGS_UTXO);

            try {
                bindUTXO(transactionString);

            } catch (InvalidProtocolBufferException | NullPointerException exception) {
                BBLog.d(TAG, "Failed to parse utxo.");
                dismiss();
            }
        }

        return view;
    }


    private void bindUTXO(ByteString utxoString) throws InvalidProtocolBufferException {
        mUTXO = Utxo.parseFrom(utxoString);

        mBSDScrollableMainView.setTitle("UTXO");

        String amountLabel = getString(R.string.amount) + ":";
        mAmountLabel.setText(amountLabel);
        String transactionIDLabel = getString(R.string.transactionID) + ":";
        mTransactionIDLabel.setText(transactionIDLabel);
        String addressLabel = getString(R.string.address) + ":";
        mAddressLabel.setText(addressLabel);
        String confirmationsLabel = getString(R.string.confirmations) + ":";
        mConfirmationsLabel.setText(confirmationsLabel);

        mAmount.setAmountSat(mUTXO.getAmountSat());

        mAddress.setText(mUTXO.getAddress());
        mTransactionID.setText(mUTXO.getOutpoint().getTxidStr());

        mTransactionID.setOnClickListener(view -> new BlockExplorer().showTransaction(mUTXO.getOutpoint().getTxidStr(), getActivity()));
        mAddress.setOnClickListener(view -> new BlockExplorer().showAddress(mUTXO.getAddress(), getActivity()));
        mTransactionIDCopyButton.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "TransactionID", mUTXO.getOutpoint().getTxidStr()));
        mAddressCopyButton.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "Address", mUTXO.getAddress()));

        mConfirmations.setText(String.valueOf(mUTXO.getConfirmations()));
    }
}
