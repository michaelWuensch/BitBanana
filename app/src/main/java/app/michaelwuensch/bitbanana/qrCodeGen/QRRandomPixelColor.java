package app.michaelwuensch.bitbanana.qrCodeGen;

import android.graphics.Paint;

import androidx.annotation.NonNull;

import com.github.alexzhirkevich.customqrgenerator.style.Neighbors;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrPaintMode;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor;


public class QRRandomPixelColor implements QrVectorColor {

    private QrPaintMode mode = QrPaintMode.Separate;
    private final int mBaseColor;
    private final int mHighlightColor;
    private final float mHighlightProbability;
    private final int mDataLength;
    private final int mMaxHighlightCount;
    private int mNumberOfHighlights;

    public QRRandomPixelColor(String data, int baseColor, int highlightColor, float highlightProbability, int maxHighlightCount) {
        mDataLength = data.length();
        mBaseColor = baseColor;
        mHighlightColor = highlightColor;
        if (mDataLength < 40) {
            mHighlightProbability = highlightProbability;
        } else {
            mHighlightProbability = highlightProbability / 2;
        }

        mMaxHighlightCount = maxHighlightCount;
    }

    @Override
    public void paint(Paint paint, float width, float height, @NonNull Neighbors neighbors) {
        paint.setColor(getColor());
    }

    private int getColor() {
        // no highlight pixels for dense codes
        if (mDataLength > 100)
            return mBaseColor;
        // enforce hard limit for highlight pixels
        if (mMaxHighlightCount > 0 && mNumberOfHighlights >= mMaxHighlightCount)
            return mBaseColor;

        double rand = Math.random();
        if (rand > 1 - mHighlightProbability) {
            mNumberOfHighlights++;
            return mHighlightColor;
        }
        return mBaseColor;
    }

    @Override
    public QrPaintMode getMode() {
        return mode;
    }
}


