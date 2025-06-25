package app.michaelwuensch.bitbanana.appLock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBPasswordInputFieldView;
import app.michaelwuensch.bitbanana.util.BiometricUtil;
import app.michaelwuensch.bitbanana.util.KeystoreUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UtilFunctions;


public class PasswordSetupFragment extends Fragment {

    public static final int CREATE_MODE = 0;
    public static final int ENTER_MODE = 1;
    private static final String LOG_TAG = PasswordSetupFragment.class.getSimpleName();
    private static final String ARG_MODE = "passwordMode";
    private static final String ARG_PROMPT = "promptString";

    private BBButton mBtnContinue;
    private Button mBtnPasswordRemove;
    private BBButton mBtnBiometrics;
    private BBPasswordInputFieldView mPasswordInput;
    private BBPasswordInputFieldView mConfirmPasswordInput;

    private BiometricPrompt mBiometricPrompt;
    private BiometricPrompt.PromptInfo mPromptInfo;

    private TextView mTvPrompt;
    private String mPromptString;
    private Vibrator mVibrator;
    private int mNumFails;
    private int mMode;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param mode   set the mode to either create, confirm or enter pin.
     * @param prompt Short text to describe what is happening.
     * @return A new instance of fragment PinFragment.
     */
    public static PasswordSetupFragment newInstance(int mode, String prompt) {
        PasswordSetupFragment fragment = new PasswordSetupFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        args.putString(ARG_PROMPT, prompt);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMode = getArguments().getInt(ARG_MODE);
            mPromptString = getArguments().getString(ARG_PROMPT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_app_lock_password, container, false);

        mNumFails = PrefsUtil.getPrefs().getInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0);

        mBtnContinue = view.findViewById(R.id.continueButton);
        mBtnPasswordRemove = view.findViewById(R.id.passwordRemoveButton);
        mBtnBiometrics = view.findViewById(R.id.biometricsButton);
        mPasswordInput = view.findViewById(R.id.passwordInput);
        mConfirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);

        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        // Set all layout element states to the current user input (empty right now)
        updateVisibilities();

        if (mMode == ENTER_MODE) {
            mPasswordInput.setDescription(R.string.password_enter_old);
        }

        if (mMode == CREATE_MODE)
            mBtnContinue.setText(getString(R.string.save));

        if (mMode == CREATE_MODE && PrefsUtil.isPasswordEnabled()) {
            mPasswordInput.setDescription(R.string.password_enter_new);
            mConfirmPasswordInput.setDescription(R.string.password_confirm_new);
        }

        mPasswordInput.requestFocus();
        showKeyboard();


        // Make biometrics Button visible if supported.
        if (mMode == ENTER_MODE && PrefsUtil.isBiometricEnabled() && BiometricUtil.hardwareAvailable()) {
            mBtnBiometrics.setVisibility(View.VISIBLE);
        } else {
            mBtnBiometrics.setVisibility(View.GONE);
        }

        Executor executor = Executors.newSingleThreadExecutor();

        mPromptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getResources().getString(R.string.biometricPrompt_title))
                .setNegativeButtonText(getResources().getString(R.string.cancel))
                .build();


        mBiometricPrompt = new BiometricPrompt(requireActivity(), executor, new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                // Go to next step
                if (mMode == ENTER_MODE) {

                    PrefsUtil.editPrefs().putInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0)
                            .putBoolean(PrefsUtil.BIOMETRICS_PREFERRED, true).apply();

                    ((AppLockInterface) getActivity()).correctAccessDataEntered();
                }

            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED) {
                    exitBiometricsPrompt();
                } else {
                    // This has to happen on the UI thread. Only this thread can change the recycler view.
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), errString, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });


        // Show biometric prompt if preferred
        if (mMode == ENTER_MODE && PrefsUtil.isBiometricEnabled() && BiometricUtil.hardwareAvailable()) {
            if (PrefsUtil.isBiometricPreferred()) {
                // This is disabled for now, as it crashes the app when called to early
                //showBiometricPrompt();
            }
        }

        // Call BiometricsPrompt on click on fingerprint symbol
        mBtnBiometrics.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BiometricUtil.notSetup()) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.biometricPrompt_title)
                            .setMessage(R.string.biometricNotSetup)
                            .setCancelable(true)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            });
                    Dialog dlg = adb.create();
                    // Apply FLAG_SECURE to dialog to prevent screen recording
                    if (PrefsUtil.isScreenRecordingPrevented()) {
                        dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    }
                    dlg.show();
                } else {
                    showBiometricPrompt();
                }
            }
        });

        // Set action on continue button
        mBtnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                continuePressed();
            }
        });

        mBtnPasswordRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PrefsUtil.editEncryptedPrefs().remove(PrefsUtil.PASSWORD_HASH).commit();
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
                try {
                    new KeystoreUtil().removeAppLockActiveKey();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                getActivity().finish();
            }
        });

        // If the user closed and restarted the activity he still has to wait until the password input delay is over.
        if (mMode == ENTER_MODE && mNumFails >= RefConstants.APP_LOCK_MAX_FAILS) {

            long timeDiff = System.currentTimeMillis() - PrefsUtil.getPrefs().getLong("failedLoginTimestamp", 0L);

            if (timeDiff < RefConstants.APP_LOCK_DELAY_TIME * 1000) {

                mBtnContinue.setButtonEnabled(false);

                String message = getResources().getString(R.string.pin_entered_wrong_wait, String.valueOf((int) ((RefConstants.APP_LOCK_DELAY_TIME * 1000 - timeDiff) / 1000)));
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBtnContinue.setButtonEnabled(true);
                    }
                }, RefConstants.APP_LOCK_DELAY_TIME * 1000 - timeDiff);
            }
        }

        mPasswordInput.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateVisibilities();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return view;
    }

    private void updateVisibilities() {
        switch (mMode) {
            case CREATE_MODE:
                mConfirmPasswordInput.setVisibility(View.VISIBLE);
                mBtnPasswordRemove.setVisibility(PrefsUtil.isPasswordEnabled() && (mPasswordInput.getData() == null || mPasswordInput.getData().isEmpty()) ? View.VISIBLE : View.GONE);
                mBtnContinue.setVisibility((!PrefsUtil.isPasswordEnabled() || (mPasswordInput.getData() != null && !mPasswordInput.getData().isEmpty())) ? View.VISIBLE : View.GONE);
                break;
            case ENTER_MODE:
                mConfirmPasswordInput.setVisibility(View.GONE);
                mBtnPasswordRemove.setVisibility(View.GONE);
                mBtnContinue.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void continuePressed() {
        if (mPasswordInput.getData() == null || mPasswordInput.getData().isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.backup_data_password_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if (mMode == ENTER_MODE) {
            // Check if password was correct
            boolean correct = false;
            String userEnteredPin = mPasswordInput.getData();
            String hashedInput = UtilFunctions.appLockDataHash(userEnteredPin);
            try {
                correct = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.PASSWORD_HASH, "").equals(hashedInput);
                if (correct) {
                    PrefsUtil.editPrefs().putInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0)
                            .putBoolean(PrefsUtil.BIOMETRICS_PREFERRED, false).apply();

                    ((AppLockInterface) getActivity()).correctAccessDataEntered();
                } else {
                    mNumFails++;
                    PrefsUtil.editPrefs().putInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, mNumFails)
                            .putBoolean(PrefsUtil.BIOMETRICS_PREFERRED, false).apply();

                    final Animation animShake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
                    View view = getActivity().findViewById(R.id.rootPasswordInputLayout);
                    view.startAnimation(animShake);
                    mVibrator.vibrate(200);

                    if (mNumFails >= RefConstants.APP_LOCK_MAX_FAILS) {
                        mBtnContinue.setButtonEnabled(false);
                        String message = getResources().getString(R.string.pin_entered_wrong_wait, String.valueOf(RefConstants.APP_LOCK_DELAY_TIME));
                        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

                        // Save timestamp. This way the delay can also be forced upon restart of the activity.
                        PrefsUtil.editPrefs().putLong("failedLoginTimestamp", System.currentTimeMillis()).apply();

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mBtnContinue.setButtonEnabled(true);
                            }
                        }, RefConstants.APP_LOCK_DELAY_TIME * 1000);
                    } else {
                        // Show error
                        Toast.makeText(getActivity(), R.string.error_wrong_password, Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        }

        if (mMode == CREATE_MODE) {
            if (mPasswordInput.getData().equals(mConfirmPasswordInput.getData())) {
                if (mPasswordInput.getData().length() > 7) {
                    ((PasswordSetupActivity) getActivity()).passwordCreated(mPasswordInput.getData());
                } else {
                    Toast.makeText(getContext(), getString(R.string.backup_data_password_empty), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), getString(R.string.backup_data_password_mismatch), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBiometricPrompt() {
        mPasswordInput.setVisibility(View.GONE);
        mBtnContinue.setVisibility(View.GONE);
        mBtnBiometrics.setVisibility(View.GONE);
        mBiometricPrompt.authenticate(mPromptInfo);
    }

    private void exitBiometricsPrompt() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnBiometrics.setVisibility(View.VISIBLE);
                mBtnContinue.setVisibility(View.VISIBLE);
                mPasswordInput.setVisibility(View.VISIBLE);
                mPasswordInput.getEditText().requestFocus();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showKeyboard();
                    }
                }, 250);
            }
        });
    }

    public void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(mPasswordInput.getEditText(), InputMethodManager.SHOW_IMPLICIT);
    }
}
