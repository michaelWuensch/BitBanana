package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.listViews.channels.ManageChannelsActivity;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class PickChannelsView extends ConstraintLayout {

    private TextView mTvSummary;
    private ImageView mArrowImage;
    private ClickableConstraintLayoutGroup mGroupMain;
    private Group mGroupExpandableContent;
    private BBButton mBtnSelectFirstHop;
    private BBButton mBtnSelectLastHop;
    private OnPickChannelViewButtonListener mOnPickChannelViewButtonListener;
    private ClearFocusListener mClearFocusListener;
    private TextView mFirstHopLabel;
    private TextView mLastHopLabel;
    private View mFirstHopSelectionLayout;
    private BBButton mBtnRemoveFirstHop;
    private TextView mTvFirstHopSelectedChannelName;
    private View mLastHopSelectionLayout;
    private BBButton mBtnRemoveLastHop;
    private TextView mTvLastHopSelectedChannelName;
    private View mLastHopLayout;
    private ImageButton mHelpButton;
    private boolean mLastHopEnabled = true;

    private ActivityResultLauncher<Intent> mActivityResultLauncher;

    private ShortChannelId mSelectedFirstHop;
    private String mSelectedLastHopPubKey;

    public PickChannelsView(Context context) {
        super(context);
        init(context);
    }

    public PickChannelsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PickChannelsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = inflate(getContext(), R.layout.view_pick_channels, this);

        mGroupMain = view.findViewById(R.id.mainGroup);
        mGroupExpandableContent = view.findViewById(R.id.expandableContentGroup);
        mTvSummary = view.findViewById(R.id.summary);
        mArrowImage = view.findViewById(R.id.arrowImage);
        mBtnSelectFirstHop = view.findViewById(R.id.selectButtonFirstHop);
        mBtnSelectLastHop = view.findViewById(R.id.selectButtonLastHop);
        mFirstHopLabel = view.findViewById(R.id.firstHopLabel);
        mLastHopLabel = view.findViewById(R.id.lastHopLabel);
        mFirstHopSelectionLayout = view.findViewById(R.id.firstHopSelectionLayout);
        mBtnRemoveFirstHop = view.findViewById(R.id.removeFirstHopBtn);
        mTvFirstHopSelectedChannelName = view.findViewById(R.id.firstHopSelectedChannelName);
        mLastHopSelectionLayout = view.findViewById(R.id.lastHopSelectionLayout);
        mBtnRemoveLastHop = view.findViewById(R.id.removeLastHopBtn);
        mTvLastHopSelectedChannelName = view.findViewById(R.id.lastHopSelectedChannelName);
        mLastHopLayout = view.findViewById(R.id.lastHopLayout);
        mHelpButton = findViewById(R.id.lastHopHelpButton);

        String selectButtonText = getContext().getString(R.string.select) + " ...";
        mBtnSelectFirstHop.setText(selectButtonText);
        mBtnSelectLastHop.setText(selectButtonText);

        if (!isInEditMode())
            setupView();
    }

    public void setupView() {
        mFirstHopLabel.setText(getContext().getString(R.string.first_hop) + ":");
        mLastHopLabel.setText(getContext().getString(R.string.last_hop) + ":");

        //HelpButton
        mHelpButton.setVisibility(FeatureManager.isHelpButtonsEnabled() ? VISIBLE : GONE);
        mHelpButton.setOnClickListener(view1 -> {
            HelpDialogUtil.showDialog(getContext(), R.string.help_dialog_lastHop);
        });

        updateLastHopVisibility();

        mBtnSelectFirstHop.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                selectFirstHopClicked();
            }
        });

        mFirstHopSelectionLayout.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                selectFirstHopClicked();
            }
        });

        mBtnRemoveFirstHop.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                resetFirstHop();
            }
        });

        mBtnSelectLastHop.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                selectLastHopClicked();
            }
        });

        mLastHopSelectionLayout.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                selectLastHopClicked();
            }
        });

        mBtnRemoveLastHop.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                resetLastHop();
            }
        });

        // Toggle expanding
        mGroupMain.setOnAllClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                hideKeyboard();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toggleExpandState();
                    }
                }, 100);
            }
        });
    }

    public void setLastHopEnabled(boolean enabled) {
        mLastHopEnabled = enabled;
        updateLastHopVisibility();
    }

    private void updateLastHopVisibility() {
        if (mLastHopEnabled && BackendManager.getCurrentBackend().supportsPickLastHop())
            mLastHopLayout.setVisibility(VISIBLE);
        else
            mLastHopLayout.setVisibility(GONE);
    }

    private void selectFirstHopClicked() {
        if (mActivityResultLauncher != null && mOnPickChannelViewButtonListener != null) {
            Intent intent = new Intent(getContext(), ManageChannelsActivity.class);
            intent.putExtra(ManageChannelsActivity.EXTRA_CHANNELS_ACTIVITY_MODE, ManageChannelsActivity.MODE_SELECT);
            intent.putExtra(ManageChannelsActivity.EXTRA_SELECTION_TYPE, ManageChannelsActivity.SELECTION_TYPE_FIRST_HOP);
            intent.putExtra(ManageChannelsActivity.EXTRA_TRANSACTION_AMOUNT, mOnPickChannelViewButtonListener.onSelectChannelClicked());
            mActivityResultLauncher.launch(intent);
        }
    }

    private void selectLastHopClicked() {
        if (mActivityResultLauncher != null && mOnPickChannelViewButtonListener != null) {
            Intent intent = new Intent(getContext(), ManageChannelsActivity.class);
            intent.putExtra(ManageChannelsActivity.EXTRA_CHANNELS_ACTIVITY_MODE, ManageChannelsActivity.MODE_SELECT);
            intent.putExtra(ManageChannelsActivity.EXTRA_SELECTION_TYPE, ManageChannelsActivity.SELECTION_TYPE_LAST_HOP);
            intent.putExtra(ManageChannelsActivity.EXTRA_TRANSACTION_AMOUNT, mOnPickChannelViewButtonListener.onSelectChannelClicked());
            mActivityResultLauncher.launch(intent);
        }
    }

    public void setActivityResultLauncher(ActivityResultLauncher<Intent> activityResultLauncher) {
        mActivityResultLauncher = activityResultLauncher;
    }

    private void toggleExpandState() {
        boolean isExpandedContentVisible = mGroupExpandableContent.getVisibility() == View.VISIBLE;
        setExpandState(!isExpandedContentVisible);
    }

    private void setExpandState(boolean expand) {
        TransitionManager.beginDelayedTransition((ViewGroup) getRootView());
        mArrowImage.setImageResource(expand ? R.drawable.ic_arrow_up_24dp : R.drawable.ic_arrow_down_24dp);
        mGroupExpandableContent.setVisibility(expand ? View.VISIBLE : View.GONE);
        if (mClearFocusListener != null)
            mClearFocusListener.onClearFocus();
    }

    public ShortChannelId getFirstHop() {
        return mSelectedFirstHop;
    }

    public String getLastHopPubkey() {
        return mSelectedLastHopPubKey;
    }

    // Handle the result
    public void handleActivityResult(Intent data) {
        if (data == null) {
            return;
        }

        OpenChannel channel = (OpenChannel) data.getSerializableExtra(ManageChannelsActivity.EXTRA_SELECTED_CHANNEL);

        int hopType = data.getIntExtra(ManageChannelsActivity.EXTRA_SELECTION_TYPE, 0);

        switch (hopType) {
            case ManageChannelsActivity.SELECTION_TYPE_FIRST_HOP:
                mSelectedFirstHop = channel.getShortChannelId();
                mBtnSelectFirstHop.setVisibility(GONE);
                mFirstHopSelectionLayout.setVisibility(VISIBLE);
                mTvFirstHopSelectedChannelName.setText(AliasManager.getInstance().getAlias(channel.getRemotePubKey()));
                break;
            case ManageChannelsActivity.SELECTION_TYPE_LAST_HOP:
                mSelectedLastHopPubKey = channel.getRemotePubKey();
                mBtnSelectLastHop.setVisibility(GONE);
                mLastHopSelectionLayout.setVisibility(VISIBLE);
                mTvLastHopSelectedChannelName.setText(AliasManager.getInstance().getAlias(channel.getRemotePubKey()));
                break;
        }

        mTvSummary.setText(R.string.manually);
    }

    private void resetFirstHop() {
        mSelectedFirstHop = null;
        mBtnSelectFirstHop.setVisibility(VISIBLE);
        mFirstHopSelectionLayout.setVisibility(GONE);

        if (mSelectedLastHopPubKey == null)
            mTvSummary.setText(R.string.automatic);
        if (mOnPickChannelViewButtonListener != null)
            mOnPickChannelViewButtonListener.onResetPickedChannelClicked();
    }

    private void resetLastHop() {
        mSelectedLastHopPubKey = null;
        mBtnSelectLastHop.setVisibility(VISIBLE);
        mLastHopSelectionLayout.setVisibility(GONE);

        if (mSelectedFirstHop == null)
            mTvSummary.setText(R.string.automatic);
        if (mOnPickChannelViewButtonListener != null)
            mOnPickChannelViewButtonListener.onResetPickedChannelClicked();
    }

    public interface OnPickChannelViewButtonListener {
        long onSelectChannelClicked();

        void onResetPickedChannelClicked();
    }

    // Set the listener
    public void setPickChannelsViewButtonListener(OnPickChannelViewButtonListener listener) {
        this.mOnPickChannelViewButtonListener = listener;
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getRootView().getWindowToken(), 0);
    }

    public void setClearFocusListener(ClearFocusListener listener) {
        mClearFocusListener = listener;
    }
}
