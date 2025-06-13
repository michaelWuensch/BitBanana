package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;

public class BSDScrollableMainView extends ConstraintLayout {
    private NestedScrollView mContentView;
    private ImageButton mHelpButton;
    private ImageButton mMoreButton;
    private ImageView mSeparatorLine;
    private ImageView mTitleIcon;
    private TextView mTitle;
    private OnCloseListener mOnCloseListener;
    private OnMoreListener mOnMoreListener;
    private int mHelpMessage;

    public BSDScrollableMainView(Context context) {
        super(context);
        init();
    }

    public BSDScrollableMainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BSDScrollableMainView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //Inflate and attach your child XML
        inflate(getContext(), R.layout.bsd_base_scrollable, this);

        //Get a reference to the layout where you want children to be placed
        mContentView = findViewById(R.id.content);

        mTitle = findViewById(R.id.title);
        mTitleIcon = findViewById(R.id.titleIcon);
        mSeparatorLine = findViewById(R.id.separatorLine);

        //Close Button
        ImageButton closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view1 -> {
            if (mOnCloseListener != null)
                mOnCloseListener.onClosed();
        });

        //HelpButton
        mHelpButton = findViewById(R.id.helpButton);
        mHelpButton.setOnClickListener(view1 -> {
            if (mHelpMessage != 0)
                HelpDialogUtil.showDialog(getContext(), mHelpMessage);
        });

        //MoreButton
        mMoreButton = findViewById(R.id.moreButton);
        mMoreButton.setOnClickListener(view1 -> {
            if (mOnMoreListener != null)
                mOnMoreListener.onMore();
        });

    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setTitle(int resID) {
        mTitle.setText(resID);
    }

    public void setTitleVisibility(boolean visible) {
        mTitle.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setHelpButtonVisibility(boolean visible) {
        if (FeatureManager.isHelpButtonsEnabled())
            mHelpButton.setVisibility(visible ? VISIBLE : GONE);
        else
            mHelpButton.setVisibility(GONE);
    }

    public void setMoreButtonVisibility(boolean visible) {
        mMoreButton.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setMoreButtonStyle(MoreButtonStyle style) {
        switch (style) {
            case MORE:
                mMoreButton.setImageResource(R.drawable.ic_baseline_more_vert_24);
                break;
            case OPTIONS:
                mMoreButton.setImageResource(R.drawable.baseline_settings_24);
        }
    }

    public void setTitleIcon(int resID) {
        mTitleIcon.setImageResource(resID);
    }

    public void setTitleIconVisibility(boolean visible) {
        mTitleIcon.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setSeparatorVisibility(boolean visible) {
        mSeparatorLine.setVisibility(visible ? VISIBLE : GONE);
    }

    public void animateTitleOut() {
        TransitionManager.beginDelayedTransition((ViewGroup) getRootView());
        mTitleIcon.setVisibility(INVISIBLE);
        mTitle.setVisibility(INVISIBLE);
    }


    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (mContentView == null) {
            super.addView(child, index, params);
        } else {
            //Forward these calls to the content view
            mContentView.addView(child, index, params);
        }
    }

    public void setOnCloseListener(OnCloseListener listener) {
        mOnCloseListener = listener;
    }

    public void setHelpMessage(int message) {
        mHelpMessage = message;
    }

    public void setOnMoreListener(OnMoreListener listener) {
        mOnMoreListener = listener;
    }

    public interface OnCloseListener {
        void onClosed();
    }

    public interface OnMoreListener {
        void onMore();
    }

    public final void focusOnView(final View view, int offset) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int vTop = view.getTop();
                int vBottom = view.getBottom();
                int sHeight = mContentView.getBottom();
                mContentView.smoothScrollTo(0, ((vTop + vBottom - sHeight + offset) / 2));
            }
        }, 250);
    }

    public enum MoreButtonStyle {
        MORE,
        OPTIONS;
    }
}
