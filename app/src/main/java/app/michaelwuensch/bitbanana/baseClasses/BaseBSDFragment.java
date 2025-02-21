package app.michaelwuensch.bitbanana.baseClasses;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.PrefsUtil;

public class BaseBSDFragment extends RxBSDFragment {

    BottomSheetBehavior<FrameLayout> mBottomSheetBehavior;

    // This function is used to fix a problem where background dimming did not work correctly sometimes when coming from the scan screen.
    public void showDelayed(FragmentManager fragmentManager, String tag) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                show(fragmentManager, tag);
            }
        }, 100);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FrameLayout bottomSheet = getDialog().findViewById(R.id.design_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        // We do not want the bottom sheet dialog to cover the whole screen
        mBottomSheetBehavior.setPeekHeight((int) (metrics.heightPixels * 0.9));

        // Set scroll box max height in relation to screen height
        // Only for layouts with ScrollableBottomSheet
        try {
            ConstraintLayout csLayout = bottomSheet.findViewById(R.id.scrollableBsdRoot);
            ConstraintSet csRoot = new ConstraintSet();
            csRoot.clone(csLayout);
            csRoot.constrainMaxHeight(R.id.content, (int) (metrics.heightPixels * 0.7));
            csRoot.applyTo(csLayout);

            // Adjust Resize did not work correctly. It was not possible to scroll all the way down on a bottom sheet when soft keyboard was shown.
            // We try to fix this by decreasing the size when the keyboard opens.
            KeyboardVisibilityEvent.setEventListener(getActivity(), getViewLifecycleOwner(), new KeyboardVisibilityEventListener() {
                @Override
                public void onVisibilityChanged(boolean b) {
                    ConstraintSet csRoot = new ConstraintSet();
                    csRoot.clone(csLayout);
                    if (b) {
                        csRoot.constrainMaxHeight(R.id.content, (int) (metrics.heightPixels * 0.4));
                        csRoot.applyTo(csLayout);
                    } else {
                        csRoot.constrainMaxHeight(R.id.content, (int) (metrics.heightPixels * 0.7));
                        csRoot.applyTo(csLayout);
                    }
                }
            });

        } catch (Exception ignored) {
        }

        // Apply FLAG_SECURE to dialog to prevent screen recording
        if (PrefsUtil.isScreenRecordingPrevented()) {
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    public int getTheme() {
        return R.style.BBBottomSheetDialogTheme;
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getView().getRootView().getWindowToken(), 0);
    }

    public void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    protected void showError(String message, int durationMS) {
        if (getView() != null && getView().findViewById(R.id.coordinator) != null) {
            Snackbar snackbar = Snackbar.make(getView().findViewById(R.id.coordinator), message, durationMS);
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
            snackbar.show();
        } else {
            showToast(message, durationMS > 3000 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        }
    }

    protected void showToast(String message, int length) {
        Toast.makeText(getActivity(), message, length).show();
    }
}
