package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.R;

public class AdvancedChannelDetailView extends ConstraintLayout {

    private TextView mTvDetailLabel;
    private TextView mTvDetailValue;
    private TextView mTvDetailExplanation;
    private ImageView mExpandArrowImage;
    private ImageView mLine;
    private View mVBasicDetails;
    private ClickableConstraintLayoutGroup mGroupExpandedContent;


    public AdvancedChannelDetailView(Context context) {
        super(context);
        init(context, null);
    }

    public AdvancedChannelDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AdvancedChannelDetailView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(getContext(), R.layout.view_advanced_channel_detail, this);

        mTvDetailLabel = view.findViewById(R.id.detailLabel);
        mTvDetailValue = view.findViewById(R.id.detailValue);
        mTvDetailExplanation = view.findViewById(R.id.detailExplanation);
        mExpandArrowImage = view.findViewById(R.id.feeArrowUnitImage);
        mLine = view.findViewById(R.id.line);

        mVBasicDetails = view.findViewById(R.id.basicDetails);
        mGroupExpandedContent = view.findViewById(R.id.expandedContent);

        mVBasicDetails.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                boolean isExpanded = mGroupExpandedContent.getVisibility() == View.VISIBLE;
                toggleExpandState(isExpanded);
            }
        });

        mGroupExpandedContent.setOnAllClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                boolean isExpanded = mGroupExpandedContent.getVisibility() == View.VISIBLE;
                toggleExpandState(isExpanded);
            }
        });

        // Apply attributes from XML
        if (attrs != null) {
            // Obtain the custom attribute value from the XML attributes
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AdvancedChannelDetailView);
            String attrLabel = a.getString(R.styleable.AdvancedChannelDetailView_label);
            String attrValue = a.getString(R.styleable.AdvancedChannelDetailView_value);
            String attrExplanation = a.getString(R.styleable.AdvancedChannelDetailView_explanation);
            boolean attrHasLine = a.getBoolean(R.styleable.AdvancedChannelDetailView_hasLine, true);

            if (attrLabel != null)
                setLabel(attrLabel);
            if (attrValue != null)
                setValue(attrValue);
            if (attrExplanation != null)
                setExplanation(attrExplanation);
            setLineVisibility(attrHasLine);

            // Don't forget to recycle the TypedArray
            a.recycle();
        }
    }

    public void setContent(int label, String value, int explanation) {
        mTvDetailLabel.setText(getContext().getResources().getString(label));
        mTvDetailValue.setText(value);
        mTvDetailExplanation.setText(getContext().getResources().getString(explanation));
    }

    public void setLabel(String value) {
        mTvDetailLabel.setText(value);
    }

    public void setValue(String value) {
        mTvDetailValue.setText(value);
    }

    public void setExplanation(String value) {
        mTvDetailExplanation.setText(value);
    }

    public void setLineVisibility(boolean isVisible) {
        mLine.setVisibility(isVisible ? VISIBLE : INVISIBLE);
    }

    /**
     * Show or hide expanded content
     */
    private void toggleExpandState(boolean hide) {
        TransitionManager.beginDelayedTransition((ViewGroup) getRootView());
        mExpandArrowImage.setImageResource(hide ? R.drawable.ic_arrow_down_24dp : R.drawable.ic_arrow_up_24dp);
        mGroupExpandedContent.setVisibility(hide ? View.GONE : View.VISIBLE);
    }
}
