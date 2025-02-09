package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import app.michaelwuensch.bitbanana.R;

public class BBButton extends FrameLayout {

    private FrameLayout frameLayout;
    private LinearLayout contentLayout;
    private ImageView imageView;
    private TextView textView;
    private ProgressBar progressBar;
    private int contentColor;

    public BBButton(Context context) {
        super(context);
        init(context, null);
    }

    public BBButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BBButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    // Inflate the layout and apply custom attributes.
    private void init(Context context, AttributeSet attrs) {
        // Inflate our custom layout into this FrameLayout.
        LayoutInflater.from(context).inflate(R.layout.view_button, this, true);

        // Get references to the child views.
        frameLayout = findViewById(R.id.custom_button_container);
        contentLayout = findViewById(R.id.content_layout);
        imageView = findViewById(R.id.custom_button_image);
        textView = findViewById(R.id.custom_button_text);
        progressBar = findViewById(R.id.progress_bar);

        // If attributes are provided, read and apply them.
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BBButton);

            // Set text if provided.
            String text = ta.getString(R.styleable.BBButton_bbbutton_text);
            if (text != null && !text.isEmpty()) {
                textView.setText(text);
                textView.setVisibility(VISIBLE);
            } else {
                textView.setVisibility(GONE);
            }

            // Set image if provided.
            Drawable drawable = ta.getDrawable(R.styleable.BBButton_bbbutton_image);
            if (drawable != null) {
                imageView.setImageDrawable(drawable);
                imageView.setVisibility(VISIBLE);
            } else {
                imageView.setVisibility(GONE);
            }

            // Optionally apply content color.
            if (ta.hasValue(R.styleable.BBButton_bbbutton_contentColor)) {
                contentColor = ta.getColor(R.styleable.BBButton_bbbutton_contentColor, 0);
            } else {
                contentColor = ContextCompat.getColor(getContext(), R.color.banana_yellow);
            }

            // Optionally apply text size.
            if (ta.hasValue(R.styleable.BBButton_bbbutton_textSize)) {
                float textSize = ta.getDimensionPixelSize(R.styleable.BBButton_bbbutton_textSize, 0);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            }

            // Optionally make the button bright
            if (ta.hasValue(R.styleable.BBButton_bbbutton_bright)) {
                boolean bright = ta.getBoolean(R.styleable.BBButton_bbbutton_bright, false);
                if (bright) {
                    frameLayout.setBackgroundResource(R.drawable.bg_clickable_item_bright);
                }
            }

            // Optionally make the button transparent
            if (ta.hasValue(R.styleable.BBButton_bbbutton_transparent)) {
                boolean transparent = ta.getBoolean(R.styleable.BBButton_bbbutton_transparent, false);
                if (transparent) {
                    frameLayout.setBackgroundColor(getResources().getColor(R.color.transparent));
                }
            }

            // Optionally apply horizontal content padding
            if (ta.hasValue(R.styleable.BBButton_bbbutton_horizontalContentPadding)) {
                int horizontalContentPadding = ta.getDimensionPixelSize(R.styleable.BBButton_bbbutton_horizontalContentPadding, 0);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) contentLayout.getLayoutParams();
                params.setMargins(horizontalContentPadding, 0, horizontalContentPadding, 0);
            }

            ta.recycle();
        }

        setButtonEnabled(true);
    }

    public void setButtonEnabled(boolean enabled) {
        setEnabled(enabled);
        setClickable(enabled);
        setFocusable(enabled);

        int color;
        if (enabled) {
            color = contentColor;
        } else {
            color = ContextCompat.getColor(getContext(), R.color.gray);
        }

        textView.setTextColor(color);
        imageView.setImageTintList(ColorStateList.valueOf(color));
    }

    /**
     * Show the progress view while hiding the normal content.
     */
    public void showProgress() {
        contentLayout.setVisibility(GONE);
        progressBar.setVisibility(VISIBLE);
        setClickable(false);
    }

    /**
     * Hide the progress view and show the normal content.
     */
    public void hideProgress() {
        contentLayout.setVisibility(VISIBLE);
        progressBar.setVisibility(GONE);
        setClickable(true);
    }

    public void setText(String text) {
        if (text != null && !text.isEmpty())
            textView.setVisibility(VISIBLE);
        textView.setText(text);
    }
}
