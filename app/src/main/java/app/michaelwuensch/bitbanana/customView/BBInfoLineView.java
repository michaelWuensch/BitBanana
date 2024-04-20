package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;

public class BBInfoLineView extends LinearLayout {

    private static final String LOG_TAG = BBInfoLineView.class.getSimpleName();

    private TextView mTvLabel;
    private TextView mTvData;


    public BBInfoLineView(Context context) {
        super(context);
        init(context, null);
    }

    public BBInfoLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBInfoLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.view_info_line, this);

        mTvLabel = view.findViewById(R.id.label);
        mTvData = view.findViewById(R.id.data);

        // Apply attributes from XML
        if (attrs != null) {
            // Obtain the custom attribute value from the XML attributes
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BBInfoLineView);
            String attrLabel = a.getString(R.styleable.BBInfoLineView_infoLine_label);
            String attrData = a.getString(R.styleable.BBInfoLineView_infoLine_data);
            boolean attrEllipsizeEnd = a.getBoolean(R.styleable.BBInfoLineView_infoLine_ellipsizeEnd, false);
            boolean attrEllipsizeMiddle = a.getBoolean(R.styleable.BBInfoLineView_infoLine_ellipsizeMiddle, false);
            int attrTextSize = a.getDimensionPixelSize(R.styleable.BBInfoLineView_infoLine_textSize, 0);
            int attrDataWidth = a.getDimensionPixelSize(R.styleable.BBInfoLineView_infoLine_dataWidth, 0);

            if (attrLabel != null)
                setLabel(attrLabel);
            if (attrData != null)
                setData(attrData);
            if (attrEllipsizeEnd)
                mTvData.setEllipsize(TextUtils.TruncateAt.END);
            if (attrEllipsizeMiddle)
                mTvData.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            if (attrDataWidth != 0) {
                LinearLayout.LayoutParams textParam = new LinearLayout.LayoutParams(attrDataWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
                mTvData.setLayoutParams(textParam);
            }

            // Don't forget to recycle the TypedArray
            a.recycle();
        }
    }

    public void setLabel(String label) {
        mTvLabel.setText(label + ":");
    }

    public void setData(String data) {
        mTvData.setText(data);
    }

}
