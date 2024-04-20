package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.transition.TransitionManager;

import com.google.android.material.tabs.TabLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.util.FeeEstimationUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;

public class OnChainFeeView extends ConstraintLayout implements FeeEstimationUtil.FeeEstimationListener {

    private AmountView mTvSendFeeAmount;
    private ProgressBar mPbCalculateFee;
    private TextView mTvSendFeeSpeed;
    private TabLayout mTabLayoutSendFeeSpeed;
    private ImageView mFeeArrowUnitImage;
    private ImageButton mModeSwitch;
    private SeekBar mSlider;
    private ViewSwitcher mModeContentSwitcher;
    private ClickableConstraintLayoutGroup mGroupSendFeeAmount;
    private Group mGroupSendFeeDuration;
    private FeeTierChangedListener mFeeTierChangedListener;
    private OnChainFeeView.OnChainFeeTier mOnChainFeeTier;
    private long mTransactionSizeVByte;
    private boolean mManualMode;

    public OnChainFeeView(Context context) {
        super(context);
        init();
    }

    public OnChainFeeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OnChainFeeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_onchain_fee, this);

        mTvSendFeeAmount = view.findViewById(R.id.sendFeeOnChainAmount);
        mPbCalculateFee = view.findViewById(R.id.sendFeeOnChainProgressBar);
        mTvSendFeeSpeed = view.findViewById(R.id.sendFeeSpeed);
        mTabLayoutSendFeeSpeed = view.findViewById(R.id.feeSpeedTabLayout);
        mModeSwitch = view.findViewById(R.id.modeSwitch);
        mSlider = view.findViewById(R.id.slider);
        mModeContentSwitcher = view.findViewById(R.id.modeContentSwitcher);

        mGroupSendFeeAmount = view.findViewById(R.id.sendFeeOnChainAmountGroup);
        mFeeArrowUnitImage = view.findViewById(R.id.feeArrowUnitImage);

        mGroupSendFeeDuration = view.findViewById(R.id.feeDurationGroup);
        mModeSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mManualMode = !mManualMode;
                mModeSwitch.setImageResource(mManualMode ? R.drawable.baseline_speed_24 : R.drawable.baseline_tune_24);
                if (mManualMode)
                    mModeContentSwitcher.showNext();
                else {
                    mModeContentSwitcher.showPrevious();
                    setFeeTier(OnChainFeeTier.parseFromString(PrefsUtil.getOnChainFeeTier()));
                    FeeEstimationUtil.getInstance().getFeeEstimates();
                }
            }
        });

        mSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (mManualMode) {
                    setFeeDetails(0, i);
                    if (mTransactionSizeVByte != 0)
                        mTvSendFeeAmount.setAmountMsat((mTransactionSizeVByte * i * 1000L));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Toggle tier settings view on amount click
        mGroupSendFeeAmount.setOnAllClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                boolean isFeeDurationVisible = mGroupSendFeeDuration.getVisibility() == View.VISIBLE;
                toggleFeeTierView(isFeeDurationVisible);
            }
        });

        // Listen for tier change by user
        mTabLayoutSendFeeSpeed.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() != null) {
                    if (tab.getText().equals(getResources().getString(OnChainFeeTier.SLOW.getTitle()))) {
                        setFeeTier(OnChainFeeTier.SLOW);
                    } else if (tab.getText().equals(getResources().getString(OnChainFeeTier.MEDIUM.getTitle()))) {
                        setFeeTier(OnChainFeeTier.MEDIUM);
                    } else if (tab.getText().equals(getResources().getString(OnChainFeeTier.FAST.getTitle()))) {
                        setFeeTier(OnChainFeeTier.FAST);
                    }
                    FeeEstimationUtil.getInstance().getFeeEstimates();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    // This stuff has to be outside of init(), otherwise the preview does not work in Android Studio
    public void initialSetup() {
        // Set tier from shared preferences
        mSlider.setMax(getSliderMax());
        setFeeTier(OnChainFeeTier.parseFromString(PrefsUtil.getOnChainFeeTier()));
        mTabLayoutSendFeeSpeed.getTabAt(mOnChainFeeTier.ordinal()).select();

        // Init fee estimation update, so we don't work with old data
        FeeEstimationUtil.getInstance().registerFeeEstimationListener(this);
        FeeEstimationUtil.getInstance().getFeeEstimates();
    }

    public OnChainFeeTier getFeeTier() {
        return mOnChainFeeTier;
    }

    /**
     * Show progress bar while calculating fee
     */
    public void onCalculating() {
        mTvSendFeeAmount.overrideWithText(null);
        mPbCalculateFee.setVisibility(View.VISIBLE);
        mTvSendFeeAmount.setVisibility(View.INVISIBLE);
    }

    /**
     * Set current fee tier and notify listeners
     */
    private void setFeeTier(OnChainFeeTier feeTier) {
        mOnChainFeeTier = feeTier;
        setFeeDetails(feeTier.getConfirmationBlockTarget(), feeRateFromFeeTier());
        mSlider.setProgress((int) feeRateFromFeeTier());
        updateAbsoluteFee();

        // Notify listener about changed tier
        if (mFeeTierChangedListener != null) {
            mFeeTierChangedListener.onFeeTierChanged(feeTier);
        }

        // Update choice to shared preferences
        PrefsUtil.editPrefs().putString(PrefsUtil.ON_CHAIN_FEE_TIER, feeTier.name()).apply();
    }

    public void setFeeTierChangedListener(FeeTierChangedListener feeTierChangedListener) {
        mFeeTierChangedListener = feeTierChangedListener;
    }

    public void onSizeCalculatedSuccess(long vByte) {
        mTransactionSizeVByte = vByte;
        updateAbsoluteFee();
    }

    public void onSizeCalculationFailure() {
        mTransactionSizeVByte = 0;
        updateAbsoluteFee();
    }

    private void updateAbsoluteFee() {
        if (BackendManager.getCurrentBackend().supportsAbsoluteOnChainFeeEstimation()) {
            if (mTransactionSizeVByte == 0) {
                mTvSendFeeAmount.overrideWithText(R.string.fee_not_available);
                mTvSendFeeAmount.setVisibility(View.VISIBLE);
                mPbCalculateFee.setVisibility(View.GONE);
            } else {
                mTvSendFeeAmount.setAmountMsat(mTransactionSizeVByte * mSlider.getProgress() * 1000);
                mTvSendFeeAmount.setVisibility(View.VISIBLE);
                mPbCalculateFee.setVisibility(View.GONE);
            }
        } else {
            mTvSendFeeAmount.setVisibility(GONE);
            mPbCalculateFee.setVisibility(View.GONE);
        }
    }

    /**
     * Show or hide tabs to choose fee tier
     */
    private void toggleFeeTierView(boolean hide) {
        TransitionManager.beginDelayedTransition((ViewGroup) getRootView());
        mFeeArrowUnitImage.setImageResource(hide ? R.drawable.ic_arrow_down_24dp : R.drawable.ic_arrow_up_24dp);
        mGroupSendFeeDuration.setVisibility(hide ? View.GONE : View.VISIBLE);
    }

    /**
     * Show estimated time of settlement
     */
    private void setFeeDetails(int blockTarget, long satPerVByte) {
        String details = "";
        String separator = "";
        if (blockTarget != 0) {
            details = details + "~" + TimeFormatUtil.formattedBlockDuration((long) blockTarget, getContext());
            separator = ", ";
        }
        if (satPerVByte != 0)
            details = details + separator + satPerVByte + " sat/vB";
        mTvSendFeeSpeed.setText(details);
    }

    private long feeRateFromFeeTier() {
        switch (mOnChainFeeTier) {
            case FAST:
                return PrefsUtil.getFeeEstimate_NextBlock();
            case MEDIUM:
                return PrefsUtil.getFeeEstimate_Hour();
            default:
                return PrefsUtil.getFeeEstimate_Day();
        }
    }

    @Override
    public void onFeeEstimationUpdated() {
        if (!mManualMode) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mSlider.setMax(getSliderMax());
                    setFeeTier(OnChainFeeTier.parseFromString(PrefsUtil.getOnChainFeeTier()));
                }
            });
        }
    }

    @Override
    public void onFeeEstimationUpdateFailed(int error, int duration) {

    }

    public long getSatPerVByteFee() {
        return mSlider.getProgress();
    }

    public boolean isLowerThanMinimum() {
        return getSatPerVByteFee() < PrefsUtil.getFeeEstimate_Minimum();
    }

    private int getSliderMax() {
        int nextBlock = PrefsUtil.getFeeEstimate_NextBlock();
        return Math.max(nextBlock + 20, nextBlock + (int) (0.2 * nextBlock));
    }

    public enum OnChainFeeTier {
        FAST,
        MEDIUM,
        SLOW;

        public static OnChainFeeTier parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return FAST;
            }
        }

        public int getTitle() {
            switch (this) {
                case FAST:
                    return R.string.fee_tier_fast_title;
                case MEDIUM:
                    return R.string.fee_tier_medium_title;
                case SLOW:
                    return R.string.fee_tier_slow_title;
                default:
                    return R.string.fee_tier_fast_title;
            }
        }

        public int getDescription() {
            switch (this) {
                case FAST:
                    return R.string.fee_tier_fast_description;
                case MEDIUM:
                    return R.string.fee_tier_medium_description;
                case SLOW:
                    return R.string.fee_tier_slow_description;
                default:
                    return R.string.fee_tier_fast_description;
            }
        }

        /**
         * In the future a user should be able to set
         * those values from the settings.
         */
        public int getConfirmationBlockTarget() {
            switch (this) {
                case FAST:
                    return 1;
                case MEDIUM:
                    return 6;
                case SLOW:
                    return 144;
                default:
                    return 1;
            }
        }
    }

    public interface FeeTierChangedListener {
        void onFeeTierChanged(OnChainFeeTier onChainFeeTier);
    }
}
