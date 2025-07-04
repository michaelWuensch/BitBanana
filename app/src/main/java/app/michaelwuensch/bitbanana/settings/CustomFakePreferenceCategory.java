package app.michaelwuensch.bitbanana.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.FeatureManager;

public class CustomFakePreferenceCategory extends Preference {

    private OnButtonClickListener mOnButtonClickListener;

    public CustomFakePreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.fake_preference_category);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView titleView = (TextView) holder.findViewById(R.id.title);
        if (titleView != null) {
            titleView.setText(getTitle());
        }

        ImageButton button = (ImageButton) holder.findViewById(R.id.button);
        try { // to enable preview
            setupInfoButton(button);
        } catch (Exception ignored) {
        }
    }

    private void setupInfoButton(ImageButton button) {
        button.setVisibility(FeatureManager.isHelpButtonsEnabled() ? View.VISIBLE : View.GONE);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnButtonClickListener != null)
                        mOnButtonClickListener.onButtonClick();
                }
            });
        }
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.mOnButtonClickListener = listener;
    }

    public interface OnButtonClickListener {
        void onButtonClick();
    }
}