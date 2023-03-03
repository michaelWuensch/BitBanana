package app.michaelwuensch.bitbanana.signVerify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class SignVerifyPagerAdapter extends PagerAdapter {

    private final Context mContext;

    public SignVerifyPagerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View signVerifyView;
        if (position == 0) {
            // sign
            signVerifyView = new SignView(mContext);//inflater.inflate(R.layout.view_sign, container, false);

        } else {
            // verify
            signVerifyView = new VerifyView(mContext);//inflater.inflate(R.layout.view_verify, container, false);
        }

        container.addView(signVerifyView);
        return signVerifyView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}

