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

public class CustomViewPager extends ViewPager {
    private FixedSpeedScroller mScroller = null;

    private boolean isSwipeable = true;
    private boolean mForceNoSwipe = false;

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
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            // Always reenable swiping on an up event
            isSwipeable = true;
        }
        if (mForceNoSwipe) {
            return false;
        }
        if (isSwipeable) {
            if (ev.getX() < 0 || ev.getY() < 0) {
                return false;
            }
            MotionEvent safeEvent = MotionEvent.obtain(ev);
            if (isRtl()) {
                MotionEvent mirrored = mirrorEvent(safeEvent);
                boolean result = super.onTouchEvent(mirrored);
                mirrored.recycle();
                return result;
            }
            boolean result = super.onTouchEvent(safeEvent);
            safeEvent.recycle();
            return result;
        } else {
            return false;
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

        if (isRtl()) {
            MotionEvent mirrored = mirrorEvent(ev);
            boolean handled = super.onInterceptTouchEvent(mirrored);
            mirrored.recycle();
            return handled;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        // Flip direction queries so edge-glow/overscroll logic stays correct.
        if (mForceNoSwipe) return false;
        if (isRtl()) direction = -direction;
        return super.canScrollHorizontally(direction);
    }
}
