package app.michaelwuensch.bitbanana;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.IdentitySwitchView;
import app.michaelwuensch.bitbanana.customView.UserAvatarView;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;

public class OwnWatchtowerActivity extends BaseAppCompatActivity {

    private UserAvatarView mWatchtowerAvatarView;
    private BottomNavigationView mBottomButtons;
    private IdentitySwitchView mIdentitySwitchView;
    private TextView mTvWatchtowerString;
    private TextView mTvTapHint;
    private boolean mHasMultipleAddresses;
    private LightningNodeUri mWatchtowerUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity);

        mWatchtowerAvatarView = findViewById(R.id.userAvatarView);
        mIdentitySwitchView = findViewById(R.id.identityTypeSwitcher);
        mBottomButtons = findViewById(R.id.bottomButtons);
        mTvWatchtowerString = findViewById(R.id.identityString);
        mTvTapHint = findViewById(R.id.tapHint);

        mTvTapHint.setText(R.string.own_watchtower_tap_hint);

        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            mWatchtowerUri = (LightningNodeUri) extras.getSerializable("lightningNodeUri");
        }

        mWatchtowerAvatarView.setupWithNodeUri(mWatchtowerUri, true);
        mHasMultipleAddresses = false;
        if (mHasMultipleAddresses) {
            mIdentitySwitchView.setVisibility(View.VISIBLE);
            mIdentitySwitchView.setIdentityTypeChangedListener(new IdentitySwitchView.IdentityTypeChangedListener() {
                @Override
                public void onIdentityTypeChanged(IdentitySwitchView.IdentityType identityType) {
                    switch (identityType) {
                        case TOR:
                            mWatchtowerAvatarView.showIdentity(true);
                            break;
                        case PUBLIC:
                            mWatchtowerAvatarView.showIdentity(false);
                            break;
                        default:
                            mWatchtowerAvatarView.showIdentity(true);
                    }
                    mTvWatchtowerString.setText(mWatchtowerAvatarView.getCurrentNodeIdentity().getAsString());
                }
            });
            if (!PrefsUtil.getPrefs().getBoolean(PrefsUtil.SHOW_IDENTITY_TAP_HINT, true)) {
                mTvTapHint.setVisibility(View.GONE);
            }
        } else {
            mIdentitySwitchView.setVisibility(View.GONE);
        }

        mWatchtowerAvatarView.setOnStateChangedListener(new UserAvatarView.OnStateChangedListener() {
            @Override
            public void onReveal() {
                mTvWatchtowerString.setVisibility(View.VISIBLE);
                mTvTapHint.setVisibility(View.GONE);
                mTvWatchtowerString.setText(mWatchtowerAvatarView.getCurrentNodeIdentity().getAsString());
                PrefsUtil.editPrefs().putBoolean(PrefsUtil.SHOW_IDENTITY_TAP_HINT, false).apply();
            }

            @Override
            public void onHide() {
                mTvWatchtowerString.setVisibility(View.GONE);
                if (!mHasMultipleAddresses) {
                    mTvTapHint.setVisibility(View.VISIBLE);
                }
            }
        });


        mBottomButtons.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                switch (id) {
                    case R.id.action_share:
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, mWatchtowerAvatarView.getCurrentNodeIdentity().getAsString());
                        shareIntent.setType("text/plain");
                        String title = getResources().getString(R.string.shareDialogTitle);
                        startActivity(Intent.createChooser(shareIntent, title));
                        break;
                    case R.id.action_copy:
                        // Ask user to confirm risks about clipboard manipulation

                        new UserGuardian(OwnWatchtowerActivity.this, new UserGuardian.OnGuardianConfirmedListener() {
                            @Override
                            public void onConfirmed() {
                                // Copy data to clipboard
                                ClipBoardUtil.copyToClipboard(getApplicationContext(), "WatchtowerUri", mWatchtowerAvatarView.getCurrentNodeIdentity().getAsString());
                            }

                            @Override
                            public void onCancelled() {

                            }
                        }).securityCopyToClipboard(mWatchtowerAvatarView.getCurrentNodeIdentity().getAsString(), UserGuardian.CLIPBOARD_DATA_TYPE_NODE_URI);
                        break;
                }
                // Return false as we actually don't want to select it.
                return false;
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
            HelpDialogUtil.showDialog(OwnWatchtowerActivity.this, R.string.help_dialog_own_watchtower);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}