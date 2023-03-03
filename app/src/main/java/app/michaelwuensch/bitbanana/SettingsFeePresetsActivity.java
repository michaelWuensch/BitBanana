package app.michaelwuensch.bitbanana;


import android.os.Bundle;

import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.fragments.SettingsFeePresetsFragment;


public class SettingsFeePresetsActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainContent, new SettingsFeePresetsFragment())
                .commit();
    }
}
