package app.michaelwuensch.bitbanana.signVerify;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class SignView extends LinearLayout {

    private static final String LOG_TAG = SignView.class.getSimpleName();
    private EditText mEtMessageToSign;
    private TextView mTVGeneratedSignature;
    private View mViewGeneratedSignatureLayout;
    private ImageView mIVCopySignature;
    private Button mBtnSign;

    private CompositeDisposable mCompositeDisposable;


    public SignView(Context context) {
        super(context);
        init();
    }

    public SignView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_sign, this);

        mCompositeDisposable = new CompositeDisposable();
        mEtMessageToSign = view.findViewById(R.id.messageToSign);
        mViewGeneratedSignatureLayout = view.findViewById(R.id.generatedSignatureLayout);
        mTVGeneratedSignature = view.findViewById(R.id.generatedSignature);
        mIVCopySignature = view.findViewById(R.id.signatureCopyIcon);
        mBtnSign = view.findViewById(R.id.signButton);

        mViewGeneratedSignatureLayout.setVisibility(GONE);
        mBtnSign.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // Hide software keyboard
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                // Sign slightly delayed so the sofKeyboard is actually gone before the copy to clipboard toast message is added.
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    sign();
                }, 150);
            }
        });
        mIVCopySignature.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                ClipBoardUtil.copyToClipboard(getContext(), "Signature", mTVGeneratedSignature.getText());
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        mCompositeDisposable.dispose();
        super.onDetachedFromWindow();
    }

    private void sign() {
        if (BackendManager.hasBackendConfigs()) {
            String message = mEtMessageToSign.getText().toString();
            if (!message.isEmpty()) {
                mCompositeDisposable.add(BackendManager.api().signMessageWithNode(message)
                        .subscribe(response -> {
                            BBLog.v(LOG_TAG, "Created signature: " + response);
                            updateSignatureInfo(response.getZBase());
                        }, throwable -> BBLog.d(LOG_TAG, "Sign message failed: " + throwable.fillInStackTrace())));
            }
        } else {
            Toast.makeText(getContext(), R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSignatureInfo(String signature) {
        mTVGeneratedSignature.setText(signature);
        mViewGeneratedSignatureLayout.setVisibility(VISIBLE);
        ClipBoardUtil.copyToClipboard(getContext(), "Signature", signature);
    }
}
