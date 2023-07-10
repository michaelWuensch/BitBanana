package app.michaelwuensch.bitbanana.customView;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.github.lightningnetwork.lnd.lnrpc.PayReq;

import java.net.URL;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.connection.BaseNodeConfig;
import app.michaelwuensch.bitbanana.lightning.LightningNodeUri;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.channel.LnUrlHostedChannelResponse;
import app.michaelwuensch.bitbanana.lnurl.pay.LnUrlPayResponse;
import app.michaelwuensch.bitbanana.lnurl.withdraw.LnUrlWithdrawResponse;
import app.michaelwuensch.bitbanana.util.BitcoinStringAnalyzer;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ManualSendInputView extends ConstraintLayout {

    private Button mBtnContinue;
    private EditText mEditText;
    private ProgressBar mSpinner;
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
        mBtnContinue = view.findViewById(R.id.continueButton);
        mSpinner = view.findViewById(R.id.spinner);
    }

    public void setupView(CompositeDisposable cd) {
        mCompositeDisposable = cd;
        mBtnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mData = mEditText.getText().toString();
                mSpinner.setVisibility(VISIBLE);
                mBtnContinue.setVisibility(INVISIBLE);
                /* We are not allowed to access LNURL links twice.
                Therefore we first have to check if it is a LNURL and then hand over to the HomeActivity.
                Executing the rest twice doesn't harm anyone.
                 */
                if (BitcoinStringAnalyzer.isLnUrl(mData)) {
                    mListener.onValid(mData);
                }
                BitcoinStringAnalyzer.analyze(getContext(), mCompositeDisposable, mData, new BitcoinStringAnalyzer.OnDataDecodedListener() {
                    @Override
                    public void onValidLightningInvoice(PayReq paymentRequest, String invoice) {
                        mListener.onValid(mData);
                    }

                    @Override
                    public void onValidBitcoinInvoice(String address, long amount, String message, String lightningInvoice) {
                        mListener.onValid(mData);
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
                    public void onValidInternetIdentifier(LnUrlPayResponse payResponse) {
                        mListener.onValid(mData);
                    }

                    @Override
                    public void onValidLndConnectString(BaseNodeConfig baseNodeConfig) {
                        invalidInput();
                    }

                    @Override
                    public void onValidBTCPayConnectData(BaseNodeConfig baseNodeConfig) {
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
            mBtnContinue.setVisibility(VISIBLE);
            mSpinner.setVisibility(INVISIBLE);
        });
    }
}
