package app.michaelwuensch.bitbanana.transactionHistory.transactionDetails;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.utils.widget.ImageFilterView;

import com.github.lightningnetwork.lnd.lnrpc.Invoice;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.fragments.BaseBSDFragment;
import app.michaelwuensch.bitbanana.qrCodeGen.QRCodeGenerator;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;
import app.michaelwuensch.bitbanana.util.Wallet;

public class InvoiceDetailBSDFragment extends BaseBSDFragment {

    public static final String TAG = InvoiceDetailBSDFragment.class.getSimpleName();
    public static final String ARGS_TRANSACTION = "TRANSACTION";

    private BSDScrollableMainView mBSDScrollableMainView;
    private TextView mAmountLabel;
    private AmountView mAmount;
    private TextView mMemoLabel;
    private TextView mMemo;
    private TextView mDateLabel;
    private TextView mDate;
    private TextView mExpiryLabel;
    private TextView mExpiry;
    private ImageFilterView mQRCodeView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_invoice_detail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mAmountLabel = view.findViewById(R.id.amountLabel);
        mAmount = view.findViewById(R.id.amount);
        mMemoLabel = view.findViewById(R.id.memoLabel);
        mMemo = view.findViewById(R.id.memo);
        mDateLabel = view.findViewById(R.id.dateLabel);
        mDate = view.findViewById(R.id.date);
        mExpiryLabel = view.findViewById(R.id.expiryLabel);
        mExpiry = view.findViewById(R.id.expiry);
        mQRCodeView = view.findViewById(R.id.requestQRCode);

        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            ByteString transactionString = (ByteString) getArguments().getSerializable(ARGS_TRANSACTION);
            try {
                bindInvoice(transactionString);
            } catch (InvalidProtocolBufferException | NullPointerException exception) {
                BBLog.d(TAG, "Failed to parse invoice.");
                dismiss();
            }
        }

        return view;
    }


    private void bindInvoice(ByteString transactionString) throws InvalidProtocolBufferException {

        Invoice invoice = Invoice.parseFrom(transactionString);

        String amountLabel = getString(R.string.amount) + ":";
        mAmountLabel.setText(amountLabel);
        String memoLabel = getString(R.string.memo) + ":";
        mMemoLabel.setText(memoLabel);
        String dateLabel = getString(R.string.date) + ":";
        mDateLabel.setText(dateLabel);
        String expiryLabel = getString(R.string.expiry) + ":";
        mExpiryLabel.setText(expiryLabel);

        mDate.setText(TimeFormatUtil.formatTimeAndDateLong(invoice.getCreationDate(), getActivity()));

        if (invoice.getMemo().isEmpty()) {
            mMemo.setVisibility(View.GONE);
            mMemoLabel.setVisibility(View.GONE);
        } else {
            mMemo.setText(invoice.getMemo());
        }

        Long invoiceAmount = invoice.getValue();
        Long amountPayed = invoice.getAmtPaidSat();

        if (invoiceAmount.equals(0L)) {
            // if no specific value was requested
            if (!amountPayed.equals(0L)) {
                // The invoice has been payed
                bindPayedInvoice(invoice);
            } else {
                // The invoice has not been payed yet
                if (Wallet.getInstance().isInvoiceExpired(invoice)) {
                    bindExpiredInvoice(invoice);
                } else {
                    // The invoice has not yet expired
                    bindOpenInvoice(invoice);
                }
            }
        } else {
            // if a specific value was requested
            if (Wallet.getInstance().isInvoicePayed(invoice)) {
                // The invoice has been payed
                bindPayedInvoice(invoice);
            } else {
                // The invoice has not been payed yet
                if (Wallet.getInstance().isInvoiceExpired(invoice)) {
                    // The invoice has expired
                    bindExpiredInvoice(invoice);
                } else {
                    // The invoice has not yet expired
                    bindOpenInvoice(invoice);
                }
            }
        }
    }

    private void bindOpenInvoice(Invoice invoice) {
        mBSDScrollableMainView.setTitle(R.string.invoice_detail);

        mAmount.setAmount(invoice.getValue());

        String lightningUri = UriUtil.generateLightningUri(invoice.getPaymentRequest());
        // Generate "QR-Code"
        Bitmap bmpQRCode = QRCodeGenerator.bitmapFromText(lightningUri, 500);
        mQRCodeView.setImageBitmap(bmpQRCode);
        mQRCodeView.setOnClickListener(view ->
                ClipBoardUtil.copyToClipboard(getContext(), "Invoice", lightningUri)
        );

        ScheduledExecutorService expiryUpdateSchedule =
                Executors.newSingleThreadScheduledExecutor();

        expiryUpdateSchedule.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        long timeLeft = (invoice.getCreationDate() + invoice.getExpiry()) - (System.currentTimeMillis() / 1000);
                        String expiryText = TimeFormatUtil.formattedDuration(timeLeft, getActivity()) + " " + getActivity().getResources().getString(R.string.remaining);

                        mExpiry.setText(expiryText);
                    }
                }, 0, 1, TimeUnit.SECONDS);
    }

    private void bindPayedInvoice(Invoice invoice) {
        mBSDScrollableMainView.setTitle(R.string.transaction_detail);
        mExpiryLabel.setVisibility(View.GONE);
        mExpiry.setVisibility(View.GONE);
        mAmount.setAmount(invoice.getAmtPaidSat());
        mQRCodeView.setVisibility(View.GONE);
    }

    private void bindExpiredInvoice(Invoice invoice) {
        mBSDScrollableMainView.setTitle(R.string.invoice_detail);
        mAmount.setAmount(invoice.getValue());
        mExpiry.setText(R.string.expired);
        mQRCodeView.setVisibility(View.GONE);
    }

}
