package app.michaelwuensch.bitbanana.util.inputFilters;

import android.text.InputFilter;
import android.text.Spanned;

public class InputFilterNoWhitespaces implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

        StringBuilder sb = new StringBuilder();

        for (int i = start; i < end; i++) {

            if (Character.isSpaceChar(source.charAt(i))) {
                // is is a space, we don't want that;
                return "";
            }

            // Add character to String builder
            sb.append(source.charAt(i));
        }
        return sb.toString();
    }
}
