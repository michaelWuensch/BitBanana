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
        if (PrefsUtil.isBiometricEnabled() && BiometricUtil.hardwareAvailable()) {
            mBtnBiometrics.setVisibility(View.VISIBLE);
        } else {
            mBtnBiometrics.setVisibility(View.GONE);
        }

        Executor executor = Executors.newSingleThreadExecutor();

        mPromptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getResources().getString(R.string.biometricPrompt_title))
                .setNegativeButtonText(getResources().getString(R.string.cancel))
                .build();


        mBiometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

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
                if (BiometricUtil.notSetup()) {
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
        if (mNumFails >= RefConstants.APP_LOCK_MAX_FAILS) {

            long timeDiff = System.currentTimeMillis() - PrefsUtil.getPrefs().getLong("failedLoginTimestamp", 0L);

            if (timeDiff < RefConstants.APP_LOCK_DELAY_TIME * 1000) {

                mBtnContinue.setButtonEnabled(false);

                String message = getResources().getString(R.string.pin_entered_wrong_wait, String.valueOf((int) ((RefConstants.APP_LOCK_DELAY_TIME * 1000 - timeDiff) / 1000)));
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBtnContinue.setButtonEnabled(true);
                    }
                }, RefConstants.APP_LOCK_DELAY_TIME * 1000 - timeDiff);
            }
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
        if (mPasswordInput.getData() == null || mPasswordInput.getData().isEmpty()) {
            Toast.makeText(this, getString(R.string.backup_data_password_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if password was correct
        String hashedInput = UtilFunctions.appLockDataHash(mPasswordInput.getData());
        boolean correct = false;
        try {
            correct = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.PASSWORD_HASH, "").equals(hashedInput);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        if (correct) {
            TimeOutUtil.getInstance().restartTimer();

            PrefsUtil.editPrefs().putInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0)
                    .putBoolean(PrefsUtil.BIOMETRICS_PREFERRED, false).apply();

            if (mClearHistory) {
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

            if (mNumFails >= RefConstants.APP_LOCK_MAX_FAILS) {
                mBtnContinue.setButtonEnabled(false);
                String message = getResources().getString(R.string.pin_entered_wrong_wait, String.valueOf(RefConstants.APP_LOCK_DELAY_TIME));
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                // Save timestamp. This way the delay can also be forced upon app restart.
                PrefsUtil.editPrefs().putLong("failedLoginTimestamp", System.currentTimeMillis()).apply();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBtnContinue.setButtonEnabled(true);
                    }
                }, RefConstants.APP_LOCK_DELAY_TIME * 1000);
            } else {
                Toast.makeText(this, R.string.error_wrong_password, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBiometricsPrompt() {
        hideKeyboard();
        mBtnBiometrics.setVisibility(View.GONE);
        mBtnContinue.setVisibility(View.GONE);
        mPasswordInput.setVisibility(View.GONE);
        mInputPasswordTitle.setVisibility(View.GONE);
        mBiometricPrompt.authenticate(mPromptInfo);
    }

    private void exitBiometricsPrompt() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnBiometrics.setVisibility(View.VISIBLE);
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
        if (PrefsUtil.isBiometricPreferred() && PrefsUtil.isBiometricEnabled() && BiometricUtil.hardwareAvailable()) {
            showBiometricsPrompt();
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
