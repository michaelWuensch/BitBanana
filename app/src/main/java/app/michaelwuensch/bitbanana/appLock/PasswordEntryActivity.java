package app.michaelwuensch.bitbanana.appLock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBPasswordInputFieldView;
import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.util.AppLockUtil;
import app.michaelwuensch.bitbanana.util.BiometricUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.util.UtilFunctions;


public class PasswordEntryActivity extends BaseAppCompatActivity {

    public static final String EXTRA_CLEAR_HISTORY = "ClearHistory";

    private BBButton mBtnContinue;
    private BBButton mBtnBiometrics;
    private BBPasswordInputFieldView mPasswordInput;
    private TextView mInputPasswordTitle;

    private BiometricPrompt mBiometricPrompt;
    private BiometricPrompt.PromptInfo mPromptInfo;

    private Vibrator mVibrator;
    private int mNumFails;
    private boolean mClearHistory;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_lock_password_input);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mClearHistory = extras.getBoolean(EXTRA_CLEAR_HISTORY);
        }

        // Disable back button by adding a callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing, effectively disabling the back button
            }
        });

        mInputPasswordTitle = findViewById(R.id.inputPasswordTitle);
        mBtnContinue = findViewById(R.id.continueButton);
        mBtnBiometrics = findViewById(R.id.biometricsButton);
        mPasswordInput = findViewById(R.id.passwordInput);
        mVibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        mNumFails = PrefsUtil.getPrefs().getInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0);

        mPasswordInput.getEditText().requestFocus();
        showKeyboard();

        // Make biometrics Button visible if enabled.
        if (BiometricUtil.isBiometricUnlockOffered()) {
            mBtnBiometrics.setVisibility(View.VISIBLE);
        } else {
            mBtnBiometrics.setVisibility(View.GONE);
        }

        Executor executor = Executors.newSingleThreadExecutor();

        mPromptInfo = BiometricUtil.createPromptInfo(getResources().getString(R.string.biometricPrompt_title), getResources().getString(R.string.cancel));


        mBiometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                if (!BiometricUtil.isAuthenticationValid(result)) {
                    exitBiometricsPrompt();
                    return;
                }

                PrefsUtil.editPrefs().putBoolean(PrefsUtil.BIOMETRICS_PREFERRED, true).apply();

                TimeOutUtil.getInstance().restartTimer();

                PrefsUtil.editPrefs().putInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0).apply();

                if (mClearHistory) {
                    Intent intent = new Intent(PasswordEntryActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    AppLockUtil.isLockScreenShown = false;
                } else {
                    AppLockUtil.isLockScreenShown = false;
                    finish();
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
                    runOnUiThread(new Runnable() {
                        public void run() {
                            exitBiometricsPrompt();
                            Toast.makeText(PasswordEntryActivity.this, errString, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

        });


        // Call BiometricsPrompt on click on fingerprint symbol
        mBtnBiometrics.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BiometricUtil.noBiometricsEnrolledOnDevice()) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(PasswordEntryActivity.this)
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
                    showBiometricsPrompt();
                }
            }
        });

        mBtnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onContinueClicked();
            }
        });

        // If the user closed and restarted the app he still has to wait until the password input delay is over.
        long remainingDelay = AppLockUtil.getRemainingUnlockDelayMillis(mNumFails);
        if (remainingDelay > 0) {
            Toast.makeText(this, AppLockUtil.getUnlockDelayMessage(this, remainingDelay), Toast.LENGTH_LONG).show();
            disableContinueButtonFor(remainingDelay);
        }

        // Make sure the "Ok" button from the software keyboard works as well
        mPasswordInput.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        actionId == EditorInfo.IME_ACTION_SEND ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                    onContinueClicked();

                    return true; // consume the action
                }
                return false;
            }
        });
    }

    public void onContinueClicked() {
        // Ensure app lock delay is enforced even if Android Software Keyboard is used to get here...
        long remainingDelay = AppLockUtil.getRemainingUnlockDelayMillis(mNumFails);
        if (remainingDelay > 0) {
            Toast.makeText(this, AppLockUtil.getUnlockDelayMessage(this, remainingDelay), Toast.LENGTH_LONG).show();
            return;
        }

        if (mPasswordInput.getData() == null || mPasswordInput.getData().isEmpty()) {
            Toast.makeText(this, getString(R.string.backup_data_password_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if password was correct
        String hashedInput = UtilFunctions.appLockDataHash(mPasswordInput.getData());
        boolean correct = false;
        boolean emergencyUnlock = false;
        try {
            emergencyUnlock = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.EMERGENCY_PASSWORD_HASH, "").equals(hashedInput);
            correct = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.PASSWORD_HASH, "").equals(hashedInput) || emergencyUnlock;
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        if (correct) {
            TimeOutUtil.getInstance().restartTimer();
            AppLockUtil.isEmergencyUnlocked = emergencyUnlock;

            PrefsUtil.editPrefs().putInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0)
                    .putBoolean(PrefsUtil.BIOMETRICS_PREFERRED, false).apply();

            if (!emergencyUnlock)
                BiometricUtil.onAppLockCredentialVerified();

            if (emergencyUnlock && PrefsUtil.getEmergencyUnlockMode().equals("erase"))
                AppLockUtil.emergencyClearAll();

            if (mClearHistory || emergencyUnlock || (BackendManager.getCurrentBackendConfig() != null && !PrefsUtil.getCurrentBackendConfig().equals(BackendManager.getCurrentBackendConfig().getId()))) {
                Intent intent = new Intent(PasswordEntryActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                AppLockUtil.isLockScreenShown = false;
            } else {
                AppLockUtil.isLockScreenShown = false;
                finish();
            }
        } else {
            mNumFails++;

            PrefsUtil.editPrefs().putInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, mNumFails).apply();

            final Animation animShake = AnimationUtils.loadAnimation(this, R.anim.shake);
            View view = findViewById(R.id.rootPasswordInputLayout);
            view.startAnimation(animShake);
            mVibrator.vibrate(RefConstants.VIBRATE_LONG);

            // Start the input delay if required. It is persisted, this way the delay is also enforced upon app restart.
            long delay = AppLockUtil.registerFailedUnlockAttempt(mNumFails);
            if (delay > 0) {
                Toast.makeText(this, AppLockUtil.getUnlockDelayMessage(this, delay), Toast.LENGTH_LONG).show();
                disableContinueButtonFor(delay);
            } else {
                Toast.makeText(this, R.string.error_wrong_password, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void disableContinueButtonFor(long millis) {
        mBtnContinue.setButtonEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBtnContinue.setButtonEnabled(true);
            }
        }, millis);
    }

    private void showBiometricsPrompt() {
        BiometricPrompt.CryptoObject cryptoObject = BiometricUtil.createCryptoObject();
        if (cryptoObject == null) {
            // The biometrics of the device changed. Biometric unlock got disabled.
            mBtnBiometrics.setVisibility(View.GONE);
            BiometricUtil.showBiometricUnlockDisabledDialogIfPending(this);
            return;
        }
        hideKeyboard();
        mBtnBiometrics.setVisibility(View.GONE);
        mBtnContinue.setVisibility(View.GONE);
        mPasswordInput.setVisibility(View.GONE);
        mInputPasswordTitle.setVisibility(View.GONE);
        mBiometricPrompt.authenticate(mPromptInfo, cryptoObject);
    }

    private void exitBiometricsPrompt() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnBiometrics.setVisibility(BiometricUtil.isBiometricUnlockOffered() ? View.VISIBLE : View.GONE);
                mBtnContinue.setVisibility(View.VISIBLE);
                mPasswordInput.setVisibility(View.VISIBLE);
                mInputPasswordTitle.setVisibility(View.VISIBLE);
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

    @Override
    protected void onResume() {
        super.onResume();

        // Show biometric prompt if preferred
        if (PrefsUtil.isBiometricPreferred() && BiometricUtil.isBiometricUnlockOffered() && !BiometricUtil.noBiometricsEnrolledOnDevice()) {
            showBiometricsPrompt();
        } else {
            BiometricUtil.showBiometricUnlockDisabledDialogIfPending(this);
        }
    }

    public void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(mPasswordInput.getEditText(), InputMethodManager.SHOW_IMPLICIT);
    }

    public void hideKeyboard() {
        View view = getWindow().getDecorView();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
