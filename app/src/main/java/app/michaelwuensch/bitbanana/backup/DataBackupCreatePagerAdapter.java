package app.michaelwuensch.bitbanana.backup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBPasswordInputFieldView;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class DataBackupCreatePagerAdapter extends PagerAdapter {

    private Context mContext;
    private BackupAction mBackupAction;

    private View mBackupCreationProcess;
    private View mBackupCreationFinished;
    private View mBackupCreationSuccess;
    private View mBackupCreationFailed;

    public DataBackupCreatePagerAdapter(Context context, BackupAction backupAction) {
        mContext = context;
        mBackupAction = backupAction;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View backupView;
        if (position == 0) {
            // backup create confirm
            backupView = inflater.inflate(R.layout.view_data_backup_confirm, container, false);

            CheckBox checkBoxConfirm = backupView.findViewById(R.id.data_backup_confirm_checkbox_confirm);
            BBButton buttonContinue = backupView.findViewById(R.id.data_backup_confirm_continue_button);
            buttonContinue.setButtonEnabled(false);
            BBButton buttonCancel = backupView.findViewById(R.id.data_backup_confirm_cancel_button);

            checkBoxConfirm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                buttonContinue.setButtonEnabled(isChecked);
            });

            buttonCancel.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    mBackupAction.onFinish();
                }
            });

            buttonContinue.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    // Before we finish we remove the seed from the preferences
                    // this will also trigger a notification update
                    mBackupAction.onConfirmed();
                }
            });
        } else if (position == 1) {
            // password
            backupView = inflater.inflate(R.layout.view_data_backup_password, container, false);

            BBButton buttonCreate = backupView.findViewById(R.id.data_backup_continue_button);
            BBPasswordInputFieldView pw1 = backupView.findViewById(R.id.pw1_input);
            BBPasswordInputFieldView pw2 = backupView.findViewById(R.id.pw2_input);

            pw1.getEditText().requestFocus();

            buttonCreate.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    if (pw1.getData() != null && pw1.getData().length() > 7) {
                        if (pw1.getData().equals(pw2.getData())) {
                            String password = pw1.getData();
                            pw1.setValue("");
                            pw2.setValue("");
                            mBackupAction.onCreateBackupPasswordEntered(password);
                        } else {
                            Toast.makeText(mContext, mContext.getString(R.string.backup_data_password_mismatch), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(mContext, mContext.getString(R.string.backup_data_password_empty), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // finished
            backupView = inflater.inflate(R.layout.view_data_backup_create_finish, container, false);

            mBackupCreationProcess = backupView.findViewById(R.id.creatingBackupProgress);
            mBackupCreationFinished = backupView.findViewById(R.id.creatingBackupFinished);
            mBackupCreationSuccess = backupView.findViewById(R.id.creatingBackupSuccess);
            mBackupCreationFailed = backupView.findViewById(R.id.creatingBackupFailed);

            BBButton finishButton = backupView.findViewById(R.id.data_backup_create_finish_button);
            finishButton.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    mBackupAction.onFinish();
                }
            });
        }

        container.addView(backupView);
        return backupView;
    }

    public void setBackupCreationFinished(boolean success) {
        mBackupCreationProcess.setVisibility(View.GONE);
        mBackupCreationFinished.setVisibility(View.VISIBLE);
        if (success) {
            mBackupCreationSuccess.setVisibility(View.VISIBLE);
        } else {
            mBackupCreationFailed.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    interface BackupAction {

        void onCreateBackupPasswordEntered(String password);

        void onConfirmed();

        void onFinish();
    }
}

