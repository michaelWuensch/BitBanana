package app.michaelwuensch.bitbanana.lightning;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LNAddress implements Serializable {
    private String mUsername;
    private String mDomain;
    private final boolean mIsValid;

    public LNAddress(String address) {
        mIsValid = validateFormat(address);
        if (isValid()) {
            String[] parts = address.split("@");
            mUsername = parts[0];
            mDomain = parts[1];
        }
    }

    public String getUsername() {
        return mUsername;
    }

    public String getDomain() {
        return mDomain;
    }

    public boolean isTor() {
        return mDomain.endsWith(".onion");
    }

    public boolean isValid() {
        return mIsValid;
    }

    @NonNull
    public String toString() {
        return mUsername + "@" + mDomain;
    }

    private static boolean validateFormat(String address) {
        /* Simplified regex checking the following:
            - the string has no white spaces
            - username is a lowercase alphanumeric (including "." and "_") string of at least one character
            - username is followed by exactly one "@"
            - domain name has at leas one "."
            - domain name uses only alphanumeric character (including "-")
        */
        String regexPattern = "^[a-z0-9_.]+@[a-zA-Z0-9-.]+\\.[a-zA-Z0-9-]+$";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(address);

        return matcher.matches();
    }
}
