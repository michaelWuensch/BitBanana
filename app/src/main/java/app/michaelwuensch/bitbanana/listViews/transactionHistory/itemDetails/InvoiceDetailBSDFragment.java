package app.michaelwuensch.bitbanana.listViews.transactionHistory.itemDetails;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.utils.widget.ImageFilterView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.labels.LabelActivity;
import app.michaelwuensch.bitbanana.labels.Labels;
import app.michaelwuensch.bitbanana.labels.LabelsManager;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.qrCodeGen.QRCodeGenerator;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.util.UriUtil;

public class InvoiceDetailBSDFragment extends BaseBSDFragment implements LabelsManager.LabelChangedListener {

    public static final String TAG = InvoiceDetailBSDFragment.class.getSimpleName();
    public static final String ARGS_TRANSACTION = "TRANSACTION";

    private BSDScrollableMainView mBSDScrollableMainView;
    private TextView mAmountLabel;
    private AmountView mAmount;
    private TextView mMemoLabel;
    private TextView mMemo;
    private TextView mBolt12PayerNoteLabel;
    private TextView mBolt12PayerNote;
    private TextView mDateLabel;
    private TextView mDate;
    private TextView mExpiryLabel;
    private TextView mExpiry;
    private ImageFilterView mQRCodeView;
    private TextView mLabelLabel;
    private TextView mLabel;
    private BBButton mLabelButton;
    private String mLabelString;
    private LnInvoice mLnInvoice;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_invoice_detail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mAmountLabel = view.findViewById(R.id.amountLabel);
        mAmount = view.findViewById(R.id.amount);
        mMemoLabel = view.findViewById(R.id.memoLabel);
        mMemo = view.findViewById(R.id.memo);
        mBolt12PayerNoteLabel = view.findViewById(R.id.bolt12PayerNoteLabel);
        mBolt12PayerNote = view.findViewById(R.id.bolt12PayerNote);
        mDateLabel = view.findViewById(R.id.dateLabel);
        mDate = view.findViewById(R.id.date);
        mExpiryLabel = view.findViewById(R.id.expiryLabel);
        mExpiry = view.findViewById(R.id.expiry);
        mQRCodeView = view.findViewById(R.id.requestQRCode);
        mLabelLabel = view.findViewById(R.id.labelLabel);
        mLabel = view.findViewById(R.id.label);
        mLabelButton = view.findViewById(R.id.labelButton);

        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            bindInvoice((LnInvoice) getArguments().getSerializable(ARGS_TRANSACTION));
        }

        mLabelButton.setVisibility(FeatureManager.isLabelsEnabled() ? View.VISIBLE : View.GONE);
        mLabelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent labelIntent = new Intent(getContext(), LabelActivity.class);
                labelIntent.putExtra(LabelActivity.EXTRA_LABEL_ID, mLnInvoice.getPaymentHash());
                labelIntent.putExtra(LabelActivity.EXTRA_LABEL_TYPE, Labels.LabelType.LN_INVOICE);
                if (mLabelString != null)
                    labelIntent.putExtra(LabelActivity.EXTRA_LABEL, mLabelString);
                startActivity(labelIntent);
            }
        });

        return view;
    }


    private void bindInvoice(LnInvoice invoice) {
        mLnInvoice = invoice;

        String amountLabel = getString(R.string.amount) + ":";
        mAmountLabel.setText(amountLabel);
        String memoLabel = getString(R.string.memo) + ":";
        mMemoLabel.setText(memoLabel);
        String bolt12PayerNoteLabel = getString(R.string.bolt12_payer_note) + ":";
        mBolt12PayerNoteLabel.setText(bolt12PayerNoteLabel);
        String dateLabel = getString(R.string.date) + ":";
        mDateLabel.setText(dateLabel);
        String expiryLabel = getString(R.string.expiry) + ":";
        mExpiryLabel.setText(expiryLabel);
        String labelLabel = getString(R.string.label) + ":";
        mLabelLabel.setText(labelLabel);

        mDate.setText(TimeFormatUtil.formatTimeAndDateLong(invoice.getCreatedAt(), getActivity()));


        // Set description
        if (invoice.hasMemo()) {
            mMemo.setText(invoice.getMemo());
        } else {
            if (invoice.hasKeysendMessage())
                mMemo.setText(invoice.getKeysendMessage());
            else {
                mMemo.setVisibility(View.GONE);
                mMemoLabel.setVisibility(View.GONE);
            }
        }

        // Set bolt12 payer note
        if (invoice.hasBolt12PayerNote()) {
            mBolt12PayerNote.setText(invoice.getBolt12PayerNote());
        } else {
            mBolt12PayerNote.setVisibility(View.GONE);
            mBolt12PayerNoteLabel.setVisibility(View.GONE);
        }

        // Label
        if (FeatureManager.isLabelsEnabled()) {
            String label = LabelsManager.getLabel(invoice);
            if (label != null) {
                mLabelLabel.setVisibility(View.VISIBLE);
                mLabel.setVisibility(View.VISIBLE);
                mLabel.setText(label);
                mLabelString = label;
                mLabelButton.setText(getString(R.string.label_edit));
            } else {
                mLabelLabel.setVisibility(View.GONE);
                mLabel.setVisibility(View.GONE);
                mLabelString = null;
                mLabelButton.setText(getString(R.string.label_add));
            }
        } else {
            mLabelLabel.setVisibility(View.GONE);
            mLabel.setVisibility(View.GONE);
        }

        if (invoice.isPaid()) {
            bindPayedInvoice(invoice);
        } else {
            if (invoice.isExpired()) {
                bindExpiredInvoice(invoice);
            } else {
                bindOpenInvoice(invoice);
            }
        }
    }

    private void bindOpenInvoice(LnInvoice invoice) {
        mBSDScrollableMainView.setTitle(R.string.invoice_detail);

        mAmount.setAmountMsat(invoice.getAmountRequested());

        String lightningUri = UriUtil.generateLightningUri(invoice.getBolt11());
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
                        long timeLeft = invoice.getExpiresAt() - (System.currentTimeMillis() / 1000);
                        String expiryText = TimeFormatUtil.formattedDuration(timeLeft, getActivity()) + " " + getActivity().getResources().getString(R.string.remaining);

                        mExpiry.setText(expiryText);
                    }
                }, 0, 1, TimeUnit.SECONDS);
    }

    private void bindPayedInvoice(LnInvoice invoice) {
        mBSDScrollableMainView.setTitle(R.string.transaction_detail);
        mExpiryLabel.setVisibility(View.GONE);
        mExpiry.setVisibility(View.GONE);
        mAmount.setAmountMsat(invoice.getAmountPaid());
        mQRCodeView.setVisibility(View.GONE);
    }

    private void bindExpiredInvoice(LnInvoice invoice) {
        mBSDScrollableMainView.setTitle(R.string.invoice_detail);
        mAmount.setAmountMsat(invoice.getAmountRequested());
        mExpiry.setText(R.string.expired);
        mQRCodeView.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        LabelsManager.getInstance().registerLabelChangedListener(this);
    }

    @Override
    public void onDestroy() {
        LabelsManager.getInstance().unregisterLabelChangedListener(this);
        super.onDestroy();
    }

    @Override
    public void onLabelChanged() {
        bindInvoice(mLnInvoice);
    }
}
