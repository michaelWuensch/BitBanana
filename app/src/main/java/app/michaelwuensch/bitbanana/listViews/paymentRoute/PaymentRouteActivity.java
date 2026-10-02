package app.michaelwuensch.bitbanana.listViews.paymentRoute;

import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.BaseAppCompatActivity;
import app.michaelwuensch.bitbanana.customView.CustomViewPager;
import app.michaelwuensch.bitbanana.listViews.paymentRoute.items.HopListItem;
import app.michaelwuensch.bitbanana.models.LnHop;
import app.michaelwuensch.bitbanana.models.LnPayment;


public class PaymentRouteActivity extends BaseAppCompatActivity {

    private static final String LOG_TAG = PaymentRouteActivity.class.getSimpleName();

    public static final String EXTRA_LNPAYMENT = "LnPayment";

    private CustomViewPager mViewPager;
    private PaymentRoutePagerAdapter mPagerAdapter;
    private LnPayment mLnPayment;
    private PaymentRouteSummaryView mRouteSummaryView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_route);

        // Receive data from last activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(EXTRA_LNPAYMENT)) {
                mLnPayment = (LnPayment) extras.getSerializable(EXTRA_LNPAYMENT);
            }
        }

        mRouteSummaryView = findViewById(R.id.paymentPathSummary);

        // Setup view pager
        mViewPager = findViewById(R.id.paymentRoute_viewpager);
        mPagerAdapter = new PaymentRoutePagerAdapter(getSupportFragmentManager(), mLnPayment);
        mViewPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabDots);
        if (mLnPayment.getRoutes().size() == 1)
            tabLayout.setVisibility(View.GONE);
        tabLayout.setupWithViewPager(mViewPager, true);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateSummary(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // This prevents the swipeRefreshLayout to not interfere on swiping
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateRouteView();
            }
        }, 10);
    }

    private void updateRouteView() {
        List<List<HopListItem>> routeItems = new ArrayList<>();
        for (int i = 0; i < mLnPayment.getRoutes().size(); i++) {
            List<HopListItem> listItems = new ArrayList<>();
            // Add first fake hop
            LnHop fakeHop = LnHop.newBuilder()
                    .setIdInRoute(0)
                    .setAmount(mLnPayment.getRoutes().get(i).getHops().get(0).getAmount() + mLnPayment.getRoutes().get(i).getHops().get(0).getFee())
                    .build();
            HopListItem fakeItem = new HopListItem(fakeHop);
            listItems.add(fakeItem);
            for (LnHop hop : mLnPayment.getRoutes().get(i).getHops()) {
                HopListItem listItem = new HopListItem(hop);
                listItems.add(listItem);
            }
            routeItems.add(listItems);
        }
        mPagerAdapter.setRouteItems(routeItems);
        updateSummary(mViewPager.getCurrentItem());
    }

    private void updateSummary(int position) {
        mRouteSummaryView.updateSummary(position + 1, mLnPayment.getRoutes().size(), mLnPayment.getRoutes().get(position).getAmount(), mLnPayment.getRoutes().get(position).getFee());
    }


    public class PaymentRoutePagerAdapter extends FragmentPagerAdapter {
        private final int mRouteCount;
        private final SparseArray<PaymentRouteListFragment> mFragments = new SparseArray<>();
        private List<List<HopListItem>> mRouteItems;

        public PaymentRoutePagerAdapter(FragmentManager fm, LnPayment lnPayment) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mRouteCount = lnPayment.getRoutes().size();
        }

        @Override
        public Fragment getItem(int pos) {
            return new PaymentRouteListFragment();
        }

        // getItem() is not called for fragments the FragmentManager restored after the activity got recreated.
        // Keep references to the fragments that are actually displayed, no matter if they are new or restored.
        // Fragments are only created when their page gets close to being displayed, therefore we also pass them their items here.
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int pos) {
            PaymentRouteListFragment fragment = (PaymentRouteListFragment) super.instantiateItem(container, pos);
            mFragments.put(pos, fragment);
            if (mRouteItems != null)
                fragment.replaceAllItems(mRouteItems.get(pos));
            return fragment;
        }

        @Override
        public int getCount() {
            return mRouteCount;
        }

        public void setRouteItems(List<List<HopListItem>> routeItems) {
            mRouteItems = routeItems;
            for (int i = 0; i < mFragments.size(); i++) {
                mFragments.valueAt(i).replaceAllItems(routeItems.get(mFragments.keyAt(i)));
            }
        }
    }
}
