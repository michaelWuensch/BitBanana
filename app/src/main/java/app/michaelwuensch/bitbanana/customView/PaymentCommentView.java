package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;

public class PaymentCommentView extends LinearLayout {

    private static final String LOG_TAG = PaymentCommentView.class.getSimpleName();

    private TextView mTvCharCount;
    private EditText mEtComment;
    private int mCharLimit;

    public PaymentCommentView(Context context) {
        super(context);
        init();
    }

    public PaymentCommentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PaymentCommentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_payment_comment, this);

        mTvCharCount = view.findViewById(R.id.charCount);
        mEtComment = view.findViewById(R.id.inputComment);

        mEtComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateCharCount();
            }
        });
    }

    /**
     * Enforces a length limitation for the message.
     *
     * @param charLimit If set to 0 no limit is applied.
     */
    public void setupCharLimit(int charLimit) {
        mCharLimit = charLimit;
        if (charLimit > 0) {
            mEtComment.setFilters(new InputFilter[]{new InputFilter.LengthFilter(charLimit)});
            mTvCharCount.setVisibility(VISIBLE);
            updateCharCount();
        } else
            mTvCharCount.setVisibility(GONE);
    }

    public String getData() {
        return mEtComment.getText().toString();
    }

    private void updateCharCount() {
        String limitString = "" + mEtComment.getText().length() + "/" + mCharLimit;
        mTvCharCount.setText(limitString);
    }
}
