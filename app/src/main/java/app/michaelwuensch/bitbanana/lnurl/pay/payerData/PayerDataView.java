package app.michaelwuensch.bitbanana.lnurl.pay.payerData;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.Wallet;

public class PayerDataView extends LinearLayout {

    private static final String LOG_TAG = PayerDataView.class.getSimpleName();
    private PayerDataEntryView mPayerDataName;
    private PayerDataEntryView mPayerDataIdentifier;
    private PayerDataEntryView mPayerDataEmail;
    private PayerDataEntryView mPayerDataPubkey;
    private Context mActivity;
    private ImageButton mHelpButton;

    public PayerDataView(Context context) {
        super(context);
        init();
    }

    public PayerDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PayerDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_payer_data, this);

        mPayerDataName = view.findViewById(R.id.payerDataName);
        mPayerDataIdentifier = view.findViewById(R.id.payerDataIdentifier);
        mPayerDataEmail = view.findViewById(R.id.payerDataEmail);
        mPayerDataPubkey = view.findViewById(R.id.payerDataPubkey);

        //HelpButton
        mHelpButton = findViewById(R.id.helpButton);
        mHelpButton.setVisibility(FeatureManager.isHelpButtonsEnabled() ? VISIBLE : GONE);
        mHelpButton.setOnClickListener(view1 -> {
            HelpDialogUtil.showDialog(mActivity, R.string.help_dialog_payer_data_payment);
        });
    }

    public void setupView(LnUrlpRequestedPayerData requestedPayerData, Context context) {
        mActivity = context;
        mPayerDataName.setVisibility(requestedPayerData.isNameSupported() ? VISIBLE : GONE);
        mPayerDataIdentifier.setVisibility(requestedPayerData.isIdentifierSupported() ? VISIBLE : GONE);
        mPayerDataEmail.setVisibility(requestedPayerData.isEmailSupported() ? VISIBLE : GONE);
        mPayerDataPubkey.setVisibility(requestedPayerData.isPubkeySupported() ? VISIBLE : GONE);

        mPayerDataName.setMandatory(requestedPayerData.isNameMandatory());
        mPayerDataIdentifier.setMandatory(requestedPayerData.isIdentifierMandatory());
        mPayerDataEmail.setMandatory(requestedPayerData.isEmailMandatory());
        mPayerDataPubkey.setMandatory(requestedPayerData.isPubkeyMandatory());

        mPayerDataIdentifier.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mPayerDataEmail.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        mPayerDataName.setDescription(getContext().getString(R.string.lnurl_payer_data_name));
        mPayerDataIdentifier.setDescription("LN Address");
        mPayerDataEmail.setDescription(getContext().getString(R.string.lnurl_payer_data_email));
        mPayerDataPubkey.setDescription("Pubkey");
        mPayerDataPubkey.setHideInput(true);

        // Prefill values
        mPayerDataName.setValue(PrefsUtil.getPrefs().getString("payerDataName", ""));
        mPayerDataIdentifier.setValue(PrefsUtil.getPrefs().getString("payerDataIdentifier", ""));
        mPayerDataEmail.setValue(PrefsUtil.getPrefs().getString("payerDataEmail", ""));
        mPayerDataPubkey.setValue(Wallet.getInstance().getIdentityPubKey());
    }

    public LnUrlpPayerData getData() {
        LnUrlpPayerData.Builder pdb = new LnUrlpPayerData.Builder();

        if (mPayerDataName.isChecked())
            pdb.setName(mPayerDataName.getData());
        if (mPayerDataIdentifier.isChecked())
            pdb.setIdentifier(mPayerDataIdentifier.getData());
        if (mPayerDataEmail.isChecked())
            pdb.setEmail(mPayerDataEmail.getData());
        if (mPayerDataPubkey.isChecked())
            pdb.setPubkey(mPayerDataPubkey.getData());

        return pdb.build();
    }
}
