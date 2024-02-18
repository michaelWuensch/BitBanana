package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;

public class BBInputFieldView extends LinearLayout {

    private static final String LOG_TAG = BBInputFieldView.class.getSimpleName();

    private TextView mTvLabel;
    private TextView mTvDetails;
    private EditText mEtInput;
    private ImageButton mIbtnHelp;
    private String mHelpStringResource;

    private int mMinLines = 1;
    private int mMaxLines = 1;
    private int mMaxLinesFocused = 1;


    public BBInputFieldView(Context context) {
        super(context);
        init(context, null);
    }

    public BBInputFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBInputFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.view_input_field, this);

        mTvLabel = view.findViewById(R.id.inputLabel);
        mTvDetails = view.findViewById(R.id.inputDetails);
        mEtInput = view.findViewById(R.id.inputEditText);
        mIbtnHelp = view.findViewById(R.id.helpButton);

        mIbtnHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                HelpDialogUtil.showDialog(context, mHelpStringResource);
            }
        });

        mEtInput.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (mMaxLinesFocused > mMaxLines) {
                    if (b)
                        setLineCount(mMinLines, mMaxLinesFocused);
                    else
                        setLineCount(mMinLines, mMaxLines);
                }
            }
        });

        // Apply attributes from XML
        if (attrs != null) {
            // Obtain the custom attribute value from the XML attributes
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BBInputFieldView);
            String attrLabel = a.getString(R.styleable.BBInputFieldView_inputLabel);
            String attrLabelDetails = a.getString(R.styleable.BBInputFieldView_inputLabelDetails);
            boolean attrShowHelpButton = a.getBoolean(R.styleable.BBInputFieldView_showHelpButton, false);
            mHelpStringResource = a.getString(R.styleable.BBInputFieldView_helpButtonText);
            String attrDefaultValue = a.getString(R.styleable.BBInputFieldView_defaultInputValue);
            mMinLines = a.getInt(R.styleable.BBInputFieldView_minLines, 1);
            mMaxLines = a.getInt(R.styleable.BBInputFieldView_maxLines, 1);
            mMaxLinesFocused = a.getInt(R.styleable.BBInputFieldView_maxLinesFocused, 1);
            Drawable bg = a.getDrawable(R.styleable.BBInputFieldView_inputAreaBgDrawable);

            if (attrLabel != null)
                setDescription(attrLabel);
            if (attrLabelDetails != null)
                setDescriptionDetail(attrLabelDetails);
            setShowHelpButton(attrShowHelpButton);
            if (attrDefaultValue != null)
                setValue(attrDefaultValue);
            if ((mMinLines > 1 || mMaxLines > 1) && mMaxLines >= mMinLines)
                setLineCount(mMinLines, mMaxLines);
            if (bg != null) {
                mEtInput.setBackground(bg);
            }

            // Don't forget to recycle the TypedArray
            a.recycle();
        }
    }

    public void setDescription(String description) {
        mTvLabel.setText(description);
    }

    public void setDescriptionDetail(String description) {
        mTvDetails.setText(description);
    }

    public void setShowHelpButton(boolean showHelpButton) {
        if (!isInEditMode()) {
            if (!FeatureManager.isHelpButtonsEnabled()) {
                mIbtnHelp.setVisibility(INVISIBLE);
                return;
            }
        }
        mIbtnHelp.setVisibility(showHelpButton ? VISIBLE : INVISIBLE);
    }

    public void setValue(String value) {
        mEtInput.setText(value);
    }

    public String getData() {
        String data = mEtInput.getText().toString();
        if (data.isEmpty()) {
            return null;
        }
        return data;
    }

    public EditText getEditText() {
        return mEtInput;
    }

    public void setSingleLine(boolean singleLine) {
        getEditText().setSingleLine(singleLine);
    }

    public void setInputType(int type) {
        getEditText().setInputType(type);
    }

    public void setLineCount(int min, int max) {
        getEditText().setMinLines(min);
        getEditText().setMaxLines(max);
    }
}
