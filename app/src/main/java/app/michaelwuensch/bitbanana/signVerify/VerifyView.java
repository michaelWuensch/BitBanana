package app.michaelwuensch.bitbanana.signVerify;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class VerifyView extends LinearLayout {

    private static final String LOG_TAG = SignView.class.getSimpleName();
    private EditText mEtMessageToVerify;
    private EditText mEtSignatureToVerify;
    private Button mBtnVerify;
    private View mViewVerifyLayout;
    private TextView mTVValidationInfo;
    private TextView mTVPubkeyLabel;
    private TextView mTVPubkey;
    private ImageView mIVCopyPubkey;

    private CompositeDisposable mCompositeDisposable;


    public VerifyView(Context context) {
        super(context);
        init();
    }

    public VerifyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VerifyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_verify, this);

        mCompositeDisposable = new CompositeDisposable();

        mEtMessageToVerify = view.findViewById(R.id.messageToVerify);
        mEtSignatureToVerify = view.findViewById(R.id.signatureToVerify);
        mBtnVerify = view.findViewById(R.id.verifyButton);
        mViewVerifyLayout = view.findViewById(R.id.verifiedSignatureLayout);
        mTVValidationInfo = view.findViewById(R.id.signatureValidationInfo);
        mTVPubkeyLabel = view.findViewById(R.id.signaturePubKeyLabel);
        mTVPubkey = view.findViewById(R.id.signaturePubKey);
        mIVCopyPubkey = view.findViewById(R.id.pubkeyCopyIcon);

        mViewVerifyLayout.setVisibility(GONE);
        mBtnVerify.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // Hide software keyboard
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                // verify
                verify();
            }
        });
        mIVCopyPubkey.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                ClipBoardUtil.copyToClipboard(getContext(), "Pubkey", mTVPubkey.getText());
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        mCompositeDisposable.dispose();
        super.onDetachedFromWindow();
    }

    /**
     * Verifies the message. The signature is only deemed valid if the recovered public key corresponds to a node key in the public Lightning network.
     */
    private void verify() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            String message = mEtMessageToVerify.getText().toString();
            String signature = mEtSignatureToVerify.getText().toString();
            if (!message.isEmpty() && !signature.isEmpty()) {
                mCompositeDisposable.add(BackendManager.api().verifyMessageWithNode(message, signature)
                        .subscribe(response -> {
                            BBLog.v(LOG_TAG, "Signature is valid: " + response.isValid());
                            BBLog.v(LOG_TAG, "PubKey of signature: " + response.getPubKey());
                            updateVerificationInfo(response.isValid(), response.getPubKey());
                        }, throwable -> {
                            BBLog.d(LOG_TAG, "Verify message failed: " + throwable.fillInStackTrace());
                            if (throwable.getMessage().contains("pubkey not found in the graph"))
                                updateVerificationInfo(false, "none");
                            else {
                                mTVValidationInfo.setText(throwable.getMessage());
                                showFailure();
                            }
                        }));
            }
        } else {
            Toast.makeText(getContext(), R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateVerificationInfo(boolean valid, String pubkey) {
        mViewVerifyLayout.setVisibility(VISIBLE);
        if (valid) {
            mTVValidationInfo.setText(R.string.signature_valid);
            mTVValidationInfo.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
            mTVPubkeyLabel.setVisibility(VISIBLE);
            mTVPubkey.setVisibility(VISIBLE);
            mTVPubkey.setText(pubkey);
            mIVCopyPubkey.setVisibility(VISIBLE);
        } else {
            mTVValidationInfo.setText(R.string.signature_invalid);
            showFailure();
        }
    }

    private void showFailure() {
        mViewVerifyLayout.setVisibility(VISIBLE);
        mTVValidationInfo.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
        mTVPubkeyLabel.setVisibility(GONE);
        mTVPubkey.setVisibility(GONE);
        mIVCopyPubkey.setVisibility(GONE);
    }
}
