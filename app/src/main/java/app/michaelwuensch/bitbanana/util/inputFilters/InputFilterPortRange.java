package app.michaelwuensch.bitbanana.util.inputFilters;

import android.text.InputFilter;
import android.text.Spanned;

public class InputFilterPortRange implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        StringBuilder sb = new StringBuilder();

        // Append new characters being added to the existing characters
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            sb.append(c);
        }

        // Create the full resulting text
        String resultingText = dest.subSequence(0, dstart) + sb.toString() + dest.subSequence(dend, dest.length());

        // Check if the resulting text is within the port range
        try {
            int portNumber = Integer.parseInt(resultingText);
            if (portNumber < 1 || portNumber > 65535) {
                return "";  // Reject input outside the valid port range
            }
        } catch (NumberFormatException e) {
            // If parsing fails, check if it's because the field is currently empty or partially complete
            if (!resultingText.isEmpty()) {
                return "";  // Only reject input if it's not empty
            }
        }

        // If no issues, return null to accept the original input
        return null;
    }
}
