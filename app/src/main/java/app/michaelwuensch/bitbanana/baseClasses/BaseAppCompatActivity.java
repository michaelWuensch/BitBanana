package app.michaelwuensch.bitbanana.baseClasses;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RtlTransitions;

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        RtlTransitions.applyOpenTransition(this);
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        super.startActivity(intent, options);
        RtlTransitions.applyOpenTransition(this);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        RtlTransitions.applyOpenTransition(this);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
        RtlTransitions.applyOpenTransition(this);
    }

    @Override
    public void finish() {
        super.finish();
        RtlTransitions.applyCloseTransition(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeScreenRecordingSecurity();

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
