package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.constraintlayout.widget.ConstraintLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.FeatureManager;

public class BBPasswordInputFieldView extends ConstraintLayout {

    private static final String LOG_TAG = BBPasswordInputFieldView.class.getSimpleName();

    BBInputFieldView mPasswordInput;
    ImageButton mPasswordVisibilityToggle;

    private boolean pwVisible = false;
    protected View mView;

    public BBPasswordInputFieldView(Context context) {
        super(context);
        init(context, null);
    }

    public BBPasswordInputFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBPasswordInputFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        mView = inflate(context, R.layout.view_password_input_field, this);

        mPasswordInput = mView.findViewById(R.id.passwordInputFieldPasswordInput);
        mPasswordVisibilityToggle = mView.findViewById(R.id.passwordInputFieldPasswordVisibilityToggle);

        // Apply attributes from XML
        if (attrs != null) {
            // Obtain the custom attribute value from the XML attributes
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BBInputFieldView);

            String attrLabel = a.getString(R.styleable.BBInputFieldView_inputLabel);

            String attrLabelDetails = a.getString(R.styleable.BBInputFieldView_inputLabelDetails);
            boolean attrShowHelpButton = a.getBoolean(R.styleable.BBInputFieldView_showHelpButton, false);
            mPasswordInput.setHelpStringResource(a.getString(R.styleable.BBInputFieldView_helpButtonText));
            String attrDefaultValue = a.getString(R.styleable.BBInputFieldView_defaultInputValue);
            mPasswordInput.setMinLines(a.getInt(R.styleable.BBInputFieldView_minLines, 1));
            mPasswordInput.setMaxLines(a.getInt(R.styleable.BBInputFieldView_maxLines, 1));
            mPasswordInput.setMaxLinesFocused(a.getInt(R.styleable.BBInputFieldView_maxLinesFocused, 1));
            Drawable bg = a.getDrawable(R.styleable.BBInputFieldView_inputAreaBgDrawable);

            if (attrLabel != null)
                setDescription(attrLabel);

            if (attrLabelDetails != null)
                setDescriptionDetail(attrLabelDetails);
            setShowHelpButton(attrShowHelpButton);

            if (attrDefaultValue != null)
                setValue(attrDefaultValue);
            if (bg != null) {
                getEditText().setBackground(bg);
            }
            mPasswordInput.updateLineCount();

            // Don't forget to recycle the TypedArray
            a.recycle();
        }

        getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        mPasswordVisibilityToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Save current selection
                int selectionStart = getEditText().getSelectionStart();
                int selectionEnd = getEditText().getSelectionEnd();

                if (pwVisible) {
                    getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mPasswordVisibilityToggle.setImageDrawable(getResources().getDrawable(R.drawable.outline_visibility_off_24));
                } else {
                    getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    mPasswordVisibilityToggle.setImageDrawable(getResources().getDrawable(R.drawable.outline_visibility_24));
                }

                // Restore selection
                getEditText().setSelection(selectionStart, selectionEnd);

                pwVisible = !pwVisible;
            }
        });
    }

    public void setDescription(String description) {
        mPasswordInput.setDescription(description);
    }

    public void setDescription(int resId) {
        mPasswordInput.setDescription(resId);
    }

    public void setDescriptionDetail(String description) {
        mPasswordInput.setDescriptionDetail(description);
    }

    public void setShowHelpButton(boolean showHelpButton) {
        if (!isInEditMode()) {
            if (!FeatureManager.isHelpButtonsEnabled()) {
                mPasswordInput.setShowHelpButton(false);
                return;
            }
        }
        mPasswordInput.setShowHelpButton(showHelpButton);
    }

    public void setValue(String value) {
        getEditText().setText(value);
    }

    public String getData() {
        String data = getEditText().getText().toString();
        if (data.isEmpty()) {
            return null;
        }
        return data;
    }

    public EditText getEditText() {
        return mPasswordInput.getEditText();
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
