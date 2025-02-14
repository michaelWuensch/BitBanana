package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class BBExpandableTextInfoBox extends ConstraintLayout {

    private TextView mTvTextLabel;
    private TextView mTvTextPreview;
    private TextView mTvTextFull;
    private ImageView mExpandArrowImage;
    private View mVBasicView;
    private ClearFocusListener mClearfocusListener;
    private boolean isExpandable = false;
    private ClickableConstraintLayoutGroup mGroupExpandedContent;


    public BBExpandableTextInfoBox(Context context) {
        super(context);
        init(context, null);
    }

    public BBExpandableTextInfoBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBExpandableTextInfoBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(getContext(), R.layout.view_expandable_text_info_box, this);

        mTvTextLabel = view.findViewById(R.id.textLabel);
        mTvTextPreview = view.findViewById(R.id.textPreview);
        mTvTextFull = view.findViewById(R.id.textFull);
        mExpandArrowImage = view.findViewById(R.id.arrowImage);

        mVBasicView = view.findViewById(R.id.basicView);
        mGroupExpandedContent = view.findViewById(R.id.expandedContent);

        // If attributes are provided, read and apply them.
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BBExpandableTextInfoBox);

            // Optionally set Label
            if (ta.hasValue(R.styleable.BBExpandableTextInfoBox_BBExpandableTextInfoBox_setLabel)) {
                String label = ta.getString(R.styleable.BBExpandableTextInfoBox_BBExpandableTextInfoBox_setLabel);
                mTvTextLabel.setText(label);
            }

            // Optionally set content
            if (ta.hasValue(R.styleable.BBExpandableTextInfoBox_BBExpandableTextInfoBox_setContent)) {
                String content = ta.getString(R.styleable.BBExpandableTextInfoBox_BBExpandableTextInfoBox_setContent);
                mTvTextPreview.setText(content);
                mTvTextFull.setText(content);
            }

            // Don't forget to recycle the TypedArray
            ta.recycle();
        }

        mVBasicView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (isExpandable) {
                    boolean isExpanded = mGroupExpandedContent.getVisibility() == View.VISIBLE;
                    toggleExpandState(isExpanded);
                }
            }
        });

        mGroupExpandedContent.setOnAllClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (isExpandable) {
                    boolean isExpanded = mGroupExpandedContent.getVisibility() == View.VISIBLE;
                    toggleExpandState(isExpanded);
                }
            }
        });
    }

    public void setAll(int label, int content) {
        setAll(getContext().getResources().getString(label), getContext().getResources().getString(content));
    }

    public void setAll(int label, String explanation) {
        setAll(getContext().getResources().getString(label), explanation);
    }

    public void setAll(@Nullable String label, String content) {
        if (label != null)
            mTvTextLabel.setText(label);
        mTvTextPreview.setText(content);
        mTvTextFull.setText(content);

        mTvTextPreview.post(new Runnable() {
            @Override
            public void run() {
                boolean isEllipsize = true;
                if (mTvTextPreview.getLayout() != null) {
                    isEllipsize = !((mTvTextPreview.getLayout().getText().toString()).equalsIgnoreCase(content));
                }
                isExpandable = isEllipsize;
                mExpandArrowImage.setVisibility(isEllipsize ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void setContent(int content) {
        setContent(getContext().getResources().getString(content));
    }

    public void setContent(String content) {
        setAll(null, content);
    }

    public void setEllipsize(TextUtils.TruncateAt where) {
        mTvTextPreview.setEllipsize(where);
    }

    public void setClearFocusListener(ClearFocusListener listener) {
        mClearfocusListener = listener;
    }

    /**
     * Show or hide expanded content
     */
    private void toggleExpandState(boolean hide) {
        hideKeyboard();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition((ViewGroup) getRootView());
                mExpandArrowImage.setImageResource(hide ? R.drawable.ic_arrow_down_24dp : R.drawable.ic_arrow_up_24dp);
                mTvTextPreview.setVisibility(hide ? View.VISIBLE : View.GONE);
                mGroupExpandedContent.setVisibility(hide ? View.GONE : View.VISIBLE);
                if (mClearfocusListener != null)
                    mClearfocusListener.onClearFocus();
            }
        }, 100);

    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getRootView().getWindowToken(), 0);
    }
}
