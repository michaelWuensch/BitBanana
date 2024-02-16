package app.michaelwuensch.bitbanana.listViews.contacts;

import static app.michaelwuensch.bitbanana.home.ScanActivity.EXTRA_GENERIC_SCAN_DATA;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.contacts.Contact;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.customView.ManualSendInputView;
import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.listViews.contacts.itemDetails.ContactDetailsActivity;
import app.michaelwuensch.bitbanana.listViews.contacts.items.ContactItemViewHolder;
import app.michaelwuensch.bitbanana.models.LNAddress;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ManageContactsActivity extends BaseAppCompatActivity implements ContactSelectListener {

    public static final String EXTRA_CONTACT = "contactExtra";
    public static final String EXTRA_CONTACT_ACTIVITY_MODE = "contactActivityMode";
    public static final int MODE_MANAGE = 0;
    public static final int MODE_SEND = 1;
    public static final int MODE_OPEN_CHANNEL = 2;
    private static final String LOG_TAG = ManageContactsActivity.class.getSimpleName();
    private static int REQUEST_CODE_ADD_CONTACT = 111;
    private static int REQUEST_CODE_CONTACT_ACTION = 112;
    private RecyclerView mRecyclerView;
    private ContactItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private View mVEFab;
    private View mVEFabOptionManually;
    private View mVEFabOptionScan;
    private View mContactsHeaderLayout;
    private ManualSendInputView mManualInput;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private List<Contact> mContactItems;
    private TextView mEmptyListText;

    private int mMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_contacts);

        mVEFab = findViewById(R.id.expandable_fab_layout);
        mVEFabOptionManually = findViewById(R.id.efabManualOption);
        mVEFabOptionScan = findViewById(R.id.efabScanOption);
        mContactsHeaderLayout = findViewById(R.id.contactsHeaderLayout);
        mManualInput = findViewById(R.id.manualInput);
        mRecyclerView = findViewById(R.id.contactsList);
        mEmptyListText = findViewById(R.id.listEmpty);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mMode = extras.getInt(EXTRA_CONTACT_ACTIVITY_MODE);
        } else {
            mMode = 0;
        }

        switch (mMode) {
            case MODE_MANAGE:
                setTitle(R.string.activity_manage_contacts);
                break;
            case MODE_SEND:
                setTitle(R.string.activity_manage_contacts_send_mode);
                setupSendMode();
                break;
            case MODE_OPEN_CHANNEL:
                setTitle(R.string.activity_manage_contacts_open_channel_mode);
                mVEFab.setVisibility(View.GONE);
                break;
        }

        mContactItems = new ArrayList<>();

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(ManageContactsActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new ContactItemAdapter(this);
        mRecyclerView.setAdapter(mAdapter);


        mVEFabOptionScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Add a new contact by scanning
                Intent intent = new Intent(ManageContactsActivity.this, ScanContactActivity.class);
                startActivityForResult(intent, REQUEST_CODE_ADD_CONTACT);
            }
        });

        mVEFabOptionManually.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Add a new contact manually
                Intent intent = new Intent(ManageContactsActivity.this, ManualAddContactActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateContactDisplayList();
    }

    private void updateContactDisplayList() {
        mContactItems.clear();
        if (FeatureManager.isContactsEnabled()) {
            ContactsManager contactsManager = ContactsManager.getInstance();
            mContactItems.addAll(contactsManager.getAllContacts());

            // Show "No wallets" if the list is empty
            if (mContactItems.size() == 0) {
                mEmptyListText.setVisibility(View.VISIBLE);
            } else {
                mEmptyListText.setVisibility(View.GONE);
            }

            mAdapter.replaceAll(mContactItems);

            // The following is needed for the names to change after renaming.
            for (int i = 0; i < mAdapter.getItemCount(); i++) {
                String correctName = mAdapter.getItemAtPosition(i).getAlias();
                RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(i);
                if (viewHolder != null) {
                    String displayedName = ((ContactItemViewHolder) viewHolder).getName();
                    if (!correctName.equals(displayedName)) {
                        mAdapter.notifyItemChanged(i);
                    }
                }
            }
            BBLog.v(LOG_TAG, "Contacts list updated!");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_CONTACT && resultCode == RESULT_OK) {
            if (data != null) {
                ContactsManager cm = ContactsManager.getInstance();
                LightningNodeUri nodeUri = (LightningNodeUri) data.getSerializableExtra(ScanContactActivity.EXTRA_NODE_URI);
                if (nodeUri != null) {
                    if (cm.doesContactDataExist(nodeUri.getPubKey())) {
                        Toast.makeText(this, R.string.contact_already_exists, Toast.LENGTH_LONG).show();
                    } else {
                        cm.showContactNameInputDialog(this, null, nodeUri.getPubKey(), Contact.ContactType.NODEPUBKEY, new ContactsManager.OnNameConfirmedListener() {
                            @Override
                            public void onNameAccepted() {
                                mAdapter.add(cm.getContactByContactData(nodeUri.getPubKey()));
                                mEmptyListText.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancelled() {

                            }
                        });
                    }
                } else {
                    LNAddress lnAddress = (LNAddress) data.getSerializableExtra(ScanContactActivity.EXTRA_LN_ADDRESS);
                    if (cm.doesContactDataExist(lnAddress.toString())) {
                        Toast.makeText(this, R.string.contact_already_exists, Toast.LENGTH_LONG).show();
                    } else {
                        cm.showContactNameInputDialog(this, lnAddress.getUsername(), lnAddress.toString(), Contact.ContactType.LNADDRESS, new ContactsManager.OnNameConfirmedListener() {
                            @Override
                            public void onNameAccepted() {
                                mAdapter.add(cm.getContactByContactData(lnAddress.toString()));
                                mEmptyListText.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancelled() {

                            }
                        });
                    }
                }
            }
        }

        if (requestCode == REQUEST_CODE_CONTACT_ACTION) {
            if (data != null) {
                LightningNodeUri nodeUri = (LightningNodeUri) data.getSerializableExtra(ScanContactActivity.EXTRA_NODE_URI);
                LNAddress lnAddress = (LNAddress) data.getSerializableExtra(ScanContactActivity.EXTRA_LN_ADDRESS);
                Intent intent = new Intent();
                intent.putExtra(ScanContactActivity.EXTRA_NODE_URI, nodeUri);
                intent.putExtra(ScanContactActivity.EXTRA_LN_ADDRESS, lnAddress);
                setResult(resultCode, intent);
                finish();
            }
        }
    }

    @Override
    public void onContactSelect(Contact contact, boolean clickOnAvatar) {

        switch (mMode) {
            case MODE_MANAGE:
                goToContactDetails(contact);
                break;
            case MODE_SEND:
                if (clickOnAvatar) {
                    goToContactDetails(contact);
                } else {
                    Intent intent = new Intent();
                    switch (contact.getContactType()) {
                        case NODEPUBKEY:
                            intent.putExtra(ScanContactActivity.EXTRA_NODE_URI, contact.getAsNodeUri());
                            break;
                        case LNADDRESS:
                            intent.putExtra(ScanContactActivity.EXTRA_LN_ADDRESS, contact.getLightningAddress());
                    }
                    setResult(ContactDetailsActivity.RESPONSE_CODE_SEND_MONEY, intent);
                    finish();
                }
                break;
            case MODE_OPEN_CHANNEL:
                if (clickOnAvatar) {
                    goToContactDetails(contact);
                } else {
                    break;
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.searchButton);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint(getResources().getString(R.string.search));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                final List<Contact> filteredContactList = filter(mContactItems, newText);
                mAdapter.replaceAll(filteredContactList);
                mRecyclerView.scrollToPosition(0);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void goToContactDetails(Contact contact) {
        Intent intent = new Intent(ManageContactsActivity.this, ContactDetailsActivity.class);
        intent.putExtra(EXTRA_CONTACT, contact);
        startActivityForResult(intent, REQUEST_CODE_CONTACT_ACTION);
    }

    private static List<Contact> filter(List<Contact> contacts, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        final List<Contact> filteredContactList = new ArrayList<>();
        for (Contact contact : contacts) {
            final String text = contact.getAlias().toLowerCase();
            if (text.contains(lowerCaseQuery)) {
                filteredContactList.add(contact);
            }
        }
        return filteredContactList;
    }

    private void setupSendMode() {
        mVEFab.setVisibility(View.GONE);
        mContactsHeaderLayout.setVisibility(View.VISIBLE);
        mManualInput.setVisibility(View.VISIBLE);
        mManualInput.setupView(mCompositeDisposable);

        if (!FeatureManager.isContactsEnabled()) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyListText.setVisibility(View.GONE);
            mContactsHeaderLayout.setVisibility(View.GONE);
        }

        mManualInput.setOnResultListener(new ManualSendInputView.OnResultListener() {
            @Override
            public void onValid(String data) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_GENERIC_SCAN_DATA, data);
                setResult(HomeActivity.RESULT_CODE_GENERIC_SCAN, intent);
                finish();
            }

            @Override
            public void onError(String error, int duration) {
                showError(error, duration);
            }
        });
    }
}
