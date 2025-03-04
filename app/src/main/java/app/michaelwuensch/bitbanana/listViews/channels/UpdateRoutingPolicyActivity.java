package app.michaelwuensch.bitbanana.listViews.channels;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.listViews.channels.itemDetails.ChannelDetailBSDFragment;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.RoutingPolicy;
import app.michaelwuensch.bitbanana.models.Channels.UpdateRoutingPolicyRequest;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.Version;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class UpdateRoutingPolicyActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = UpdateRoutingPolicyActivity.class.getSimpleName();
    public static final String EXTRA_ROUTING_POLICY = "extraRoutingPolicy";

    private BBInputFieldView mBaseFee;
    private BBInputFieldView mFeeRate;
    private BBInputFieldView mInboundBaseFee;
    private BBInputFieldView mInboundFeeRate;
    private BBInputFieldView mTimelock;
    private BBInputFieldView mMinHTLC;
    private BBInputFieldView mMaxHTLC;
    private TextView mAllChannelsWarning;
    private BBButton mBtnSubmit;
    private OpenChannel mChannel;


    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_update_routing_policy);

        mCompositeDisposable = new CompositeDisposable();

        // reference views
        mBaseFee = findViewById(R.id.baseFee);
        mFeeRate = findViewById(R.id.feeRate);
        mInboundBaseFee = findViewById(R.id.inboundBaseFee);
        mInboundFeeRate = findViewById(R.id.inboundFeeRate);
        mTimelock = findViewById(R.id.timelockDelta);
        mMinHTLC = findViewById(R.id.htlcMin);
        mMaxHTLC = findViewById(R.id.htlcMax);
        mAllChannelsWarning = findViewById(R.id.allChannelsWarning);
        mBtnSubmit = findViewById(R.id.submitButton);

        // setup input types
        mBaseFee.setInputType(InputType.TYPE_CLASS_NUMBER);
        mFeeRate.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mInboundBaseFee.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        mInboundFeeRate.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        mTimelock.setInputType(InputType.TYPE_CLASS_NUMBER);
        mMinHTLC.setInputType(InputType.TYPE_CLASS_NUMBER);
        mMaxHTLC.setInputType(InputType.TYPE_CLASS_NUMBER);

        // hide input fields based on backend
        if (BackendManager.getCurrentBackendType() == BackendConfig.BackendType.CORE_LIGHTNING_GRPC)
            mTimelock.setVisibility(View.GONE);

        if (!(BackendManager.getCurrentBackendType() == BackendConfig.BackendType.LND_GRPC && Wallet.getInstance().getCurrentNodeInfo().getVersion().compareTo(new Version("0.18.0")) >= 0)) { //ToDo: Remove version check when lnd 0.17 is no longer supported
            mInboundBaseFee.setVisibility(View.GONE);
            mInboundFeeRate.setVisibility(View.GONE);
        }

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(ChannelDetailBSDFragment.ARGS_CHANNEL)) {
                mChannel = (OpenChannel) extras.getSerializable(ChannelDetailBSDFragment.ARGS_CHANNEL);
                RoutingPolicy routingPolicy = (RoutingPolicy) extras.getSerializable(EXTRA_ROUTING_POLICY);
                setDefaultValues(routingPolicy);
            }
        }

        // replace translated descriptions
        mFeeRate.setDescriptionDetail("(" + getResources().getString(R.string.percent) + ")");
        String timelockDetailDescription = "(" + getResources().getQuantityString(R.plurals.blocks, 1000).replace("%d ", "") + ")";
        mTimelock.setDescriptionDetail(timelockDetailDescription);

        if (mChannel != null) {
            mAllChannelsWarning.setVisibility(View.GONE);
        }

        // On submit
        mBtnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                    // Validate input
                    if (mFeeRate.getData() != null) {
                        if (Double.parseDouble(mFeeRate.getData()) > 25.0) {
                            showError(getResources().getString(R.string.error_routing_policy_fee_rate_too_high, 25.0), RefConstants.ERROR_DURATION_MEDIUM);
                            return;
                        }
                    }

                    if (Wallet.getInstance().isConnectedToNode()) {

                        UpdateRoutingPolicyRequest.Builder builder = UpdateRoutingPolicyRequest.newBuilder();

                        if (mChannel != null)
                            builder.setChannel(mChannel);

                        // BaseFee if specified, will be set to 0 on lnd if not set
                        if (mBaseFee.getData() != null)
                            builder.setFeeBase(Long.parseLong(mBaseFee.getData()));

                        // Fee Rate if specified, will be set to 0 on lnd if not set
                        if (mFeeRate.getData() != null)
                            builder.setFeeRate((long) (Double.parseDouble(mFeeRate.getData()) * 10000));

                        // Inbound BaseFee if specified
                        if (mInboundBaseFee.getVisibility() == View.VISIBLE && mInboundBaseFee.getData() != null)
                            builder.setInboundFeeBase(Long.parseLong(mInboundBaseFee.getData()));

                        // Inbound Fee Rate if specified
                        if (mInboundFeeRate.getVisibility() == View.VISIBLE && mInboundFeeRate.getData() != null)
                            builder.setInboundFeeRate((long) (Double.parseDouble(mInboundFeeRate.getData()) * 10000));

                        // Timelock delta if specified, will be set to 0 on lnd if not set
                        if (mTimelock.getData() != null)
                            builder.setDelay(Integer.parseInt(mTimelock.getData()));

                        // MinHTLC if specified
                        if (mMinHTLC.getData() != null)
                            builder.setMinHTLC(Long.parseLong(mMinHTLC.getData()));

                        // MaxHTLC if specified
                        if (mMaxHTLC.getData() != null)
                            builder.setMaxHTLC(Long.parseLong(mMaxHTLC.getData()) * 1000L);

                        mCompositeDisposable.add(BackendManager.api().updateRoutingPolicy(builder.build())
                                .subscribe(errorList -> {
                                    if (errorList.size() > 0) {
                                        if (mChannel == null)
                                            showError(getResources().getString(R.string.error_routing_policy_only_partially_updated, errorList.size()), RefConstants.ERROR_DURATION_LONG);
                                        else
                                            showError(errorList.get(0), RefConstants.ERROR_DURATION_MEDIUM);
                                    } else {
                                        Toast.makeText(UpdateRoutingPolicyActivity.this, R.string.routing_policy_update_success, Toast.LENGTH_SHORT).show();
                                    }
                                }, throwable -> {
                                    BBLog.w(LOG_TAG, "Exception in updating chan policy: " + throwable.getMessage());
                                    showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                                }));
                    }
                } else {
                    // The wallet is not setup. Show setup wallet message.
                    Toast.makeText(UpdateRoutingPolicyActivity.this, R.string.demo_setupNodeFirst, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }

    private void setDefaultValues(RoutingPolicy policy) {
        mBaseFee.setValue(String.valueOf(policy.getFeeBase()));
        mFeeRate.setValue(BigDecimal.valueOf((double) (policy.getFeeRate() / 10000d)).stripTrailingZeros().toPlainString());
        mInboundBaseFee.setValue(String.valueOf(policy.getInboundFeeBase()));
        mInboundFeeRate.setValue(BigDecimal.valueOf((double) (policy.getInboundFeeRate() / 10000d)).stripTrailingZeros().toPlainString());
        mTimelock.setValue(String.valueOf(policy.getDelay()));
        mMinHTLC.setValue(String.valueOf(policy.getMinHTLC()));
        mMaxHTLC.setValue(String.valueOf(policy.getMaxHTLC() / 1000));
    }
}