package app.michaelwuensch.bitbanana.listViews.transactionHistory.itemDetails;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.labels.LabelActivity;
import app.michaelwuensch.bitbanana.labels.Labels;
import app.michaelwuensch.bitbanana.labels.LabelsManager;
import app.michaelwuensch.bitbanana.listViews.paymentRoute.PaymentRouteActivity;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;

public class LnPaymentDetailBSDFragment extends BaseBSDFragment implements LabelsManager.LabelChangedListener {

    public static final String TAG = LnPaymentDetailBSDFragment.class.getSimpleName();
    public static final String ARGS_TRANSACTION = "TRANSACTION";

    private BSDScrollableMainView mBSDScrollableMainView;
    private TextView mPayeeLabel;
    private TextView mPayee;
    private ImageView mPayeeCopyIcon;
    private TextView mAmountLabel;
    private AmountView mAmount;
    private TextView mDescriptionLabel;
    private TextView mDescription;
    private TextView mPayerNoteLabel;
    private TextView mPayerNote;
    private TextView mFeeLabel;
    private AmountView mFee;
    private TextView mDateLabel;
    private TextView mDate;
    private TextView mPreimageLabel;
    private TextView mPreimage;
    private ImageView mPreimageCopyIcon;
    private BBButton mShowPaymentRouteButton;
    private TextView mLabelLabel;
    private TextView mLabel;
    private BBButton mLabelButton;
    private String mLabelString;
    private LnPayment mLnPayment;

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
        mDescriptionLabel = view.findViewById(R.id.descriptionLabel);
        mDescription = view.findViewById(R.id.description);
        mPayerNoteLabel = view.findViewById(R.id.payerNoteLabel);
        mPayerNote = view.findViewById(R.id.payerNote);
        mFeeLabel = view.findViewById(R.id.feeLabel);
        mFee = view.findViewById(R.id.fee);
        mDateLabel = view.findViewById(R.id.dateLabel);
        mDate = view.findViewById(R.id.date);
        mPreimageLabel = view.findViewById(R.id.preimageLabel);
        mPreimage = view.findViewById(R.id.preimage);
        mPreimageCopyIcon = view.findViewById(R.id.preimageCopyIcon);
        mShowPaymentRouteButton = view.findViewById(R.id.showPaymentRouteButton);
        mLabelLabel = view.findViewById(R.id.labelLabel);
        mLabel = view.findViewById(R.id.label);
        mLabelButton = view.findViewById(R.id.labelButton);


        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            bindPayment((LnPayment) getArguments().getSerializable(ARGS_TRANSACTION));
        }

        mLabelButton.setVisibility(FeatureManager.isLabelsEnabled() ? View.VISIBLE : View.GONE);
        mLabelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent labelIntent = new Intent(getContext(), LabelActivity.class);
                labelIntent.putExtra(LabelActivity.EXTRA_LABEL_ID, mLnPayment.getPaymentHash());
                labelIntent.putExtra(LabelActivity.EXTRA_LABEL_TYPE, Labels.LabelType.LN_PAYMENT);
                if (mLabelString != null)
                    labelIntent.putExtra(LabelActivity.EXTRA_LABEL, mLabelString);
                startActivity(labelIntent);
            }
        });
        return view;
    }

    private void bindPayment(LnPayment payment) {
        mLnPayment = payment;

        String payeeLabel = getString(R.string.payee) + ":";
        mPayeeLabel.setText(payeeLabel);
        String amountLabel = getString(R.string.amount) + ":";
        mAmountLabel.setText(amountLabel);
        String descriptionLabel = getString(R.string.description) + ":";
        mDescriptionLabel.setText(descriptionLabel);
        String payerNoteLabel = getString(R.string.bolt12_payer_note) + ":";
        mPayerNoteLabel.setText(payerNoteLabel);
        String feeLabel = getString(R.string.fee) + ":";
        mFeeLabel.setText(feeLabel);
        String dateLabel = getString(R.string.date) + ":";
        mDateLabel.setText(dateLabel);
        String preimageLabel = getString(R.string.preimage) + ":";
        mPreimageLabel.setText(preimageLabel);
        String labelLabel = getString(R.string.label) + ":";
        mLabelLabel.setText(labelLabel);

        mBSDScrollableMainView.setTitle(R.string.transaction_detail);


        if (payment.hasDestinationPubKey()) {
            mPayee.setText(ContactsManager.getInstance().getNameByContactData(payment.getDestinationPubKey()));
            mPayeeCopyIcon.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "Payee", payment.getDestinationPubKey()));
        } else {
            mPayeeLabel.setVisibility(View.GONE);
            mPayee.setVisibility(View.GONE);
            mPayeeCopyIcon.setVisibility(View.GONE);
        }

        if (payment.hasRoutes()) {
            mShowPaymentRouteButton.setVisibility(View.VISIBLE);
            mShowPaymentRouteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), PaymentRouteActivity.class);
                    intent.putExtra(PaymentRouteActivity.EXTRA_LNPAYMENT, payment);
                    startActivity(intent);
                }
            });
        }

        mAmount.setAmountMsat(payment.getAmountPaid());
        mFee.setAmountMsat(payment.getFee());
        mPreimage.setText(payment.getPaymentPreimage());
        mPreimageCopyIcon.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "Payment Preimage", payment.getPaymentPreimage()));
        mDate.setText(TimeFormatUtil.formatTimeAndDateLong(payment.getCreatedAt(), getActivity()));


        if (payment.hasDescription()) {
            mDescription.setText(payment.getDescription());
        } else {
            if (payment.hasKeysendMessage())
                mDescription.setText(payment.getKeysendMessage());
            else {
                // Nothing there, hide the view
                mDescription.setVisibility(View.GONE);
                mDescriptionLabel.setVisibility(View.GONE);
            }
        }

        if (payment.hasBolt12PayerNote()) {
            mPayerNote.setText(payment.getBolt12PayerNote());
        } else {
            mPayerNote.setVisibility(View.GONE);
            mPayerNoteLabel.setVisibility(View.GONE);
        }
        if (FeatureManager.isLabelsEnabled()) {
            String label = LabelsManager.getLabel(payment);
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
        bindPayment(mLnPayment);
    }
}
