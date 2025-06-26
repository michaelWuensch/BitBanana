package app.michaelwuensch.bitbanana.backup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBPasswordInputFieldView;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class DataBackupRestorePagerAdapter extends PagerAdapter {

    private Context mContext;
    private BackupAction mBackupAction;

    private View mBackupRestoringProcess;
    private View mBackupRestoringFinished;
    private View mBackupRestoringSuccess;
    private View mBackupRestoringFailed;
    private TextView mBackupRestoringFailedDescription;

    public DataBackupRestorePagerAdapter(Context context, BackupAction backupAction) {
        mContext = context;
        mBackupAction = backupAction;
    }

    @Override
    public int getCount() {
        return 2;
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
            // password
            backupView = inflater.inflate(R.layout.view_data_backup_password, container, false);

            BBButton buttonCreate = backupView.findViewById(R.id.data_backup_continue_button);
            BBPasswordInputFieldView pw1 = backupView.findViewById(R.id.pw1_input);
            BBPasswordInputFieldView pw2 = backupView.findViewById(R.id.pw2_input);
            pw2.setVisibility(View.GONE);

            pw1.requestFocus();

            buttonCreate.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    if (pw1.getData() != null && pw1.getData().length() > 7) {
                        String password = pw1.getData();
                        pw1.setValue("");
                        pw2.setValue("");
                        mBackupAction.onRestoreBackupPasswordEntered(password);
                    } else {
                        Toast.makeText(mContext, mContext.getString(R.string.backup_data_password_empty), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // finished
            backupView = inflater.inflate(R.layout.view_data_backup_restore_finish, container, false);

            mBackupRestoringProcess = backupView.findViewById(R.id.restoringBackupProgress);
            mBackupRestoringFinished = backupView.findViewById(R.id.restoringBackupFinished);
            mBackupRestoringSuccess = backupView.findViewById(R.id.restoringBackupSuccess);
            mBackupRestoringFailed = backupView.findViewById(R.id.restoringBackupFailed);
            mBackupRestoringFailedDescription = backupView.findViewById(R.id.data_backup_finish_description_failed);

            BBButton finishButton = backupView.findViewById(R.id.data_backup_restore_finish_button);
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

    public void setBackupRestoreFinished(boolean success, int message) {
        mBackupRestoringProcess.setVisibility(View.GONE);
        mBackupRestoringFinished.setVisibility(View.VISIBLE);
        if (success) {
            mBackupRestoringSuccess.setVisibility(View.VISIBLE);
        } else {
            mBackupRestoringFailed.setVisibility(View.VISIBLE);
            mBackupRestoringFailedDescription.setText(mContext.getString(message));
        }
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    interface BackupAction {

        void onRestoreBackupPasswordEntered(String password);

        void onFinish();
    }
}

