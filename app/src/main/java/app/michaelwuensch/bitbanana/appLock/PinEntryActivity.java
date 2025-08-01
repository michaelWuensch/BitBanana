package app.michaelwuensch.bitbanana.appLock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.home.HomeActivity;
import app.michaelwuensch.bitbanana.util.AppLockUtil;
import app.michaelwuensch.bitbanana.util.BiometricUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.ScrambledNumpad;
import app.michaelwuensch.bitbanana.util.TimeOutUtil;
import app.michaelwuensch.bitbanana.util.UtilFunctions;


public class PinEntryActivity extends BaseAppCompatActivity {

    public static final String EXTRA_CLEAR_HISTORY = "ClearHistory";

    private int mPinLength = 0;

    private ImageButton mBtnPinConfirm;
    private Button mBtnPinRemove;
    private ImageButton mBtnPinBack;
    private ImageButton mBtnBiometrics;
    private ImageView[] mPinHints = new ImageView[10];
    private Button[] mBtnNumpad = new Button[10];
    private View mPinInputLayout;
    private View mLogo;

    private BiometricPrompt mBiometricPrompt;
    private BiometricPrompt.PromptInfo mPromptInfo;

    private TextView mTvPrompt;
    private ScrambledNumpad mNumpad;
    private StringBuilder mUserInput;
    private Vibrator mVibrator;
    private int mNumFails;
    private boolean mScramble;
    private boolean mClearHistory;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_input);

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

        mUserInput = new StringBuilder();
        mNumpad = new ScrambledNumpad();
        mTvPrompt = findViewById(R.id.pinPrompt);
        mTvPrompt.setText(R.string.pin_enter);
        mLogo = findViewById(R.id.pinLogo);
        mPinInputLayout = findViewById(R.id.pinInputLayout);

        mVibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        mNumFails = PrefsUtil.getPrefs().getInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0);

        mPinLength = PrefsUtil.getPrefs().getInt(PrefsUtil.PIN_LENGTH, RefConstants.PIN_MIN_LENGTH);

        mScramble = PrefsUtil.getPrefs().getBoolean("scramblePin", true);


        // Define buttons

        mBtnNumpad[0] = findViewById(R.id.pinNumpad1);
        mBtnNumpad[0].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(0).getValue()) : "1");
        mBtnNumpad[1] = findViewById(R.id.pinNumpad2);
        mBtnNumpad[1].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(1).getValue()) : "2");
        mBtnNumpad[2] = findViewById(R.id.pinNumpad3);
        mBtnNumpad[2].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(2).getValue()) : "3");
        mBtnNumpad[3] = findViewById(R.id.pinNumpad4);
        mBtnNumpad[3].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(3).getValue()) : "4");
        mBtnNumpad[4] = findViewById(R.id.pinNumpad5);
        mBtnNumpad[4].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(4).getValue()) : "5");
        mBtnNumpad[5] = findViewById(R.id.pinNumpad6);
        mBtnNumpad[5].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(5).getValue()) : "6");
        mBtnNumpad[6] = findViewById(R.id.pinNumpad7);
        mBtnNumpad[6].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(6).getValue()) : "7");
        mBtnNumpad[7] = findViewById(R.id.pinNumpad8);
        mBtnNumpad[7].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(7).getValue()) : "8");
        mBtnNumpad[8] = findViewById(R.id.pinNumpad9);
        mBtnNumpad[8].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(8).getValue()) : "9");
        mBtnNumpad[9] = findViewById(R.id.pinNumpad0);
        mBtnNumpad[9].setText(mScramble ? Integer.toString(mNumpad.getNumpad().get(9).getValue()) : "0");

        mBtnPinConfirm = findViewById(R.id.pinConfirm);
        mBtnPinRemove = findViewById(R.id.pinRemove);
        mBtnPinBack = findViewById(R.id.pinBack);
        mBtnBiometrics = findViewById(R.id.pinBiometrics);


        // Get pin hints
        mPinHints[0] = findViewById(R.id.pinHint1);
        mPinHints[1] = findViewById(R.id.pinHint2);
        mPinHints[2] = findViewById(R.id.pinHint3);
        mPinHints[3] = findViewById(R.id.pinHint4);
        mPinHints[4] = findViewById(R.id.pinHint5);
        mPinHints[5] = findViewById(R.id.pinHint6);
        mPinHints[6] = findViewById(R.id.pinHint7);
        mPinHints[7] = findViewById(R.id.pinHint8);
        mPinHints[8] = findViewById(R.id.pinHint9);
        mPinHints[9] = findViewById(R.id.pinHint10);


        // Set all layout element states to the current user input (empty right now)
        displayUserInput();

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
                    Intent intent = new Intent(PinEntryActivity.this, HomeActivity.class);
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
                            Toast.makeText(PinEntryActivity.this, errString, Toast.LENGTH_SHORT).show();
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
                    AlertDialog.Builder adb = new AlertDialog.Builder(PinEntryActivity.this)
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


        mBtnPinBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mUserInput.toString().length() > 0) {
                    mUserInput.deleteCharAt(mUserInput.length() - 1);
                    if (PrefsUtil.getPrefs().getBoolean("hapticPin", true)) {
                        mVibrator.vibrate(RefConstants.VIBRATE_SHORT);
                    }
                }
                displayUserInput();

            }
        });

        mBtnPinBack.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mUserInput.toString().length() > 0) {
                    mUserInput.setLength(0);
                    if (PrefsUtil.getPrefs().getBoolean("hapticPin", true)) {
                        mVibrator.vibrate(RefConstants.VIBRATE_SHORT);
                    }
                }
                displayUserInput();
                return false;
            }
        });

        // If the user closed and restarted the app he still has to wait until the PIN input delay is over.
        if (mNumFails >= RefConstants.APP_LOCK_MAX_FAILS) {

            long timeDiff = System.currentTimeMillis() - PrefsUtil.getPrefs().getLong("failedLoginTimestamp", 0L);

            if (timeDiff < RefConstants.APP_LOCK_DELAY_TIME * 1000) {

                for (Button btn : mBtnNumpad) {
                    btn.setEnabled(false);
                    btn.setAlpha(0.3f);
                }

                String message = getResources().getString(R.string.pin_entered_wrong_wait, String.valueOf((int) ((RefConstants.APP_LOCK_DELAY_TIME * 1000 - timeDiff) / 1000)));
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        for (Button btn : mBtnNumpad) {
                            btn.setEnabled(true);
                            btn.setAlpha(1f);
                        }
                    }
                }, RefConstants.APP_LOCK_DELAY_TIME * 1000 - timeDiff);
            }
        }

        // Set action for numpad buttons
        for (Button btn : mBtnNumpad) {
            btn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // vibrate
                    if (PrefsUtil.getPrefs().getBoolean("hapticPin", true)) {
                        mVibrator.vibrate(RefConstants.VIBRATE_SHORT);
                    }
                    // Add input
                    mUserInput.append(((Button) v).getText().toString());
                    displayUserInput();

                    if (mUserInput.toString().length() == mPinLength) {
                        // We want to start the pin check after UI has updated, otherwise it doesn't look good.
                        Handler handler = new Handler();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                pinEntered();
                            }
                        });
                    }
                }
            });
        }

    }

    private void displayUserInput() {

        // Correctly display number of hints as visual PIN representation

        // Mark with highlight color
        for (int i = 0; i < mUserInput.toString().length(); i++) {
            mPinHints[i].setColorFilter(ContextCompat.getColor(this, R.color.white));
        }
        // Set missing
        for (int i = mUserInput.toString().length(); i < mPinLength; i++) {
            mPinHints[i].setColorFilter(ContextCompat.getColor(this, R.color.gray_dark));
        }
        // Hide not used PIN hints
        for (int i = mPinLength; i < mPinHints.length; i++) {
            mPinHints[i].setVisibility(View.GONE);
        }

        // Hide confirm and remove buttons
        mBtnPinConfirm.setVisibility(View.INVISIBLE);
        mBtnPinRemove.setVisibility(View.INVISIBLE);

        // Disable back button if user input is empty.
        if (mUserInput.toString().length() > 0) {
            mBtnPinBack.setEnabled(true);
            mBtnPinBack.setAlpha(1f);
        } else {
            mBtnPinBack.setEnabled(false);
            mBtnPinBack.setAlpha(0.3f);
        }

    }

    public void pinEntered() {
        // Check if PIN was correct
        String userEnteredPin = mUserInput.toString();
        String hashedInput = UtilFunctions.appLockDataHash(userEnteredPin);
        boolean correct = false;
        boolean emergencyUnlock = false;
        try {
            emergencyUnlock = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.EMERGENCY_PIN_HASH, "").equals(hashedInput);
            correct = PrefsUtil.getEncryptedPrefs().getString(PrefsUtil.PIN_HASH, "").equals(hashedInput) || emergencyUnlock;
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        if (correct) {
            TimeOutUtil.getInstance().restartTimer();
            AppLockUtil.isEmergencyUnlocked = emergencyUnlock;

            PrefsUtil.editPrefs().putInt(PrefsUtil.APP_NUM_UNLOCK_FAILS, 0)
                    .putBoolean(PrefsUtil.BIOMETRICS_PREFERRED, false).apply();

            if (emergencyUnlock && PrefsUtil.getEmergencyUnlockMode().equals("erase"))
                AppLockUtil.emergencyClearAll();

            if (mClearHistory || emergencyUnlock || (BackendManager.getCurrentBackendConfig() != null && !PrefsUtil.getCurrentBackendConfig().equals(BackendManager.getCurrentBackendConfig().getId()))) {
                Intent intent = new Intent(PinEntryActivity.this, HomeActivity.class);
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
            View view = findViewById(R.id.pinInputLayout);
            view.startAnimation(animShake);

            if (PrefsUtil.getPrefs().getBoolean("hapticPin", true)) {
                mVibrator.vibrate(RefConstants.VIBRATE_LONG);
            }

            // clear the user input
            mUserInput.setLength(0);
            displayUserInput();


            if (mNumFails >= RefConstants.APP_LOCK_MAX_FAILS) {
                for (Button btn : mBtnNumpad) {
                    btn.setEnabled(false);
                    btn.setAlpha(0.3f);
                }
                String message = getResources().getString(R.string.pin_entered_wrong_wait, String.valueOf(RefConstants.APP_LOCK_DELAY_TIME));
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                // Save timestamp. This way the delay can also be forced upon app restart.
                PrefsUtil.editPrefs().putLong("failedLoginTimestamp", System.currentTimeMillis()).apply();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        for (Button btn : mBtnNumpad) {
                            btn.setEnabled(true);
                            btn.setAlpha(1f);
                        }
                    }
                }, RefConstants.APP_LOCK_DELAY_TIME * 1000);
            } else {
                Toast.makeText(this, R.string.pin_entered_wrong, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Show biometric prompt if preferred
        if (PrefsUtil.isBiometricPreferred() && PrefsUtil.isBiometricEnabled() && BiometricUtil.hardwareAvailable()) {
            showBiometricsPrompt();
        }
    }

    private void showBiometricsPrompt() {
        mPinInputLayout.setVisibility(View.GONE);
        mLogo.setVisibility(View.VISIBLE);
        mBiometricPrompt.authenticate(mPromptInfo);
    }

    private void exitBiometricsPrompt() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPinInputLayout.setVisibility(View.VISIBLE);
                mLogo.setVisibility(View.GONE);
            }
        });
    }
}
