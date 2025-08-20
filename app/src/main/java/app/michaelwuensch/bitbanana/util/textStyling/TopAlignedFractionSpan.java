package app.michaelwuensch.bitbanana.util.textStyling;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class TopAlignedFractionSpan extends MetricAffectingSpan {
    private final float scale; // e.g., 0.7f

    public TopAlignedFractionSpan(float scale) {
        this.scale = scale;
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        apply(p);
    }

    @Override
    public void updateDrawState(TextPaint p) {
        apply(p);
    }

    private void apply(TextPaint p) {
        // 1) shrink
        p.setTextSize(p.getTextSize() * scale);
        // 2) shift baseline up so the tops align
        // ascent is negative; (1 - scale) * ascent is a negative shift (upwards)
        p.baselineShift += (int) (p.ascent() * (1f - scale));
    }
}
