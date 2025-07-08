package app.michaelwuensch.bitbanana.labels;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBTextInputBox;
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
        Label label = new Label();
        label.setId(mLabelID);
        label.setLabel(mLabel.getData());
        LabelsManager.getInstance().saveLabel(label, mLabelType);
        LabelsManager.getInstance().broadcastLabelChanged();
        finish();
    }

    private void delete() {
        Label label = new Label();
        label.setId(mLabelID);
        label.setLabel(mOriginalLabel);
        LabelsManager.getInstance().deleteLabel(label, mLabelType);
        LabelsManager.getInstance().broadcastLabelChanged();
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
            if (getLabel() != null && !getLabel().isEmpty()) {
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
}