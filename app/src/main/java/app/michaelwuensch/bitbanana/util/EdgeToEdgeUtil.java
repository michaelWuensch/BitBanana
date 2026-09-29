package app.michaelwuensch.bitbanana.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.ActionBarOverlayLayout;

/**
 * Starting with Android 15 (API 35) edge-to-edge is enforced for apps targeting SDK 35 or higher
 * and starting with SDK 36 the opt-out (windowOptOutEdgeToEdgeEnforcement) is no longer possible.
 * <p>
 * This util restores the look BitBanana had before edge-to-edge was enforced:
 * - The content of the activity is laid out between the status bar and the navigation bar.
 * - The status bar and navigation bar backgrounds are painted in the colors defined in the theme
 * (android:statusBarColor and android:navigationBarColor).
 * - The soft input modes (adjustResize, adjustPan, ...) behave as before.
 * <p>
 * On devices below Android 15 nothing is changed, as the system still handles everything for us.
 */
public class EdgeToEdgeUtil {

    /**
     * Has to be called after the content view of the activity was set, e.g. in onPostCreate().
     */
    @SuppressLint("RestrictedApi") // ActionBarOverlayLayout is only used for an instanceof check.
    public static void applyLegacySystemBarLayout(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM)
            return;

        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        View contentRoot = findContentRoot(decorView);
        if (contentRoot == null)
            return;

        // Make sure we are drawing edge-to-edge, so that the behavior is identical on all API 35+ devices.
        window.setDecorFitsSystemWindows(false);
        // The system would otherwise draw a translucent scrim behind the 3-button navigation bar.
        window.setNavigationBarContrastEnforced(false);
        window.setStatusBarContrastEnforced(false);

        // Read the system bar colors from the theme. Window.getStatusBarColor() can not be used as it is not read from the theme with enforced edge-to-edge.
        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{android.R.attr.statusBarColor, android.R.attr.navigationBarColor});
        int statusBarColor = a.getColor(0, Color.BLACK);
        int navigationBarColor = a.getColor(1, Color.BLACK);
        a.recycle();

        SystemBarsBackgroundDrawable barsBackground = new SystemBarsBackgroundDrawable(contentRoot.getBackground(), statusBarColor, navigationBarColor);
        contentRoot.setBackground(barsBackground);

        contentRoot.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                // The (deprecated) system window insets exactly represent what the system used to fit the content into before edge-to-edge was enforced.
                // They contain the status and navigation bar, the IME only if the (resolved) soft input mode is adjustResize and no status bar if the window is fullscreen.
                @SuppressWarnings("deprecation")
                Insets legacyInsets = insets.getSystemWindowInsets();
                // Before edge-to-edge the content never extended into a display cutout (LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT).
                Insets cutoutInsets = insets.getInsets(WindowInsets.Type.displayCutout());
                Insets applied = Insets.max(legacyInsets, cutoutInsets);

                v.setPadding(applied.left, applied.top, applied.right, applied.bottom);

                int statusBarHeight = insets.getInsets(WindowInsets.Type.statusBars()).top;
                Insets navigationBars = insets.getInsets(WindowInsets.Type.navigationBars());
                // If there is no visible status bar (e.g. fullscreen) the cutout area used to be letterboxed in black.
                barsBackground.setStatusBarVisible(legacyInsets.top > 0 && statusBarHeight > 0);
                barsBackground.setBars(applied.top,
                        Math.min(navigationBars.bottom, applied.bottom),
                        applied.left,
                        applied.right);

                // Children must not handle these insets again. Before edge-to-edge they also didn't receive them.
                return insets.inset(applied);
            }
        });

        // If the window decor has an action bar, AppCompat's ActionBarOverlayLayout no longer places the content below the action bar
        // when edge-to-edge is enforced. Instead it dispatches the action bar height as inset to the content view, which we apply here.
        View content = decorView.findViewById(android.R.id.content);
        if (content != null && content.getParent() instanceof ActionBarOverlayLayout) {
            content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @NonNull
                @Override
                public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                    @SuppressWarnings("deprecation")
                    Insets actionBarInsets = insets.getSystemWindowInsets();
                    v.setPadding(actionBarInsets.left, actionBarInsets.top, actionBarInsets.right, actionBarInsets.bottom);
                    return insets.inset(actionBarInsets);
                }
            });
        }

        contentRoot.requestApplyInsets();
    }

    /**
     * Returns the top most view of the activities view hierarchy that is a direct child of the DecorView.
     * This view contains the action bar (if present) and the content.
     */
    @Nullable
    private static View findContentRoot(View decorView) {
        View view = decorView.findViewById(android.R.id.content);
        if (view == null)
            return null;
        ViewParent parent = view.getParent();
        while (parent instanceof ViewGroup && parent != decorView) {
            view = (View) parent;
            parent = view.getParent();
        }
        return parent == decorView ? view : null;
    }

    /**
     * Draws the backgrounds of the system bars inside the padding area of the content root.
     * Everything else is delegated to the original background of the view (usually none).
     */
    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private static class SystemBarsBackgroundDrawable extends Drawable {

        private final Drawable mOriginalBackground;
        private final Paint mStatusBarPaint = new Paint();
        private final Paint mNavigationBarPaint = new Paint();
        private final Paint mLetterboxPaint = new Paint();
        private boolean mStatusBarVisible = true;
        private int mTop;
        private int mBottom;
        private int mLeft;
        private int mRight;

        SystemBarsBackgroundDrawable(@Nullable Drawable originalBackground, int statusBarColor, int navigationBarColor) {
            mOriginalBackground = originalBackground;
            mStatusBarPaint.setColor(statusBarColor);
            mNavigationBarPaint.setColor(navigationBarColor);
            mLetterboxPaint.setColor(Color.BLACK);
        }

        void setStatusBarVisible(boolean visible) {
            mStatusBarVisible = visible;
        }

        void setBars(int top, int bottom, int left, int right) {
            mTop = top;
            mBottom = bottom;
            mLeft = left;
            mRight = right;
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(@NonNull Rect bounds) {
            if (mOriginalBackground != null)
                mOriginalBackground.setBounds(bounds);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (mOriginalBackground != null)
                mOriginalBackground.draw(canvas);

            Rect b = getBounds();
            if (mTop > 0)
                canvas.drawRect(b.left, b.top, b.right, b.top + mTop, mStatusBarVisible ? mStatusBarPaint : mLetterboxPaint);
            if (mBottom > 0)
                canvas.drawRect(b.left, b.bottom - mBottom, b.right, b.bottom, mNavigationBarPaint);
            if (mLeft > 0)
                canvas.drawRect(b.left, b.top + mTop, b.left + mLeft, b.bottom - mBottom, mNavigationBarPaint);
            if (mRight > 0)
                canvas.drawRect(b.right - mRight, b.top + mTop, b.right, b.bottom - mBottom, mNavigationBarPaint);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
