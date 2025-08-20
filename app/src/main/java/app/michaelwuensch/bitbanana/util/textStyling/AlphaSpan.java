package app.michaelwuensch.bitbanana.util.textStyling;

import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;

public class AlphaSpan extends CharacterStyle implements UpdateAppearance {
    private final float alphaScale; // 0..1, e.g. 0.6f for 60% opacity

    public AlphaSpan(float alphaScale) {
        this.alphaScale = alphaScale;
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setAlpha(Math.round(tp.getAlpha() * alphaScale));
    }
}
