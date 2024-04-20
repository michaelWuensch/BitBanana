package app.michaelwuensch.bitbanana.listViews.channels;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.listViews.channels.itemDetails.ChannelDetailBSDFragment;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.RoutingPolicy;
import app.michaelwuensch.bitbanana.models.Channels.UpdateRoutingPolicyRequest;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class UpdateRoutingPolicyActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = UpdateRoutingPolicyActivity.class.getSimpleName();
    public static final String EXTRA_ROUTING_POLICY = "extraRoutingPolicy";

    private BBInputFieldView mBaseFee;
    private BBInputFieldView mFeeRate;
    private BBInputFieldView mTimelock;
    private BBInputFieldView mMinHTLC;
    private BBInputFieldView mMaxHTLC;
    private TextView mAllChannelsWarning;
    private Button mBtnSubmit;
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
        mTimelock = findViewById(R.id.timelockDelta);
        mMinHTLC = findViewById(R.id.htlcMin);
        mMaxHTLC = findViewById(R.id.htlcMax);
        mAllChannelsWarning = findViewById(R.id.allChannelsWarning);
        mBtnSubmit = findViewById(R.id.submitButton);

        // setup input types
        mBaseFee.setInputType(InputType.TYPE_CLASS_NUMBER);
        mFeeRate.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mTimelock.setInputType(InputType.TYPE_CLASS_NUMBER);
        mMinHTLC.setInputType(InputType.TYPE_CLASS_NUMBER);
        mMaxHTLC.setInputType(InputType.TYPE_CLASS_NUMBER);

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
                        if (Double.valueOf(mFeeRate.getData()) > 25.0) {
                            showError(getResources().getString(R.string.error_routing_policy_fee_rate_too_high, 25.0), RefConstants.ERROR_DURATION_MEDIUM);
                            return;
                        }
                    }

                    if (LndConnection.getInstance().getLightningService() != null) {

                        UpdateRoutingPolicyRequest.Builder builder = UpdateRoutingPolicyRequest.newBuilder();

                        if (mChannel != null)
                            builder.setChannel(mChannel);

                        // BaseFee if specified, will be set to 0 on lnd if not set
                        if (mBaseFee.getData() != null)
                            builder.setFeeBase(Integer.valueOf(mBaseFee.getData()));

                        // Fee Rate if specified, will be set to 0 on lnd if not set
                        if (mFeeRate.getData() != null)
                            builder.setFeeRate((long) (Double.valueOf(mFeeRate.getData()) * 10000));

                        // Timelock delta if specified, will be set to 0 on lnd if not set
                        if (mTimelock.getData() != null)
                            builder.setDelay(Integer.valueOf(mTimelock.getData()));

                        // MinHTLC if specified
                        if (mMinHTLC.getData() != null)
                            builder.setMinHTLC(Integer.valueOf(mMinHTLC.getData()));

                        // MaxHTLC if specified
                        if (mMaxHTLC.getData() != null)
                            builder.setMaxHTLC(Integer.valueOf(mMaxHTLC.getData()) * 1000);

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
        mFeeRate.setValue(String.valueOf(policy.getFeeRate() / 10000d));
        mTimelock.setValue(String.valueOf(policy.getDelay()));
        mMinHTLC.setValue(String.valueOf(policy.getMinHTLC()));
        mMaxHTLC.setValue(String.valueOf(policy.getMaxHTLC() / 1000));
    }
}