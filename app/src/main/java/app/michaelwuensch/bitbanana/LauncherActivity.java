package app.michaelwuensch.bitbanana;

import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.NfcUtil;

public class LauncherActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = LauncherActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Save data when App was started with a task.

        // BitBanana was started from an URI link.
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            App.getAppContext().setUriSchemeData(uri.toString());
            BBLog.d(LOG_TAG, "URI was detected: " + uri.toString());
        }

        // BitBanana was started using NFC.
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NfcUtil.readTag(LauncherActivity.this, intent, payload -> App.getAppContext().setUriSchemeData(payload));
        }

        Intent landingIntent = new Intent(this, LandingActivity.class);
        landingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        // FinishAffinity is needed here, otherwise the app will close when activating stealth mode
        finishAffinity();

        startActivity(landingIntent);
    }
}
