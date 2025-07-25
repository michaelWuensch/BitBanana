package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
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

    public void setSwipeable(boolean swipeable) {
        isSwipeable = swipeable;
    }

    public void setForceNoSwipe(boolean forceNoSwipe) {
        mForceNoSwipe = forceNoSwipe;
    }

    // lets us disable scrolling
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
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
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return !mForceNoSwipe && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return !mForceNoSwipe && super.canScrollHorizontally(direction);
    }
}
