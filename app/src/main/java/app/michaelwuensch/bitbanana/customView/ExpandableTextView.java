package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class ExpandableTextView extends ConstraintLayout {

    private TextView mTvTextLabel;
    private TextView mTvTextPreview;
    private TextView mTvTextFull;
    private ImageView mExpandArrowImage;
    private View mVBasicView;
    private boolean isExpandable = false;
    private ClickableConstraintLayoutGroup mGroupExpandedContent;


    public ExpandableTextView(Context context) {
        super(context);
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_expandable_text, this);

        mTvTextLabel = view.findViewById(R.id.textLabel);
        mTvTextPreview = view.findViewById(R.id.textPreview);
        mTvTextFull = view.findViewById(R.id.textFull);
        mExpandArrowImage = view.findViewById(R.id.arrowImage);

        mVBasicView = view.findViewById(R.id.basicView);
        mGroupExpandedContent = view.findViewById(R.id.expandedContent);

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

    public void setContent(int label, int explanation) {
        setContent(getContext().getResources().getString(label), getContext().getResources().getString(explanation));
    }

    public void setContent(int label, String explanation) {
        setContent(getContext().getResources().getString(label), explanation);
    }

    public void setContent(String label, String explanation) {
        mTvTextLabel.setText(label);
        mTvTextPreview.setText(explanation);
        mTvTextFull.setText(explanation);

        mTvTextPreview.post(new Runnable() {
            @Override
            public void run() {
                boolean isEllipsize = true;
                if (mTvTextPreview.getLayout() != null) {
                    isEllipsize = !((mTvTextPreview.getLayout().getText().toString()).equalsIgnoreCase(explanation));
                }
                isExpandable = isEllipsize;
                mExpandArrowImage.setVisibility(isEllipsize ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Show or hide expanded content
     */
    private void toggleExpandState(boolean hide) {
        TransitionManager.beginDelayedTransition((ViewGroup) getRootView());
        mExpandArrowImage.setImageResource(hide ? R.drawable.ic_arrow_down_24dp : R.drawable.ic_arrow_up_24dp);
        mTvTextPreview.setVisibility(hide ? View.VISIBLE : View.GONE);
        mGroupExpandedContent.setVisibility(hide ? View.GONE : View.VISIBLE);
    }
}
