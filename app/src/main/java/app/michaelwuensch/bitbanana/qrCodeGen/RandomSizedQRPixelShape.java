package app.michaelwuensch.bitbanana.qrCodeGen;

import android.graphics.Path;

import com.github.alexzhirkevich.customqrgenerator.style.Neighbors;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapeModifier;

import java.util.List;

public class RandomSizedQRPixelShape implements QrVectorPixelShape {
    private final List<QrVectorShapeModifier> from;

    public RandomSizedQRPixelShape(List<QrVectorShapeModifier> from) {
        this.from = from;
    }

    @Override
    public Path createPath(float size, Neighbors neighbors) {
        return from.get((int) (Math.random() * from.size())).createPath(size, neighbors);
    }
}
