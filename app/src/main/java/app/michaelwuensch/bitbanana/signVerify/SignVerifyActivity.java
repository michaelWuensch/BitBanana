package app.michaelwuensch.bitbanana.signVerify;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.CustomViewPager;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;

public class SignVerifyActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = SignVerifyActivity.class.getSimpleName();

    private TabLayout mTabLayoutMode;
    private CustomViewPager mViewPager;
    private SignVerifyPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_verify);

        mViewPager = findViewById(R.id.sign_verify_viewpager);
        //mViewPager.setScrollDuration(250);
        mAdapter = new SignVerifyPagerAdapter(this);
        mViewPager.setForceNoSwipe(true);
        mViewPager.setAdapter(mAdapter);

        mTabLayoutMode = findViewById(R.id.modeTabLayout);
        mTabLayoutMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        mViewPager.setCurrentItem(0);
                        break;
                    case 1:
                        mViewPager.setCurrentItem(1);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(SignVerifyActivity.this, R.string.help_dialog_sign_verify);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
