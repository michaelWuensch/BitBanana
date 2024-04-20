package app.michaelwuensch.bitbanana.settings;


import android.os.Bundle;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;


public class SettingsCustomFeeEstimationProviderActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainContent, new SettingsCustomFeeEstimationProviderFragment())
                .commit();
    }
}