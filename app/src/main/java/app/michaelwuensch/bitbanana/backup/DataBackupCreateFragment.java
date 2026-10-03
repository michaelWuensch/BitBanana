package app.michaelwuensch.bitbanana.backup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.CustomViewPager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;


public class DataBackupCreateFragment extends Fragment implements DataBackupCreatePagerAdapter.BackupAction {

    public static final String TAG = DataBackupCreateFragment.class.getSimpleName();

    private CustomViewPager mViewPager;
    private DataBackupCreatePagerAdapter mAdapter;
    private Handler mHandler;
    private String mTempPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_data_backup, container, false);
        mViewPager = view.findViewById(R.id.data_backup_viewpager);

        mAdapter = new DataBackupCreatePagerAdapter(getContext(), this);
        mViewPager.setForceNoSwipe(true);
        mViewPager.setAdapter(mAdapter);
        mHandler = new Handler();

        return view;
    }

    public void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getView().getRootView().getWindowToken(), 0);
    }

    @Override
    public void onCreateBackupPasswordEntered(String password) {
        mTempPassword = password;
        hideKeyboard();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        openSaveFileDialog("BitBananaBackup_" + timeStamp);
        mViewPager.setCurrentItem(2);
    }

    @Override
    public void onConfirmed() {
        mViewPager.setCurrentItem(1);
        showKeyboard();
    }

    @Override
    public void onFinish() {
        getActivity().finish();
    }

    ActivityResultLauncher<Intent> saveDialogResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getData() != null) {
                        try {
                            OutputStream outputStream = getActivity().getContentResolver().openOutputStream(data.getData());
                            startWritingBackupFile(outputStream);
                        } catch (IOException e) {
                            BBLog.printStackTrace(e);
                            mAdapter.setBackupCreationFinished(false);
                            BBLog.w(TAG, "Error writing backup file.");
                        }
                    } else {
                        mAdapter.setBackupCreationFinished(false);
                        BBLog.w(TAG, "The result data was empty. Unable to create backup.");
                        getActivity().finish();
                    }
                }
            });


    public void openSaveFileDialog(String title) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/bbb");
        intent.putExtra(Intent.EXTRA_TITLE, title + ".bbb");
        saveDialogResultLauncher.launch(intent);
    }

    public void startWritingBackupFile(OutputStream outputStream) {
        mHandler.postDelayed(() -> {
            hideKeyboard();
            String password = mTempPassword;
            mTempPassword = "";
            // The key derivation takes a while. Do it in the background to not block the UI.
            new Thread(() -> {
                boolean success = false;
                try {
                    byte[] backup = DataBackupUtil.createBackup(password, RefConstants.DATA_BACKUP_VERSION);
                    if (backup != null) {
                        outputStream.write(backup);
                        success = true;
                    }
                } catch (IOException e) {
                    BBLog.printStackTrace(e);
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        success = false;
                    }
                }
                boolean finalSuccess = success;
                mHandler.post(() -> {
                    if (finalSuccess)
                        BBLog.d(TAG, "Backup file creation was successful.");
                    else
                        BBLog.w(TAG, "Error writing backup file.");
                    if (isAdded())
                        mAdapter.setBackupCreationFinished(finalSuccess);
                });
            }).start();
        }, 500);

    }
}

