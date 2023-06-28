package app.michaelwuensch.bitbanana.util.inputFilters;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputFilterHex implements InputFilter {
    private boolean mOutputCaps;

    public InputFilterHex(boolean outputCaps) {
        mOutputCaps = outputCaps;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

        Pattern pattern = Pattern.compile("^\\p{XDigit}+$");

        StringBuilder sb = new StringBuilder();

        for (int i = start; i < end; i++) {

            if (!Character.isLetterOrDigit(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) {
                // is not(Letter or Digit or space);
                return "";
            }

            // Only allow characters "0123456789ABCDEF";
            Matcher matcher = pattern.matcher(String.valueOf(source.charAt(i)));
            if (!matcher.matches()) {
                return "";
            }

            // Add character to String builder
            sb.append(source.charAt(i));

            /*counterForSpace++;
            if(counterForSpace>1){
                //Restart counter
                counterForSpace = 0;
                //Add space!
                sb.append(" ");
            }*/

        }
        if (mOutputCaps)
            return sb.toString().toUpperCase();
        else
            return sb.toString().toLowerCase();
    }
}
