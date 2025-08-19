package app.michaelwuensch.bitbanana.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import app.michaelwuensch.bitbanana.R;

public final class RtlTransitions {

    private RtlTransitions() {
    }

    private static boolean isRtl(Context ctx) {
        return ctx.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Call immediately after startActivity(...) to animate the OPEN transition.
     */
    public static void applyOpenTransition(Activity activity) {
        if (isRtl(activity)) {
            activity.overridePendingTransition(
                    R.anim.slide_in_end_rtl,
                    R.anim.slide_out_start_rtl
            );
        }
    }

    /**
     * Call immediately after finish() (or onBackPressed()) to animate the CLOSE transition.
     */
    public static void applyCloseTransition(Activity activity) {
        if (isRtl(activity)) {
            activity.overridePendingTransition(
                    R.anim.slide_in_start_rtl,
                    R.anim.slide_out_end_rtl
            );
        }
    }
}
