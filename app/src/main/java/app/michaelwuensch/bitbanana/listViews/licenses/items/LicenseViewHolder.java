package app.michaelwuensch.bitbanana.listViews.licenses.items;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.listViews.licenses.Dependency;

public class LicenseViewHolder extends RecyclerView.ViewHolder {

    View mRootView;
    View mContentView;
    Context mContext;
    private TextView mProjectName;
    private TextView mURL;
    private TextView mDependency;
    private TextView mDevelopers;
    private TextView mLicenseLabel;
    private TextView mLicenseURL;
    private TextView mLicenseURL2;


    public LicenseViewHolder(@NonNull View itemView) {
        super(itemView);

        mRootView = itemView.findViewById(R.id.licenseRootView);
        mContentView = itemView.findViewById(R.id.licenseContent);
        mContext = itemView.getContext();
        mProjectName = itemView.findViewById(R.id.projectName);
        mURL = itemView.findViewById(R.id.projectUrl);
        mDependency = itemView.findViewById(R.id.dependency);
        mDevelopers = itemView.findViewById(R.id.projectDevelopers);
        mLicenseLabel = itemView.findViewById(R.id.licensesLabel);
        mLicenseURL = itemView.findViewById(R.id.licenseUrl);
        mLicenseURL2 = itemView.findViewById(R.id.licenseUrl2);
    }


    public void bindLicenseListItem(final LicenseListItem licenseListItem) {
        Dependency dependency = licenseListItem.getDependency();

        // Set name
        mProjectName.setText(dependency.getProject());

        if (dependency.getUrl() != null && !dependency.getUrl().isEmpty()) {
            mURL.setText(licenseListItem.getDependency().getUrl());
            mURL.setVisibility(View.VISIBLE);
            mURL.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dependency.getUrl()));
                    mContext.startActivity(browserIntent);
                }
            });
        } else {
            mURL.setVisibility(View.GONE);
        }

        if (dependency.getDependency() != null && !dependency.getDependency().isEmpty()) {
            mDependency.setText("Dependency:\n" + dependency.getDependency());
            mDependency.setVisibility(View.VISIBLE);
        } else {
            mDependency.setVisibility(View.GONE);
        }

        if (dependency.getDevelopers() != null && dependency.getDevelopers().length > 0) {
            String developers = "By:\n";
            for (String developer : dependency.getDevelopers()) {
                developers = developers + developer + ", ";
            }
            developers = developers.substring(0, developers.length() - 2);
            mDevelopers.setText(developers);
            mDevelopers.setVisibility(View.VISIBLE);
        } else {
            mDevelopers.setVisibility(View.GONE);
        }

        if (dependency.getLicenses() != null && dependency.getLicenses().length > 0) {
            String licenseLabelString = mContext.getResources().getString(R.string.licenses) + ":";
            mLicenseLabel.setText(licenseLabelString);
            mLicenseLabel.setVisibility(View.VISIBLE);
            mLicenseURL.setVisibility(View.VISIBLE);
            mLicenseURL2.setVisibility(View.GONE);
            mLicenseURL.setText(dependency.getLicenses()[0].getLicense());
            mLicenseURL.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dependency.getLicenses()[0].getLicenseUrl()));
                    mContext.startActivity(browserIntent);
                }
            });
            if (dependency.getLicenses().length >= 2) {
                mLicenseURL2.setVisibility(View.VISIBLE);
                mLicenseURL2.setText(dependency.getLicenses()[1].getLicense());
                mLicenseURL2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dependency.getLicenses()[1].getLicenseUrl()));
                        mContext.startActivity(browserIntent);
                    }
                });
            }
        } else {
            mLicenseLabel.setVisibility(View.GONE);
            mLicenseURL.setVisibility(View.GONE);
            mLicenseURL2.setVisibility(View.GONE);
        }
    }
}
