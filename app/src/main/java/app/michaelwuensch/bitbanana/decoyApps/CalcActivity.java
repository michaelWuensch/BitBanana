package app.michaelwuensch.bitbanana.decoyApps;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.mariuszgromada.math.mxparser.Expression;

import app.michaelwuensch.bitbanana.LandingActivity;
import app.michaelwuensch.bitbanana.R;

/**
 * A simple calculator app that actually works and can be used as decoy app to hide BitBanana
 * and protect it against physical phone search.
 */
public class CalcActivity extends AppCompatActivity {

    private TextView mTvResult;
    private EditText mEtInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoy_app_calculator);

        mTvResult = findViewById(R.id.calcTvResult);
        mEtInput = findViewById(R.id.calcEtInput);
        mEtInput.setShowSoftInputOnFocus(false);
        mEtInput.requestFocus();

        Button btn_1 = findViewById(R.id.calcBtnOne);
        Button btn_2 = findViewById(R.id.calcBtnTwo);
        Button btn_3 = findViewById(R.id.calcBtnThree);
        Button btn_4 = findViewById(R.id.calcBtnFour);
        Button btn_5 = findViewById(R.id.calcBtnFive);
        Button btn_6 = findViewById(R.id.calcBtnSix);
        Button btn_7 = findViewById(R.id.calcBtnSeven);
        Button btn_8 = findViewById(R.id.calcBtnEight);
        Button btn_9 = findViewById(R.id.calcBtnNine);
        Button btn_0 = findViewById(R.id.calcBtnZero);
        Button btn_point = findViewById(R.id.calcBtnPoint);
        Button btn_plus = findViewById(R.id.calcBtnPlus);
        Button btn_minus = findViewById(R.id.calcBtnMinus);
        Button btn_multiply = findViewById(R.id.calcBtnMult);
        Button btn_divide = findViewById(R.id.calcBtnDivide);
        Button btn_percent = findViewById(R.id.calcBtnPercent);
        Button btn_sqrt = findViewById(R.id.calcBtnSqrt);
        Button btn_bracket_left = findViewById(R.id.calcBtnLeftBracket);
        Button btn_bracket_right = findViewById(R.id.calcBtnRightBracket);
        Button btn_equal = findViewById(R.id.calcBtnEqual);
        Button btn_pi = findViewById(R.id.calcBtnPi);
        Button btn_ac = findViewById(R.id.calcBtnAC);
        Button btn_power = findViewById(R.id.calcBtnPower);
        Button btn_backspace = findViewById(R.id.calcBtnBackspace);


        // Click listener for all buttons that just add their assigned value / symbol to the edit text field.
        View.OnClickListener simpleInputButtonClickListener = view -> {
            addInput(((Button) view).getText().toString());
            calculate(false);
        };

        Button[] simpleInputButtons = {btn_1, btn_2, btn_3, btn_4, btn_5, btn_6, btn_7, btn_8, btn_9, btn_0, btn_point, btn_plus, btn_minus, btn_multiply, btn_percent, btn_sqrt, btn_bracket_left, btn_bracket_right};
        for (Button btn : simpleInputButtons) {
            btn.setOnClickListener(simpleInputButtonClickListener);
        }

        btn_divide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addInput("/");
                calculate(false);
            }
        });

        btn_power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addInput("^");
                calculate(false);
            }
        });

        btn_pi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addInput("pi");
                calculate(false);
            }
        });

        btn_ac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEtInput.setText("");
                mTvResult.setText("");
            }
        });

        btn_backspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeInput();
                calculate(false);
            }
        });

        btn_equal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculate(true);
                try {
                    if (mEtInput.getText().toString().equals("21")) {
                        secretCodeEntered();
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void addInput(String input) {
        int start = Math.max(mEtInput.getSelectionStart(), 0);
        int end = Math.max(mEtInput.getSelectionEnd(), 0);
        mEtInput.getText().replace(Math.min(start, end), Math.max(start, end),
                input, 0, input.length());
    }

    private void removeInput() {
        if (mEtInput != null) {
            boolean selection = mEtInput.getSelectionStart() != mEtInput.getSelectionEnd();

            int start = Math.max(mEtInput.getSelectionStart(), 0);
            int end = Math.max(mEtInput.getSelectionEnd(), 0);

            String before = mEtInput.getText().toString().substring(0, start);
            String after = mEtInput.getText().toString().substring(end);

            if (selection) {
                String outputText = before + after;
                mEtInput.setText(outputText);
                mEtInput.setSelection(start);
            } else {
                if (before.length() >= 1) {
                    String newBefore = before.substring(0, before.length() - 1);
                    String outputText = newBefore + after;
                    mEtInput.setText(outputText);
                    mEtInput.setSelection(start - 1);
                }
            }
        }
    }

    private void calculate(boolean equalPressed) {
        Expression expr = new Expression(mEtInput.getText().toString());
        Double result = expr.calculate();
        String resultString = String.valueOf(result);
        if (resultString.endsWith(".0"))
            resultString = resultString.substring(0, resultString.length() - 2);
        if (equalPressed) {
            mEtInput.setText(resultString);
            mEtInput.setSelection(mEtInput.length());
            mTvResult.setText("");
        } else {
            mTvResult.setText(resultString);
        }
        if (mEtInput.getText().toString().length() > 10) {
            mEtInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        } else {
            mEtInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 55);
        }
    }

    private void secretCodeEntered() {
        Intent homeIntent = new Intent(CalcActivity.this, LandingActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        // FinishAffinity is needed here as this forces the on destroy events from previous activities to be executed before continuing.
        finishAffinity();

        startActivity(homeIntent);
    }
}