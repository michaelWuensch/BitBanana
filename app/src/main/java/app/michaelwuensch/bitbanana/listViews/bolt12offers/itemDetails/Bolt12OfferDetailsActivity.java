package app.michaelwuensch.bitbanana.listViews.bolt12offers.itemDetails;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.BBButton;
import app.michaelwuensch.bitbanana.customView.BBExpandablePropertyView;
import app.michaelwuensch.bitbanana.listViews.bolt12offers.Bolt12QRActivity;
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.RefConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Bolt12OfferDetailsActivity extends BaseAppCompatActivity {

    static final String LOG_TAG = Bolt12OfferDetailsActivity.class.getSimpleName();
    private TextView mTvLabel;
    private BBExpandablePropertyView mDetailActive;
    private BBExpandablePropertyView mDetailUsed;
    private BBExpandablePropertyView mDetailType;
    private BBExpandablePropertyView mDetailDescription;
    private BBExpandablePropertyView mDetailID;
    private BBExpandablePropertyView mDetailBolt12;
    private BBButton mBtnSwitchEnabledState;

    private Bolt12Offer mBolt12Offer;

    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolt12_offer_details);

        mTvLabel = findViewById(R.id.offerLabel);
        mDetailActive = findViewById(R.id.active);
        mDetailUsed = findViewById(R.id.used);
        mDetailType = findViewById(R.id.type);
        mDetailDescription = findViewById(R.id.description);
        mDetailID = findViewById(R.id.id);
        mDetailBolt12 = findViewById(R.id.bolt12);
        mBtnSwitchEnabledState = findViewById(R.id.switchEnabledSateButton);

        mCompositeDisposable = new CompositeDisposable();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mBolt12Offer = (Bolt12Offer) extras.getSerializable("bolt12offer");
        }

        mBtnSwitchEnabledState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchEnabledState();
            }
        });

        bindBolt12Offer(mBolt12Offer);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.scanButton) {
            Intent intentQDetails = new Intent(this, Bolt12QRActivity.class);
            intentQDetails.putExtra("bolt12offer", mBolt12Offer);
            startActivity(intentQDetails);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindBolt12Offer(Bolt12Offer offer) {

        // Label
        if (offer.getLabel() == null || offer.getLabel().isEmpty())
            mTvLabel.setVisibility(View.GONE);
        else
            mTvLabel.setText(offer.getLabel());

        // Active
        if (offer.getIsActive()) {
            mDetailActive.setValue(getResources().getString(R.string.yes));
        } else {
            mDetailActive.setValue(getResources().getString(R.string.no));
        }

        // Used
        if (offer.getWasAlreadyUsed()) {
            mDetailUsed.setValue(getResources().getString(R.string.yes));
        } else {
            mDetailUsed.setValue(getResources().getString(R.string.no));
        }

        // type
        if (offer.getIsSingleUse()) {
            mDetailType.setValue(getResources().getString(R.string.offer_single_use));
        } else {
            mDetailType.setValue(getResources().getString(R.string.offer_multi_use));
        }

        // description
        if (offer.getDecodedBolt12().getDescription() != null) {
            mDetailDescription.setVisibility(View.VISIBLE);
            mDetailDescription.setValue(offer.getDecodedBolt12().getDescription());
        }

        // id
        mDetailID.setValue(offer.getOfferId());
        mDetailID.getmValueTextView().setEllipsize(TextUtils.TruncateAt.MIDDLE);
        mDetailID.getmValueTextView().setMaxLines(1);

        // bolt 12
        mDetailBolt12.setValue(offer.getDecodedBolt12().getBolt12String());
        mDetailBolt12.getmValueTextView().setEllipsize(TextUtils.TruncateAt.MIDDLE);
        mDetailBolt12.getmValueTextView().setMaxLines(1);

        // button
        if (mBolt12Offer.getIsActive())
            mBtnSwitchEnabledState.setText(getString(R.string.disable));
        else
            mBtnSwitchEnabledState.setText(getString(R.string.enable));
    }

    private void switchEnabledState() {
        if (mBolt12Offer.getIsActive()) {
            mCompositeDisposable.add(BackendManager.api().disableBolt12Offer(mBolt12Offer.getOfferId())
                    .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                    .subscribe(() -> {
                        mDetailActive.setValue(getResources().getString(R.string.no));
                        mBtnSwitchEnabledState.setText(getString(R.string.enable));
                        mBolt12Offer.updateActiveState(false);
                    }, throwable -> {
                        BBLog.w(LOG_TAG, "Disabling offer failed: " + throwable.getMessage());
                        showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                    }));
        } else {
            mCompositeDisposable.add(BackendManager.api().enableBolt12Offer(mBolt12Offer.getOfferId())
                    .timeout(ApiUtil.timeout_long(), TimeUnit.SECONDS)
                    .subscribe(() -> {
                        mDetailActive.setValue(getResources().getString(R.string.yes));
                        mBtnSwitchEnabledState.setText(getString(R.string.disable));
                        mBolt12Offer.updateActiveState(true);
                    }, throwable -> {
                        BBLog.w(LOG_TAG, "Enabling offer failed: " + throwable.getMessage());
                        showError(throwable.getMessage(), RefConstants.ERROR_DURATION_MEDIUM);
                    }));
        }
    }
}