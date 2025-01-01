package app.michaelwuensch.bitbanana.listViews.bolt12offers.items;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.listViews.bolt12offers.Bolt12OfferSelectListener;
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;

public class Bolt12OfferItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = Bolt12OfferItemViewHolder.class.getSimpleName();

    private View mRootView;
    private TextView mTvLabel;
    private TextView mTvDescription;
    private TextView mTvActive;
    private TextView mTvUsed;
    private ImageButton mIvQrButton;
    private Bolt12OfferSelectListener mBolt12OfferSelectListener;


    public Bolt12OfferItemViewHolder(View v) {
        super(v);

        mRootView = v.findViewById(R.id.bolt12OfferRootView);
        mTvLabel = v.findViewById(R.id.offerLabel);
        mTvDescription = v.findViewById(R.id.offerDescription);
        mTvActive = v.findViewById(R.id.offerActive);
        mTvUsed = v.findViewById(R.id.offerUsed);
        mIvQrButton = v.findViewById(R.id.qrButton);
    }

    public void bindBolt12OfferListItem(Bolt12OfferListItem bolt12OfferListItem) {
        Bolt12Offer offer = bolt12OfferListItem.getBolt12Offer();

        // Set on click listener
        setOnRootViewClickListener(bolt12OfferListItem);
        setOnQrButtonClickListener(bolt12OfferListItem);

        if (offer.getLabel().isEmpty()) {
            mTvLabel.setVisibility(View.GONE);
        } else {
            mTvLabel.setVisibility(View.VISIBLE);
            mTvLabel.setText(offer.getLabel());
        }

        if (offer.getDecodedBolt12().getDescription() == null || offer.getDecodedBolt12().getDescription().isEmpty()) {
            mTvDescription.setVisibility(View.GONE);
        } else {
            mTvDescription.setVisibility(View.VISIBLE);
            mTvDescription.setText(offer.getDecodedBolt12().getDescription());
        }

        if (offer.getIsActive()) {
            mTvActive.setText(R.string.active);
            mTvActive.setTextColor(App.getAppContext().getResources().getColor(R.color.green));
        } else {
            mTvActive.setText(R.string.inactive);
            mTvActive.setTextColor(App.getAppContext().getResources().getColor(R.color.red));
        }

        if (offer.getWasAlreadyUsed()) {
            mTvUsed.setText(R.string.used);
        } else {
            mTvUsed.setText(R.string.unused);
        }
    }

    public void addOnBolt12OfferSelectListener(Bolt12OfferSelectListener bolt12OfferSelectListener) {
        mBolt12OfferSelectListener = bolt12OfferSelectListener;
    }

    void setOnRootViewClickListener(@NonNull Bolt12OfferListItem item) {
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mBolt12OfferSelectListener != null) {
                    mBolt12OfferSelectListener.onOfferSelect(item.getBolt12Offer());
                }
            }
        });
    }

    void setOnQrButtonClickListener(@NonNull Bolt12OfferListItem item) {
        mIvQrButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mBolt12OfferSelectListener != null) {
                    mBolt12OfferSelectListener.onQrCodeSelect(item.getBolt12Offer());
                }
            }
        });
    }
}
