package app.michaelwuensch.bitbanana.listViews.utxos.itemDetails;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.customView.AmountView;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.labels.LabelActivity;
import app.michaelwuensch.bitbanana.labels.Labels;
import app.michaelwuensch.bitbanana.labels.LabelsManager;
import app.michaelwuensch.bitbanana.models.LeaseUTXORequest;
import app.michaelwuensch.bitbanana.models.ReleaseUTXORequest;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.BlockExplorer;
import app.michaelwuensch.bitbanana.util.ClipBoardUtil;
import app.michaelwuensch.bitbanana.util.FeatureManager;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.TimeFormatUtil;
import app.michaelwuensch.bitbanana.util.UserGuardian;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;

public class UTXODetailBSDFragment extends BaseBSDFragment implements Wallet_TransactionHistory.UtxoSubscriptionListener, LabelsManager.LabelChangedListener {

    public static final String LOG_TAG = UTXODetailBSDFragment.class.getSimpleName();
    public static final String ARGS_UTXO = "UTXO";

    private BSDScrollableMainView mBSDScrollableMainView;
    private Utxo mUTXO;
    private TextView mAmountLabel;
    private AmountView mAmount;
    private TextView mTransactionIDLabel;
    private TextView mTransactionID;
    private ImageView mTransactionIDCopyButton;
    private TextView mAddressLabel;
    private TextView mAddress;
    private TextView mConfirmationsLabel;
    private TextView mConfirmations;
    private TextView mLeasedTimeoutLabel;
    private TextView mLeasedTimeout;
    private TextView mLabelLabel;
    private TextView mLabel;
    private ImageView mAddressCopyButton;
    private BBButton mLockUnlockButton;
    private BBButton mLabelButton;
    private String mLabelString;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_utxo_detail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mAmountLabel = view.findViewById(R.id.amountLabel);
        mAmount = view.findViewById(R.id.amount);
        mTransactionIDLabel = view.findViewById(R.id.transactionIDLabel);
        mTransactionID = view.findViewById(R.id.transactionID);
        mTransactionIDCopyButton = view.findViewById(R.id.txIDCopyIcon);
        mAddressLabel = view.findViewById(R.id.addressLabel);
        mAddress = view.findViewById(R.id.address);
        mAddressCopyButton = view.findViewById(R.id.addressCopyIcon);
        mConfirmationsLabel = view.findViewById(R.id.confirmationsLabel);
        mConfirmations = view.findViewById(R.id.confirmations);
        mLeasedTimeoutLabel = view.findViewById(R.id.leasedTimeoutLabel);
        mLeasedTimeout = view.findViewById(R.id.leasedTimeout);
        mLockUnlockButton = view.findViewById(R.id.lockUnlockButton);
        mLabelButton = view.findViewById(R.id.labelButton);
        mLabelLabel = view.findViewById(R.id.labelLabel);
        mLabel = view.findViewById(R.id.label);

        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            mUTXO = (Utxo) getArguments().getSerializable(ARGS_UTXO);
            bindUTXO();
        }

        mLockUnlockButton.setVisibility(BackendManager.getCurrentBackend().supportsManuallyLeasingUTXOs() ? View.VISIBLE : View.GONE);
        mLockUnlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUTXO.isLeased()) {
                    if (!mUTXO.getLease().getId().toLowerCase().equals(RefConstants.BITBANANA_UTXO_LEASE_ID.toLowerCase())) {
                        new UserGuardian(getContext(), new UserGuardian.OnGuardianConfirmedListener() {
                            @Override
                            public void onConfirmed() {
                                releaseUTXO();
                            }

                            @Override
                            public void onCancelled() {

                            }
                        }).securityReleaseUTXO();
                    } else {
                        releaseUTXO();
                    }
                } else {
                    leaseUTXO();
                }
            }
        });

        mLabelButton.setVisibility(FeatureManager.isLabelsEnabled() ? View.VISIBLE : View.GONE);
        mLabelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent labelIntent = new Intent(getContext(), LabelActivity.class);
                labelIntent.putExtra(LabelActivity.EXTRA_LABEL_ID, mUTXO.getOutpoint().toString());
                labelIntent.putExtra(LabelActivity.EXTRA_LABEL_TYPE, Labels.LabelType.UTXO);
                if (mLabelString != null)
                    labelIntent.putExtra(LabelActivity.EXTRA_LABEL, mLabelString);
                startActivity(labelIntent);
            }
        });

        return view;
    }


    private void bindUTXO() {

        mBSDScrollableMainView.setTitle("UTXO");

        String amountLabel = getString(R.string.amount) + ":";
        mAmountLabel.setText(amountLabel);
        String transactionIDLabel = getString(R.string.transactionID) + ":";
        mTransactionIDLabel.setText(transactionIDLabel);
        String addressLabel = getString(R.string.address) + ":";
        mAddressLabel.setText(addressLabel);
        String confirmationsLabel = getString(R.string.confirmations) + ":";
        mConfirmationsLabel.setText(confirmationsLabel);
        String leaseTimeoutLabel = getString(R.string.locked_until) + ":";
        mLeasedTimeoutLabel.setText(leaseTimeoutLabel);
        String labelLabel = getString(R.string.label) + ":";
        mLabelLabel.setText(labelLabel);

        mAmount.setAmountMsat(mUTXO.getAmount());

        if (mUTXO.hasAddress()) {
            mAddress.setText(mUTXO.getAddress());
            mAddressLabel.setVisibility(View.VISIBLE);
            mAddress.setVisibility(View.VISIBLE);
            mAddressCopyButton.setVisibility(View.VISIBLE);
        } else {
            mAddressLabel.setVisibility(View.GONE);
            mAddress.setVisibility(View.GONE);
            mAddressCopyButton.setVisibility(View.GONE);
        }

        mTransactionID.setText(mUTXO.getOutpoint().getTransactionID());

        mTransactionID.setOnClickListener(view -> new BlockExplorer().showTransaction(mUTXO.getOutpoint().getTransactionID(), getActivity()));
        mAddress.setOnClickListener(view -> new BlockExplorer().showAddress(mUTXO.getAddress(), getActivity()));
        mTransactionIDCopyButton.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "TransactionID", mUTXO.getOutpoint().getTransactionID()));
        mAddressCopyButton.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "Address", mUTXO.getAddress()));

        mConfirmations.setText(String.valueOf(mUTXO.getConfirmations()));

        if (mUTXO.isLeased()) {
            mLockUnlockButton.setText(getResources().getString(R.string.unlock_utxo));
            mConfirmationsLabel.setVisibility(View.GONE);
            mConfirmations.setVisibility(View.GONE);
            if (mUTXO.getLease().hasExpiration()) {
                mLeasedTimeoutLabel.setVisibility(View.VISIBLE);
                mLeasedTimeout.setVisibility(View.VISIBLE);
                mLeasedTimeout.setText(TimeFormatUtil.formatTimeAndDateLong(mUTXO.getLease().getExpiration(), getContext()));
            } else {
                mLeasedTimeoutLabel.setVisibility(View.GONE);
                mLeasedTimeout.setVisibility(View.GONE);
            }
        } else {
            mLockUnlockButton.setText(getResources().getString(R.string.lock_utxo));
            mConfirmationsLabel.setVisibility(View.VISIBLE);
            mConfirmations.setVisibility(View.VISIBLE);
            mLeasedTimeoutLabel.setVisibility(View.GONE);
            mLeasedTimeout.setVisibility(View.GONE);
        }

        if (FeatureManager.isLabelsEnabled()) {
            String label = LabelsManager.getLabel(mUTXO);
            if (label != null) {
                mLabelLabel.setVisibility(View.VISIBLE);
                mLabel.setVisibility(View.VISIBLE);
                mLabel.setText(label);
                mLabelString = label;
                mLabelButton.setText(getString(R.string.label_edit));
            } else {
                mLabelLabel.setVisibility(View.GONE);
                mLabel.setVisibility(View.GONE);
                mLabelString = null;
                mLabelButton.setText(getString(R.string.label_add));
            }
        } else {
            mLabelLabel.setVisibility(View.GONE);
            mLabel.setVisibility(View.GONE);
        }
    }

    private void releaseUTXO() {
        ReleaseUTXORequest request = ReleaseUTXORequest.newBuilder()
                .setId(mUTXO.getLease().getId())
                .setOutpoint(mUTXO.getLease().getOutpoint())
                .build();

        getCompositeDisposable().add(BackendManager.api().releaseUTXO(request)
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(() -> {
                    Wallet_TransactionHistory.getInstance().fetchUTXOs();
                    Wallet_Balance.getInstance().fetchBalances(); // This is needed so send limits will get updated.
                }, throwable -> {
                    BBLog.w(LOG_TAG, "Releasing UTXO failed: " + throwable.getMessage());
                    Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                }));
    }

    private void leaseUTXO() {
        LeaseUTXORequest request = LeaseUTXORequest.newBuilder()
                .setId(RefConstants.BITBANANA_UTXO_LEASE_ID)
                .setOutpoint(mUTXO.getOutpoint())
                .setExpiration(60 * 60 * 24)
                .build();
        getCompositeDisposable().add(BackendManager.api().leaseUTXO(request)
                .timeout(ApiUtil.getBackendTimeout(), TimeUnit.SECONDS)
                .subscribe(() -> {
                    Wallet_TransactionHistory.getInstance().fetchUTXOs();
                    Wallet_Balance.getInstance().fetchBalances(); // This is needed so send limits will get updated.
                }, throwable -> {
                    BBLog.w(LOG_TAG, "Leasing UTXO failed: " + throwable.getMessage());
                    Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                }));
    }

    @Override
    public void onUtxoListUpdated() {
        for (Utxo utxo : Wallet_TransactionHistory.getInstance().getUTXOList()) {
            if (utxo.getOutpoint().toString().equals(mUTXO.getOutpoint().toString())) {
                mUTXO = utxo;
                bindUTXO();
                break;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Wallet_TransactionHistory.getInstance().registerUtxoSubscriptionListener(this);
        LabelsManager.getInstance().registerLabelChangedListener(this);
    }

    @Override
    public void onDestroy() {
        Wallet_TransactionHistory.getInstance().unregisterUtxoSubscriptionListener(this);
        LabelsManager.getInstance().unregisterLabelChangedListener(this);
        super.onDestroy();
    }

    @Override
    public void onLabelChanged() {
        bindUTXO();
    }
}
