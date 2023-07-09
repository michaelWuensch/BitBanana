package app.michaelwuensch.bitbanana.qrCodeGen;

import android.graphics.Path;

import androidx.annotation.NonNull;

import com.github.alexzhirkevich.customqrgenerator.style.Neighbors;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapeModifier;

import java.util.List;

public class QRRandomSizedPixelShape implements QrVectorPixelShape {
    private final List<QrVectorShapeModifier> from;

    public QRRandomSizedPixelShape(List<QrVectorShapeModifier> from) {
        this.from = from;
    }


    @NonNull
    @Override
    public Path shape(@NonNull Path path, float size, @NonNull Neighbors neighbors) {
        return from.get((int) (Math.random() * from.size())).shape(path, size, neighbors);
    }
}
