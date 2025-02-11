package app.michaelwuensch.bitbanana.backup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.UtilFunctions;


public class DataBackupIntroFragment extends Fragment {

    public static final String TAG = DataBackupIntroFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_data_backup_intro, container, false);

        BBButton buttonStartBackup = view.findViewById(R.id.data_backup_intro_create_button);
        buttonStartBackup.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (DataBackupUtil.isThereAnythingToBackup()) {
                    ((BackupActivity) getActivity()).changeFragment(new DataBackupCreateFragment());
                } else {
                    Toast.makeText(getActivity(), R.string.backup_data_no_data, Toast.LENGTH_LONG).show();
                }
            }
        });
        BBButton buttonRestoreBackup = view.findViewById(R.id.data_backup_intro_restore_button);
        buttonRestoreBackup.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (BackendConfigsManager.getInstance().hasAnyBackendConfigs() || ContactsManager.getInstance().hasAnyContacts()) {
                    new UserGuardian(getActivity(), new UserGuardian.OnGuardianConfirmedListener() {
                        @Override
                        public void onConfirmed() {
                            openOpenFileDialog();
                        }

                        @Override
                        public void onCancelled() {

                        }
                    }).securityBackupOverridesExistingData();
                } else {
                    openOpenFileDialog();
                }
            }
        });

        return view;
    }

    ActivityResultLauncher<Intent> openDialogResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Uri uri = data.getData();

                    try {
                        InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                        byte[] fileBytes = ByteStreams.toByteArray(inputStream);
                        byte[] fileIdentifierBytes = Arrays.copyOfRange(fileBytes, 0, 10);
                        boolean validFile = new String(fileIdentifierBytes, StandardCharsets.UTF_8).equals(DataBackupUtil.BACKUP_FILE_IDENTIFIER) || new String(fileIdentifierBytes, StandardCharsets.UTF_8).equals(DataBackupUtil.ZAP_BACKUP_FILE_IDENTIFIER);
                        byte[] backupVersionBytes = Arrays.copyOfRange(fileBytes, 10, 14);
                        int backupVersion = UtilFunctions.intFromByteArray(backupVersionBytes);
                        byte[] encryptedBackupBytes = Arrays.copyOfRange(fileBytes, 14, fileBytes.length);
                        ((BackupActivity) getActivity()).changeFragment(DataBackupRestoreFragment.newInstance(encryptedBackupBytes, validFile, backupVersion));
                    } catch (IOException e) {
                        e.printStackTrace();
                        ((BaseAppCompatActivity) getActivity()).showError(getResources().getString(R.string.backup_data_open_backupfile_error), RefConstants.ERROR_DURATION_MEDIUM);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        ((BaseAppCompatActivity) getActivity()).showError(getResources().getString(R.string.error_no_permission_to_read_file), RefConstants.ERROR_DURATION_LONG);
                    }
                }
            });

    public void openOpenFileDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        openDialogResultLauncher.launch(intent);
    }
}

