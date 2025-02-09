package app.michaelwuensch.bitbanana.lnurl.channel;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.connection.HttpClient;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.lnurl.LnUrlResponse;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.Peer;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class LnUrlChannelBSDFragment extends BaseBSDFragment {

    public static final String TAG = LnUrlChannelBSDFragment.class.getSimpleName();
    private static final String EXTRA_LNURL_CHANNEL_RESPONSE = "lnurlChannelResponse";

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDResultView mResultView;
    private BSDProgressView mProgressView;
    private ConstraintLayout mContentTopLayout;
    private View mInfoView;
    private TextView mServiceName;
    private CheckBox mPrivateCheckbox;

    private LnUrlChannelResponse mLnUrlChannelResponse;

    public static LnUrlChannelBSDFragment createLnURLChannelDialog(LnUrlChannelResponse lnUrlChannelResponse) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_LNURL_CHANNEL_RESPONSE, lnUrlChannelResponse);
        LnUrlChannelBSDFragment lnUrlChannelBottomSheetDialog = new LnUrlChannelBSDFragment();
        lnUrlChannelBottomSheetDialog.setArguments(intent.getExtras());
        return lnUrlChannelBottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle args = getArguments();
        mLnUrlChannelResponse = (LnUrlChannelResponse) args.getSerializable(EXTRA_LNURL_CHANNEL_RESPONSE);

        View view = inflater.inflate(R.layout.bsd_lnurl_channel, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mServiceName = view.findViewById(R.id.serviceName);
        mInfoView = view.findViewById(R.id.infoView);
        mProgressView = view.findViewById(R.id.paymentProgressLayout);
        mPrivateCheckbox = view.findViewById(R.id.privateCheckBox);


        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(false);
        mBSDScrollableMainView.setTitle("");

        URL url = null;
        try {
            url = new URL(mLnUrlChannelResponse.getCallback());
            String host = url.getHost();
            mServiceName.setText(host);
        } catch (MalformedURLException e) {
            mServiceName.setText(R.string.unknown);
            e.printStackTrace();
        }


        // Action when clicked on "Open Channel"
        BBButton btnOpen = view.findViewById(R.id.openButton);
        btnOpen.setOnClickListener(v -> {
            if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                switchToProgressScreen();
                openChannel();
            } else {
                Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton privateHelpButton = view.findViewById(R.id.privateHelpButton);
        privateHelpButton.setOnClickListener(view1 -> HelpDialogUtil.showDialog(getActivity(), R.string.help_dialog_private_channels));

        mResultView.setOnOkListener(this::dismiss);

        return view;
    }

    private void openChannel() {

        BBLog.v(TAG, "Remote Node uri: " + mLnUrlChannelResponse.getUri());
        LightningNodeUri nodeUri = LightningNodeUriParser.parseNodeUri(mLnUrlChannelResponse.getUri());

        if (nodeUri == null) {
            BBLog.e(TAG, "Node Uri could not be parsed");
            switchToFailedScreen(getActivity().getResources().getString(R.string.lnurl_service_provided_invalid_data));
            return;
        }

        getCompositeDisposable().add(BackendManager.api().listPeers()
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    boolean connected = false;
                    for (Peer node : response) {
                        if (node.getPubKey().equals(nodeUri.getPubKey())) {
                            connected = true;
                            break;
                        }
                    }

                    if (connected) {
                        BBLog.v(TAG, "Already connected to peer, moving on...");
                        sendFinalRequestToService();
                    } else {
                        BBLog.v(TAG, "Not connected to peer, trying to connect...");
                        connectPeer(nodeUri);
                    }
                }, throwable -> {
                    BBLog.e(TAG, "Error listing peers request: " + throwable.getMessage());
                    if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        switchToFailedScreen(getActivity().getResources().getString(R.string.error_get_peers_timeout));
                    } else {
                        switchToFailedScreen(getActivity().getResources().getString(R.string.error_get_peers));
                    }
                }));
    }

    private void connectPeer(LightningNodeUri nodeUri) {
        getCompositeDisposable().add(BackendManager.api().connectPeer(nodeUri)
                .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    BBLog.v(TAG, "Successfully connected to peer");
                    sendFinalRequestToService();
                }, throwable -> {
                    BBLog.e(TAG, "Error connecting to peer: " + throwable.getMessage());

                    if (throwable.getMessage().toLowerCase().contains("refused")) {
                        switchToFailedScreen(getActivity().getResources().getString(R.string.error_connect_peer_refused));
                    } else if (throwable.getMessage().toLowerCase().contains("self")) {
                        switchToFailedScreen(getActivity().getResources().getString(R.string.error_connect_peer_self));
                    } else if (throwable.getMessage().toLowerCase().contains("terminated")) {
                        switchToFailedScreen(getActivity().getResources().getString(R.string.error_connect_peer_timeout));
                    } else {
                        switchToFailedScreen(throwable.getMessage());
                    }
                }));
    }

    private void sendFinalRequestToService() {
        LnUrlFinalOpenChannelRequest lnUrlFinalOpenChannelRequest = new LnUrlFinalOpenChannelRequest.Builder()
                .setCallback(mLnUrlChannelResponse.getCallback())
                .setK1(mLnUrlChannelResponse.getK1())
                .setRemoteId(Wallet.getInstance().getCurrentNodeInfo().getPubKey())
                .setIsPrivate(mPrivateCheckbox.isChecked())
                .build();

        BBLog.v(TAG, lnUrlFinalOpenChannelRequest.requestAsString());


        okhttp3.Request lnUrlRequest = new okhttp3.Request.Builder()
                .url(lnUrlFinalOpenChannelRequest.requestAsString())
                .build();

        HttpClient.getInstance().getClient().newCall(lnUrlRequest).enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                BBLog.e(TAG, "Final request failed");
                switchToFailedScreen("Final request failed");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    BBLog.v(TAG, responseData);
                    validateFinalResponse(responseData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void validateFinalResponse(@NonNull String openChannelResponse) {
        LnUrlResponse lnUrlResponse = new Gson().fromJson(openChannelResponse, LnUrlResponse.class);

        if (lnUrlResponse.getStatus().equals("OK")) {
            BBLog.d(TAG, "LNURL: Success. The service initiated the channel opening.");
            switchToSuccessScreen();
        } else {
            BBLog.e(TAG, "LNURL: Failed to open channel. " + lnUrlResponse.getReason());
            switchToFailedScreen(lnUrlResponse.getReason());
        }
    }

    private void switchToProgressScreen() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Code to be executed on the main thread
                mProgressView.setVisibility(View.VISIBLE);
                mInfoView.setVisibility(View.INVISIBLE);
                mProgressView.startSpinning();
            }
        });
    }

    private void switchToSuccessScreen() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Code to be executed on the main thread
                mProgressView.spinningFinished(true);
                TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
                mInfoView.setVisibility(View.GONE);
                mResultView.setVisibility(View.VISIBLE);
                mResultView.setHeading(R.string.opened_channel, true);
            }
        });
    }

    private void switchToFailedScreen(String error) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Code to be executed on the main thread

                mProgressView.spinningFinished(false);
                TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
                mInfoView.setVisibility(View.GONE);
                mResultView.setVisibility(View.VISIBLE);

                // Set failed states
                mResultView.setHeading(R.string.error, false);
                mResultView.setDetailsText(error);
            }
        });
    }
}
