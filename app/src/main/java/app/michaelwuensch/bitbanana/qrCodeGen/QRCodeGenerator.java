package app.michaelwuensch.bitbanana.qrCodeGen;

import static com.github.alexzhirkevich.customqrgenerator.style.QrColorKt.Color;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.alexzhirkevich.customqrgenerator.QrData;
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel;
import com.github.alexzhirkevich.customqrgenerator.style.BitmapScale;
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawableKt;
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBackground;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.baseClasses.App;

public class QRCodeGenerator {

    // drawables had bad performance in animations. Bitmaps see to work fine.
    public static Bitmap bitmapFromText(String text, int size) {
        return drawableToBitmap(drawableFromText(text), size);
    }

    public static Drawable drawableFromText(String text) {
        QrData qrData = new QrData.Text(text);
        return QrCodeDrawableKt.QrCodeDrawable(qrData, getQrVectorOptions(), StandardCharsets.UTF_8);
    }

    private static QrVectorOptions getQrVectorOptions() {
        // ToDo: If we go for fancier QR-Codes, we might have to expose a setting to chose QR Codes styles for compatibility reasons. For now we go with QR-Code close to original.
        return getCompatibilityQrVectorOptions();
    }

    private static QrVectorOptions getDesignQrVectorOptions() {
        QrVectorOptions options = getDefaultOptionsBuilder()
                .setShapes(
                        new QrVectorShapes(
                                new RandomSizedQRPixelShape(Arrays.asList(
                                        new QrVectorPixelShape.Circle(0.65f),
                                        new QrVectorPixelShape.Circle(0.75f),
                                        new QrVectorPixelShape.Circle(0.85f)
                                )),
                                new QrVectorPixelShape.RoundCorners(.5f),
                                new QrVectorBallShape.RoundCorners(.15f, true, true, true, true),
                                new QrVectorFrameShape.RoundCorners(.15f, 1f, true, true, true, true),
                                true
                        )
                )
                .setColors(
                        new QrVectorColors(
                                new QrVectorColor.Solid(ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue)),
                                new QrVectorColor.Solid(Color(0xffffffff)),
                                new QrVectorColor.Solid(ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue)),
                                new QrVectorColor.Solid(ContextCompat.getColor(App.getAppContext(), R.color.deep_sea_blue))
                        )
                )
                .build();


        return options;
    }

    private static QrVectorOptions getCompatibilityQrVectorOptions() {
        QrVectorOptions options = getDefaultOptionsBuilder()
                .setShapes(
                        new QrVectorShapes(
                                new QrVectorPixelShape.RoundCorners(.35f),
                                new QrVectorPixelShape.RoundCorners(.35f),
                                new QrVectorBallShape.RoundCorners(.15f, true, true, true, true),
                                new QrVectorFrameShape.RoundCorners(.15f, 1f, true, true, true, true),
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
