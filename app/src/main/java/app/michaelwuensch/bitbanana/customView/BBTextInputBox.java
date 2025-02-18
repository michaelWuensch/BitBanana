package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;

public class BBTextInputBox extends LinearLayout {

    private static final String LOG_TAG = BBTextInputBox.class.getSimpleName();

    private TextView mTvLabel;
    private TextView mTvCharCount;
    private EditText mEtComment;
    private onCommentFocusChangedListener mOnFocusChangedListener;
    private int mCharLimit;

    public BBTextInputBox(Context context) {
        super(context);
        init(context, null);
    }

    public BBTextInputBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBTextInputBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(getContext(), R.layout.view_text_input_box, this);

        mTvLabel = view.findViewById(R.id.label);
        mTvCharCount = view.findViewById(R.id.charCount);
        mEtComment = view.findViewById(R.id.inputComment);

        // If attributes are provided, read and apply them.
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BBTextInputBox);

            // Optionally set Label
            if (ta.hasValue(R.styleable.BBTextInputBox_BBTextInputBox_Label)) {
                String label = ta.getString(R.styleable.BBTextInputBox_BBTextInputBox_Label);
                mTvLabel.setText(label);
            }

            // Optionally set SingleLine
            if (ta.getBoolean(R.styleable.BBTextInputBox_BBTextInputBox_SingleLine, false)) {
                mEtComment.setSingleLine(true);
                mEtComment.setLines(1);
            }

            // Optionally set ImeOption next
            if (ta.getBoolean(R.styleable.BBTextInputBox_BBTextInputBox_ImeOption_next, false)) {
                setImeTypeNext();
            }

            // Optionally set ImeOption done
            if (ta.getBoolean(R.styleable.BBTextInputBox_BBTextInputBox_ImeOption_done, false)) {
                setImeTypeDone();
            }

            // Don't forget to recycle the TypedArray
            ta.recycle();
        }

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

        mEtComment.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (mOnFocusChangedListener != null)
                    mOnFocusChangedListener.onFocusChanged(view, b);
            }
        });
    }

    public void setImeTypeNext() {
        mEtComment.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mEtComment.setRawInputType(InputType.TYPE_CLASS_TEXT);
    }

    public void setImeTypeDone() {
        mEtComment.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mEtComment.setRawInputType(InputType.TYPE_CLASS_TEXT);
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

    public void setOnFocusChangedListener(onCommentFocusChangedListener listener) {
        mOnFocusChangedListener = listener;
    }

    private void updateCharCount() {
        String limitString = "" + mEtComment.getText().length() + "/" + mCharLimit;
        mTvCharCount.setText(limitString);
    }

    public interface onCommentFocusChangedListener {
        void onFocusChanged(View view, boolean b);
    }
}