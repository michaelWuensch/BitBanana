package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;

public class MainBalanceView extends MotionLayout {
    private static final String LOG_TAG = MainBalanceView.class.getSimpleName();

    private MotionLayout mMotionLayout;
    private TextView mTvPrimaryBalance;
    private TextView mTvPrimaryBalanceUnit;
    private TextView mTvSecondaryBalance;
    private TextView mTvSecondaryBalanceUnit;
    private TextView mTvMode;
    private ConstraintLayout mClBalanceLayout;
    private ImageView mIvLogo;
    private ImageView mIvSwitchButton;
    private ImageView mIvHandleIcon;
    private FrameLayout mFLHandleForClick;
    private Animation mBalanceFadeOutAnimation;
    private Animation mLogoFadeInAnimation;
    private boolean mAnimationAborted;
    private AmountView mAvOnChain;
    private AmountView mAvOnChainPending;
    private AmountView mAvLighting;
    private AmountView mAvLightningPending;
    private View mBalanceDetails;
    private View mVHandleFadeout;
    private View mVSecondaryBalanceVisibility;
    private View mVSwitchButtonVisibility;

    private boolean mIsExpanded;
    private boolean mIsTransitioning;

    public MainBalanceView(Context context) {
        super(context);
        init();
    }

    public MainBalanceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MainBalanceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.main_balance_view, this);

        mMotionLayout = view.findViewById(R.id.balanceViewMotionLayout);
        mClBalanceLayout = view.findViewById(R.id.BalanceLayout);
        mIvLogo = view.findViewById(R.id.logo);
        mIvHandleIcon = view.findViewById(R.id.handleIcon);
        mFLHandleForClick = view.findViewById(R.id.handleForClick);
        mIvSwitchButton = view.findViewById(R.id.switchButtonImage);
        mTvPrimaryBalance = view.findViewById(R.id.BalancePrimary);
        mTvPrimaryBalanceUnit = view.findViewById(R.id.BalancePrimaryUnit);
        mTvSecondaryBalance = view.findViewById(R.id.BalanceSecondary);
        mTvSecondaryBalanceUnit = view.findViewById(R.id.BalanceSecondaryUnit);
        mTvMode = view.findViewById(R.id.mode);
        mBalanceFadeOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.balance_fade_out);
        mLogoFadeInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.logo_fade_in);
        mAvOnChain = view.findViewById(R.id.onChainConfirmed);
        mAvOnChainPending = view.findViewById(R.id.onChainPending);
        mAvLighting = view.findViewById(R.id.lightningConfirmed);
        mAvLightningPending = view.findViewById(R.id.lightningPending);
        mBalanceDetails = view.findViewById(R.id.balanceDetails);
        mVHandleFadeout = view.findViewById(R.id.handleFadeOut);
        mVSecondaryBalanceVisibility = view.findViewById(R.id.secondaryBalanceVisibility);
        mVSwitchButtonVisibility = view.findViewById(R.id.switchButtonVisibility);

        updateBalanceDetailsVisibility();

        /*
        Making it possible to touch drag and click at the same time turned out to be tricky.
        Adding an OnClickListener to the handle stopped drag touch to work.
        Here is how it is done now:
        1. We register an onTouchListener for the whole MotionLayout. The area is obviously much bigger than the handle
           Therefore we added a isInside() function that can check if the motion Event happened inside a specific view.
           We cannot just use the handle to check this, as the handle is animated in ScaleY it will yield incorrect results.
        2. Therefore we add another Handle, which is transparent and at the exact same location and does not get animated in ScaleY.
        3. If the motionAction is UP and it was inside the bounds of that second handle, we trigger the motion scene to animate.
         */
        mMotionLayout.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isInside(mFLHandleForClick, motionEvent)) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        // Handle touch up
                        if (mIsExpanded)
                            mMotionLayout.transitionToStart();
                        else
                            mMotionLayout.transitionToEnd();
                    }
                }
                return false;
            }
        });

        mMotionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int i, int i1) {

            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int i, int i1, float v) {
                mIsTransitioning = true;
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int i) {
                // update isExpanded state
                mIsExpanded = i == R.id.end;
                if (mIsExpanded) {
                    mBalanceFadeOutAnimation.reset();
                    abortAnimation();
                    setVisibilityOfBalanceFadeoutViews(View.VISIBLE);
                    mIvLogo.setVisibility(View.INVISIBLE);
                } else {
                    if (hideMainBalance()) {
                        restartFadeOut();
                    }
                }
                mIsTransitioning = false;
            }


            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int i, boolean b, float v) {

            }
        });


        mBalanceFadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation arg0) {
                setVisibilityOfBalanceFadeoutViews(View.VISIBLE);
                mIvLogo.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                if (!mAnimationAborted) {
                    setVisibilityOfBalanceFadeoutViews(View.INVISIBLE);
                    mIvLogo.setVisibility(View.VISIBLE);
                    mIvLogo.startAnimation(mLogoFadeInAnimation);
                }
            }
        });


        // Action when clicked on the logo
        mIvLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartFadeOut();
            }
        });


        // Swap action when clicked on balance or cancel the fade out in case balance is hidden
        mClBalanceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hideMainBalance() && !mIsExpanded) {
                    restartFadeOut();
                } else {
                    MonetaryUtil.getInstance().switchToNextCurrency();
                }
            }
        });

        // Swap action when clicked swap icon next to balance
        mIvSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MonetaryUtil.getInstance().switchToNextCurrency();

                // also cancel fade out if hideTotalBalance option is active
                if (hideMainBalance() && !mIsExpanded) {
                    restartFadeOut();
                }
            }
        });
    }

    public void updateBalances() {
        updateBalancesInternal(0);
    }

    public void updateBalancesDelayed(int delayInMilliSeconds) {
        updateBalancesInternal(delayInMilliSeconds);
    }

    private void updateBalancesInternal(int delayInMilliSeconds) {
        // Finish transition before updating the balances. If done while transitioning, layout gets unrecoverably broken.
        if (mIsTransitioning) {
            if (mIsExpanded)
                mMotionLayout.jumpToState(R.id.start);
            else
                mMotionLayout.jumpToState(R.id.end);
        }

        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                updateBalanceDetailsVisibility();

                // Adapt unit text size depending on its length
                if (MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit().length() > 2) {
                    mTvPrimaryBalanceUnit.setTextSize(20);
                } else {
                    mTvPrimaryBalanceUnit.setTextSize(32);
                }

                Balances balances;
                if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
                    balances = Wallet_Balance.getInstance().getBalances();
                } else {
                    balances = Wallet_Balance.getInstance().getDemoBalances();
                }

                mTvPrimaryBalance.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayAmountStringFromMSats(balances.total(), false));
                mTvPrimaryBalanceUnit.setText(MonetaryUtil.getInstance().getCurrentCurrencyDisplayUnit());

                if (MonetaryUtil.getInstance().hasMoreThanOneCurrency()) {
                    mVSwitchButtonVisibility.setVisibility(VISIBLE);
                    mVSecondaryBalanceVisibility.setVisibility(VISIBLE);
                    mTvSecondaryBalance.setText(MonetaryUtil.getInstance().getNextCurrencyDisplayAmountStringFromMSats(balances.total(), false));
                    mTvSecondaryBalanceUnit.setText(MonetaryUtil.getInstance().getNextCurrencyDisplayUnit());
                } else {
                    mVSwitchButtonVisibility.setVisibility(GONE);
                    mVSecondaryBalanceVisibility.setVisibility(GONE);
                }

                // Balance details
                mAvOnChain.setAmountMsat(balances.onChainConfirmed());
                mAvOnChainPending.setAmountMsat(balances.onChainUnconfirmed());
                mAvLighting.setAmountMsat(balances.channelBalance());
                mAvLightningPending.setAmountMsat(balances.channelBalancePending());

                BBLog.v(LOG_TAG, "Total balance display updated");
            }
        }, delayInMilliSeconds);
    }

    public void updateNetworkInfo() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            switch (Wallet.getInstance().getNetwork()) {
                case MAINNET:
                    mTvMode.setVisibility(View.GONE);
                    break;
                case TESTNET:
                    mTvMode.setText("TESTNET");
                    mTvMode.setVisibility(View.VISIBLE);
                    break;
                case REGTEST:
                    mTvMode.setText("REGTEST");
                    mTvMode.setVisibility(View.VISIBLE);
            }
        } else {
            // Wallet is not setup
            mTvMode.setVisibility(View.GONE);
        }
    }

    public void hideBalance() {
        setVisibilityOfBalanceFadeoutViews(View.INVISIBLE);
        mIvLogo.setVisibility(View.VISIBLE);
    }

    public void showBalance() {
        setVisibilityOfBalanceFadeoutViews(View.VISIBLE);
        mIvLogo.setVisibility(View.INVISIBLE);
    }

    private boolean isInside(View v, MotionEvent e) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);

        float rawX = e.getRawX();
        float rawY = e.getRawY();

        return (rawX >= location[0] && rawX <= (location[0] + v.getWidth())
                && rawY >= location[1] && rawY <= (location[1] + v.getHeight()));
    }

    private boolean hideMainBalance() {
        return !PrefsUtil.getPrefs().getString(PrefsUtil.BALANCE_HIDE_TYPE, "off").equals("off");

    }

    private void restartFadeOut() {
        mBalanceFadeOutAnimation.reset();
        mClBalanceLayout.startAnimation(mBalanceFadeOutAnimation);
        mIvSwitchButton.startAnimation(mBalanceFadeOutAnimation);
        mVHandleFadeout.startAnimation(mBalanceFadeOutAnimation);
        mAnimationAborted = false;
    }

    private void setVisibilityOfBalanceFadeoutViews(int visibility) {
        mClBalanceLayout.setVisibility(visibility);
        mIvSwitchButton.setVisibility(visibility);
        mVHandleFadeout.setVisibility(visibility);
    }

    private void abortAnimation() {
        mAnimationAborted = true;
        mClBalanceLayout.clearAnimation();
        mIvSwitchButton.clearAnimation();
        mVHandleFadeout.clearAnimation();
    }

    private void updateBalanceDetailsVisibility() {

        int detailsVisibility = FeatureManager.isBalanceDetailsEnabled() ? View.VISIBLE : View.GONE;
        mIvHandleIcon.setVisibility(detailsVisibility);
        mBalanceDetails.setVisibility(detailsVisibility);

        if (mIsExpanded && !FeatureManager.isBalanceDetailsEnabled())
            mMotionLayout.jumpToState(R.id.start);

        mMotionLayout.getDefinedTransitions().forEach(transition -> transition.setEnabled(FeatureManager.isBalanceDetailsEnabled()));
    }
}
