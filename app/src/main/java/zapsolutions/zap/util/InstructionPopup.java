package zapsolutions.zap.util;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import zapsolutions.zap.R;

public class InstructionPopup {

    private WindowManager mWindowManager;

    protected Context mContext;
    private PopupWindow mWindow;

    private TextView mHelpTextView;
    private ImageView mUpImageView;
    private ImageView mDownImageView;
    private View mView;
    public String mPopupID;

    private Drawable mBackgroundDrawable = null;
    private ShowListener showListener;

    public InstructionPopup(Context context, String text, int viewResource, String popupID) {
        mContext = context;
        mWindow = new PopupWindow(context);
        mPopupID = popupID;

        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);


        LayoutInflater layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setContentView(layoutInflater.inflate(viewResource, null));

        mHelpTextView = mView.findViewById(R.id.text);
        mUpImageView = mView.findViewById(R.id.arrow_up);
        mDownImageView = mView.findViewById(R.id.arrow_down);

        mHelpTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mHelpTextView.setSelected(true);
    }

    public InstructionPopup(Context context, String popupID) {
        this(context, "", R.layout.instruction_popup_layout, popupID);

    }

    public InstructionPopup(Context context, String text, String popupID) {
        this(context, popupID);

        setText(text);
    }

    public void showDelayed(View anchor) {
        new Handler().postDelayed(() -> {
            // Calling this delayed helps us to use this before the view is created.
            show(anchor);
        }, 300);
    }

    public void showInstant(View anchor) {
        show(anchor);
    }

    private void show(View anchor) {

        // Remove the previous Popup if the same popup already exists.
        if (InstructionPopupManager.getInstance().isPopupOpen(this)) {
            InstructionPopupManager.getInstance().removePopup(this);
        }
        InstructionPopupManager.getInstance().addPopup(this);

        preShow();

        // Get the rect of the view the popup will be attached to
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        Rect anchorRect = new Rect(location[0], location[1], location[0]
                + anchor.getWidth(), location[1] + anchor.getHeight());

        // If the view is actually not visible, this will be 0, in this case we don't want to show it.
        if (anchorRect.left == 0) {
            InstructionPopupManager.getInstance().removePopup(this);
            return;
        }

        // Get the popup size
        mView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        int popupHeight = mView.getMeasuredHeight();
        int popupWidth = mView.getMeasuredWidth();

        // Get the screen size
        Point outPoint = new Point();
        mWindowManager.getDefaultDisplay().getSize(outPoint);
        final int screenWidth = outPoint.x;
        final int screenHeight = outPoint.y;

        // Calculate the y position of the popup and determine if it shown above or beyond the view which it is attached to.
        int yPopupPos = anchorRect.top - popupHeight;
        boolean onTop = true;
        if (anchorRect.top < screenHeight / 2) {
            yPopupPos = anchorRect.bottom;
            onTop = false;
        }

        // Get the arrow that is needed and set the visibility
        int whichArrow;
        whichArrow = ((onTop) ? R.id.arrow_down : R.id.arrow_up);
        View visibleArrow = whichArrow == R.id.arrow_up ? mUpImageView : mDownImageView;
        View hiddenArrow = whichArrow == R.id.arrow_up ? mDownImageView : mUpImageView;
        visibleArrow.setVisibility(View.VISIBLE);
        hiddenArrow.setVisibility(View.INVISIBLE);

        // Calculate the x position of the popup
        int xPopupPos = 0;
        if (anchorRect.left + popupWidth > screenWidth) {
            // Extreme right
            xPopupPos = (screenWidth - popupWidth);
        } else if (anchorRect.centerX() - (popupWidth / 2) < 0) {
            // Extreme left
            xPopupPos = 0;
        } else {
            // Inbetween
            xPopupPos = (anchorRect.centerX() - (popupWidth / 2));
        }

        // Move the Arrow to the correct position
        ViewGroup.MarginLayoutParams visibleArrowLayoutParams = (ViewGroup.MarginLayoutParams) visibleArrow.getLayoutParams();
        final int arrowWidth = visibleArrow.getMeasuredWidth();
        visibleArrowLayoutParams.leftMargin = (anchorRect.centerX() - xPopupPos) - (arrowWidth / 2);

        // Limit max popup height
        if (onTop) {
            mHelpTextView.setMaxHeight(anchorRect.top - anchorRect.height());

        } else {
            mHelpTextView.setMaxHeight(screenHeight - yPopupPos);
        }

        // Show the popup
        mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPopupPos, yPopupPos);

        // Animate the popup
        mView.setAnimation(AnimationUtils.loadAnimation(mContext,
                R.anim.instruction_popup_bounce));
    }

    private void preShow() {
        if (mView == null)
            throw new IllegalStateException("view undefined");

        if (showListener != null) {
            showListener.onPreShow();
            showListener.onShow();
        }

        if (mBackgroundDrawable == null)
            mWindow.setBackgroundDrawable(null);
        else
            mWindow.setBackgroundDrawable(mBackgroundDrawable);

        mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setTouchable(true);
        mWindow.setFocusable(true);
        mWindow.setOutsideTouchable(true);
        mWindow.setTouchModal(false);
        mWindow.setContentView(mView);
    }

    public void setBackgroundDrawable(Drawable background) {
        mBackgroundDrawable = background;
    }

    public void setContentView(View root) {
        mView = root;

        mWindow.setContentView(root);
    }

    public void setContentView(int layoutResID) {
        LayoutInflater inflator = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setContentView(inflator.inflate(layoutResID, null));
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mWindow.setOnDismissListener(listener);
    }

    public void dismiss() {
        mWindow.dismiss();
        if (showListener != null) {
            showListener.onDismiss();
        }
    }

    public void setText(String text) {
        mHelpTextView.setText(text);
    }

    public static interface ShowListener {
        void onPreShow();

        void onDismiss();

        void onShow();
    }

    public void setShowListener(ShowListener showListener) {
        this.showListener = showListener;
    }
}

