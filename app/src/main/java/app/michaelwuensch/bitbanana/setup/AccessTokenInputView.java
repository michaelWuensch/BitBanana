package app.michaelwuensch.bitbanana.setup;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.tabs.TabLayout;
import com.google.common.io.BaseEncoding;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.customView.BBInputFieldView;
import app.michaelwuensch.bitbanana.util.HexUtil;

public class AccessTokenInputView extends BBInputFieldView {

    private View mAddedTabs;
    private TabLayout mTabLayout;
    private TabLayout.Tab mLastTab;

    private static final String LOG_TAG = AccessTokenInputView.class.getSimpleName();

    public AccessTokenInputView(Context context) {
        super(context);
        init(context, null);
    }

    public AccessTokenInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AccessTokenInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        super.init(context, attrs);
        LinearLayout container = mView.findViewById(R.id.additionalContentContainer);
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        if (mAddedTabs == null) {
            mAddedTabs = layoutInflater.inflate(R.layout.tab_layout_access_token, container);
            mTabLayout = mAddedTabs.findViewById(R.id.accessTokenEncodingTabLayout);
            mLastTab = mTabLayout.getTabAt(mTabLayout.getSelectedTabPosition());

            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    String tabText = String.valueOf(tab.getText());
                    switch (tabText) {
                        case "Hex":
                            setValue(HexUtil.bytesToHex(getDataAsBytes()));
                            break;
                        case "Base64":
                            setValue(convertCurrentDataToBase64());
                            break;
                        case "Base64Url":
                            if (getDataAsBytes() != null)
                                setValue(BaseEncoding.base64Url().encode(getDataAsBytes()));
                            else
                                setValue(null);
                            break;
                    }
                    mLastTab = tab;
                }


                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        }
    }

    @Override
    public String getData() {
        if (getDataAsBytes() != null)
            return HexUtil.bytesToHex(getDataAsBytes());
        else
            return null;
    }

    private byte[] getDataAsBytes() {
        try {
            String base64 = convertCurrentDataToBase64();
            return BaseEncoding.base64().decode(base64);
        } catch (Exception e) {
            return null;
        }
    }

    private String convertCurrentDataToBase64() {
        try {
            byte[] byteData = null;
            String tabText = String.valueOf(mLastTab.getText());
            switch (tabText) {
                case "Hex":
                    if (!HexUtil.isHex(mEtInput.getText().toString()))
                        return null;
                    byteData = HexUtil.hexToBytes(mEtInput.getText().toString());
                    break;
                case "Base64":
                    byteData = BaseEncoding.base64().decode(mEtInput.getText().toString());
                    break;
                case "Base64Url":
                    byteData = BaseEncoding.base64Url().decode(mEtInput.getText().toString());
                    break;
            }
            return BaseEncoding.base64().encode(byteData);
        } catch (Exception e) {
            return null;
        }
    }
}
