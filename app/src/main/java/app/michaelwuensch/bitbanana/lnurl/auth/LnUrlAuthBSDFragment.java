package app.michaelwuensch.bitbanana.lnurl.auth;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import java.net.URL;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;


public class LnUrlAuthBSDFragment extends BaseBSDFragment {

    public static final String TAG = LnUrlAuthBSDFragment.class.getSimpleName();
    private static final String EXTRA_LNURL_AUTH_RESPONSE = "lnurlAuthResponse";

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private ConstraintLayout mContentTopLayout;
    private View mInfoView;
    private TextView mServiceName;
    private Button mAuthButton;
    private TextView mInfoQuestion;

    private LnUrlAuth mLnUrlAuth;


    public static LnUrlAuthBSDFragment createLnUrlAuthDialog(URL url) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_LNURL_AUTH_RESPONSE, url);
        LnUrlAuthBSDFragment lnUrlAuthBottomSheetDialog = new LnUrlAuthBSDFragment();
        lnUrlAuthBottomSheetDialog.setArguments(intent.getExtras());
        return lnUrlAuthBottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle args = getArguments();
        mLnUrlAuth = new LnUrlAuth(getContext(), (URL) args.getSerializable(EXTRA_LNURL_AUTH_RESPONSE));

        View view = inflater.inflate(R.layout.bsd_lnurl_auth, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mServiceName = view.findViewById(R.id.serviceName);
        mInfoView = view.findViewById(R.id.infoView);
        mProgressView = view.findViewById(R.id.progressLayout);
        mAuthButton = view.findViewById(R.id.authButton);
        mInfoQuestion = view.findViewById(R.id.infoQuestion);


        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(false);
        mBSDScrollableMainView.setTitle("");
        mBSDScrollableMainView.setHelpButtonVisibility(true);
        mBSDScrollableMainView.setHelpMessage(R.string.help_dialog_lnurl_auth);

        mServiceName.setText(mLnUrlAuth.getHost());

        switch (mLnUrlAuth.getAction()) {
            case LnUrlAuth.ACTION_REGISTER:
                mInfoQuestion.setText(getResources().getString(R.string.lnurl_auth_register_info));
                mAuthButton.setText(getResources().getString(R.string.register));
                break;
            case LnUrlAuth.ACTION_LINK:
                mInfoQuestion.setText(getResources().getString(R.string.lnurl_auth_link_info));
                mAuthButton.setText(getResources().getString(R.string.auth_link_account));
                break;
            case LnUrlAuth.ACTION_AUTH:
                mInfoQuestion.setText(getResources().getString(R.string.lnurl_auth_auth_info));
                mAuthButton.setText(getResources().getString(R.string.authenticate));
                break;
            default:
                mInfoQuestion.setText(getResources().getString(R.string.lnurl_auth_login_info));
                mAuthButton.setText(getResources().getString(R.string.login_verb));
        }

        mAuthButton.setOnClickListener(v -> {
            if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                switchToProgressScreen();
                mLnUrlAuth.authenticate(getCompositeDisposable(), new LnUrlAuth.AuthListener() {
                    @Override
                    public void onError(String message) {
                        switchToFailedScreen(message);
                    }

                    @Override
                    public void onSuccess() {
                        switchToSuccessScreen();
                    }
                });
            } else {
                Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
            }
        });

        mResultView.setOnOkListener(this::dismiss);

        return view;
    }


    private void switchToProgressScreen() {
        // We need to make sure the results are executed on the UI Thread to prevent crashes.
        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressView.setVisibility(View.VISIBLE);
                mInfoView.setVisibility(View.INVISIBLE);
                mProgressView.startSpinning();
            }
        });
    }

    private void switchToSuccessScreen() {
        // We need to make sure the results are executed on the UI Thread to prevent crashes.
        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressView.spinningFinished(true);
                TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
                mInfoView.setVisibility(View.GONE);
                mResultView.setVisibility(View.VISIBLE);
                mResultView.setHeading(R.string.success, true);
                mResultView.setDetailsText(R.string.lnurl_auth_success);
            }
        });
    }

    private void switchToFailedScreen(String error) {
        // We need to make sure the results are executed on the UI Thread to prevent crashes.
        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressView.spinningFinished(false);
                TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
                mInfoView.setVisibility(View.GONE);
                mResultView.setVisibility(View.VISIBLE);

                // Set failed states
                mResultView.setHeading(R.string.lnurl_auth_fail, false);
                mResultView.setDetailsText(error);
            }
        });
    }
}
