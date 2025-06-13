package app.michaelwuensch.bitbanana.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.listViews.bolt12offers.Bolt12OffersActivity;
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.models.LnAddress;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.wallet.QuickReceiveConfig;
import app.michaelwuensch.bitbanana.wallet.QuickReceiveConfig.QuickReceiveType;
import app.michaelwuensch.bitbanana.wallet.Wallet_Bolt12Offers;

public class QuickReceiveSetup extends BaseAppCompatActivity {

    private static final String LOG_TAG = QuickReceiveSetup.class.getSimpleName();

    private Spinner mSpType;
    private BBButton mBtnSave;
    private BBInputFieldView mEtLnAddress;
    private TextView mTvInfoLabel;
    private TextView mTvInfoText;
    private BBButton mBtnDocumentation;
    private View mBolt12OfferLayout;
    private View mSelectedBolt12OfferLayout;
    private BBButton mBtnSelectBolt12Offer;
    private BBButton mBtnRemoveBolt12Offer;
    private TextView mTvBolt12OfferLabel;
    private QuickReceiveType mQuickReceiveType;
    private BackendConfig mOriginalBackendConfig;
    private ActivityResultLauncher<Intent> mActivityResultLauncher;
    private Bolt12Offer mSelectedBolt12Offer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_quick_receive_setup);

        mSpType = findViewById(R.id.typeSpinner);
        mBtnSave = findViewById(R.id.saveButton);
        mEtLnAddress = findViewById(R.id.inputLnAddress);
        mTvInfoLabel = findViewById(R.id.infoLabel);
        mTvInfoText = findViewById(R.id.infoText);
        mBtnDocumentation = findViewById(R.id.documentationButton);
        mBolt12OfferLayout = findViewById(R.id.bolt12Layout);
        mSelectedBolt12OfferLayout = findViewById(R.id.selectedOfferLayout);
        mBtnSelectBolt12Offer = findViewById(R.id.selectBolt12Button);
        mBtnRemoveBolt12Offer = findViewById(R.id.removeSelectedOfferButton);
        mTvBolt12OfferLabel = findViewById(R.id.offerLabel);

        // Initialize the ActivityResultLauncher for Channel selection
        mActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        handleActivityResult(data);
                    }
                }
        );

        String selectLabelText = getString(R.string.select) + " ...";
        mBtnSelectBolt12Offer.setText(selectLabelText);

        mOriginalBackendConfig = BackendConfigsManager.getInstance().getCurrentBackendConfig();

        String[] items = new String[BackendManager.getCurrentBackend().getSupportedQuickReceiveTypes().size()];
        int i = 0;
        for (QuickReceiveType quickReceiveType : BackendManager.getCurrentBackend().getSupportedQuickReceiveTypes()) {
            items[i] = quickReceiveType.getDisplayName();
            i++;
        }

        mSpType.setAdapter(new ArrayAdapter<String>(this, R.layout.spinner_item, items));
        mSpType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mQuickReceiveType = BackendManager.getCurrentBackend().getSupportedQuickReceiveTypes().get(position);
                switch (mQuickReceiveType) {
                    case OFF:
                        mEtLnAddress.setVisibility(View.GONE);
                        mTvInfoText.setText(R.string.quick_receive_info_off);
                        mBtnDocumentation.setVisibility(View.GONE);
                        mBolt12OfferLayout.setVisibility(View.GONE);
                        break;
                    case LN_ADDRESS:
                        mEtLnAddress.setVisibility(View.VISIBLE);
                        String lnAddressInfoText = getString(R.string.refer_to_docs_ln_address);
                        mTvInfoText.setText(lnAddressInfoText);
                        mBtnDocumentation.setVisibility(View.VISIBLE);
                        setDocumentationButtonLink(RefConstants.URL_DOCS_LIGHTNING_ADDRESS);
                        mBolt12OfferLayout.setVisibility(View.GONE);
                        break;
                    case BOLT12:
                        mEtLnAddress.setVisibility(View.GONE);
                        mTvInfoText.setText(R.string.quick_receive_info_bolt12);
                        mBtnDocumentation.setVisibility(View.GONE);
                        mBolt12OfferLayout.setVisibility(View.VISIBLE);
                        break;
                    case ON_CHAIN_ADDRESS:
                        mEtLnAddress.setVisibility(View.GONE);
                        String addressInfoText = "";
                        if (BackendManager.getCurrentBackend().supportsBolt11Receive())
                            addressInfoText = getString(R.string.quick_receive_info_on_chain_address) + " " + getString(R.string.quick_receive_info_on_chain_address_part2);
                        else
                            addressInfoText = getString(R.string.quick_receive_info_on_chain_address);
                        mTvInfoText.setText(addressInfoText);
                        mBtnDocumentation.setVisibility(View.GONE);
                        mBolt12OfferLayout.setVisibility(View.GONE);
                        break;
                    case ON_CHAIN_AND_LN_ADDRESS:
                        mEtLnAddress.setVisibility(View.VISIBLE);
                        String infoText = getString(R.string.quick_receive_info_on_chain_and_ln_address) + " " + getString(R.string.refer_to_docs_ln_address);
                        mTvInfoText.setText(infoText);
                        mBtnDocumentation.setVisibility(View.VISIBLE);
                        setDocumentationButtonLink(RefConstants.URL_DOCS_LIGHTNING_ADDRESS);
                        mBolt12OfferLayout.setVisibility(View.GONE);
                        break;
                    case ON_CHAIN_AND_BOLT12:
                        mEtLnAddress.setVisibility(View.GONE);
                        mTvInfoText.setText(R.string.quick_receive_info_on_chain_and_bolt12);
                        mBtnDocumentation.setVisibility(View.GONE);
                        mBolt12OfferLayout.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Fill in the currently saved values
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            mSpType.setSelection(getTypeIndex(mOriginalBackendConfig.getQuickReceiveConfig().getQuickReceiveType()));
            mEtLnAddress.setValue(mOriginalBackendConfig.getQuickReceiveConfig().getLnAddress());
            if (mOriginalBackendConfig.getQuickReceiveConfig().getBolt12ID() != null) {
                Bolt12Offer bolt12Offer = Wallet_Bolt12Offers.getInstance().getBolt12OfferById(mOriginalBackendConfig.getQuickReceiveConfig().getBolt12ID());
                if (bolt12Offer != null)
                    setBolt12Offer(bolt12Offer);
            }
        }

        mBtnSelectBolt12Offer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(QuickReceiveSetup.this, Bolt12OffersActivity.class);
                intent.putExtra(Bolt12OffersActivity.EXTRA_BOLT12_OFFERS_ACTIVITY_MODE, Bolt12OffersActivity.MODE_SELECT);
                mActivityResultLauncher.launch(intent);
            }
        });

        mBtnRemoveBolt12Offer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedBolt12Offer = null;
                mBtnSelectBolt12Offer.setVisibility(View.VISIBLE);
                mSelectedBolt12OfferLayout.setVisibility(View.GONE);
            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });
    }

    private void handleActivityResult(Intent data) {
        setBolt12Offer((Bolt12Offer) data.getSerializableExtra(Bolt12OffersActivity.EXTRA_SELECTED_OFFER));
    }

    private void setBolt12Offer(Bolt12Offer bolt12Offer) {
        mSelectedBolt12Offer = bolt12Offer;
        mBtnSelectBolt12Offer.setVisibility(View.GONE);
        mSelectedBolt12OfferLayout.setVisibility(View.VISIBLE);
        String offerLabel;
        if (mSelectedBolt12Offer.getLabel() != null && !mSelectedBolt12Offer.getLabel().isEmpty())
            offerLabel = mSelectedBolt12Offer.getLabel();
        else {
            if (mSelectedBolt12Offer.getDecodedBolt12().getDescription() != null && !mSelectedBolt12Offer.getDecodedBolt12().getDescription().isEmpty())
                offerLabel = mSelectedBolt12Offer.getDecodedBolt12().getDescription();
            else
                offerLabel = "ID: " + mSelectedBolt12Offer.getOfferId();
        }
        mTvBolt12OfferLabel.setText(offerLabel);
    }

    private void setDocumentationButtonLink(String link) {
        mBtnDocumentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            }
        });
    }


    private void save() {
        // check if Demo mode
        if (!BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            showError(getString(R.string.demo_setupNodeFirst), 5000);
            return;
        }

        // validate input
        switch (mQuickReceiveType) {
            case LN_ADDRESS:
            case ON_CHAIN_AND_LN_ADDRESS:
                if (mEtLnAddress.getData() == null) {
                    showError(getString(R.string.error_invalid_ln_address_format), 5000);
                    return;
                }
                LnAddress lnAddress = new LnAddress(mEtLnAddress.getData());
                if (!(lnAddress.isValidBip353DnsRecordAddress() || lnAddress.isValidLnurlAddress())) {
                    showError(getString(R.string.error_invalid_ln_address_format), 5000);
                    return;
                }
                break;
            case BOLT12:
            case ON_CHAIN_AND_BOLT12:
                if (mSelectedBolt12Offer == null) {
                    showError(getString(R.string.error_no_bolt12_offer_selected), 3000);
                    return;
                }
                break;
        }

        // everything is ok, save the changes
        mOriginalBackendConfig.setQuickReceiveConfig(getQuickReceiveConfig());
        BackendConfigsManager.getInstance().updateBackendConfig(mOriginalBackendConfig);
        try {
            BackendConfigsManager.getInstance().apply();
            BBLog.d(LOG_TAG, "QuickReceive method saved.");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private QuickReceiveConfig getQuickReceiveConfig() {
        QuickReceiveConfig quickReceiveConfig = new QuickReceiveConfig();
        quickReceiveConfig.setQuickReceiveType(mQuickReceiveType);
        if (mEtLnAddress.getData() != null)
            quickReceiveConfig.setLnAddress(mEtLnAddress.getData());
        if (mSelectedBolt12Offer != null)
            quickReceiveConfig.setBolt12ID(mSelectedBolt12Offer.getOfferId());
        return quickReceiveConfig;
    }

    private int getTypeIndex(QuickReceiveType type) {
        int i = 0;
        for (QuickReceiveType quickReceiveType : BackendManager.getCurrentBackend().getSupportedQuickReceiveTypes()) {
            if (quickReceiveType.equals(type))
                return i;
            i++;
        }
        return -1;
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
            HelpDialogUtil.showDialogWithLink(QuickReceiveSetup.this, R.string.help_dialog_quick_receive_setup, getString(R.string.documentation), RefConstants.URL_DOCS_QUICK_RECEIVE_SETUP);
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mOriginalBackendConfig == null) {
            QuickReceiveSetup.super.onBackPressed();
            return;
        }

        String original = new Gson().toJson(mOriginalBackendConfig.getQuickReceiveConfig());
        String actual = new Gson().toJson(getQuickReceiveConfig());
        if (!original.equals(actual)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.unsaved_changes)
                    .setCancelable(true)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            save();
                        }
                    })
                    .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            QuickReceiveSetup.super.onBackPressed();
                        }
                    })
                    .show();
        } else {
            QuickReceiveSetup.super.onBackPressed();
        }
    }
}