package app.michaelwuensch.bitbanana.customView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.net.URL;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.home.ManualSendScanActivity;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.models.Bip21Invoice;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.util.BitcoinStringAnalyzer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.StaticInternetIdentifierReader;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ManualSendInputView extends ConstraintLayout {

    private ActivityResultLauncher<Intent> mActivityResultLauncher;

    private BBButton mBtnContinue;
    private ImageButton mImgBtnPaste;
    private ImageButton mImgBtnScan;
    private EditText mEditText;
    private OnResultListener mListener;
    private CompositeDisposable mCompositeDisposable;
    private String mData;

    public ManualSendInputView(Context context) {
        super(context);
        init();
    }

    public ManualSendInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ManualSendInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        View view = inflate(getContext(), R.layout.view_manual_send_input, this);

        mEditText = view.findViewById(R.id.sendInput);
        mImgBtnPaste = view.findViewById(R.id.pasteButton);
        mImgBtnScan = view.findViewById(R.id.scanButton);
        mBtnContinue = view.findViewById(R.id.continueButton);
    }

    public void setupView(CompositeDisposable cd) {
        mCompositeDisposable = cd;

        mImgBtnPaste.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mEditText.setText(ClipBoardUtil.getPrimaryContent(getContext(), false));
            }
        });

        mImgBtnScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent scanIntent = new Intent(getContext(), ManualSendScanActivity.class);
                mActivityResultLauncher.launch(scanIntent);
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mEditText.getText() != null) {
                    if (mEditText.getText().toString().isEmpty())
                        mBtnContinue.setButtonEnabled(false);
                    else
                        mBtnContinue.setButtonEnabled(true);
                } else {
                    mBtnContinue.setButtonEnabled(false);
                }
            }
        });

        mBtnContinue.setButtonEnabled(false);
        mBtnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mData = mEditText.getText().toString();
                mBtnContinue.showProgress();
                /* We are not allowed to access LNURL links twice.
                Therefore we first have to check if it is a LNURL and then hand over to the HomeActivity.
                Executing the rest twice doesn't harm anyone.
                 */
                if (BitcoinStringAnalyzer.isLnUrl(mData)) {
                    mListener.onValid(mData);
                    return;
                }
                if (StaticInternetIdentifierReader.isLnAddress(mData)) {
                    mListener.onValid(mData);
                    return;
                }
                BitcoinStringAnalyzer.analyze(getContext(), mCompositeDisposable, mData, new BitcoinStringAnalyzer.OnDataDecodedListener() {
                    @Override
                    public void onValidLightningInvoice(DecodedBolt11 decodedBolt11, Bip21Invoice fallbackOnChainInvoice) {
                        mListener.onValid(mData);
                    }

                    @Override
                    public void onValidBitcoinInvoice(Bip21Invoice onChainInvoice) {
                        mListener.onValid(mData);
                    }

                    @Override
                    public void onValidBolt12Offer(DecodedBolt12 decodedBolt12, Bip21Invoice fallbackOnChainInvoice) {
                        mListener.onValid(mData);
                    }

                    @Override
                    public void onLnAddressFound() {

                    }

                    @Override
                    public void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse) {
                        invalidInput();
                    }

                    @Override
                    public void onValidLnUrlChannel(LnUrlChannelResponse channelResponse) {
                        invalidInput();
                    }

                    @Override
                    public void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse) {
                        invalidInput();
                    }

                    @Override
                    public void onValidLnUrlPay(LnUrlPayResponse payResponse) {
                        mListener.onValid(mData);
                    }

                    @Override
                    public void onValidLnUrlAuth(URL url) {
                        invalidInput();
                    }

                    @Override
                    public void onValidConnectData(BackendConfig backendConfig) {
                        invalidInput();
                    }

                    @Override
                    public void onValidNodeUri(LightningNodeUri nodeUri) {
                        mListener.onValid(mData);
                    }

                    @Override
                    public void onValidURL(String url) {
                        invalidInput();
                    }

                    @Override
                    public void onError(String error, int duration) {
                        errorReadingData(error, duration);
                    }

                    @Override
                    public void onNoReadableData() {
                        errorReadingData(getContext().getString(R.string.string_analyzer_unrecognized_data), RefConstants.ERROR_DURATION_SHORT);
                    }
                });
            }
        });
    }

    public void setOnResultListener(OnResultListener listener) {
        mListener = listener;
    }

    public interface OnResultListener {
        void onValid(String data);

        void onError(String error, int duration);
    }

    private void errorReadingData(String error, int duration) {
        mListener.onError(error, duration);
        resetContinueButton();
    }

    private void invalidInput() {
        mListener.onError(getContext().getString(R.string.error_only_payment_data_allowed), RefConstants.ERROR_DURATION_MEDIUM);
        resetContinueButton();
    }

    private void resetContinueButton() {
        // This has to be executed on the main tread. If not it will crash if it is a callback from a http request.
        ((Activity) getContext()).runOnUiThread(() -> {
            mBtnContinue.hideProgress();
        });
    }

    public void setInputText(String text) {
        mEditText.setText(text);
    }

    public void setActivityResultLauncher(ActivityResultLauncher<Intent> launcher) {
        mActivityResultLauncher = launcher;
    }
}
