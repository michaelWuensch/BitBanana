package app.michaelwuensch.bitbanana.lnurl.pay.payerData;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;

public class PayerDataEntryView extends LinearLayout {

    private static final String LOG_TAG = PayerDataEntryView.class.getSimpleName();
    private CheckBox mPayerDataCheckbox;
    private TextView mPayerDataDescription;
    private EditText mPayerDataInput;


    public PayerDataEntryView(Context context) {
        super(context);
        init();
    }

    public PayerDataEntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PayerDataEntryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_payer_data_entry, this);

        mPayerDataCheckbox = view.findViewById(R.id.payerDataCheckBox);
        mPayerDataDescription = view.findViewById(R.id.payerDataDescription);
        mPayerDataInput = view.findViewById(R.id.payerDataInput);

        mPayerDataCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPayerDataInput.setEnabled(isChecked);
            }
        });
    }

    public void setMandatory(boolean isMandatory) {
        if (isMandatory) {
            mPayerDataCheckbox.setChecked(true);
            mPayerDataCheckbox.setEnabled(false);
            mPayerDataInput.setEnabled(true);
        } else {
            mPayerDataCheckbox.setEnabled(true);
            mPayerDataInput.setEnabled(mPayerDataCheckbox.isChecked());
        }
    }

    public void setHideInput(boolean hideInput) {
        mPayerDataInput.setVisibility(hideInput ? GONE : VISIBLE);
    }

    public void setDescription(String description) {
        mPayerDataDescription.setText(description);
    }

    public void setValue(String value) {
        mPayerDataInput.setText(value);
    }

    public String getData() {
        if (mPayerDataCheckbox.isChecked()) {
            String data = mPayerDataInput.getText().toString();
            if (data.isEmpty()) {
                return null;
            }
            return data;
        } else {
            return null;
        }
    }

    public boolean isChecked() {
        return mPayerDataCheckbox.isChecked();
    }

    public EditText getEditText() {
        return mPayerDataInput;
    }
}
