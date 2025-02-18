package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.transition.TransitionManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.utxos.UTXOsActivity;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class UtxoOptionsView extends ConstraintLayout {

    private TextView mTvUtxoSummary;
    private ImageView mArrowImage;
    private ClickableConstraintLayoutGroup mGroupMain;
    private Group mGroupExpandableContent;
    private BBButton mBtnSelect;
    private BBButton mBtnReset;
    private LinearLayout mUtxoContainer;
    private OnUtxoSelectClickListener mOnUtxoSelectClickListener;
    private ClearFocusListener mClearFocusListener;

    private ActivityResultLauncher<Intent> mActivityResultLauncher;

    private List<Outpoint> mSelectedUTXOs;

    public UtxoOptionsView(Context context) {
        super(context);
        init(context);
    }

    public UtxoOptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public UtxoOptionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = inflate(getContext(), R.layout.view_utxo_options, this);

        mGroupMain = view.findViewById(R.id.mainGroup);
        mGroupExpandableContent = view.findViewById(R.id.expandableContentGroup);
        mTvUtxoSummary = view.findViewById(R.id.utxoSummary);
        mArrowImage = view.findViewById(R.id.arrowImage);
        mBtnSelect = view.findViewById(R.id.selectButton);
        mBtnReset = view.findViewById(R.id.resetButton);
        mUtxoContainer = view.findViewById(R.id.utxoContainer);

        String selectButtonText = context.getString(R.string.select) + " ...";
        mBtnSelect.setText(selectButtonText);

        mBtnReset.setButtonEnabled(false);

        mBtnReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                reset();
            }
        });

        mBtnSelect.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mActivityResultLauncher != null && mOnUtxoSelectClickListener != null) {
                    Intent intent = new Intent(getContext(), UTXOsActivity.class);
                    intent.putExtra(UTXOsActivity.EXTRA_UTXO_ACTIVITY_MODE, UTXOsActivity.MODE_SELECT);
                    if (mSelectedUTXOs != null && !mSelectedUTXOs.isEmpty())
                        intent.putExtra(UTXOsActivity.EXTRA_UTXO_PRESELECTED, (Serializable) mSelectedUTXOs);
                    intent.putExtra(UTXOsActivity.EXTRA_TRANSACTION_AMOUNT, mOnUtxoSelectClickListener.onSelectUtxosClicked());
                    mActivityResultLauncher.launch(intent);
                }
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
        if (mSelectedUTXOs == null || mSelectedUTXOs.isEmpty())
            mUtxoContainer.setVisibility(GONE);
        if (mClearFocusListener != null)
            mClearFocusListener.onClearFocus();
    }

    public List<Outpoint> getSelectedUTXOs() {
        return mSelectedUTXOs;
    }

    // Handle the result
    public void handleActivityResult(Intent data) {
        if (data == null) {
            reset();
            return;
        }

        List<Utxo> utxoList = (List<Utxo>) data.getSerializableExtra(UTXOsActivity.EXTRA_UTXO_SELECTED);
        if (utxoList == null || utxoList.isEmpty()) {
            reset();
            return;
        }

        mBtnSelect.setText(getContext().getString(R.string.change) + " ...");

        mSelectedUTXOs = new ArrayList<>();
        for (Utxo utxo : utxoList)
            mSelectedUTXOs.add(utxo.getOutpoint());

        // Add custom views to the container
        mUtxoContainer.removeAllViews();
        for (Utxo utxo : utxoList) {
            BBSelectedUtxo customView = new BBSelectedUtxo(getContext());
            customView.setData(utxo.getAddress(), utxo.getAmount());
            mUtxoContainer.addView(customView);
        }
        mUtxoContainer.setVisibility(VISIBLE);

        mTvUtxoSummary.setText(R.string.manually);
        mBtnReset.setButtonEnabled(true);
    }

    private void reset() {
        if (mSelectedUTXOs != null)
            mSelectedUTXOs.clear();
        mTvUtxoSummary.setText(R.string.automatic);
        mUtxoContainer.removeAllViews();
        mBtnReset.setButtonEnabled(false);
        mBtnSelect.setText(getContext().getString(R.string.select) + " ...");
        mUtxoContainer.setVisibility(GONE);
    }

    public interface OnUtxoSelectClickListener {
        long onSelectUtxosClicked();
    }

    // Set the listener
    public void setUtxoSelectClickListener(OnUtxoSelectClickListener listener) {
        this.mOnUtxoSelectClickListener = listener;
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getRootView().getWindowToken(), 0);
    }

    public void setClearFocusListener(ClearFocusListener listener) {
        mClearFocusListener = listener;
    }
}
