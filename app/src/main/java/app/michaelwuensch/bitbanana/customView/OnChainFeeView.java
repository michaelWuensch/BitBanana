package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.transition.TransitionManager;

import com.google.android.material.tabs.TabLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;

public class OnChainFeeView extends ConstraintLayout {

    private AmountView mTvSendFeeAmount;
    private ProgressBar mPbCalculateFee;
    private TextView mTvSendFeeSpeed;
    private TabLayout mTabLayoutSendFeeSpeed;
    private TextView mTvSendFeeDuration;
    private ImageView mFeeArrowUnitImage;
    private ClickableConstraintLayoutGroup mGroupSendFeeAmount;
    private Group mGroupSendFeeDuration;
    private FeeTierChangedListener mFeeTierChangedListener;
    private OnChainFeeView.OnChainFeeTier mOnChainFeeTier;

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
        mTvSendFeeDuration = view.findViewById(R.id.feeDurationText);
        mGroupSendFeeAmount = view.findViewById(R.id.sendFeeOnChainAmountGroup);
        mFeeArrowUnitImage = view.findViewById(R.id.feeArrowUnitImage);

        mGroupSendFeeDuration = view.findViewById(R.id.feeDurationGroup);

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
        setFeeTier(OnChainFeeTier.parseFromString(PrefsUtil.getOnChainFeeTier()));
        mTabLayoutSendFeeSpeed.getTabAt(mOnChainFeeTier.ordinal()).select();

        // Set initial block target time
        setBlockTargetTime(mOnChainFeeTier.getConfirmationBlockTarget());
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
        mTvSendFeeSpeed.setText(feeTier.getDescription());
        setBlockTargetTime(feeTier.getConfirmationBlockTarget());

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

    public void onFeeSuccess(long sats) {
        mTvSendFeeAmount.setAmountMsat(sats * 1000);
        mTvSendFeeAmount.setVisibility(View.VISIBLE);
        mPbCalculateFee.setVisibility(View.GONE);
    }

    public void onFeeFailure() {
        mTvSendFeeAmount.overrideWithText(R.string.fee_not_available);
        mTvSendFeeAmount.setVisibility(View.VISIBLE);
        mPbCalculateFee.setVisibility(View.GONE);
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
    private void setBlockTargetTime(int blockTarget) {
        String estimatedTime = TimeFormatUtil.formattedBlockDuration((long) blockTarget, getContext());
        mTvSendFeeDuration.setText(getContext().getString(R.string.fee_estimated_duration, estimatedTime));
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
                    return Integer.parseInt(PrefsUtil.getPrefs().getString(PrefsUtil.FEE_PRESET_FAST, PrefsUtil.DEFAULT_FEE_PRESET_VALUE_FAST));
                case MEDIUM:
                    return Integer.parseInt(PrefsUtil.getPrefs().getString(PrefsUtil.FEE_PRESET_MEDIUM, PrefsUtil.DEFAULT_FEE_PRESET_VALUE_MEDIUM));
                case SLOW:
                    return Integer.parseInt(PrefsUtil.getPrefs().getString(PrefsUtil.FEE_PRESET_SLOW, PrefsUtil.DEFAULT_FEE_PRESET_VALUE_SLOW));
                default:
                    return Integer.parseInt(PrefsUtil.getPrefs().getString(PrefsUtil.FEE_PRESET_FAST, PrefsUtil.DEFAULT_FEE_PRESET_VALUE_FAST));
            }
        }
    }

    public interface FeeTierChangedListener {
        void onFeeTierChanged(OnChainFeeTier onChainFeeTier);
    }
}
