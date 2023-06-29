package app.michaelwuensch.bitbanana.contacts;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.UserAvatarView;
import app.michaelwuensch.bitbanana.lightning.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.UserGuardian;

public class ContactDetailsActivity extends BaseAppCompatActivity {

    public static final int RESPONSE_CODE_SEND_MONEY = 212;
    public static final int RESPONSE_CODE_OPEN_CHANNEL = 213;

    private String mDataToEncode;
    private Button mBtnContactName;
    private UserAvatarView mUserAvatarView;
    private View mContactsDetailsView;
    private TextView mTVContactType;
    private TextView mTvDetailedData;
    private Contact mContact;
    private BottomNavigationView mBottomButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mContact = (Contact) extras.getSerializable(ManageContactsActivity.EXTRA_CONTACT);
        }

        mUserAvatarView = findViewById(R.id.userAvatarView);
        mBtnContactName = findViewById(R.id.contactNameButton);
        mBottomButtons = findViewById(R.id.bottomButtons);
        mTvDetailedData = findViewById(R.id.detailedData);
        mTVContactType = findViewById(R.id.contactType);
        mContactsDetailsView = findViewById(R.id.contactDetailsView);

        switch (mContact.getContactType()) {
            case NODEPUBKEY:
                mUserAvatarView.setupWithNodeUri(new LightningNodeUri.Builder().setPubKey(mContact.getContactData()).build(), true);
                mTVContactType.setText(getApplicationContext().getString(R.string.contact_type_ln_node));
                break;
            case LNADDRESS:
                mBottomButtons.getMenu().clear(); //clear old inflated items.
                mBottomButtons.inflateMenu(R.menu.contact_details_menu_bottom_ln_address);
                mUserAvatarView.setupWithLNAddress(mContact.getLightningAddress(), true);
                mTVContactType.setText(getApplicationContext().getString(R.string.ln_address));
                break;
        }

        mDataToEncode = mContact.getContactData();
        mBtnContactName.setText(mContact.getAlias());
        mTvDetailedData.setText(mContact.getContactData());

        // Action when clicked on contact name
        mBtnContactName.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                rename();
            }
        });

        mUserAvatarView.setOnStateChangedListener(new UserAvatarView.OnStateChangedListener() {
            @Override
            public void onReveal() {
                mContactsDetailsView.setVisibility(View.VISIBLE);
                mBtnContactName.setVisibility(View.GONE);
            }

            @Override
            public void onHide() {
                mContactsDetailsView.setVisibility(View.GONE);
                mBtnContactName.setVisibility(View.VISIBLE);
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
                        shareIntent.putExtra(Intent.EXTRA_TEXT, mDataToEncode);
                        shareIntent.setType("text/plain");
                        String title = getResources().getString(R.string.shareDialogTitle);
                        startActivity(Intent.createChooser(shareIntent, title));
                        break;
                    case R.id.action_copy:
                        switch (mContact.getContactType()) {
                            case NODEPUBKEY:
                                // Ask user to confirm risks about clipboard manipulation
                                new UserGuardian(ContactDetailsActivity.this, () -> {
                                    // Copy data to clipboard
                                    ClipBoardUtil.copyToClipboard(getApplicationContext(), "NodePubKey", mDataToEncode);
                                }).securityCopyToClipboard(mDataToEncode, UserGuardian.CLIPBOARD_DATA_TYPE_NODE_URI);
                                break;
                            case LNADDRESS:
                                ClipBoardUtil.copyToClipboard(getApplicationContext(), "Lightning Address", mDataToEncode);
                                break;
                        }
                        break;
                    case R.id.action_send_money:
                        Intent intent = new Intent();
                        switch (mContact.getContactType()) {
                            case NODEPUBKEY:
                                intent.putExtra(ScanContactActivity.EXTRA_NODE_URI, mContact.getAsNodeUri());
                                break;
                            case LNADDRESS:
                                intent.putExtra(ScanContactActivity.EXTRA_LN_ADDRESS, mContact.getLightningAddress());
                                break;
                        }
                        setResult(RESPONSE_CODE_SEND_MONEY, intent);
                        finish();
                        break;
                    case R.id.action_open_channel:
                        Intent intentOpenChannel = new Intent();
                        intentOpenChannel.putExtra(ScanContactActivity.EXTRA_NODE_URI, mContact.getAsNodeUri());
                        setResult(RESPONSE_CODE_OPEN_CHANNEL, intentOpenChannel);
                        finish();
                }
                // Return false as we actually don't want to select it.
                return false;
            }
        });
    }


    private void rename() {
        ContactsManager cm = ContactsManager.getInstance();
        cm.showContactNameInputDialog(ContactDetailsActivity.this, mContact.getAlias(), mContact.getContactData(), mContact.getContactType(), new ContactsManager.OnNameConfirmedListener() {
            @Override
            public void onNameAccepted() {
                mContact = cm.getContactByContactData(mContact.getContactData());
                mBtnContactName.setText(mContact.getAlias());
            }

            @Override
            public void onCancelled() {

            }
        });
    }

    private void delete() {
        ContactsManager.getInstance().removeContact(mContact);
        try {
            ContactsManager.getInstance().apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finish();
    }

    @Override
    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.contact_details_menu, menu);

        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;

            //noinspection RestrictedApi
            m.setOptionalIconsVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_edit:
                rename();
                break;
            case R.id.action_delete:
                delete();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}