package app.michaelwuensch.bitbanana.labels;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBTextInputBox;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;

public class LabelActivity extends BaseAppCompatActivity {

    public static final String EXTRA_LABEL_ID = "label_id";
    public static final String EXTRA_LABEL = "extra_label";
    public static final String EXTRA_LABEL_TYPE = "label_type";

    private static final String LOG_TAG = LabelActivity.class.getSimpleName();

    private BBTextInputBox mLabel;
    private BBButton mBtnSave;
    private String mLabelID;
    private String mOriginalLabel;
    private Labels.LabelType mLabelType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mLabelID = extras.getString(EXTRA_LABEL_ID);
            mLabelType = (Labels.LabelType) extras.getSerializable(EXTRA_LABEL_TYPE);
            if (extras.containsKey(EXTRA_LABEL)) {
                mOriginalLabel = extras.getString(EXTRA_LABEL);
            }
        }

        if (mOriginalLabel != null) {
            setTitle(R.string.label_edit);
        } else {
            setTitle(R.string.label_add);
        }

        setContentView(R.layout.activity_label);

        mLabel = findViewById(R.id.labelInput);
        mBtnSave = findViewById(R.id.saveButton);

        mLabel.setupCharLimit(150);
        mLabel.getEditText().requestFocus();

        // Fill in existing label if provided
        if (mOriginalLabel != null) {
            mLabel.setValue(mOriginalLabel);
        }

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });
    }

    private String getLabel() {
        return mLabel.getData();
    }

    private void save() {
        if (mLabel.getData() == null || mLabel.getData().isEmpty()) {
            showError("Label cannot be empty", 3000);
            return;
        }
        Labels currentBackendLabels = BackendManager.getCurrentBackendConfig().getLabels();
        Label label = new Label();
        label.setId(mLabelID);
        label.setLabel(mLabel.getData());
        switch (mLabelType) {
            case UTXO:
                currentBackendLabels.setUtxoLabels(updateLabel(BackendManager.getCurrentBackendConfig().getLabels().getUtxoLabels(), label));
                break;
            case ON_CHAIN_TRANSACTION:
                currentBackendLabels.setTransactionLabels(updateLabel(BackendManager.getCurrentBackendConfig().getLabels().getTransactionLabels(), label));
                break;
            case LN_PAYMENT:
                currentBackendLabels.setPaymentLabels(updateLabel(BackendManager.getCurrentBackendConfig().getLabels().getPaymentLabels(), label));
                break;
            case LN_INVOICE:
                currentBackendLabels.setInvoiceLabels(updateLabel(BackendManager.getCurrentBackendConfig().getLabels().getInvoiceLabels(), label));
                break;
        }
        BackendManager.getCurrentBackendConfig().setLabels(currentBackendLabels);
        BackendConfigsManager backendConfigsManager = BackendConfigsManager.getInstance();
        backendConfigsManager.updateBackendConfig(BackendManager.getCurrentBackendConfig());
        try {
            backendConfigsManager.apply();
        } catch (GeneralSecurityException | IOException e) {
            BBLog.e(LOG_TAG, "Error saving label.");
            throw new RuntimeException(e);
        }
        LabelsUtil.getInstance().broadcastLabelChanged();
        finish();
    }

    private void delete() {
        Labels currentBackendLabels = BackendManager.getCurrentBackendConfig().getLabels();
        Label label = new Label();
        label.setId(mLabelID);
        label.setLabel(mOriginalLabel);
        switch (mLabelType) {
            case UTXO:
                currentBackendLabels.setUtxoLabels(deleteLabel(BackendManager.getCurrentBackendConfig().getLabels().getUtxoLabels(), label));
                break;
            case ON_CHAIN_TRANSACTION:
                currentBackendLabels.setTransactionLabels(deleteLabel(BackendManager.getCurrentBackendConfig().getLabels().getTransactionLabels(), label));
                break;
            case LN_PAYMENT:
                currentBackendLabels.setPaymentLabels(deleteLabel(BackendManager.getCurrentBackendConfig().getLabels().getPaymentLabels(), label));
                break;
            case LN_INVOICE:
                currentBackendLabels.setInvoiceLabels(deleteLabel(BackendManager.getCurrentBackendConfig().getLabels().getInvoiceLabels(), label));
                break;
        }
        BackendManager.getCurrentBackendConfig().setLabels(currentBackendLabels);
        BackendConfigsManager backendConfigsManager = BackendConfigsManager.getInstance();
        backendConfigsManager.updateBackendConfig(BackendManager.getCurrentBackendConfig());
        try {
            backendConfigsManager.apply();
        } catch (GeneralSecurityException | IOException e) {
            BBLog.e(LOG_TAG, "Error deleting label.");
            throw new RuntimeException(e);
        }
        LabelsUtil.getInstance().broadcastLabelChanged();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.help_menu, menu);
        if (mOriginalLabel != null)
            getMenuInflater().inflate(R.menu.delete_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(LabelActivity.this, R.string.help_dialog_label);
            return true;
        } else if (id == R.id.action_delete) {
            delete();
        } else if (id == android.R.id.home) {
            onBackPressed();
            return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mOriginalLabel != null) {
            // we are in edit mode
            String original = mOriginalLabel;
            String actual = getLabel();

            if (!original.equals(actual))
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
                                LabelActivity.super.onBackPressed();
                            }
                        })
                        .show();
            else
                LabelActivity.super.onBackPressed();
        } else {
            // we are in add label mode
            if (getLabel() != null) {
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
                                LabelActivity.super.onBackPressed();
                            }
                        })
                        .show();
            } else {
                LabelActivity.super.onBackPressed();
            }
        }
    }

    // This adds a label to the set if it is not present and updates it if it was present.
    private ArrayList<Label> updateLabel(ArrayList<Label> labels, Label label) {
        // remove old label if one was available
        for (Label l : labels) {
            if (l.getId().equals(label.getId())) {
                labels.remove(l);
                break;
            }
        }
        labels.add(label);
        return labels;
    }

    private ArrayList<Label> deleteLabel(ArrayList<Label> labels, Label label) {
        for (Label l : labels) {
            if (l.getId().equals(label.getId())) {
                labels.remove(l);
                break;
            }
        }
        return labels;
    }
}