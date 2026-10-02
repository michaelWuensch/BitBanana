package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import androidx.viewpager.widget.ViewPager;

import java.lang.reflect.Field;

import app.michaelwuensch.bitbanana.util.BBLog;

public class CustomViewPager extends ViewPager {
    private static final String LOG_TAG = CustomViewPager.class.getSimpleName();

    private FixedSpeedScroller mScroller = null;

    private boolean isSwipeable = true;
    private boolean mForceNoSwipe = false;
    // True while the ViewPager has received events of the current gesture.
    private boolean mGestureForwarded = false;

    public CustomViewPager(Context context) {
        super(context);
        init();
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private boolean isRtl() {
        return getResources().getConfiguration().getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    private void mirrorAnimation() {
        setPageTransformer(false, (page, position) -> {
            page.setTranslationX(-position * page.getWidth() * 2);
        });
    }

    /**
     * Mirror the MotionEvent horizontally around the center of this View.
     */
    private MotionEvent mirrorEvent(MotionEvent ev) {
        MotionEvent copy = MotionEvent.obtain(ev);
        // Matrix mirror: x' = width - x (i.e., scaleX = -1 around center)
        Matrix m = new Matrix();
        m.setScale(-1f, 1f, getWidth() / 2f, 0f);
        copy.transform(m); // applies to all pointers (multi-touch safe)
        return copy;
    }

    public void setSwipeable(boolean swipeable) {
        isSwipeable = swipeable;
    }

    public void setForceNoSwipe(boolean forceNoSwipe) {
        mForceNoSwipe = forceNoSwipe;
    }

    // lets us disable scrolling
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isRtl()) {
            mirrorAnimation();
        }
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_UP) {
            // Always reenable swiping on an up event
            isSwipeable = true;
        }
        if (mForceNoSwipe || !isSwipeable) {
            cancelForwardedGesture(ev);
            return false;
        }
        boolean result = forwardTouchEvent(ev);
        mGestureForwarded = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL;
        return result;
    }

    /**
     * The ViewPager tracks the pointers of a gesture. If it stops receiving events in the middle of a gesture
     * (e.g. because swiping got disabled), it misses pointer changes and crashes with "pointerIndex out of range"
     * on the next event it receives. Therefore we cancel its gesture instead of just withholding the events.
     */
    private void cancelForwardedGesture(MotionEvent ev) {
        if (!mGestureForwarded) {
            return;
        }
        mGestureForwarded = false;
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        forwardTouchEvent(cancel);
        cancel.recycle();
    }

    private boolean forwardTouchEvent(MotionEvent ev) {
        MotionEvent event = isRtl() ? mirrorEvent(ev) : ev;
        try {
            return super.onTouchEvent(event);
        } catch (IllegalArgumentException e) {
            // Safety net for the known ViewPager "pointerIndex out of range" issue. The ViewPager resets its state on the next down event.
            BBLog.w(LOG_TAG, "ViewPager touch event failed: " + e.getMessage());
            return false;
        } finally {
            if (event != ev) {
                event.recycle();
            }
        }
    }

    /*
     * Override the Scroller instance with our own class so we can change the
     * duration
     */
    private void init() {
        try {
            Class<?> viewpager = ViewPager.class;
            Field scroller = viewpager.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            mScroller = new FixedSpeedScroller(getContext(),
                    new DecelerateInterpolator());
            scroller.set(this, mScroller);
            if (isRtl()) {
                mirrorAnimation();
            }
        } catch (Exception ignored) {
        }
    }

    /*
     * Set the factor by which the duration will change
     */
    public void setScrollDuration(int duration) {
        mScroller.setScrollDuration(duration);
    }

    private class FixedSpeedScroller extends Scroller {

        private int mDuration = 300;

        public FixedSpeedScroller(Context context) {
            super(context);
        }

        public FixedSpeedScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        public FixedSpeedScroller(Context context, Interpolator interpolator, boolean flywheel) {
            super(context, interpolator, flywheel);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        public void setScrollDuration(int duration) {
            mDuration = duration;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mForceNoSwipe) return false;
        if (!isSwipeable) return false;

        MotionEvent event = isRtl() ? mirrorEvent(ev) : ev;
        try {
            boolean intercepted = super.onInterceptTouchEvent(event);
            if (intercepted) {
                // The ViewPager started dragging, the following events of this gesture go to onTouchEvent().
                mGestureForwarded = true;
            }
            return intercepted;
        } catch (IllegalArgumentException e) {
            // Safety net for the known ViewPager "pointerIndex out of range" issue. The ViewPager resets its state on the next down event.
            BBLog.w(LOG_TAG, "ViewPager intercept touch event failed: " + e.getMessage());
            return false;
        } finally {
            if (event != ev) {
                event.recycle();
            }
        }
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        // Flip direction queries so edge-glow/overscroll logic stays correct.
        if (mForceNoSwipe) return false;
        if (isRtl()) direction = -direction;
        return super.canScrollHorizontally(direction);
    }
}
