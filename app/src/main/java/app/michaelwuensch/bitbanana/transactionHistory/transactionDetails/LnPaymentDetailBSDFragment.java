package app.michaelwuensch.bitbanana.transactionHistory.transactionDetails;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.lightningnetwork.lnd.lnrpc.Hop;
import com.github.lightningnetwork.lnd.lnrpc.PayReqString;
import com.github.lightningnetwork.lnd.lnrpc.Payment;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.connection.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.fragments.BaseBSDFragment;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;

public class LnPaymentDetailBSDFragment extends BaseBSDFragment {

    public static final String TAG = LnPaymentDetailBSDFragment.class.getSimpleName();
    public static final String ARGS_TRANSACTION = "TRANSACTION";

    private BSDScrollableMainView mBSDScrollableMainView;
    private TextView mPayeeLabel;
    private TextView mPayee;
    private ImageView mPayeeCopyIcon;
    private TextView mAmountLabel;
    private AmountView mAmount;
    private TextView mMemoLabel;
    private TextView mMemo;
    private TextView mFeeLabel;
    private AmountView mFee;
    private TextView mDateLabel;
    private TextView mDate;
    private TextView mPreimageLabel;
    private TextView mPreimage;
    private ImageView mPreimageCopyIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_payment_detail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mPayeeLabel = view.findViewById(R.id.payeeLabel);
        mPayee = view.findViewById(R.id.payee);
        mPayeeCopyIcon = view.findViewById(R.id.payeeCopyIcon);
        mAmountLabel = view.findViewById(R.id.amountLabel);
        mAmount = view.findViewById(R.id.amount);
        mMemoLabel = view.findViewById(R.id.memoLabel);
        mMemo = view.findViewById(R.id.memo);
        mFeeLabel = view.findViewById(R.id.feeLabel);
        mFee = view.findViewById(R.id.fee);
        mDateLabel = view.findViewById(R.id.dateLabel);
        mDate = view.findViewById(R.id.date);
        mPreimageLabel = view.findViewById(R.id.preimageLabel);
        mPreimage = view.findViewById(R.id.preimage);
        mPreimageCopyIcon = view.findViewById(R.id.preimageCopyIcon);

        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            ByteString transactionString = (ByteString) getArguments().getSerializable(ARGS_TRANSACTION);
            try {
                bindPayment(transactionString);
            } catch (InvalidProtocolBufferException | NullPointerException exception) {
                Log.e(TAG, "Failed to parse payment.", exception);
                dismiss();
            }
        }
        return view;
    }

    private void bindPayment(ByteString transactionString) throws InvalidProtocolBufferException {

        Payment payment = Payment.parseFrom(transactionString);

        String payeeLabel = getString(R.string.payee) + ":";
        mPayeeLabel.setText(payeeLabel);
        String amountLabel = getString(R.string.amount) + ":";
        mAmountLabel.setText(amountLabel);
        String memoLabel = getString(R.string.memo) + ":";
        mMemoLabel.setText(memoLabel);
        String feeLabel = getString(R.string.fee) + ":";
        mFeeLabel.setText(feeLabel);
        String dateLabel = getString(R.string.date) + ":";
        mDateLabel.setText(dateLabel);
        String preimageLabel = getString(R.string.preimage) + ":";
        mPreimageLabel.setText(preimageLabel);

        mBSDScrollableMainView.setTitle(R.string.transaction_detail);

        Hop lastHop = payment.getHtlcs(0).getRoute().getHops(payment.getHtlcs(0).getRoute().getHopsCount() - 1);
        String payee = lastHop.getPubKey();

        mPayee.setText(ContactsManager.getInstance().getNameByContactData(payee));
        mPayeeCopyIcon.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "Payee", payee));
        mAmount.setAmountSat(payment.getValue());
        mFee.setAmountSat(payment.getFee());
        mPreimage.setText(payment.getPaymentPreimage());
        mPreimageCopyIcon.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "Payment Preimage", payment.getPaymentPreimage()));
        mDate.setText(TimeFormatUtil.formatTimeAndDateLong(payment.getCreationDate(), getActivity()));


        if (!payment.getPaymentRequest().isEmpty()) {
            // This will only be true for payments done with LND 0.7.0-beta and later
            decodeLightningInvoice(payment.getPaymentRequest());
        } else {
            mMemo.setVisibility(View.GONE);
            mMemoLabel.setVisibility(View.GONE);
        }
    }

    private void decodeLightningInvoice(String invoice) {
        PayReqString decodePaymentRequest = PayReqString.newBuilder()
                .setPayReq(invoice)
                .build();

        getCompositeDisposable().add(LndConnection.getInstance().getLightningService().decodePayReq(decodePaymentRequest)
                .subscribe(paymentRequest -> {
                    if (!paymentRequest.getDescription().isEmpty()) {
                        // Set description
                        mMemo.setText(paymentRequest.getDescription());
                    } else {
                        mMemo.setVisibility(View.GONE);
                        mMemoLabel.setVisibility(View.GONE);
                    }
                }, throwable -> BBLog.d(TAG, "Decode payment request failed: " + throwable.fillInStackTrace())));
    }
}
