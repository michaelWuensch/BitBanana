package app.michaelwuensch.bitbanana.listViews.licenses.items;

import androidx.annotation.Nullable;

import app.michaelwuensch.bitbanana.listViews.licenses.Dependency;

public class LicenseListItem implements Comparable<LicenseListItem> {

    private Dependency mDependency;

    public int getType() {
        return 0;
    }

    public Dependency getDependency() {
        return mDependency;
    }

    public LicenseListItem(Dependency dependency) {
        mDependency = dependency;
    }

    @Override
    public int compareTo(LicenseListItem licenseListItem) {
        LicenseListItem other = licenseListItem;

        return mDependency.getProject().toLowerCase().compareTo(other.mDependency.getProject().toLowerCase());
    }

    public boolean equalsWithSameContent(@Nullable Object obj) {
        if (!equals(obj)) {
            return false;
        }

        LicenseListItem that = (LicenseListItem) obj;

        return mDependency.toString().equals(that.getDependency().toString());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        LicenseListItem that = (LicenseListItem) obj;

        boolean equalName = mDependency.getProject().toLowerCase().equals(that.mDependency.getProject().toLowerCase());
        boolean equalDependency = false;
        if (mDependency.getDependency() == null || that.getDependency() == null) {
            equalDependency = mDependency.getDependency() == null && that.getDependency() == null;
            return equalName && equalDependency;
        } else
            return mDependency.getDependency().equals(that.getDependency().getDependency());
    }

    @Override
    public int hashCode() {
        if (mDependency.getDependency() != null)
            return mDependency.getDependency().hashCode();
        else
            return mDependency.getProject().hashCode();
    }
}
