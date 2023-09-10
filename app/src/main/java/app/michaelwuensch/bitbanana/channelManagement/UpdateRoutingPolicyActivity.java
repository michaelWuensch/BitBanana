package app.michaelwuensch.bitbanana.channelManagement;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lightningnetwork.lnd.lnrpc.FailedUpdate;
import com.github.lightningnetwork.lnd.lnrpc.PolicyUpdateRequest;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.connection.lndConnection.LndConnection;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class UpdateRoutingPolicyActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = UpdateRoutingPolicyActivity.class.getSimpleName();

    private BBInputFieldView mBaseFee;
    private BBInputFieldView mFeeRate;
    private BBInputFieldView mTimelock;
    private BBInputFieldView mMinHTLC;
    private BBInputFieldView mMaxHTLC;
    private TextView mAllChannelsWarning;
    private Button mBtnSubmit;
    private boolean mGlobal;


    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_channel_fee_setup);

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

        // replace translated descriptions
        mFeeRate.setDescriptionDetail("(" + getResources().getString(R.string.percent) + ")");
        String timelockDetailDescription = "(" + getResources().getQuantityString(R.plurals.blocks, 1000).replace("%d ", "") + ")";
        mTimelock.setDescriptionDetail(timelockDetailDescription);

        // ToDo: set global to false if the settings should only be applied to a single channel
        mGlobal = true;
        if (!mGlobal) {
            mAllChannelsWarning.setVisibility(View.GONE);
        }

        // On submit
        mBtnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validate input
                if (mFeeRate.getData() != null) {
                    if (Double.valueOf(mFeeRate.getData()) > 25.0) {
                        showError(getResources().getString(R.string.error_routing_policy_fee_rate_too_high, 25.0), RefConstants.ERROR_DURATION_MEDIUM);
                        return;
                    }
                }

                if (LndConnection.getInstance().getLightningService() != null) {

                    PolicyUpdateRequest.Builder chanPolicyUpdateRequestBuilder = PolicyUpdateRequest.newBuilder()
                            .setGlobal(mGlobal);

                    // BaseFee if specified, will be set to 0 on lnd if not set
                    if (mBaseFee.getData() != null) {
                        chanPolicyUpdateRequestBuilder
                                .setBaseFeeMsat(Integer.valueOf(mBaseFee.getData()) * 1000);
                    }

                    // Fee Rate if specified, will be set to 0 on lnd if not set
                    if (mFeeRate.getData() != null) {
                        chanPolicyUpdateRequestBuilder
                                .setFeeRatePpm((int) (Double.valueOf(mFeeRate.getData()) * 10000));
                    }

                    // Timelock delta if specified, will be set to 0 on lnd if not set
                    if (mTimelock.getData() != null) {
                        chanPolicyUpdateRequestBuilder
                                .setTimeLockDelta(Integer.valueOf(mTimelock.getData()));
                    }

                    // MinHTLC if specified
                    if (mMinHTLC.getData() != null) {
                        chanPolicyUpdateRequestBuilder
                                .setMinHtlcMsat(Integer.valueOf(mMinHTLC.getData()))
                                .setMinHtlcMsatSpecified(true);
                    }

                    // MaxHTLC if specified
                    if (mMaxHTLC.getData() != null) {
                        chanPolicyUpdateRequestBuilder
                                .setMaxHtlcMsat(Integer.valueOf(mMaxHTLC.getData()) * 1000);
                    }

                    mCompositeDisposable.add(LndConnection.getInstance().getLightningService().updateChannelPolicy(chanPolicyUpdateRequestBuilder.build())
                            .subscribe(policyUpdateResponse -> {
                                if (policyUpdateResponse.getFailedUpdatesCount() > 0) {
                                    for (FailedUpdate failedUpdate : policyUpdateResponse.getFailedUpdatesList()) {
                                        BBLog.w(LOG_TAG, "Exception in updating chan policy for " + failedUpdate.getOutpoint().getTxidStr() + ": " + failedUpdate.getUpdateError());
                                        if (!mGlobal)
                                            showError(failedUpdate.getUpdateError(), RefConstants.ERROR_DURATION_MEDIUM);
                                    }
                                    if (mGlobal) {
                                        showError(getResources().getString(R.string.error_routing_policy_only_partially_updated, policyUpdateResponse.getFailedUpdatesCount()), RefConstants.ERROR_DURATION_LONG);
                                    }
                                } else {
                                    Toast.makeText(UpdateRoutingPolicyActivity.this, R.string.routing_policy_update_success, Toast.LENGTH_SHORT).show();
                                }
                            }, throwable -> {
                                BBLog.w(LOG_TAG, "Exception in updating chan policy: " + throwable.getMessage());
                                showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                            }));
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }
}