package app.michaelwuensch.bitbanana.customView;


import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import app.michaelwuensch.bitbanana.R;

public class PrefDecoyApp extends Preference {

    private Context mContext;

    public PrefDecoyApp(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public PrefDecoyApp(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PrefDecoyApp(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PrefDecoyApp(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context ctx) {
        mContext = ctx;
        setWidgetLayoutResource(R.layout.pref_decoy_app);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false); // disable parent click

        holder.itemView.setPadding(0, 0, 0, 0);

        // Converts dip into its equivalent px
        float dip = 400f;
        Resources r = mContext.getResources();
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );
        holder.itemView.setMinimumHeight((int) px);
    }

}
