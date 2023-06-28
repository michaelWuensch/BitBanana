package app.michaelwuensch.bitbanana.util.inputFilters;

import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

public class InputFilterLowerCase implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

        for (int i = start; i < end; i++) {
            if (Character.isUpperCase(source.charAt(i))) {
                char[] v = new char[end - start];
                TextUtils.getChars(source, start, end, v, 0);
                String s = new String(v).toLowerCase();

                if (source instanceof Spanned) {
                    SpannableString sp = new SpannableString(s);
                    TextUtils.copySpansFrom((Spanned) source, start, end, null, sp, 0);
                    return sp;
                } else {
                    return s;
                }
            }
        }

        return null; // keep original
    }
}
