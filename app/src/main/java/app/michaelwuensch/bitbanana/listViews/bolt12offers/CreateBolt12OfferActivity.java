package app.michaelwuensch.bitbanana.listViews.bolt12offers;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.SwitchCompat;

import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.models.CreateBolt12OfferRequest;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class CreateBolt12OfferActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = CreateBolt12OfferActivity.class.getSimpleName();

    private BBInputFieldView mLabel;
    private BBInputFieldView mDescription;
    private SwitchCompat mSingleUse;
    private BBButton mBtnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bolt12_offer);

        mLabel = findViewById(R.id.offerInputLabel);
        mDescription = findViewById(R.id.offerInputDescription);
        mSingleUse = findViewById(R.id.singleUseSwitch);
        mBtnCreate = findViewById(R.id.createButton);

        mLabel.setDescriptionDetail("(" + getString(R.string.optional) + ")");

        mBtnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateBolt12OfferRequest request = CreateBolt12OfferRequest.newBuilder()
                        .setAmount(0)
                        .setDescription(mDescription.getData())
                        .setInternalLabel(mLabel.getData())
                        .setSingleUse(mSingleUse.isChecked())
                        .build();
                new CompositeDisposable().add(BackendManager.api().createBolt12Offer(request)
                        .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                        .subscribe(response -> {
                                    finish();
                                }
                                , throwable -> {
                                    BBLog.w(LOG_TAG, "Creating bolt12 offer failed: " + throwable.getMessage());
                                    showError(throwable.getMessage(), RefConstants.ERROR_DURATION_LONG);
                                }));
            }
        });
    }
}
