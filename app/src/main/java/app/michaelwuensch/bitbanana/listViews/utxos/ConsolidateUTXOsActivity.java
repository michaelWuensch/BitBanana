package app.michaelwuensch.bitbanana.listViews.utxos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.OnChainFeeView;
import app.michaelwuensch.bitbanana.customView.UtxoOptionsView;
import app.michaelwuensch.bitbanana.models.SendOnChainPaymentRequest;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.HelpDialogUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ConsolidateUTXOsActivity extends BaseAppCompatActivity implements UtxoOptionsView.OnUtxoViewButtonListener {

    private static final String LOG_TAG = ConsolidateUTXOsActivity.class.getSimpleName();

    private OnChainFeeView mOnChainFeeView;
    private BBButton mConsolidateButton;
    private UtxoOptionsView mUtxoSelectionView;
    private ConstraintLayout mContentLayout;
    private View mResultView;
    private View mInputLayout;
    private BBButton mSuccessOkButton;
    private TextView mSuccessNrOfConsolidatedInputs;
    private int mNrOfConsolidatedInputs;
    private int mFinalNrOfConsolidatedInputs;
    private ActivityResultLauncher<Intent> mActivityResultLauncherSelectUTXOs;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    public long onSelectUtxosClicked() {
        return 0;
    }

    @Override
    public void onResetUtxoViewClicked() {
        updateConsolidateUI();
    }

    @Override
    public void onSelectAllUTXOsToggled(boolean newIsChecked) {
        updateConsolidateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consolidate_utxos);

        mUtxoSelectionView = findViewById(R.id.utxoSelectionView);
        mOnChainFeeView = findViewById(R.id.onChainFeeView);
        mConsolidateButton = findViewById(R.id.consolidateButton);
        mContentLayout = findViewById(R.id.contentLayout);
        mInputLayout = findViewById(R.id.inputLayout);
        mSuccessOkButton = findViewById(R.id.okButton);
        mSuccessNrOfConsolidatedInputs = findViewById(R.id.nrOfConsolidatedInputs);
        mResultView = findViewById(R.id.resultContent);

        mActivityResultLauncherSelectUTXOs = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        // Pass result to the utxo view
                        mUtxoSelectionView.handleActivityResult(data);

                        updateConsolidateUI();
                    }
                });

        mUtxoSelectionView.setActivityResultLauncher(mActivityResultLauncherSelectUTXOs);
        mUtxoSelectionView.setUtxoViewButtonListener(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mUtxoSelectionView.setConsolidationMode(true);
            }
        }, 1);

        mOnChainFeeView.initialSetup();
        mOnChainFeeView.setSendAllFlag(true);
        mOnChainFeeView.toggleFeeTierView(false);

        mConsolidateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new UserGuardian(ConsolidateUTXOsActivity.this, new UserGuardian.OnGuardianConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        mFinalNrOfConsolidatedInputs = mNrOfConsolidatedInputs;
                        mConsolidateButton.showProgress();
                        fetchOnChainAddress();
                    }

                    @Override
                    public void onCancelled() {

                    }
                }).privacyConsolidateUTXOs();
            }
        });

        // Initial state
        mConsolidateButton.setButtonEnabled(false);
        updateConsolidateUI();
    }

    private void updateConsolidateUI() {
        if (mUtxoSelectionView.getIsSelectAllChecked()) {
            int tempNrOfConsolidatedInputs = 0;
            for (Utxo utxo : Wallet_TransactionHistory.getInstance().getUTXOList()) {
                if (utxo.getConfirmations() > 0 && !utxo.isLeased())
                    tempNrOfConsolidatedInputs++;
            }
            mNrOfConsolidatedInputs = tempNrOfConsolidatedInputs;
        } else {
            if (mUtxoSelectionView.getSelectedUTXOs() != null)
                mNrOfConsolidatedInputs = mUtxoSelectionView.getSelectedUTXOs().size();
            else
                mNrOfConsolidatedInputs = 0;
        }
        mConsolidateButton.setButtonEnabled(mNrOfConsolidatedInputs > 1);
        if (mNrOfConsolidatedInputs > 1) {
            mConsolidateButton.setText(getString(R.string.consolidate_utxos_summary, mNrOfConsolidatedInputs));
        } else
            mConsolidateButton.setText(getString(R.string.consolidate));
    }

    private void fetchOnChainAddress() {
        mCompositeDisposable.add(BackendManager.api().getNewOnchainAddress(WalletUtil.getNewOnChainAddressRequest())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    consolidateUTXOs(response);
                }, throwable -> {
                    mConsolidateButton.hideProgress();
                    showError(getString(R.string.consolidation_failed) + ": " + throwable.getMessage(), 5000);
                    BBLog.e(LOG_TAG, "New address request failed: " + throwable.getMessage());
                }));
    }

    private void consolidateUTXOs(String address) {
        SendOnChainPaymentRequest request = SendOnChainPaymentRequest.newBuilder()
                .setAddress(address)
                .setSendAll(true)
                .setSatPerVByte(mOnChainFeeView.getSatPerVByteFee())
                .setUTXOs(mUtxoSelectionView.getSelectedUTXOs())
                .build();

        mCompositeDisposable.add(BackendManager.api().sendOnChainPayment(request)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    showSuccessScreen();
                }, throwable -> {
                    mConsolidateButton.hideProgress();
                    showError(getString(R.string.consolidation_failed) + ": " + throwable.getMessage(), 5000);
                    BBLog.e(LOG_TAG, "Consolidation transaction failed: " + throwable.getMessage());
                }));
    }

    private void showSuccessScreen() {
        TransitionManager.beginDelayedTransition(mContentLayout);
        mSuccessOkButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                finish();
            }
        });
        mSuccessNrOfConsolidatedInputs.setText(String.valueOf(mFinalNrOfConsolidatedInputs));
        mInputLayout.setVisibility(View.GONE);
        mSuccessOkButton.setVisibility(View.VISIBLE);
        mResultView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (FeatureManager.isHelpButtonsEnabled())
            getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.helpButton) {
            HelpDialogUtil.showDialog(ConsolidateUTXOsActivity.this, getString(R.string.help_dialog_utxos_consolidate));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
