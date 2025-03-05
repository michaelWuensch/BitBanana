package app.michaelwuensch.bitbanana.baseClasses;

import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.PrefsUtil;

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        initializeScreenRecordingSecurity();

        // Prevent app from turning off screen on inactivity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Enable back button if an action bar is supported by the theme
        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Secure against screenshots and automated screen recording.
     * Keep in mind that this does not prevent popups and other
     * dialogs to be secured as well. Extra security measures have to be considered.
     * Check out the following link for more details:
     * https://github.com/commonsguy/cwac-security/blob/master/docs/FLAGSECURE.md
     */
    private void initializeScreenRecordingSecurity() {
        if (PrefsUtil.isScreenRecordingPrevented()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    // Go back if back button was pressed on action bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showError(String message, int durationMS) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, durationMS);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        snackbar.show();
    }
}
