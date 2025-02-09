package app.michaelwuensch.bitbanana.listViews.contacts;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.constraintlayout.utils.widget.ImageFilterView;

import com.github.michaelwuensch.avathorlibrary.AvathorFactory;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.contacts.Contact;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnAddress;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.inputFilters.InputFilterHex;
import app.michaelwuensch.bitbanana.util.inputFilters.InputFilterLowerCase;
import app.michaelwuensch.bitbanana.util.inputFilters.InputFilterNoWhitespaces;

public class ManualAddContactActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = ManualAddContactActivity.class.getSimpleName();
    private ImageFilterView mUserAvatarView;
    private EditText mEtName;
    private EditText mEtData;
    private BBButton mBtnSave;
    private Spinner mSpType;
    private TextView mTvDataLabel;

    private Contact.ContactType mType = Contact.ContactType.LNADDRESS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_add_manual);

        mUserAvatarView = findViewById(R.id.userAvatar);
        mEtName = findViewById(R.id.nameInput);
        mEtData = findViewById(R.id.dataInput);
        mBtnSave = findViewById(R.id.saveButton);
        mSpType = findViewById(R.id.typeSpinner);
        mTvDataLabel = findViewById(R.id.dataLabel);

        mUserAvatarView.setImageResource(R.drawable.unknown_avatar);

        String[] items = new String[Contact.ContactType.values().length];
        for (int i = 0; i < Contact.ContactType.values().length; i++) {
            items[i] = getResources().getString(Contact.ContactType.values()[i].getTitle());
        }
        mSpType.setAdapter(new ArrayAdapter<String>(this, R.layout.spinner_item, items));
        mSpType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mType = Contact.ContactType.NODEPUBKEY;
                        mTvDataLabel.setText(R.string.pubkey);
                        break;
                    case 1:
                        mType = Contact.ContactType.LNADDRESS;
                        mTvDataLabel.setText(R.string.ln_address);
                        break;
                    case 2:
                        mType = Contact.ContactType.BOLT12_OFFER;
                        mTvDataLabel.setText(R.string.bolt12_offer);
                }
                updateInputFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSpType.setSelection(1);

        mEtData.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mEtData.setRawInputType(InputType.TYPE_CLASS_TEXT);

        mEtData.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mEtData.getText().toString().isEmpty())
                    mUserAvatarView.setImageResource(R.drawable.unknown_avatar);
                else
                    mUserAvatarView.setImageBitmap(AvathorFactory.getAvathor(ManualAddContactActivity.this, mEtData.getText().toString(), PrefsUtil.getAvatarSet()));
            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private void validateData() {
        String name = mEtName.getText().toString().trim();
        String data = mEtData.getText().toString();
        if (name.isEmpty()) {
            showError(getResources().getString(R.string.error_empty_node_name), RefConstants.ERROR_DURATION_SHORT);
            return;
        }

        switch (mType) {
            case NODEPUBKEY:
                LightningNodeUri nodeUri = LightningNodeUriParser.parseNodeUri(data);

                if (nodeUri != null) {
                    addContact(Contact.ContactType.NODEPUBKEY, name, data);
                } else {
                    showError(getResources().getString(R.string.error_lightning_uri_invalid), RefConstants.ERROR_DURATION_LONG);
                }
                break;
            case LNADDRESS:
                LnAddress lnAddress = new LnAddress(data);
                if (lnAddress.isValidLnurlAddress() || lnAddress.isValidBip353DnsRecordAddress()) {
                    addContact(Contact.ContactType.LNADDRESS, name, data);
                } else {
                    showError(getResources().getString(R.string.error_invalid_ln_address_format), RefConstants.ERROR_DURATION_LONG);
                    return;
                }
                break;
            case BOLT12_OFFER:
                try {
                    InvoiceUtil.decodeBolt12(data);
                    addContact(Contact.ContactType.BOLT12_OFFER, name, data);
                } catch (Exception e) {
                    showError(getResources().getString(R.string.error_invalid_bolt12_offer_format), RefConstants.ERROR_DURATION_SHORT);
                    return;
                }
                break;
        }
    }

    private void updateInputFilters() {
        switch (mType) {
            case NODEPUBKEY:
                mEtData.setFilters(new InputFilter[]{new InputFilterHex(false, true)});
                mEtData.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                break;
            case LNADDRESS:
                mEtData.setFilters(new InputFilter[]{new InputFilterLowerCase(), new InputFilterNoWhitespaces()});
                mEtData.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;
            case BOLT12_OFFER:
                mEtData.setFilters(new InputFilter[]{new InputFilterLowerCase()});
                mEtData.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }
        mEtData.setSingleLine(false);
        mEtData.setMinLines(2);
        mEtData.setMaxLines(5);
    }

    private void addContact(Contact.ContactType contactType, String name, String data) {
        ContactsManager cm = ContactsManager.getInstance();
        if (cm.doesContactDataExist(data)) {
            showError(getResources().getString(R.string.contact_already_exists), RefConstants.ERROR_DURATION_SHORT);
            return;
        }
        cm.addContact(contactType, data, name);
        try {
            cm.apply();
            finish();
        } catch (Exception e) {
            BBLog.e(LOG_TAG, "Error saving contact.");
        }
    }
}
