package app.michaelwuensch.bitbanana.qrCodeGen;

import static com.github.alexzhirkevich.customqrgenerator.style.ColorUtillsKt.Color;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.alexzhirkevich.customqrgenerator.HighlightingType;
import com.github.alexzhirkevich.customqrgenerator.QrData;
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel;
import com.github.alexzhirkevich.customqrgenerator.QrHighlighting;
import com.github.alexzhirkevich.customqrgenerator.QrHighlightingKt;
import com.github.alexzhirkevich.customqrgenerator.style.BitmapScale;
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawableKt;
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QVectorShapeUtilsKt;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBackground;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapes;

import java.nio.charset.StandardCharsets;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.util.UtilFunctions;

public class QRCodeGenerator {

    // drawables had bad performance in animations. Bitmaps see to work fine.
    public static Bitmap bitmapFromText(String data, int size) {
        return drawableToBitmap(drawableFromText(data), size);
    }

    public static Drawable drawableFromText(String data) {
        if (UtilFunctions.isHex(data)) {
            // If we have a hex string, uppercase it so Alphanumeric encoding is used instead of binary, which produces smaller codes
            data = data.toUpperCase();
        }
        QrData qrData = new QrData.Text(data);
        return QrCodeDrawableKt.QrCodeDrawable(qrData, getQrVectorOptions(data), StandardCharsets.UTF_8);
    }

    private static QrVectorOptions getQrVectorOptions(String data) {
        // The design QR-Code does not look good for high density codes.
        // Therefore, depending on how much data is encoded we fall back to a more standard qr code
        if (data.length() < 140)
            return getDesignQrVectorOptions(data);
        else
            return getCompatibilityQrVectorOptions();
    }

    private static QrVectorOptions getDesignQrVectorOptions(String data) {
        QrVectorOptions.Builder optionsBuilder = getDefaultOptionsBuilder()
                .setShapes(
                        new QrVectorShapes(
                                new QrVectorPixelShape.Rect(.75f),
                                new QrVectorPixelShape.Rect(.75f),
                                new QrVectorBallShape.RoundCorners(.15f, true, true, true, true),
                                new QrVectorFrameShape.RoundCorners(.15f, 1f, true, true, true, true, 7),
                                true
                        )
                )
                .setColors(
                        new QrVectorColors(
                                new QRRandomPixelColor(data, ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue), ContextCompat.getColor(App.getAppContext(), R.color.banana_yellow), 0.02f, 5),
                                new QrVectorColor.Solid(Color(0xffffffff)),
                                new QrVectorColor.Solid(ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue)),
                                new QrVectorColor.Solid(ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue))
                        )
                )
                .setAnchorsHighlighting(new QrHighlighting(
                        new HighlightingType.Styled(),
                        new HighlightingType.Styled(
                                QrHighlightingKt.QrVersionEyeShape(
                                        new QrVectorFrameShape.AsPixelShape(new QrVectorPixelShape.Rect(.75f), 5),
                                        QVectorShapeUtilsKt.asBallShape(new QrVectorPixelShape.Rect(.75f))),
                                new QrVectorColor.Solid(ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue)),
                                new QrVectorPixelShape.Rect(.0f),
                                new QrVectorColor.Solid(Color(0x00ffffff))),
                        new HighlightingType.Styled(new QrVectorPixelShape.Rect(.75f), new QrVectorColor.Solid(Color(0xffffffff)), new QrVectorPixelShape.Rect(.75f), new QrVectorColor.Solid(ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue))),
                        1.0f
                ));


        if (data.length() > 60) {
            optionsBuilder.setErrorCorrectionLevel(QrErrorCorrectionLevel.Low);
        } else {
            optionsBuilder.setErrorCorrectionLevel(QrErrorCorrectionLevel.Medium);
        }

        new HighlightingType.Styled(new QrVectorFrameShape.RoundCorners(.15f, 1f, true, true, true, true, 7), new QrVectorColor.Solid(Color(0xffffffff)), new QrVectorFrameShape.RoundCorners(.15f, 1f, true, true, true, true, 7), new QrVectorColor.Solid(ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue)));
        QrHighlightingKt.QrVersionEyeShape(
                new QrVectorFrameShape.AsPixelShape(
                        new QrVectorPixelShape.Rect(.75f), 5
                ),
                QVectorShapeUtilsKt.asBallShape(new QrVectorPixelShape.Rect(.75f))
        );

        return optionsBuilder.build();
    }

    private static QrVectorOptions getCompatibilityQrVectorOptions() {
        QrVectorOptions options = getDefaultOptionsBuilder()
                .setShapes(
                        new QrVectorShapes(
                                new QrVectorPixelShape.RoundCorners(.0f),
                                new QrVectorPixelShape.RoundCorners(.35f),
                                new QrVectorBallShape.RoundCorners(.15f, true, true, true, true),
                                new QrVectorFrameShape.RoundCorners(.15f, 1f, true, true, true, true, 7),
                                true
                        )
                )
                .build();
        return options;
    }

    private static QrVectorOptions.Builder getDefaultOptionsBuilder() {
        QrVectorOptions.Builder optionsBuilder = new QrVectorOptions.Builder()
                .setPadding(.2f)
                .setBackground(
                        new QrVectorBackground(
                                ContextCompat.getDrawable(App.getAppContext(), R.drawable.bg_box),
                                new BitmapScale() {
                                    @NonNull
                                    @Override
                                    public Bitmap scale(@NonNull Drawable drawable, int i, int i1) {
                                        return Bitmap.createBitmap(i, i1, Bitmap.Config.ARGB_8888);
                                    }
                                },
                                new QrVectorColor.Solid(Color(0xffffffff))
                        )
                )
                .setErrorCorrectionLevel(QrErrorCorrectionLevel.Low);
        return optionsBuilder;
    }

    private static Bitmap drawableToBitmap(Drawable drawable, int size) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
